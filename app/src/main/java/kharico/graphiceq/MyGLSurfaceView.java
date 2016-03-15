package kharico.graphiceq;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.MediaPlayer;
import android.media.audiofx.Equalizer;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.microedition.khronos.opengles.GL;

import static android.media.AudioManager.STREAM_MUSIC;

/**
 * Created by fxpa72 on 1/20/2016.
 */
public class MyGLSurfaceView extends GLSurfaceView {

    public final MyGLRenderer mRenderer;
    private int mActivePointerId = MotionEvent.INVALID_POINTER_ID;
    private float mLastTouchX;
    private float mLastTouchY;
    private float mPosX = 0.0f;
    private float mPosY = 0.0f;

    private int pointNum = 0;
    private boolean moved = false;

    public float dbFactor = 1f/0.176f;

    private Equalizer eq;

    public MyGLSurfaceView(Context context, MediaPlayer mPlay){
        super(context);

        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2);

        setZOrderOnTop(true);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        getHolder().setFormat(PixelFormat.RGBA_8888);

        mRenderer = new MyGLRenderer();

        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(mRenderer);
        // Render the view only when there is a change in the drawing data
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        eq = new Equalizer(0, mPlay.getAudioSessionId());
        eq.setEnabled(true);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {

        final int action = MotionEventCompat.getActionMasked(ev);

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                moved = false;
                final int pointerIndex = MotionEventCompat.getActionIndex(ev);
                final float touchX = MotionEventCompat.getX(ev, pointerIndex);
                final float touchY = MotionEventCompat.getY(ev, pointerIndex);

                // Remember where we started (for dragging)
                mLastTouchX = touchX;
                mLastTouchY = touchY;


                if (touchX >= 0f && touchX <= 360f) {
                    pointNum = 1;
                    float[] screenCoords = mRenderer.getScreenCoords(pointNum);
                    mPosX = screenCoords[0];
                    mPosY = screenCoords[1];
                }
                else if (touchX > 360f && touchX <= 720f) {
                    pointNum = 2;
                    float[] screenCoords = mRenderer.getScreenCoords(pointNum);
                    mPosX = screenCoords[0];
                    mPosY = screenCoords[1];
                }
                else if (touchX > 720f && touchX <= 1080f) {
                    pointNum = 3;
                    float[] screenCoords = mRenderer.getScreenCoords(pointNum);
                    mPosX = screenCoords[0];
                    mPosY = screenCoords[1];
                }
                else {
                    pointNum = 0;
                }


                // Save the ID of this pointer (for dragging)
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                // Find the index of the active pointer and fetch its position
                moved = true;
                final int pointerIndex =
                        MotionEventCompat.findPointerIndex(ev, mActivePointerId);

                final float x = MotionEventCompat.getX(ev, pointerIndex);
                final float y = MotionEventCompat.getY(ev, pointerIndex);

                // Calculate the distance moved
                final float dx = x - mLastTouchX;
                final float dy = y - mLastTouchY;

                mPosY -= dy;

                invalidate();

                // Remember this touch position for the next move event
                mLastTouchX = x;
                mLastTouchY = y;

                break;

            }

            case MotionEvent.ACTION_UP: {
                mActivePointerId = MotionEvent.INVALID_POINTER_ID;
                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                mActivePointerId = MotionEvent.INVALID_POINTER_ID;
                break;
            }

            case MotionEvent.ACTION_POINTER_UP: {

                final int pointerIndex = MotionEventCompat.getActionIndex(ev);
                final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);

                if (pointerId == mActivePointerId) {
                    // This was our active pointer going up. Choose a new
                    // active pointer and adjust accordingly.
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    mLastTouchX = MotionEventCompat.getX(ev, newPointerIndex);
                    mLastTouchY = MotionEventCompat.getY(ev, newPointerIndex);
                    mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
                }
                break;
            }
        }

        if (mPosY > 600f){
            mPosY = 600f;
        }
        else if (mPosY < 0f){
            mPosY = 0f;
        }

        float coords[] = new float[]{mPosX,mPosY};
        mRenderer.setDbPosition(coords, pointNum, moved);

        requestRender();

        final short maxEQLevel = eq.getBandLevelRange()[1];

        final short normalize = 4;

        final short lowBand = (short)0;
        final short midBand = (short)1;
        final short highBand = (short)2;

        short lowGain = (short)(mRenderer.dbPosition1*dbFactor*maxEQLevel);
        short midGain = (short)(mRenderer.dbPosition2*dbFactor*maxEQLevel);
        short highGain = (short)(mRenderer.dbPosition3*dbFactor*maxEQLevel);

        eq.setBandLevel(lowBand, (short)(lowGain/normalize)); //freq = 75
        eq.setBandLevel(midBand, (short) (midGain / normalize)); //freq = 290
        eq.setBandLevel(highBand,(short)(highGain/normalize)); //freq = 1130

        return true;



    }
}
