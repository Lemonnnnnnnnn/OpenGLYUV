/*
 * Copyright (c) 2021, Quectel Wireless Solutions Co., Ltd. All rights reserved.
 * Quectel Wireless Solutions Proprietary and Confidential.
 */
package com.quectel.camera.display;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import com.quectel.openglyuv.display.utils.Camera1Helper;


public class SurfaceViewFragment extends BaseFragment {
    private static final String TAG = "PreviewFragment";
    private SurfaceView preview;
    private SurfaceHolder surfaceHolder;
    private int mChannel;
    private int mIsPreview;
    private int preWidth;
    private int preHeight;
    private Camera1Helper helper;


    public SurfaceViewFragment() {

    }

    public void setPreviewSize(int width, int height) {
        preWidth = width;
        preHeight = height;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_surface_view, container, false);

        preview = (SurfaceView) rootView.findViewById(R.id.preview);

        surfaceHolder = preview.getHolder();
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.d(TAG, "surfaceCreated");
//                helper = new Camera1Helper(0,1280,720,preview);
//                helper.startPreview();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.d(TAG, "surfaceDestroyed");
            }
        });

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (helper != null){
            helper.stopPreview();
        }
        Log.d(TAG, "fragmentDestroyed");
    }

    @Override
    protected void startRecorder() {

    }

    @Override
    protected void stopRecorder() {

    }

    @Override
    protected boolean ismIsRecordingVideo() {
        return false;
    }
}
