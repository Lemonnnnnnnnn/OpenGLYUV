package com.quectel.openglyuv.display.opengl;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glViewport;

import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class CameraSurfaceRender implements GLSurfaceView.Renderer,OpenGLData {

    private SurfaceTexture mSurfaceTexture;

    private YUVProgram mYUVProgram;

    private byte[] buffer;

    private final GLSurfaceView mGLSurfaceView;

    private int width,height;
    private surfaceListener listener;

    public CameraSurfaceRender(GLSurfaceView glSurfaceView,int width,int height) {
        mGLSurfaceView = glSurfaceView;
        this.width = width;
        this.height = height;
    }
    public interface surfaceListener{
        void onSurfaceCreated();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

    }

    public void setSurfaceListener(surfaceListener listener) {
        this.listener = listener;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        if (listener != null){
            listener.onSurfaceCreated();
        }
        glViewport(0, 0, width, height);

        int bufferSize = this.width * this.height * 3 / 2 ;

        buffer = new byte[bufferSize];

        for (int i = this.width * this.height; i < bufferSize; i++) {
            buffer[i] = (byte) 128;
        }

        int[] textures = new int[1];
        glGenTextures(1, textures, 0);
        mSurfaceTexture = new SurfaceTexture(textures[0]);

        mYUVProgram = new YUVProgram(mGLSurfaceView.getContext(), this.width, this.height);

    }
    
    @Override
    public void onDrawFrame(GL10 gl) {
        glClearColor(1f, 1f, 1f, 1f);
        glClear(GL_COLOR_BUFFER_BIT);

        synchronized (buffer) {
            mYUVProgram.draw(buffer);
        }

        mSurfaceTexture.updateTexImage();
    }

    @Override
    public void onImageReaderFrameCallBack(byte[] data) {

        synchronized (buffer){
            System.arraycopy(data,0,buffer,0,data.length);
        }


        mGLSurfaceView.requestRender();
    }
}