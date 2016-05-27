//
// Created by fxpa72 on 4/11/2016.
//

#include <assert.h>
#include <jni.h>
#include <string.h>
#include <android/log.h>
#include <pthread.h>
#include <unistd.h>
#include <stdlib.h>
#define LOG_TAG "buffer queue"

// for native audio
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>

// engine interfaces
static SLObjectItf engineObject = NULL;
static SLEngineItf engineEngine;

// output mix interfaces
static SLObjectItf outputMixObject = NULL;

// buffer queue player interfaces
static SLObjectItf bqPlayerObject = NULL;
static SLPlayItf bqPlayerPlay;
static SLAndroidSimpleBufferQueueItf bqPlayerBufferQueue;

static SLVolumeItf bqPlayerVolume;
static jint   bqPlayerBufSize = 0;

// recorder interface
static SLAndroidSimpleBufferQueueItf recorderBufferQueue;

// URI player interfaces
static SLObjectItf uriPlayerObject = NULL;
static SLPlayItf uriPlayerPlay;
static SLPrefetchStatusItf playerPrefetch;
static SLPlaybackRateItf playRate;

// buffer data
#define BUFFERFRAMES 441*100
#define SAMPLINGRATE SL_SAMPLINGRATE_44_1

short outputBuffer[BUFFERFRAMES];
short inputBuffer[BUFFERFRAMES];
short fxBuffer[BUFFERFRAMES];
short *delayBuffer;

// thread locks
void* inlock;
void* outlock;

// effect data
jboolean BitCrushOn = JNI_FALSE;
jboolean ChorusOn = JNI_FALSE;
int BitDepth = 16;

typedef struct threadLock_{
    pthread_mutex_t m;
    pthread_cond_t c;
    unsigned char s;
} threadLock;

void* createThreadLock(void) {

    threadLock *pp;
    pp = (threadLock*) malloc(sizeof(threadLock));

    if (pp == NULL) return NULL;

    memset(pp, 0, sizeof(threadLock));

    if (pthread_mutex_init(&(pp->m), (pthread_mutexattr_t*) NULL) != 0) {
        free((void*) pp);
        return NULL;
    }

    if (pthread_cond_init(&(pp->c), (pthread_condattr_t*) NULL ) != 0) {
        pthread_mutex_destroy(&(pp->m));
        free((void*) pp);
        return NULL;
    }

    pp->s = (unsigned char) 1;
    return pp;
}

int waitThreadLock(void *lock) {

    threadLock *pp;
    pp = (threadLock*) lock;

    pthread_mutex_lock(&(pp->m));

    while (!pp->s) {
        pthread_cond_wait(&(pp->c), &(pp->m));
    }

    pp->s = (unsigned char) 0;
    pthread_mutex_unlock(&(pp->m));
}

void notifyThreadLock(void *lock) {

    threadLock *pp;
    pp = (threadLock*) lock;

    pthread_mutex_lock(&(pp->m));

    pp->s = (unsigned char) 1;
    pthread_cond_signal(&(pp->c));
    pthread_mutex_unlock(&(pp->m));
}

void destroyThreadLock(void *lock) {

    threadLock *pp;
    pp = (threadLock*) lock;

    if (pp == NULL) return;

    notifyThreadLock(pp);

    pthread_cond_destroy(&(pp->c));
    pthread_mutex_destroy(&(pp->m));
    free(pp);
}

short staticDelay(short const in, double const delay, int const idx) {
    int i = idx, j;

    delayBuffer[i] = in;
    j = i - (int)(delay * BUFFERFRAMES);

    if (j < 0) j += BUFFERFRAMES;

    return delayBuffer[j];
}

short delayChorus(short const in, int const idx) {
    float dly0 = 0.012f, dly1 = 0.017f, dly2 = 0.025f;

    if (idx == 0) {
        dly0 = (rand() % 30) / 1000.0f;
        dly1 = (rand() % 30) / 1000.0f;
        dly2 = (rand() % 30) / 1000.0f;
    }

    short const out = (short)(0.6f * in + 0.2 * staticDelay(in, dly0, idx) +
                              0.2f * staticDelay(in, dly1, idx) + 0.2f * staticDelay(in, dly2, idx));
    return out;
}

void prefetchEventCallback( SLPrefetchStatusItf caller, void *context, SLuint32 event) {
    SLpermille level = 0;
    (*caller)->GetFillLevel(caller, &level);
    SLuint32 status;

    (*caller)->GetPrefetchStatus(caller, &status);


    if ((event & (SL_PREFETCHEVENT_STATUSCHANGE|SL_PREFETCHEVENT_FILLLEVELCHANGE))
        && (level == 0) && (status == SL_PREFETCHSTATUS_UNDERFLOW)) {
        __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG,"PrefetchEventCallback: Error while prefetching data, exiting");
    }
    if (event & SL_PREFETCHEVENT_FILLLEVELCHANGE) {
        __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG,"PrefetchEventCallback: Buffer fill level is = %d", level);
    }
    if (event & SL_PREFETCHEVENT_STATUSCHANGE) {
        __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG,"PrefetchEventCallback: Prefetch Status is = %lu", status);
    }

}

