package com.termux.app.vnc;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.Surface;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class VncFrameView extends GLSurfaceView implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    private static final String TAG = "VncFrameView";

    private VncClient mVncClient;
    private int mFrameTextureId = -1;
    private int mCursorTextureId = -1;
    private boolean mFrameUpdatePending = false;
    private boolean mCursorUpdatePending = false;
    private final Object mFrameLock = new Object();
    private final Object mCursorLock = new Object();

    private int mWidth = 0;
    private int mHeight = 0;
    private float mZoomScale = 1.0f;
    private float mPanX = 0;
    private float mPanY = 0;

    public VncFrameView(Context context) {
        super(context);
        init();
    }

    public VncFrameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setEGLContextClientVersion(2);
        setRenderer(this);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
        setKeepScreenOn(false);
    }

    public void initialize(VncClient vncClient) {
        mVncClient = vncClient;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // Create frame texture
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        mFrameTextureId = textures[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFrameTextureId);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        // Create cursor texture
        GLES20.glGenTextures(1, textures, 0);
        mCursorTextureId = textures[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mCursorTextureId);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        // Initialize shaders
        initShaders();
    }

    private int mProgram;
    private int mFrameTextureUniform;
    private int mCursorTextureUniform;
    private int mPositionAttrib;
    private int mTexCoordAttrib;
    private int mTransformUniform;
    private int mCursorTransformUniform;
    private int mCursorHotspotUniform;
    private int mHasCursorUniform;

    private void initShaders() {
        String vertexShaderCode =
                "attribute vec4 aPosition;\n" +
                "attribute vec2 aTexCoord;\n" +
                "uniform mat4 uTransform;\n" +
                "varying vec2 vTexCoord;\n" +
                "void main() {\n" +
                "    gl_Position = uTransform * aPosition;\n" +
                "    vTexCoord = aTexCoord;\n" +
                "}";

        String fragmentShaderCode =
                "precision mediump float;\n" +
                "uniform sampler2D uFrameTexture;\n" +
                "uniform sampler2D uCursorTexture;\n" +
                "uniform mat3 uCursorTransform;\n" +
                "uniform vec2 uCursorHotspot;\n" +
                "uniform bool uHasCursor;\n" +
                "varying vec2 vTexCoord;\n" +
                "void main() {\n" +
                "    vec4 frameColor = texture2D(uFrameTexture, vTexCoord);\n" +
                "    if (uHasCursor) {\n" +
                "        vec2 cursorCoord = (uCursorTransform * vec3(vTexCoord, 1.0)).xy - uCursorHotspot;\n" +
                "        if (cursorCoord.x >= 0.0 && cursorCoord.x <= 1.0 && cursorCoord.y >= 0.0 && cursorCoord.y <= 1.0) {\n" +
                "            vec4 cursorColor = texture2D(uCursorTexture, cursorCoord);\n" +
                "            frameColor = mix(frameColor, cursorColor, cursorColor.a);\n" +
                "        }\n" +
                "    }\n" +
                "    gl_FragColor = frameColor;\n" +
                "}";

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vertexShader);
        GLES20.glAttachShader(mProgram, fragmentShader);
        GLES20.glLinkProgram(mProgram);

        mPositionAttrib = GLES20.glGetAttribLocation(mProgram, "aPosition");
        mTexCoordAttrib = GLES20.glGetAttribLocation(mProgram, "aTexCoord");
        mFrameTextureUniform = GLES20.glGetUniformLocation(mProgram, "uFrameTexture");
        mCursorTextureUniform = GLES20.glGetUniformLocation(mProgram, "uCursorTexture");
        mTransformUniform = GLES20.glGetUniformLocation(mProgram, "uTransform");
        mCursorTransformUniform = GLES20.glGetUniformLocation(mProgram, "uCursorTransform");
        mCursorHotspotUniform = GLES20.glGetUniformLocation(mProgram, "uCursorHotspot");
        mHasCursorUniform = GLES20.glGetUniformLocation(mProgram, "uHasCursor");

        GLES20.glDeleteShader(vertexShader);
        GLES20.glDeleteShader(fragmentShader);
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            String log = GLES20.glGetShaderInfoLog(shader);
            android.util.Log.e(TAG, "Shader compilation failed: " + log);
            GLES20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        mWidth = width;
        mHeight = height;
        updateTransformMatrix();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        if (mVncClient == null || !mVncClient.isConnected()) {
            return;
        }

        // Upload frame texture
        synchronized (mFrameLock) {
            if (mFrameUpdatePending) {
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFrameTextureId);
                mVncClient.uploadFrameTexture();
                mFrameUpdatePending = false;
            }
        }

        // Upload cursor texture
        synchronized (mCursorLock) {
            if (mCursorUpdatePending) {
                GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mCursorTextureId);
                mVncClient.uploadCursorTexture();
                mCursorUpdatePending = false;
            }
        }

        // Draw frame
        GLES20.glUseProgram(mProgram);

        // Set up vertex data
        float[] vertices = {
                -1.0f, -1.0f, 0.0f, 1.0f,
                1.0f, -1.0f, 1.0f, 1.0f,
                -1.0f, 1.0f, 0.0f, 0.0f,
                1.0f, 1.0f, 1.0f, 0.0f
        };

        GLES20.glVertexAttribPointer(mPositionAttrib, 2, GLES20.GL_FLOAT, false, 16, java.nio.ByteBuffer.allocateDirect(64).order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer().put(vertices));
        GLES20.glEnableVertexAttribArray(mPositionAttrib);

        java.nio.FloatBuffer texCoordBuffer = java.nio.ByteBuffer.allocateDirect(32).order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer();
        texCoordBuffer.put(new float[]{0, 1, 1, 1, 0, 0, 1, 0});
        texCoordBuffer.position(0);
        GLES20.glVertexAttribPointer(mTexCoordAttrib, 2, GLES20.GL_FLOAT, false, 8, texCoordBuffer);
        GLES20.glEnableVertexAttribArray(mTexCoordAttrib);

        // Set uniforms
        GLES20.glUniformMatrix4fv(mTransformUniform, 1, false, mTransformMatrix, 0);
        GLES20.glUniform1i(mFrameTextureUniform, 0);
        GLES20.glUniform1i(mCursorTextureUniform, 1);

        VncClient.CursorInfo cursorInfo = mVncClient.cursorInfo;
        if (cursorInfo != null && cursorInfo.isValid()) {
            GLES20.glUniform1i(mHasCursorUniform, 1);
            // Simple cursor transform for now
            float[] cursorTransform = {
                    1.0f, 0, 0,
                    0, 1.0f, 0,
                    0, 0, 1.0f
            };
            GLES20.glUniformMatrix3fv(mCursorTransformUniform, 1, false, cursorTransform, 0);
            GLES20.glUniform2f(mCursorHotspotUniform, cursorInfo.xHot / (float) cursorInfo.width, cursorInfo.yHot / (float) cursorInfo.height);
        } else {
            GLES20.glUniform1i(mHasCursorUniform, 0);
        }

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(mPositionAttrib);
        GLES20.glDisableVertexAttribArray(mTexCoordAttrib);
    }

    private float[] mTransformMatrix = new float[16];

    private void updateTransformMatrix() {
        // Identity matrix with zoom and pan
        mTransformMatrix[0] = mZoomScale;
        mTransformMatrix[5] = mZoomScale;
        mTransformMatrix[10] = 1.0f;
        mTransformMatrix[15] = 1.0f;
        mTransformMatrix[12] = mPanX * 2.0f;
        mTransformMatrix[13] = mPanY * 2.0f;
    }

    public void setZoom(float zoomScale) {
        mZoomScale = zoomScale;
        updateTransformMatrix();
        requestRender();
    }

    public void pan(float x, float y) {
        mPanX = x;
        mPanY = y;
        updateTransformMatrix();
        requestRender();
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        // Called when new frame is available
        requestRender();
    }

    public void onFramebufferUpdated() {
        synchronized (mFrameLock) {
            mFrameUpdatePending = true;
        }
        requestRender();
    }

    public void onCursorUpdated() {
        synchronized (mCursorLock) {
            mCursorUpdatePending = true;
        }
        requestRender();
    }

    public void onFramebufferSizeChanged(int width, int height) {
        // Handle framebuffer size change
        requestRender();
    }
}