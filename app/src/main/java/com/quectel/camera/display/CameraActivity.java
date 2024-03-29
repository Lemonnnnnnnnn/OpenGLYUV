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
    private BaseFragment fragment;
    private BaseFragment fragment1;
    private BaseFragment fragment2;
    private BaseFragment fragment3;
    private BaseFragment fragment4;
    private BaseFragment fragment5;

    Handler handler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 0:
                    fragment = Camera2VideoFragment.newInstance("0");
//                    fragment = CameraRawFragment.newInstance(0,1280,720);
                    replaceFragment(R.id.container,fragment);
                    break;
                case 1:
                    fragment1 = Camera2VideoFragment.newInstance("1");
//                    fragment1 = CameraRawFragment.newInstance(1,1280,720);
                    replaceFragment(R.id.container1,fragment1);
                    break;
                case 2:
                    fragment2 = Camera2VideoFragment.newInstance("2");
//                    fragment2 = CameraRawFragment.newInstance(2,1280,720);
                    replaceFragment(R.id.container2,fragment2);
                    break;
                case 3:
                    fragment3 = Camera2VideoFragment.newInstance("3");
//                    fragment3 = CameraRawFragment.newInstance(3,1280,720);
                    replaceFragment(R.id.container3,fragment3);
                    break;
                case 4:
                    fragment4 = Camera2VideoFragment.newInstance("4");
                    replaceFragment(R.id.container4,fragment4);
                    break;
                case 5:
                    fragment5 = Camera2VideoFragment.newInstance("5");
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
//        handler.sendEmptyMessageDelayed(1,500);
//        handler.sendEmptyMessageDelayed(2,1000);
//        handler.sendEmptyMessageDelayed(3,1500);
//        handler.sendEmptyMessageDelayed(4,2000);
//        handler.sendEmptyMessageDelayed(5,2500);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        finish();
        super.onBackPressed();
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