// this callback handler is called every time a buffer finishes playing
void bqPlayerCallback(SLAndroidSimpleBufferQueueItf bq, void *context)
{
    assert(bq == bqPlayerBufferQueue);
    assert(NULL == context);

    notifyThreadLock(inlock);
    notifyThreadLock(outlock);
}

// this callback handler is called every time a buffer finishes recording
void bqRecorderCallback(SLAndroidSimpleBufferQueueItf bq, void *context)
{
    assert(bq == recorderBufferQueue);
    assert(NULL == context);

    waitThreadLock(inlock);

    unsigned int k;

    for (k = 0; k < BUFFERFRAMES; k++) {

        if (BitCrushOn) {
            fxBuffer[k] = (inputBuffer[k]) >> (16-BitDepth);
            outputBuffer[k] = (fxBuffer[k]) << (16-BitDepth);
        }
        else if (ChorusOn) {
            outputBuffer[k] = delayChorus(inputBuffer[k], k);
        }
        else {
            outputBuffer[k] = inputBuffer[k];
        }
    }

    SLresult result;

    result = (*recorderBufferQueue)->Enqueue(recorderBufferQueue, inputBuffer, BUFFERFRAMES);
    assert(SL_RESULT_SUCCESS == result);

    waitThreadLock(outlock);

    result = (*bqPlayerBufferQueue)->Enqueue(bqPlayerBufferQueue, outputBuffer, BUFFERFRAMES);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;
}

