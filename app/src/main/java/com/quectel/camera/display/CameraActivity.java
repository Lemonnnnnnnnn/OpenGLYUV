package com.quectel.camera.display;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
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
    private static final int REQUEST_VIDEO_PERMISSIONS = 1;
    private Button btn;

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
//                .replace(R.id.container, fragment0)
                .replace(R.id.container1, fragment1)
                .commit();
    }


    @Override
    public void onDestroy() {
        if (fragment0.ismIsRecordingVideo()){
            fragment0.stopRecorder();
        }
        if (fragment1.ismIsRecordingVideo()){
            fragment1.stopRecorder();
        }
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
    }
}
