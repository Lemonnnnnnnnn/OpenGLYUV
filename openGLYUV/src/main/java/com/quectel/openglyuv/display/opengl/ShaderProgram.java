package com.quectel.openglyuv.display.opengl;

import static android.opengl.GLES20.glDeleteProgram;
import static android.opengl.GLES20.glUseProgram;

import android.content.Context;

public abstract class ShaderProgram {

    protected int mProgram;

    protected int mWidth, mHeight;

    protected final Context mContext;

    protected ShaderProgram(Context context, int vertexId, int fragId) {
        this(context, vertexId, fragId, 0, 0);
    }

    protected ShaderProgram(Context context, int vertexId, int fragId, int width, int height) {
        mContext = context;

        mProgram = ShaderHelper.buildProgram(ResourceUtils.readText(context, vertexId),
                ResourceUtils.readText(context, fragId));

        mWidth = width;
        mHeight = height;
    }

    public void useProgram() {
        glUseProgram(mProgram);
    }

    public void release() {
        glDeleteProgram(mProgram);
        mProgram = -1;
    }
}