// create the engine and output mix objects
void Java_kharico_graphiceq_AudioEffects_createEngine(JNIEnv* env, jclass clazz)
{
    inlock = createThreadLock();
    outlock = createThreadLock();

    // seed rand generator for chorus effect delays
    srand(time(0));

    SLresult result;

    // create engine
    result = slCreateEngine(&engineObject, 0, NULL, 0, NULL, NULL);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    // realize the engine
    result = (*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    // get the engine interface, which is needed in order to create other objects
    result = (*engineObject)->GetInterface(engineObject, SL_IID_ENGINE, &engineEngine);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    // create output mix, with environmental reverb specified as a non-required interface
    const SLInterfaceID ids[1] = {SL_IID_VOLUME};
    const SLboolean req[1] = {SL_BOOLEAN_FALSE};
    result = (*engineEngine)->CreateOutputMix(engineEngine, &outputMixObject, 0, NULL, NULL);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    // realize the output mix
    result = (*outputMixObject)->Realize(outputMixObject, SL_BOOLEAN_FALSE);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;
}

// create buffer queue audio player

void Java_kharico_graphiceq_AudioEffects_createBufferQueueAudioPlayer(JNIEnv* env,
                                                                       jclass clazz, jint sampleRate, jint bufSize)
{
    SLresult result;
    if (sampleRate >= 0 && bufSize >= 0 ) {
        bqPlayerBufSize = bufSize;
    }

    // configure audio source
    SLDataLocator_AndroidSimpleBufferQueue loc_bufq = {SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, 2};
    SLDataFormat_PCM format_pcm = {SL_DATAFORMAT_PCM, 1, SAMPLINGRATE,
                                   SL_PCMSAMPLEFORMAT_FIXED_16, SL_PCMSAMPLEFORMAT_FIXED_16,
                                   SL_SPEAKER_FRONT_CENTER, SL_BYTEORDER_LITTLEENDIAN};

    format_pcm.samplesPerSec = SL_SAMPLINGRATE_44_1;
    SLDataSource audioSrc = {&loc_bufq, &format_pcm};

    // configure audio sink
    SLDataLocator_OutputMix loc_outmix = {SL_DATALOCATOR_OUTPUTMIX, outputMixObject};
    SLDataSink audioSnk = {&loc_outmix, NULL};

    // create audio player
    const SLInterfaceID ids[3] = {SL_IID_ANDROIDSIMPLEBUFFERQUEUE, SL_IID_VOLUME, SL_IID_PLAYBACKRATE };
    const SLboolean req[3] = {SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE };
    result = (*engineEngine)->CreateAudioPlayer(engineEngine, &bqPlayerObject, &audioSrc, &audioSnk,
                                                3, ids, req);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    // realize the player
    result = (*bqPlayerObject)->Realize(bqPlayerObject, SL_BOOLEAN_FALSE);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    // get the play interface
    result = (*bqPlayerObject)->GetInterface(bqPlayerObject, SL_IID_PLAY, &bqPlayerPlay);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    // get the buffer queue interface
    result = (*bqPlayerObject)->GetInterface(bqPlayerObject, SL_IID_BUFFERQUEUE,
                                             &bqPlayerBufferQueue);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    // register callback on the buffer queue
    result = (*bqPlayerBufferQueue)->RegisterCallback(bqPlayerBufferQueue, bqPlayerCallback, NULL);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    // get playback rate interface
    result = (*bqPlayerObject)->GetInterface(bqPlayerObject,
                                             SL_IID_PLAYBACKRATE, &playRate);
    assert(SL_RESULT_SUCCESS == result);

    // set playback rate
    SLpermille  pRate;
    SLpermille  newRate = (SLpermille) 2000;
    (*playRate)->SetRate(playRate, newRate);
    (*playRate)->GetRate(playRate, &pRate);

    // get the volume interface
    result = (*bqPlayerObject)->GetInterface(bqPlayerObject, SL_IID_VOLUME, &bqPlayerVolume);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    // set the player's state to playing
    result = (*bqPlayerPlay)->SetPlayState(bqPlayerPlay, SL_PLAYSTATE_PLAYING);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;
}

// create URI audio player
jboolean Java_kharico_graphiceq_AudioEffects_createUriAudioPlayer(JNIEnv* env, jclass clazz,
                                                                   jstring uri, jint uriFlag)
{
    SLresult result;

    // convert Java string to UTF-8
    const char *utf8 = (*env)->GetStringUTFChars(env, uri, NULL);
    assert(NULL != utf8);

    // set playrate based on audio type *may be bug in PlaybackRateItf*
    if (uriFlag == 0) {
        SLpermille  pRate;
        (*playRate)->SetRate(playRate, (SLpermille)1000);
        (*playRate)->GetRate(playRate, &pRate);
    }
    else if (uriFlag == 1) {
        SLpermille  pRate;
        (*playRate)->SetRate(playRate, (SLpermille)2000);
        (*playRate)->GetRate(playRate, &pRate);
    }

    // configure audio source
    // (requires the INTERNET permission depending on the uri parameter)
    SLDataLocator_URI loc_uri = {SL_DATALOCATOR_URI, (SLchar *) utf8};
    SLDataFormat_MIME format_mime = {SL_DATAFORMAT_MIME, NULL, SL_CONTAINERTYPE_UNSPECIFIED};
    SLDataSource audioSrc = {&loc_uri, &format_mime};

    // configure audio sink
    SLDataLocator_AndroidSimpleBufferQueue loc_bufq = {SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, 2};
    SLDataFormat_PCM format_pcm = {SL_DATAFORMAT_PCM, 1, SAMPLINGRATE,
                                   SL_PCMSAMPLEFORMAT_FIXED_16, SL_PCMSAMPLEFORMAT_FIXED_16,
                                   SL_SPEAKER_FRONT_CENTER, SL_BYTEORDER_LITTLEENDIAN};
    SLDataSink audioSnk = {&loc_bufq, &format_pcm};

    // create audio player
    const SLInterfaceID id[2] = {SL_IID_ANDROIDSIMPLEBUFFERQUEUE, SL_IID_PREFETCHSTATUS};
    const SLboolean req[2] = {SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE };
    result = (*engineEngine)->CreateAudioPlayer(engineEngine, &uriPlayerObject, &audioSrc,
                                                &audioSnk, 2, id, req);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    // realize the player
    result = (*uriPlayerObject)->Realize(uriPlayerObject, SL_BOOLEAN_FALSE);
    if (SL_RESULT_SUCCESS != result) {
        (*uriPlayerObject)->Destroy(uriPlayerObject);
        uriPlayerObject = NULL;
        return JNI_FALSE;
    }

    // get the prefetch interface
    result = (*uriPlayerObject)->GetInterface(uriPlayerObject, SL_IID_PREFETCHSTATUS, &playerPrefetch);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    // Assign masks for callback
    result = (*playerPrefetch)->SetCallbackEventsMask(playerPrefetch,
                                                      SL_PREFETCHEVENT_FILLLEVELCHANGE | SL_PREFETCHEVENT_STATUSCHANGE);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    // Set fill mode for prefetch
    result = (*playerPrefetch)->SetFillUpdatePeriod(playerPrefetch, 10);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    // Register callback on prefetch interface
    result = (*playerPrefetch)->RegisterCallback(playerPrefetch, prefetchEventCallback, &(playerPrefetch));
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    // get the buffer interface
    result = (*uriPlayerObject)->GetInterface(uriPlayerObject, SL_IID_ANDROIDSIMPLEBUFFERQUEUE, &recorderBufferQueue);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    // register callback on the buffer queue
    result = (*recorderBufferQueue)->RegisterCallback(recorderBufferQueue, bqRecorderCallback,
                                                      NULL);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    // get the play interface
    result = (*uriPlayerObject)->GetInterface(uriPlayerObject, SL_IID_PLAY, &uriPlayerPlay);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    // Start streaming
    result = (*uriPlayerPlay)->SetPlayState(uriPlayerPlay, SL_PLAYSTATE_PAUSED);
    assert(SL_RESULT_SUCCESS == result);

    //Buffer some before start
    result = (*bqPlayerBufferQueue)->Enqueue(bqPlayerBufferQueue, outputBuffer[0], BUFFERFRAMES);
    assert(SL_RESULT_SUCCESS == result);

    result = (*recorderBufferQueue)->Enqueue(recorderBufferQueue, inputBuffer[0], BUFFERFRAMES);
    assert(SL_RESULT_SUCCESS == result);

    usleep(100 * 1000);

    // Waiting for prefetch status
    SLuint32 prefetchStatus = SL_PREFETCHSTATUS_UNDERFLOW;
    SLuint32 timeOutIndex = 100;
    while ((prefetchStatus != SL_PREFETCHSTATUS_SUFFICIENTDATA) && (timeOutIndex > 0)) {
        usleep(100 * 1000);
        (*playerPrefetch)->GetPrefetchStatus(playerPrefetch, &prefetchStatus);
        timeOutIndex--;
    }

    // Enqueue buffer
    result = (*recorderBufferQueue)->Enqueue(recorderBufferQueue, inputBuffer[0], BUFFERFRAMES);
    assert(SL_RESULT_SUCCESS == result);

    // release the Java string and UTF-8
    (*env)->ReleaseStringUTFChars(env, uri, utf8);

    return JNI_TRUE;
}

// set the playing state for the URI audio player
// to PLAYING (true) or PAUSED (false)
void Java_kharico_graphiceq_AudioEffects_setPlayingUriAudioPlayer(JNIEnv* env,
                                                                   jclass clazz, jboolean isPlaying)
{
    SLresult result;
    SLuint32 *pState;

    // make sure the URI audio player was created
    if (NULL != uriPlayerPlay) {
        // set the player's state
        result = (*uriPlayerPlay)->SetPlayState(uriPlayerPlay, isPlaying ?
                                                               SL_PLAYSTATE_PLAYING : SL_PLAYSTATE_PAUSED);
        assert(SL_RESULT_SUCCESS == result);
        (void)result;
    }

}

void Java_kharico_graphiceq_AudioEffects_SetBitCrush(JNIEnv* env, jclass clazz,
                                                      jint depth) {
    if (BitCrushOn && (BitDepth == depth)) {
        BitCrushOn = JNI_FALSE;
        __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "\n BitCrush OFF\n");
    }
    else {
        BitDepth = depth;
        BitCrushOn = JNI_TRUE;
        __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "\n BitCrush ON\n");
        __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "\n BitDepth: %d\n", BitDepth);
        __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "\n depth: %d\n", depth);
    }
}

