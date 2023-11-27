package com.quectel.camera.display;

/**
 * @author 331209
 * @email chen_shengjie@dahuatech.com
 * @time 2023/9/20 16:08
 * @describe
 */


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.quectel.openglyuv.display.encoder.Mp4Writer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class MediaCodecAty extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MediaCodecAty";
    private MediaEncode mediaEncode;
    private boolean Encoding = false;
    private Mp4Writer mp4Writer;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_codec);

        Log.d(TAG, " Environment.getDataDirectory()=" + Environment.getDataDirectory());
//        createFile();
        mediaEncode = new MediaEncode(1280, 720, 1000 * 1000 * 2, 25, new MediaEncode.IEncoderListener() {
            @Override
            public void onH264(byte[] data, boolean isKeyFrame) {
//                Log.e(TAG, "get a keyframe " + data.length+  "  "+isKeyFrame);
//                try {
//                    outputStream.write(data);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
            }
        });
        mp4Writer = new Mp4Writer(this,1280,720,30,getExternalFilesDir(null).getAbsolutePath()+"/ts_test.ts");
        mp4Writer.startWrite();


        findViewById(R.id.start).setOnClickListener(this);
        findViewById(R.id.stop).setOnClickListener(this);



        //循环得到未同意权限
        for (int i = 0; i < permissions.length; i++) {
            if (ContextCompat.checkSelfPermission(this, permissions[i]) != PackageManager.PERMISSION_GRANTED) {
                mPermissionList.add(permissions[i]);
            }
        }

        int count = 0;
        if (mPermissionList.isEmpty()) {
            //都有权限直接执行
            Encoding = true;
            ByteBuffer byteBuffer = ByteBuffer.allocate(1280 * 720 * 3 / 2);
            byte[] tempBuffer = new byte[1280 * 720 * 3 / 2];
            //externalCacheDir?.absoluteFile
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath(), "test.yuv");
            Log.d(TAG,"file path = " + file.getAbsolutePath());
            FileInputStream fileInputStream = null;
            try {
                fileInputStream = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            Log.d(TAG,"FileInputStream == null " + (fileInputStream==null));
            if (fileInputStream != null) {
                while (Encoding && count <= 1449) {
                    count ++;
                    try {
                        int readCount = fileInputStream.read(tempBuffer);
                        Log.d(TAG,"readcount = " + readCount);
                        if (readCount == 1280 * 720 * 3 / 2) {
//                            byteBuffer.put(tempBuffer);

                            if (mp4Writer != null){
                                mp4Writer.write(tempBuffer);
                            }
//                            mediaEncode.encoderYUV420Ex(byteBuffer);
//                            byteBuffer.flip();
                        } else if (readCount <= 0){
                            fileInputStream  = new FileInputStream(file);
                            readCount = fileInputStream.read(tempBuffer);
                            Log.d(TAG,"readcount = " + readCount);
                            if (readCount == 1280 * 720 * 3 / 2) {
                                if (mp4Writer != null){
                                    mp4Writer.write(tempBuffer);
                                }
                            }
//                            byteBuffer.put(tempBuffer);
//                            mediaEncode.encoderYUV420Ex(byteBuffer);
//                            byteBuffer.flip();
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Log.d(TAG, "Encoding stop");
        } else {
            //将List转为数组
            String[] permissions = mPermissionList.toArray(new String[mPermissionList.size()]);
            ActivityCompat.requestPermissions(this, permissions, 1);
        }
    }


    FileOutputStream outputStream;

    private void createFile() {
        //Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath(), "test.h264");
        try {
            if (!file.exists()) file.createNewFile();
            outputStream = new FileOutputStream(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //要使用的相机和存储权限
    String[] permissions = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    //用于存放未同意的权限
    List<String> mPermissionList = new ArrayList<>();


    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.stop) {
            Log.e(TAG, "read a yuv frame error from file,mabe EOF,please retry");
            Encoding = false;
            Log.d(TAG, "stop");
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                if(!Environment.isExternalStorageManager()){
//                    startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
//                }
//            }

        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaEncode != null) {
            mediaEncode.releaseMediaCodec();
            mediaEncode = null;
        }
        if (mp4Writer != null){
            mp4Writer.endWrite();
            mp4Writer=null;
        }
    }
}