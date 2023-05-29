package com.quectel.camera.display;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
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

    private static final int REQUEST_VIDEO_PERMISSIONS = 1;
    private Button btn;
    private Camera1VideoFragment fragment;
    private Camera1VideoFragment fragment1;
    private Camera1VideoFragment fragment2;
    private Camera1VideoFragment fragment3;
    private Camera1VideoFragment fragment4;
    private Camera1VideoFragment fragment5;

    Handler handler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 0:
                    fragment = Camera1VideoFragment.newInstance("0");
                    replaceFragment(R.id.container,fragment);
                    break;
                case 1:
                    fragment1 = Camera1VideoFragment.newInstance("1");
                    replaceFragment(R.id.container1,fragment1);
                    break;
                case 2:
                    fragment2 = Camera1VideoFragment.newInstance("2");
                    replaceFragment(R.id.container2,fragment2);
                    break;
                case 3:
                    fragment3 = Camera1VideoFragment.newInstance("3");
                    replaceFragment(R.id.container3,fragment3);
                    break;
                case 4:
                    fragment4 = Camera1VideoFragment.newInstance("4");
                    replaceFragment(R.id.container4,fragment4);
                    break;
                case 5:
                    fragment5 = Camera1VideoFragment.newInstance("5");
                    replaceFragment(R.id.container5,fragment5);
                    break;
            }
        }
    };
    private void replaceFragment(int id, Fragment fragment){
        getFragmentManager().beginTransaction()
                .replace(id, fragment)
                .commit();
    }

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
        handler.sendEmptyMessageDelayed(0,0);
        handler.sendEmptyMessageDelayed(1,1000);
        handler.sendEmptyMessageDelayed(2,2000);
        handler.sendEmptyMessageDelayed(3,3000);
        handler.sendEmptyMessageDelayed(4,4000);
        handler.sendEmptyMessageDelayed(5,5000);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void startRe(View view){
        if (fragment != null) {
            if (fragment.ismIsRecordingVideo()) {
                fragment.stopRecorder();
                btn.setText("RECORDER");
            } else {
                fragment.startRecorder();
                btn.setText("STOP");
            }
        }
        if (fragment1 != null){
            if (fragment1.ismIsRecordingVideo()){
                fragment1.stopRecorder();
            }else {
                fragment1.startRecorder();
            }
        }
        if (fragment2 != null){
            if (fragment2.ismIsRecordingVideo()){
                fragment2.stopRecorder();
            }else {
                fragment2.startRecorder();
            }
        }
        if (fragment3 != null){
            if (fragment3.ismIsRecordingVideo()){
                fragment3.stopRecorder();
            }else {
                fragment3.startRecorder();
            }
        }
        if (fragment4 != null){
            if (fragment4.ismIsRecordingVideo()){
                fragment4.stopRecorder();
            }else {
                fragment4.startRecorder();
            }
        }
        if (fragment5 != null){
            if (fragment5.ismIsRecordingVideo()){
                fragment5.stopRecorder();
            }else {
                fragment5.startRecorder();
            }
        }
    }
}
