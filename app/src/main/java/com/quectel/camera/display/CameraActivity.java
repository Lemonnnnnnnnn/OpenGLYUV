package com.quectel.camera.display;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.View;
import android.widget.Button;


public class CameraActivity extends Activity {
    String TAG = "CameraActivity";

    private static final String[] VIDEO_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
    };

    Camera2VideoFragment fragment0 = Camera2VideoFragment.newInstance("0");
    Camera2VideoFragment fragment1 = Camera2VideoFragment.newInstance("1");
    Camera2VideoFragment fragment2 = Camera2VideoFragment.newInstance("2");
    Camera2VideoFragment fragment3 = Camera2VideoFragment.newInstance("3");
    Camera2VideoFragment fragment4 = Camera2VideoFragment.newInstance("4");
    Camera2VideoFragment fragment5 = Camera2VideoFragment.newInstance("5");

//    Camera1VideoFragment fragment1_0 = Camera1VideoFragment.newInstance("0");
//    Camera1VideoFragment fragment1_1 = Camera1VideoFragment.newInstance("1");
//    Camera1VideoFragment fragment1_2 = Camera1VideoFragment.newInstance("2");
//    Camera1VideoFragment fragment1_3 = Camera1VideoFragment.newInstance("3");
//    Camera1VideoFragment fragment1_4 = Camera1VideoFragment.newInstance("4");
//    Camera1VideoFragment fragment1_5 = Camera1VideoFragment.newInstance("5");
    private static final int REQUEST_VIDEO_PERMISSIONS = 1;
    private Button btn;

//    Handler handler = new Handler(){
//        @Override
//        public void handleMessage(@NonNull Message msg) {
//            super.handleMessage(msg);
//            getFragmentManager().beginTransaction()
//                    .replace(R.id.container1, fragment1)
//                    .commit();
//        }
//    };

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        btn = findViewById(R.id.btn_startRe);
        if (null == savedInstanceState) {
            if (!hasPermissionsGranted(VIDEO_PERMISSIONS)) {
                requestPermissions(VIDEO_PERMISSIONS, REQUEST_VIDEO_PERMISSIONS);
                return;
            }
            startCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_VIDEO_PERMISSIONS) {
            Log.d(TAG,"onRequestPermissionsResult||grantResults.length =" + grantResults.length);
            if (grantResults.length == VIDEO_PERMISSIONS.length) {
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        finish();
                        break;
                    }
                }
                startCamera();
            } else {
               finish();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean hasPermissionsGranted(String[] permissions) {
        for (String permission : permissions) {
            if (checkSelfPermission(permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void startCamera() {
        getFragmentManager().beginTransaction()
                .replace(R.id.container,  fragment0)
                .replace(R.id.container1, fragment1)
                .replace(R.id.container2, fragment2)
                .replace(R.id.container3, fragment3)
                .replace(R.id.container4, fragment4)
                .replace(R.id.container5, fragment5)
                .commit();
//        handler.sendEmptyMessageDelayed(0,10000);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void startRe(View view){
        if (fragment0.ismIsRecordingVideo()){
            fragment0.stopRecorder();
            btn.setText("RECORDER");
        }else {
            fragment0.startRecorder();
            btn.setText("STOP");
        }
        if (fragment1.ismIsRecordingVideo()){
            fragment1.stopRecorder();
        }else {
            fragment1.startRecorder();
        }
        if (fragment2.ismIsRecordingVideo()){
            fragment2.stopRecorder();
        }else {
            fragment2.startRecorder();
        }
        if (fragment3.ismIsRecordingVideo()){
            fragment3.stopRecorder();
        }else {
            fragment3.startRecorder();
        }
        if (fragment4.ismIsRecordingVideo()){
            fragment4.stopRecorder();
        }else {
            fragment4.startRecorder();
        }
        if (fragment5.ismIsRecordingVideo()){
            fragment5.stopRecorder();
        }else {
            fragment5.startRecorder();
        }
//        if (fragment1_0.ismIsRecordingVideo()){
//            fragment1_0.stopRecorder();
//            btn.setText("RECORDER");
//        } else {
//            fragment1_0.startRecorder();
//            btn.setText("STOP");
//        }
//        if (fragment1_1.ismIsRecordingVideo()){
//            fragment1_1.stopRecorder();
//        }else {
//            fragment1_1.startRecorder();
//        }
//        if (fragment1_2.ismIsRecordingVideo()){
//            fragment1_2.stopRecorder();
//        }else {
//            fragment1_2.startRecorder();
//        }
//        if (fragment1_3.ismIsRecordingVideo()){
//            fragment1_3.stopRecorder();
//        }else {
//            fragment1_3.startRecorder();
//        }
//        if (fragment1_4.ismIsRecordingVideo()){
//            fragment1_4.stopRecorder();
//        }else {
//            fragment1_4.startRecorder();
//        }
//        if (fragment1_5.ismIsRecordingVideo()){
//            fragment1_5.stopRecorder();
//        }else {
//            fragment1_5.startRecorder();
//        }
    }
}
