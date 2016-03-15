package kharico.graphiceq;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.MediaController;

/**
 * Created by kharico on 10/14/2015.
 */
public class AudioController extends MediaController{
    public boolean isLooping = false;

    public AudioController(Context context) {
        super(context);
    }

    @Override
    public void setAnchorView(View view) {
        super.setAnchorView(view);

        final Button loopButton = new Button(getContext());
        loopButton.setText("Loop");
        loopButton.setTextColor(getResources().getColor(R.color.black));

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.RIGHT;
        addView(loopButton, params);
        loopButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                isLooping = !isLooping;
                if (isLooping) {
                    loopButton.setTextColor(getResources().getColor(R.color.green));
                }
                else {
                    loopButton.setTextColor(getResources().getColor(R.color.black));
                }
            }
        });
    }

}
