package kharico.graphiceq;

import android.content.Intent;
import android.media.MediaPlayer;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import android.os.Handler;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.Spinner;


import static android.media.AudioManager.STREAM_MUSIC;

public class MainActivity extends AppCompatActivity implements
        MediaPlayer.OnPreparedListener, MediaController.MediaPlayerControl,
        MediaPlayer.OnCompletionListener, AdapterView.OnItemSelectedListener {

    private Intent mRequestFileIntent;
    private ParcelFileDescriptor mInputPFD;
    private MediaPlayer mPlay;
    private AudioController mCtrl;
    private Handler handler = new Handler();
    private AudioEffects Afx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Afx = new AudioEffects(this);
        mCtrl = new AudioController(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mCtrl.show();
        return false;
    }

    public void importFile(View view) {
        mRequestFileIntent = new Intent(Intent.ACTION_GET_CONTENT);
        mRequestFileIntent.setType("audio/*");
        requestFile();

    }

    public void openEffects(View view) {
        PopupMenu popupFX= new PopupMenu(this,view);
        popupFX.inflate(R.menu.menu_effects);
        popupFX.show();
    }

    public void launchEQ(MenuItem item) {
        Afx.setupEQ(mPlay);
        Afx.viz.setEnabled(true);
    }

    public void openReverb(MenuItem item) {
        RelativeLayout reverbView = new RelativeLayout(this);
        RelativeLayout.LayoutParams rvParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        reverbView.setLayoutParams(rvParams);

        Spinner selectReverb = new Spinner(this);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.presets_reverb, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        selectReverb.setAdapter(adapter);
        RelativeLayout.LayoutParams spinnerParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);

        LinearLayout mLinLayout = (LinearLayout)findViewById(R.id.main_view);
        mLinLayout.setOrientation(LinearLayout.VERTICAL);
        reverbView.addView(selectReverb);
        mLinLayout.addView(reverbView);

        selectReverb.setOnItemSelectedListener(this);
        Afx.setupReverb(mPlay);
    }

    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        int verbage = parent.getSelectedItemPosition();
        Log.d("onItemSelected", "verbage: " + verbage);
        Afx.changeReverb(verbage);
    }

    public void onNothingSelected(AdapterView<?> parent) {

    }

    public void requestFile() {
        /** When the user requests a file send an Intent to the
         * server app files.
         */
        int requestCode = 0;
        startActivityForResult(mRequestFileIntent, requestCode);
    }

    public void playAudio(FileDescriptor fd) {
        if (mPlay != null) {
            mPlay.release();
            mPlay = null;
        }

        mPlay = new MediaPlayer();

        try {
            mPlay.setDataSource(fd);
            mPlay.prepare();
            mPlay.setOnCompletionListener(this);
            mPlay.setOnPreparedListener(this);
            mPlay.setAudioStreamType(STREAM_MUSIC);

        } catch (IOException e) {
            e.printStackTrace();
            Log.e("MainActivity", "Audio not Playing.");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent returnIntent) {
        // If the selection didn't work
        if (resultCode != RESULT_OK) {
            // Exit without doing anything else
            return;
        } else {
            // Get the file's content URI from the incoming Intent
            Uri returnUri = returnIntent.getData();
            /*
             * Try to open the file for "read" access using the
             * returned URI. If the file isn't found, write to the
             * error log and return.
             */
            try {
                /*
                 * Get the content resolver instance for this context, and use it
                 * to get a ParcelFileDescriptor for the file.
                 */
                mInputPFD = getContentResolver().openFileDescriptor(returnUri, "r");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Log.e("MainActivity", "File not found.");
                return;
            }
            // Get a regular file descriptor for the file
            FileDescriptor fd = mInputPFD.getFileDescriptor();
            playAudio(fd);
        }
    }

    //Implement MediaController methods
    public void start() {
        mPlay.start();
        Log.i("playAudio", "Audio started playing");
    }
    public void pause() {
        mPlay.pause();
    }
    public int getDuration() {
        return mPlay.getDuration();
    }
    public int getCurrentPosition() {
        return mPlay.getCurrentPosition();
    }
    public void seekTo(int i) {
        mPlay.seekTo(i);
    }
    public boolean isPlaying() {
        return mPlay.isPlaying();
    }
    public int getBufferPercentage() {
        return 0;
    }
    public boolean canPause() {
        return true;
    }
    public boolean canSeekBackward() {
        return true;
    }
    public boolean canSeekForward() {
        return true;
    }

    public int getAudioSessionId () {
        return 1;
    }
    public void onPrepared(MediaPlayer mediaplayer) {
        mCtrl.setMediaPlayer(this);
        mCtrl.setAnchorView(findViewById(R.id.main_view));
        handler.post(new Runnable() {

            public void run() {
                mCtrl.setEnabled(true);
                mCtrl.show();

            }
        });
    }
    public void onCompletion(MediaPlayer mediaplayer) {
        Log.i("onCompletion","Audio done playing");
        if (!mPlay.isLooping() && mCtrl.isLooping) {
            mPlay.setLooping(true);
            mPlay.seekTo(0);
            mPlay.start();
            Log.i("Run", "setLooping");
        }
        else if (mPlay.isLooping() && !mCtrl.isLooping) {
            mPlay.setLooping(false);
        }
    }
}

