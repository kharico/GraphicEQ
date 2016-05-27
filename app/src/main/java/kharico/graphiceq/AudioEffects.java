package kharico.graphiceq;

import android.app.ActionBar;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.EnvironmentalReverb;
import android.media.audiofx.Equalizer;
import android.media.audiofx.PresetReverb;
import android.media.audiofx.Visualizer;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import org.xmlpull.v1.XmlPullParser;


/**
 * Created by fxpa72 on 11/9/2015.
 */
public class AudioEffects {

    private static final float VISUALIZER_HEIGHT_DIP = 200f;

    //public Equalizer eq;
    public Visualizer viz;
    private VizView mVizView;
    private LinearLayout mLinLayout;
    private MyGLSurfaceView eqView;
    private RelativeLayout fxView;
    private PresetReverb pVerb;
    private EnvironmentalReverb eVerb;

    private GridLayout EnvironmentView;
    private SeekBar reverbDelay;
    private SeekBar delayLevel;
    private SeekBar preDelay;
    private SeekBar hfDecayRatio;
    private SeekBar hfDecayLevel;
    private SeekBar decayTime;
    private SeekBar Reflection;
    private SeekBar Diffusion;
    private SeekBar Density;
    private SeekBar roomLevel;

    final MainActivity main;

    private boolean nativeAudioOn = false;
    private boolean uriPlaying = false;

    public AudioEffects(MainActivity main) {
        this.main = main;
        mLinLayout = (LinearLayout)this.main.findViewById(R.id.main_view);
        mLinLayout.setOrientation(LinearLayout.VERTICAL);
    }