void Java_kharico_graphiceq_AudioEffects_SetChorus(JNIEnv* env, jclass clazz) {
    if (ChorusOn) {
        ChorusOn = JNI_FALSE;
        free(delayBuffer);
        __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "\n Chorus OFF\n");
    }
    else {
        ChorusOn = JNI_TRUE;

        if ((delayBuffer = malloc(BUFFERFRAMES * sizeof *delayBuffer)) == NULL) {
            __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "\n malloc failed\n");
            exit(EXIT_FAILURE);
        }

        unsigned long s;
        for (s=0; s < BUFFERFRAMES; s++) delayBuffer[s] = 0;

        __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "\n Chorus ON\n");
    }
}

// shut down the native audio system
void Java_kharico_graphiceq_MainActivity_shutdown(JNIEnv* env, jclass clazz)
{

    // destroy buffer queue audio player object, and invalidate all associated interfaces
    if (bqPlayerObject != NULL) {
        (*bqPlayerObject)->Destroy(bqPlayerObject);
        bqPlayerObject = NULL;
        bqPlayerPlay = NULL;
        bqPlayerBufferQueue = NULL;
        bqPlayerVolume = NULL;
    }

    // destroy URI audio player object, and invalidate all associated interfaces
    if (uriPlayerObject != NULL) {
        (*uriPlayerObject)->Destroy(uriPlayerObject);
        uriPlayerObject = NULL;
        uriPlayerPlay = NULL;
    }

    // destroy output mix object, and invalidate all associated interfaces
    if (outputMixObject != NULL) {
        (*outputMixObject)->Destroy(outputMixObject);
        outputMixObject = NULL;
    }

    // destroy engine object, and invalidate all associated interfaces
    if (engineObject != NULL) {
        (*engineObject)->Destroy(engineObject);
        engineObject = NULL;
        engineEngine = NULL;
    }

}