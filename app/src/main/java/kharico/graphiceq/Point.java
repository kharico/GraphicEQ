package kharico.graphiceq;

import android.opengl.GLES20;
import android.view.MotionEvent;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by fxpa72 on 1/20/2016.
 */
public class Point {
    private FloatBuffer VertexBuffer;

    private final String VertexShaderCode =
            // This matrix member variable provides a hook to manipulate
            // the coordinates of the objects that use this vertex shader
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    "void main() {" +
                    // the matrix must be included as a modifier of gl_Position
                    "  gl_Position = uMVPMatrix * vPosition;" +
                    "  gl_PointSize = 60.0;" +
                    "}";

    private final String FragmentShaderCode =
            "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    "  float dist = distance(vec2(0.5,0.5),gl_PointCoord);" +
                    "  if (dist >= 0.5 )" +
                    "       discard;" +
                    "  gl_FragColor = vColor;" +
                    "}";

    protected int GlProgram;
    protected int PositionHandle;
    protected int ColorHandle;
    protected int MVPMatrixHandle;

    // number of coordinates per vertex in this array
    static final int COORDS_PER_VERTEX = 3;
    float PointCoords[] = {
            0.0f, 0.0f, 0.0f
    };

    private final int VertexCount = PointCoords.length / COORDS_PER_VERTEX;
    private final int VertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

    // Set color with red, green, blue and alpha (opacity) values
    float color[] = {0.63671875f, 0.76953125f, 0.22265625f, 1.0f};

    public Point() {
        // initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(
                // (number of coordinate values * 4 bytes per float)
                PointCoords.length * 4);
        // use the device hardware's native byte order
        bb.order(ByteOrder.nativeOrder());

        // create a floating point buffer from the ByteBuffer
        VertexBuffer = bb.asFloatBuffer();
        // add the coordinates to the FloatBuffer
        VertexBuffer.put(PointCoords);
        // set the buffer to read the first coordinate
        VertexBuffer.position(0);

        int vertexShader = MyGLRenderer.loadShader(GLES20.GL_VERTEX_SHADER, VertexShaderCode);
        int fragmentShader = MyGLRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER, FragmentShaderCode);

        GlProgram = GLES20.glCreateProgram();             // create empty OpenGL ES Program
        GLES20.glAttachShader(GlProgram, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(GlProgram, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(GlProgram);                  // creates OpenGL ES program executables
    }

    public void SetVerts(float v0, float v1, float v2) {
        PointCoords[0] = v0;
        PointCoords[1] = v1;
        PointCoords[2] = v2;

        VertexBuffer.put(PointCoords);
        // set the buffer to read the first coordinate
        VertexBuffer.position(0);
    }

    public float[] getVerts() {
        return PointCoords;
    }

    public void SetColor(float red, float green, float blue, float alpha) {
        color[0] = red;
        color[1] = green;
        color[2] = blue;
        color[3] = alpha;
    }

    public void draw(float[] mvpMatrix) {
        // Add program to OpenGL ES environment
        GLES20.glUseProgram(GlProgram);

        // get handle to vertex shader's vPosition member
        PositionHandle = GLES20.glGetAttribLocation(GlProgram, "vPosition");

        // Enable a handle to the point vertices
        GLES20.glEnableVertexAttribArray(PositionHandle);

        // Prepare the point coordinate data
        GLES20.glVertexAttribPointer(PositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                VertexStride, VertexBuffer);

        // get handle to fragment shader's vColor member
        ColorHandle = GLES20.glGetUniformLocation(GlProgram, "vColor");

        // Set color for drawing the point
        GLES20.glUniform4fv(ColorHandle, 1, color, 0);

        // get handle to shape's transformation matrix
        MVPMatrixHandle = GLES20.glGetUniformLocation(GlProgram, "uMVPMatrix");

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(MVPMatrixHandle, 1, false, mvpMatrix, 0);

        // Draw the point
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, VertexCount);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(PositionHandle);

    }
}