    public void setupEQ(MediaPlayer mPlay) {
        // Create a VisualizerView (defined below), which will render the simplified audio
        // wave form to a Canvas.
        fxView = new RelativeLayout(this.main);

        eqView = new MyGLSurfaceView(this.main, mPlay);
        int vizHeight = (int) (VISUALIZER_HEIGHT_DIP *
                main.getResources().getDisplayMetrics().density);
        RelativeLayout.LayoutParams fxParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, vizHeight);
        fxView.setLayoutParams(fxParams);
        mVizView = new VizView(this.main);
        mVizView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, vizHeight));

        mVizView.setBackgroundColor(0xD9000000);
        fxView.addView(mVizView);
        mLinLayout.addView(fxView);


        // Create the Visualizer object and attach it to our media player.
        viz = new Visualizer(mPlay.getAudioSessionId());
        viz.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
        viz.setScalingMode(Visualizer.SCALING_MODE_NORMALIZED);
        viz.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
            public void onWaveFormDataCapture(Visualizer visualizer, byte[] bytes, int samplingRate) {
                //mVizView.updateViz(bytes);
            }

            public void onFftDataCapture(Visualizer visualizer, byte[] bytes, int samplingRate) {
                mVizView.updateViz(bytes);
            }
        }, Visualizer.getMaxCaptureRate() / 2, false, true);


        // Equalizer view
        eqView.setZOrderOnTop(true);
        RelativeLayout.LayoutParams eqParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT ,RelativeLayout.LayoutParams.MATCH_PARENT);
        eqView.setLayoutParams(eqParams);
        fxView.addView(eqView);
    }

    public void setupReverb (MediaPlayer mPlay) {
        pVerb = new PresetReverb(0, mPlay.getAudioSessionId());
        eVerb = new EnvironmentalReverb(0, mPlay.getAudioSessionId());
    }

    public void changeReverb(int preset){
        pVerb.setPreset(PresetReverb.PRESET_NONE);
        pVerb.setEnabled(true);
        eVerb.setEnabled(false);

        switch(preset) {
            case 0: {

                pVerb.setPreset(PresetReverb.PRESET_NONE);
                Log.d("setupReverb", "None ");
                break;
            }
            case 1: {
                pVerb.setPreset(PresetReverb.PRESET_LARGEHALL);
                Log.d("setupReverb", "LargeHall ");
                break;
            }
            case 2: {
                pVerb.setPreset(PresetReverb.PRESET_LARGEROOM);
                Log.d("setupReverb", "LargeRoom ");
                break;
            }
            case 3: {
                pVerb.setPreset(PresetReverb.PRESET_MEDIUMHALL);
                Log.d("setupReverb", "MediumHall ");
                break;
            }
            case 4: {
                pVerb.setPreset(PresetReverb.PRESET_MEDIUMROOM);
                Log.d("setupReverb", "MediumRoom ");
                break;
            }
            case 5: {
                pVerb.setPreset(PresetReverb.PRESET_PLATE);
                Log.d("setupReverb", "Plate ");
                break;
            }
            case 6: {
                pVerb.setPreset(PresetReverb.PRESET_SMALLROOM);
                Log.d("setupReverb", "SmallRoom ");
                break;
            }
            case 7: {
                customReverb();
                Log.d("setupReverb", "Custom ");
                break;
            }
        }
    }

    public void customReverb() {
        pVerb.setEnabled(false);
        eVerb.setEnabled(true);

        // Container for Environment Reverb Settings
        EnvironmentView = new GridLayout(this.main);
        LayoutInflater inflater = this.main.getLayoutInflater();
        EnvironmentView = (GridLayout) inflater.inflate(R.layout.environtextview, null);
        //EnvironmentView = (GridLayout) EnvironmentView.findViewById(R.id.environview);

        if (EnvironmentView == null) {
            Log.d("customReverb", "View is NULL");
        }
        else {
            Log.d("customReverb", "OK ");
        }
        mLinLayout.addView(EnvironmentView);


        //Containers for Individual Settings

        reverbDelay = new SeekBar(this.main);
        delayLevel = new SeekBar(this.main);
        preDelay = new SeekBar(this.main);
        hfDecayRatio = new SeekBar(this.main);
        hfDecayLevel = new SeekBar(this.main);
        decayTime = new SeekBar(this.main);
        Reflection = new SeekBar(this.main);
        Diffusion = new SeekBar(this.main);
        Density = new SeekBar(this.main);
        roomLevel = new SeekBar(this.main);

        reverbDelay = (SeekBar) EnvironmentView.findViewById(R.id.reverbDelay);
        delayLevel = (SeekBar) EnvironmentView.findViewById(R.id.delayLevel);
        preDelay = (SeekBar) EnvironmentView.findViewById(R.id.preDelay);
        hfDecayRatio = (SeekBar) EnvironmentView.findViewById(R.id.hfDecayRatio);
        hfDecayLevel = (SeekBar) EnvironmentView.findViewById(R.id.hfDecayLevel);
        decayTime = (SeekBar) EnvironmentView.findViewById(R.id.decayTime);
        Reflection = (SeekBar) EnvironmentView.findViewById(R.id.Reflection);
        Diffusion = (SeekBar) EnvironmentView.findViewById(R.id.Diffusion);
        Density = (SeekBar) EnvironmentView.findViewById(R.id.Density);
        roomLevel = (SeekBar) EnvironmentView.findViewById(R.id.roomLevel);

        reverbDelay.setMax(100);
        delayLevel.setMax(11000);
        preDelay.setMax(300);
        hfDecayRatio.setMax(1900);
        hfDecayLevel.setMax(9000);
        decayTime.setMax(19900);
        Reflection.setMax(10000);
        Diffusion.setMax(1000);
        Density.setMax(1000);
        roomLevel.setMax(9000);

        reverbDelay.setProgress(0);
        delayLevel.setProgress(3000);
        preDelay.setProgress(0);
        hfDecayRatio.setProgress(320);
        hfDecayLevel.setProgress(9000);
        decayTime.setProgress(1390);
        Reflection.setProgress(9000);
        Diffusion.setProgress(1000);
        Density.setProgress(1000);
        roomLevel.setProgress(3000);

        initializeSeekBarListener(reverbDelay);
        initializeSeekBarListener(delayLevel);
        initializeSeekBarListener(preDelay);
        initializeSeekBarListener(hfDecayRatio);
        initializeSeekBarListener(hfDecayLevel);
        initializeSeekBarListener(decayTime);
        initializeSeekBarListener(Reflection);
        initializeSeekBarListener(Diffusion);
        initializeSeekBarListener(Density);
        initializeSeekBarListener(roomLevel);

    }

    public void initializeSeekBarListener (final SeekBar bar) {

        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            short progress = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
                progress = (short)progressValue;

                if (bar == reverbDelay) {
                    eVerb.setReverbDelay(progress);
                }
                else if (bar == delayLevel) {
                    progress -= 9000;
                    eVerb.setReverbLevel(progress);
                }
                else if (bar == preDelay) {
                    eVerb.setReflectionsDelay(progress);
                }
                else if (bar == hfDecayRatio) {
                    progress += 100;
                    eVerb.setDecayHFRatio(progress);
                }
                else if (bar == hfDecayLevel) {
                    progress -= 9000;
                    eVerb.setRoomHFLevel(progress);
                }
                else if (bar == decayTime) {
                    progress += 100;
                    eVerb.setDecayTime(progress);
                }
                else if (bar == Reflection) {
                    progress -= 9000;
                    eVerb.setReflectionsLevel(progress);
                }
                else if (bar == Diffusion) {
                    eVerb.setDiffusion(progress);
                }
                else if (bar == Density) {
                    eVerb.setDensity(progress);
                }
                else if (bar == roomLevel) {
                    progress -= 9000;
                    eVerb.setRoomLevel(progress);
                }


            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    public void setupBitcrush(String URI, int uriFlag){
        if (!nativeAudioOn) {
            setupNativeAudio(URI, uriFlag);
        }

        boolean created = false;
        created = createUriAudioPlayer(URI, uriFlag);
        setPlayingUriAudioPlayer(true);
    }

    public void setupChorus(String URI, int uriFlag){
        if (!nativeAudioOn) {
            setupNativeAudio(URI, uriFlag);
        }

        boolean created = false;
        created = createUriAudioPlayer(URI, uriFlag);
        setPlayingUriAudioPlayer(true);
        SetChorus();
    }

    public void changeBitdepth(int preset){
        switch(preset) {
            case 0: {
                SetBitCrush(16);
                break;
            }
            case 1: {
                SetBitCrush(12);
                break;
            }
            case 2: {
                SetBitCrush(8);
                break;
            }
            case 3: {
                SetBitCrush(4);
                break;
            }
            case 4: {
                SetBitCrush(2);
                break;
            }
            case 5: {
                SetBitCrush(1);
                break;
            }
        }
    }

    public void setupNativeAudio(String URI, int uriFlag){

        createEngine();

        int sampleRate = 0;
        int bufSize = 0;
        /*
         * retrieve fast audio path sample rate and buf size; if we have it, we pass to native
         * side to create a player with fast audio enabled [ fast audio == low latency audio ];
         * IF we do not have a fast audio path, we pass 0 for sampleRate, which will force native
         * side to pick up the 8Khz sample rate.
         */
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            AudioManager myAudioMgr = (AudioManager) main.getSystemService(Context.AUDIO_SERVICE);
            String nativeParam = myAudioMgr.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
            sampleRate = Integer.parseInt(nativeParam);
            nativeParam = myAudioMgr.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
            bufSize = Integer.parseInt(nativeParam);
        }

        createBufferQueueAudioPlayer(sampleRate, bufSize);

        nativeAudioOn = true;
    }

    public static native void createEngine();
    public static native void createBufferQueueAudioPlayer(int sampleRate, int samplesPerBuf);
    public static native boolean createUriAudioPlayer(String URI, int uriFlag);
    public static native void setPlayingUriAudioPlayer(boolean isPlaying);
    public static native void SetBitCrush(int BitDepth);
    public static native void SetChorus();

    /** Load jni .so on initialization */
    static {
        System.loadLibrary("native-audio-jni");
    }


}
