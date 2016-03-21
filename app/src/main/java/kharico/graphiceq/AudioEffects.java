package kharico.graphiceq;

import android.app.ActionBar;
import android.media.MediaPlayer;
import android.media.audiofx.Equalizer;
import android.media.audiofx.PresetReverb;
import android.media.audiofx.Visualizer;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;


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

    final MainActivity main;

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
        pVerb.setEnabled(true);
        Log.d("setupReverb", "Enabled: " + pVerb.getEnabled());
    }

    public void changeReverb(int preset){
        pVerb.setPreset(PresetReverb.PRESET_NONE);

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
        }
    }

    public void setupBitcrush(MediaPlayer mPlay){

    }

    public void setupChorus(MediaPlayer mPlay){

    }
}
