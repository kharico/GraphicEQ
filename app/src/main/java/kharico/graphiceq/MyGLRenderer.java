package kharico.graphiceq;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;
import android.opengl.GLU;

import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by fxpa72 on 1/20/2016.
 */
public class MyGLRenderer implements GLSurfaceView.Renderer {


    private Line eq1;
    private Line eq2;
    private Line eq3;
    private Line eq4;
    private Point filter1;
    private Point filter2;
    private Point filter3;
    private Line zeroDB;

    public float screenWidth;
    public float screenHeight;


    // mMVPMatrix is an abbreviation for "Model View Projection Matrix"
    private final float[] mMVPMatrix = new float[16];
    private final float[] mProjectionMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];

    private float[] mTranslationMatrix1 = new float[16];
    private float[] mTranslationMatrix2 = new float[16];
    private float[] mTranslationMatrix3 = new float[16];

    public volatile float mAngle;
    public volatile float dbPosition1;
    public volatile float dbPosition2;
    public volatile float dbPosition3;
    public volatile float freqPosition1;
    public volatile float freqPosition2;
    public volatile float freqPosition3;

    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        // Set the background frame color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        eq1 = new Line();
        eq2 = new Line();
        eq3 = new Line();
        eq4 = new Line();
        filter1 = new Point();
        filter2 = new Point();
        filter3 = new Point();

        zeroDB = new Line();

        eq4.SetVerts(-0.875f, 0f, 0f, -1.75f, 0f, 0f);
        eq3.SetVerts(-0.25f, 0f, 0f, 0f, 0f, 0f);
        eq2.SetVerts(0f, 0f, 0f, 0.25f, 0f, 0f);
        eq1.SetVerts(1.75f, 0f, 0f, 0.875f, 0f, 0f);

        filter1.SetVerts(0.875f, 0f, 0f);
        filter2.SetVerts(0f, 0f, 0f);
        filter3.SetVerts(-0.875f, 0f, 0f);

        zeroDB.SetVerts(1.75f, 0f, 0f, -1.75f, 0f, 0f);
        zeroDB.SetColor(0.5f, 0.5f, 0.5f, 0.5f);
    }

    public void onDrawFrame(GL10 unused) {
        float[] gainsMatrix1 = new float[16];
        float[] gainsMatrix2 = new float[16];
        float[] gainsMatrix3 = new float[16];

        // Redraw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        // Set the camera position (View matrix)
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, -3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);

        Matrix.setIdentityM(mTranslationMatrix1, 0);
        Matrix.setIdentityM(mTranslationMatrix2, 0);
        Matrix.setIdentityM(mTranslationMatrix3, 0);

        Matrix.multiplyMM(gainsMatrix1, 0, mMVPMatrix, 0, mTranslationMatrix1, 0);
        Matrix.multiplyMM(gainsMatrix2, 0, mMVPMatrix, 0, mTranslationMatrix2, 0);
        Matrix.multiplyMM(gainsMatrix3, 0, mMVPMatrix, 0, mTranslationMatrix3, 0);

        eq1.draw(mMVPMatrix);
        eq2.draw(mMVPMatrix);
        eq3.draw(mMVPMatrix);
        eq4.draw(mMVPMatrix);

        zeroDB.draw(mMVPMatrix);

        filter1.draw(gainsMatrix1);
        filter2.draw(gainsMatrix2);
        filter3.draw(gainsMatrix3);
    }

    public void onSurfaceChanged(GL10 unused, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        float ratio = (float) width / height;
        screenWidth = width;
        screenHeight = height;

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 3, 7);

    }

    public static int loadShader(int type, String shaderCode) {

        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }

    public void setDbPosition(float[] coords, int pointNum,boolean moved) {

        if (moved) {
            float[] pos = new float[4];
            int[] view = {0, 0, (int) screenWidth, (int) screenHeight};

            GLU.gluUnProject(coords[0], coords[1], 0f, mViewMatrix, 0, mProjectionMatrix, 0, view, 0, pos, 0);
            pos[0] = pos[0] / pos[3];
            pos[1] = pos[1] / pos[3];
            pos[2] = pos[2] / pos[3];

            if (pointNum == 1) {
                freqPosition1 = pos[0];
                dbPosition1 = pos[1];
                filter1.SetVerts(pos[0], pos[1], 0);

                float[] verts1 = filter1.getVerts();
                float[] verts2 = filter2.getVerts();
                eq1.SetVerts(1.75f, 0f, 0f, pos[0], pos[1], 0f);
                eq2.SetVerts(verts1[0], verts1[1], 0f, verts2[0], verts2[1], 0f);

            }
            else if (pointNum == 2) {
                freqPosition2 = pos[0];
                dbPosition2 = pos[1];
                filter2.SetVerts(pos[0], pos[1], 0);

                float[] verts1 = filter1.getVerts();
                float[] verts2 = filter2.getVerts();
                float[] verts3 = filter3.getVerts();
                eq2.SetVerts(verts1[0], verts1[1], 0f, verts2[0], verts2[1], 0f);
                eq3.SetVerts(verts2[0], verts2[1], 0f, verts3[0], verts3[1], 0f);
            }
            else if (pointNum == 3) {
                freqPosition3 = pos[0];
                dbPosition3 = pos[1];
                filter3.SetVerts(pos[0], pos[1], 0);

                float[] verts2 = filter2.getVerts();
                float[] verts3 = filter3.getVerts();
                eq3.SetVerts(verts2[0], verts2[1], 0f, verts3[0], verts3[1], 0f);
                eq4.SetVerts(verts3[0], verts3[1], 0f, -1.75f, 0f, 0f);
            }

        }
        else {
            Log.e("setDB", "NOT MOVED ");
        }
    }

    public float[] getScreenCoords(int pointNum) {
        float[] screenCoords = new float[3];

        if (pointNum == 1) {
            screenCoords = filter1.getVerts();
        }
        else if (pointNum == 2) {
            screenCoords = filter2.getVerts();
        }
        else if (pointNum == 3) {
            screenCoords = filter3.getVerts();
        }

        float[] pos = new float[4];
        int[] view = {0, 0, (int) screenWidth, (int) screenHeight};

        GLU.gluProject(screenCoords[0], screenCoords[1], 0f, mViewMatrix, 0, mProjectionMatrix, 0, view, 0, pos, 0);

        return pos;
    }
}
