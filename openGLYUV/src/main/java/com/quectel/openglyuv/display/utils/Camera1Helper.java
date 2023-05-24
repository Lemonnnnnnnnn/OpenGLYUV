package com.quectel.openglyuv.display.utils;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.util.Log;
import android.view.SurfaceView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class Camera1Helper implements Camera.PreviewCallback {

    private static final String TAG = "CameraHelper";
    public static int WIDTH = 1280;
    public static int HEIGHT = 720;
    private int mCameraId;
    private Camera mCamera;
    private byte[] buffer;
    private Camera.PreviewCallback mPreviewCallback;
    private SurfaceView surfaceView;
    private SurfaceTexture surfaceTexture = new SurfaceTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);

    public Camera1Helper(int cameraId, int width, int height) {
        mCameraId = cameraId;
        WIDTH = width;
        HEIGHT = height;
    }

    public Camera1Helper(int cameraId, int width, int height, SurfaceView surfaceView) {
        mCameraId = cameraId;
        WIDTH = width;
        HEIGHT = height;
        this.surfaceView = surfaceView;

    }

    public void switchCamera() {
        if (mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
            mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
        } else {
            mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
        }
        stopPreview();
        startPreview();
    }

    public int getCameraId() {
        return mCameraId;
    }

    public void stopPreview() {
        if (mCamera != null) {
            //预览数据回调接口
            mCamera.setPreviewCallback(null);
            //停止预览
            mCamera.stopPreview();
            //释放摄像头
            mCamera.release();
            mCamera = null;
        }
    }

    public void startPreview() {
        try {
            //获得camera对象
            mCamera = Camera.open(mCameraId);
            //配置camera的属性
            Camera.Parameters parameters = mCamera.getParameters();
            //设置预览数据格式为nv21
            parameters.setPreviewFormat(ImageFormat.NV21);
            //这是摄像头宽、高
            parameters.setPreviewSize(WIDTH, HEIGHT);
            parameters.setPictureSize(WIDTH, HEIGHT);

            List<int[]> list = parameters.getSupportedPreviewFpsRange();
            if (list != null && list.size() > 0) {
                Log.d(TAG, "SupportedPreviewFpsRange:" + Arrays.toString(list.toArray()));
                int[] previewFps = list.get(list.size() - 1);
                parameters.setPreviewFpsRange(previewFps[0], previewFps[1]);
            }
            // 设置摄像头 图像传感器的角度、方向
            mCamera.setParameters(parameters);
            buffer = new byte[WIDTH * HEIGHT * 3 / 2];
            //数据缓存区
            mCamera.setPreviewCallbackWithBuffer(this);
            mCamera.addCallbackBuffer(buffer);
            //设置预览画面
            if (surfaceView != null) {
                mCamera.setPreviewDisplay(surfaceView.getHolder());
            } else {
                mCamera.setPreviewTexture(surfaceTexture);
            }

            mCamera.startPreview();
            mCamera.setErrorCallback(new Camera.ErrorCallback() {
                @Override
                public void onError(int error, Camera camera) {
                    Log.d(TAG, "camera api1 Error = " + error);
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    public void setPreviewCallback(Camera.PreviewCallback previewCallback) {
        mPreviewCallback = previewCallback;
    }


    public void takePicture(final String path) {
        if (mCamera != null) {
            try {
                mCamera.takePicture(null, null, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        mCamera.startPreview();
                        // 获取Jpeg图片，并保存在sd卡上
                        File pictureFile = new File(path);
                        try {
                            FileOutputStream fos = new FileOutputStream(pictureFile);
                            fos.write(data);
                            fos.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    boolean takePic = false;

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (takePic) {
            new Thread(new ImageSaver(data, new File("sdcard/camera1.jpg")));
            takePic = false;
        }
        // data数据依然是倒的
        if (null != mPreviewCallback) {
            mPreviewCallback.onPreviewFrame(data, camera);
        }
        camera.addCallbackBuffer(buffer);
    }

    private static class ImageSaver implements Runnable {

        /**
         * JPEG图像
         */
        private byte[] nv21;
        /**
         * 保存图像的文件
         */
        private File mFile;

        public ImageSaver() {
        }

        public ImageSaver(byte[] nv21, File file) {
            this.nv21 = nv21;
            mFile = file;
        }

        public void setFile(File file) {
            mFile = file;
        }

        @Override
        public void run() {
            YuvImage image = new YuvImage(nv21, ImageFormat.NV21, WIDTH, HEIGHT, null);
            ByteArrayOutputStream outputSteam = new ByteArrayOutputStream();
            image.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 100, outputSteam);
            byte[] jpegData = outputSteam.toByteArray();
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(mFile);
                output.write(jpegData);
                Log.d(TAG, "拍照成功");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
