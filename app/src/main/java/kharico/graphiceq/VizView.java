package kharico.graphiceq;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by fxpa72 on 11/10/2015.
 */
public class VizView extends View {
    private byte[] mBytes;
    private float[] mPoints;
    private Rect mRect = new Rect();

    private Paint mForePaint = new Paint();

    public VizView(Context context) {
        super(context);
        init();
    }

    private void init() {
        mBytes = null;

        mForePaint.setStrokeWidth(9f);
        mForePaint.setAntiAlias(true);
        mForePaint.setColor(0x99808080);
    }

    public void updateViz(byte[] bytes) {
        mBytes = bytes;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {

        boolean mTop = false;
        int mDivisions = 2;

        super.onDraw(canvas);
        if (mBytes == null) {
            return;
        }

        if (mPoints == null || mPoints.length < mBytes.length * 4) {
            mPoints = new float[mBytes.length * 4];
        }

        mRect.set(0, 0, getWidth(), getHeight());

        for (int i = 0; i < mBytes.length / mDivisions; i++) {
            mPoints[i * 4] = i * 4 * mDivisions;
            mPoints[i * 4 + 2] = i * 4 * mDivisions;
            byte rfk = mBytes[mDivisions * i];
            byte ifk = mBytes[mDivisions * i + 1];
            float magnitude = (rfk * rfk + ifk * ifk);
            int dbValue = (int) (10 * Math.log10(magnitude)*5);

            if (mTop) {
                mPoints[i * 4 + 1] = 0;
                mPoints[i * 4 + 3] = (dbValue * 2 - 10);
            }
            else {
                mPoints[i * 4 + 1] = mRect.height();
                mPoints[i * 4 + 3] = mRect.height() - (dbValue * 2 - 10);
            }

        }
        canvas.drawLines(mPoints, mForePaint);
    }
}


