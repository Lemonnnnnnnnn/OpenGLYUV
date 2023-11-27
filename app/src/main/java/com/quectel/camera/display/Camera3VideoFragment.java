package com.quectel.camera.display;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;

import com.quectel.openglyuv.display.utils.CameraController;
import com.quectel.openglyuv.display.widget.AutoFitTextureView;

public class Camera3VideoFragment extends BaseFragment{
    private final static String TAG = Camera3VideoFragment.class.getSimpleName();
    private AutoFitTextureView mTextureView;
    private String cameraId;
    private static final String CAMERA_ID = "CAMERA_ID";
    private boolean mIsRecordingVideo;
    private CameraController cameraController;
    private Button btnPhoto;
    public static Camera3VideoFragment newInstance(String cameraID) {
        Camera3VideoFragment camera3VideoFragment = new Camera3VideoFragment();
        Bundle args = new Bundle();
        args.putString(CAMERA_ID, cameraID);
        camera3VideoFragment.setArguments(args);
        Log.d(TAG, "cameraID =" + cameraID);
        return camera3VideoFragment;
    }

    @SuppressLint("ValidFragment")
    public Camera3VideoFragment() {
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            cameraId = getArguments().getString(CAMERA_ID);
        }

    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera3_video, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mTextureView = view.findViewById(R.id.previewView);
        btnPhoto = view.findViewById(R.id.btn_takePhoto);
        cameraController = new CameraController();
        cameraController.setFolderPath("/sdcard/DCIM/");
    }

    @Override
    public void onResume() {
        super.onResume();
        WindowManager windowManager = getActivity().getWindowManager();
        int orientation = getResources().getConfiguration().orientation;
        cameraController.initCamera(mTextureView,windowManager,orientation,getActivity(),cameraId);
        btnPhoto.setOnClickListener(l ->{
            if (cameraController != null){
                cameraController.takePicture();
            }
        });
    }

    @Override
    protected void startRecorder() {
        if (cameraController != null){
            cameraController.startRecordingVideo();
            mIsRecordingVideo  = true;
        }
    }

    @Override
    protected void stopRecorder() {
        if (cameraController != null){
            cameraController.stopRecordingVideo();
            mIsRecordingVideo = false;
        }
    }

    @Override
    protected boolean ismIsRecordingVideo() {
        return mIsRecordingVideo;
    }
}
