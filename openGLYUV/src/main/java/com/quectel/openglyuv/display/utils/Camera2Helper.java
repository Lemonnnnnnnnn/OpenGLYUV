package com.quectel.openglyuv.display.utils;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Camera2Helper {

    private static final String TAG = "CameraHelper";
    public static int WIDTH = 1920;
    public static int HEIGHT = 1080;
    private SurfaceTexture mSurfaceTexture;
    private Context mContext;
    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private String mCameraId;
    private Surface mSurface;
    private CaptureRequest.Builder mPreviewBuilder;
    private ImageReader imageReader;
    private android.os.Handler mMainHandler;
    private boolean checkCamera = true;
    public onCameraError onCameraError;
//    private ImageSaver saver;
    private CameraCaptureSession cameraCaptureSession;
    private boolean isShowPreview = true;
    private onPreviewFrame previewFrame;

    public Camera2Helper(String cameraId, SurfaceTexture surfaceTexture, Context context) {
        mCameraId = cameraId;
        mSurfaceTexture = surfaceTexture;
        mContext = context;
//        saver = new ImageSaver();
    }
    public Camera2Helper(String cameraId, Context context,boolean isShowPreview){
        mCameraId = cameraId;
        mContext = context;
        this.isShowPreview = isShowPreview;
//        saver = new ImageSaver();
    }
    public void setPreviewSize(Size size){
        WIDTH = size.getWidth();
        HEIGHT = size.getHeight();
    }

    public void setShowPreview(boolean showPreview) {
        isShowPreview = showPreview;
    }

    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            checkCamera = false;
            Log.d(TAG, "CameraDevice.StateCallback ======= onOpened");
            mCameraDevice = camera;
            //开启预览
            createPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.d(TAG, "CameraDevice.StateCallback ======= onDisconnected");
            checkCamera = true;
            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (onCameraError != null) {
                onCameraError.onCameraErrorListener(true);
            }
            while (checkCamera) {
                try {
                    //需要给一个延时，要不然重新打开相机会失败
                    Thread.sleep(1_000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (!checkCamera) {
                    break;
                }
                initCamera();
            }
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.d(TAG, "CameraDevice.StateCallback ======= onError == " + error);
            checkCamera = true;
            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (onCameraError != null) {
                onCameraError.onCameraErrorListener(true);
            }
            while (checkCamera) {
                try {
                    //需要给一个延时，要不然重新打开相机会失败
                    Thread.sleep(1_000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (!checkCamera) {
                    break;
                }
                initCamera();
            }
        }
    };

    private void createPreview() {
        setUpImageReader();
        List<Surface> surfaces = new ArrayList<>();
        try {
            //设置一个具有输出Surface的CaptureRequest.Builder
            mPreviewBuilder = mCameraDevice.
                    createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            if (isShowPreview){
                mSurfaceTexture.setDefaultBufferSize(WIDTH, HEIGHT);
                mSurface = new Surface(mSurfaceTexture);
                surfaces.add(mSurface);
                mPreviewBuilder.addTarget(mSurface);
            }
            Surface imageReaderSurface = imageReader.getSurface();
            surfaces.add(imageReaderSurface);
            mPreviewBuilder.addTarget(imageReaderSurface);

            //进行相机预览
            mCameraDevice.createCaptureSession(surfaces, mStateCallbackSession, mMainHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setUpImageReader() {

        imageReader = ImageReader.newInstance(WIDTH, HEIGHT,
                ImageFormat.YUV_420_888, 2);
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            int fps = 0;
            long lastFpsNanoseconds = 0;
            @Override
            public void onImageAvailable(ImageReader reader) {
                try {
                    Image image = reader.acquireLatestImage();
                    //我们可以将这帧数据转成字节数组，类似于Camera1的PreviewCallback回调的预览帧数据
                    if (image != null) {
                        fps++;
                        //统计每秒钟打印次数获取帧率
                        if (image.getTimestamp() != -1 && image.getTimestamp() - lastFpsNanoseconds > 1000_000_000) { //打印帧率
                            Log.d(TAG, "---camera ID = "+mCameraId+" ----onImageAvailable fps = " + fps +"\n "+
                                    "ImageReader.width =" + image.getWidth() + ", ImageReader.height = " + image.getHeight() );
                            fps = 0;
                            lastFpsNanoseconds = image.getTimestamp();
                        }
//                        if (saver.isTakePic) {
//                            saver.setImage(image);
//                            mMainHandler.post(saver);
//                        } else {
                            if (previewFrame != null){
                                previewFrame.previewFrameCallback(image);
                            }
//                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, mMainHandler);
    }

    private CameraCaptureSession.StateCallback mStateCallbackSession = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {

//            mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE,
//                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
//            mPreviewBuilder.set(CaptureRequest.JPEG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90);
            //mPreviewBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE,CaptureRequest.NOISE_REDUCTION_MODE_FAST);
//            mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE,
//                    CaptureRequest.CONTROL_AF_MODE_AUTO);
            cameraCaptureSession = session;
            try {
                //发送请求
                mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                cameraCaptureSession.setRepeatingRequest(mPreviewBuilder.build(),
                        null, mMainHandler);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            Log.d(TAG, "onConfigureFailed: ");
        }
    };

    public void initCamera() {
        //得到CameraManager
        mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        try {

            HandlerThread mCameraThread = new HandlerThread("CameraThread"+mCameraId);
            mCameraThread.start();
            mMainHandler = new android.os.Handler(mCameraThread.getLooper());
            if (mCameraId != null) {
                //打开摄像头
                if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                Log.d(TAG, "Ethan open camera Id = " + mCameraId);
                mCameraManager.openCamera(mCameraId, mStateCallback
                        , mMainHandler);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void takePic() {
//        saver.setFile(new File("sdcard/img_" + mCameraId.replaceAll("/", "") + ".jpeg"));
//        saver.setTakePic(true);
    }

    public void stopPreview() {
        if (null != mCameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    /**
     * 将JPG保存到指定的文件中。
     */
    private static class ImageSaver implements Runnable {

        /**
         * JPEG图像
         */
        private Image mImage;
        /**
         * 保存图像的文件
         */
        private File mFile;

        public boolean isTakePic = false;

        public ImageSaver() {
        }

        public ImageSaver(Image image, File file) {
            mImage = image;
            mFile = file;
        }

        public void setFile(File file) {
            mFile = file;
        }

        public void setImage(Image image) {
            mImage = image;
        }

        public void setTakePic(boolean takePic) {
            isTakePic = takePic;
        }

        @Override
        public void run() {
            if (!isTakePic) {
                mImage.close();
                return;
            }
            byte[] nv21 = CameraUtil.getNV21FromImage(mImage);
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
                isTakePic = false;
                mImage.close();
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

    public void setOnCameraErrorListener(onCameraError onCameraError) {
        this.onCameraError = onCameraError;
    }

    public void setPreviewFrame(onPreviewFrame previewFrame) {
        this.previewFrame = previewFrame;
    }

    public interface onCameraError {
        void onCameraErrorListener(boolean isError);
    }

    public interface onPreviewFrame{
        void previewFrameCallback(Image image);
    }
}
