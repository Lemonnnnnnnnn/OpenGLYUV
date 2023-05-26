package com.quectel.camera.display;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;


import com.quectel.openglyuv.display.encoder.MediaRecorderThread;
import com.quectel.openglyuv.display.encoder.Mp4Writer;
import com.quectel.openglyuv.display.opengl.CameraSurfaceRender;
import com.quectel.openglyuv.display.utils.Camera2Helper;
import com.quectel.openglyuv.display.utils.CameraUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class Camera2VideoFragment extends Fragment
        implements View.OnClickListener {
    private static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
    private static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 270;
    private static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();
    private static final String TAG = "Camera2VideoFragment";

    private FileOutputStream previewCBFileStream = null;
    private File mFile;

    static {
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 270);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    static {
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0);
    }

    private AutoFitTextureView mTextureView;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mPreviewSession;
    private TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture,
                                              int width, int height) {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture,
                                                int width, int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        }

    };

    private Size mPreviewSize = new Size(1920, 1080);
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            Log.d(TAG, "onOpened");
            mCameraDevice = cameraDevice;
//            new Handler().postDelayed(new Runnable() {
//                @Override
//                public void run() {
                    startPreview();
//                }
//            }, 1000);

            mCameraOpenCloseLock.release();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            Log.i(TAG, "onDisconnected=" + cameraId);
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            Log.i(TAG, "onError=" + cameraId + ";" + error);
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            Activity activity = getActivity();
            if (null != activity) {
                activity.finish();
            }
        }

        @Override
        public void onClosed(@NonNull CameraDevice camera) {
            super.onClosed(camera);
            Log.i(TAG, "onClosed=" + cameraId);
        }
    };
    private CaptureRequest.Builder mPreviewBuilder;
    private ImageReader mImageReader;
    private String cameraId;
    private static final String CAMERA_ID = "CAMERA_ID";
    private static final String ACTION = "ACTION";
    private boolean mIsRecordingVideo;
    private Button recorder;
    private MediaRecorderThread mediaRecorderThread;
    private Mp4Writer mp4Writer;
    private Range<Integer>[] fpsRange;
    private GLSurfaceView glSurfaceView;
    private CameraSurfaceRender render;
    private ImageView ivPreview;

    public static Camera2VideoFragment newInstance(String cameraID) {
        Camera2VideoFragment camera2VideoFragment = new Camera2VideoFragment();
        Bundle args = new Bundle();
        args.putString(CAMERA_ID, cameraID);
        camera2VideoFragment.setArguments(args);
        Log.d(TAG, "cameraID =" + cameraID);
        return camera2VideoFragment;
    }

    @SuppressLint("ValidFragment")
    public Camera2VideoFragment() {
    }

    public static Camera2VideoFragment newInstance(String cameraID, int action) {
        Camera2VideoFragment camera2VideoFragment = new Camera2VideoFragment();
        Bundle args = new Bundle();
        args.putString(CAMERA_ID, cameraID);
        args.putInt(ACTION, action);
        camera2VideoFragment.setArguments(args);
        Log.d(TAG, "cameraID =" + cameraID);
        return camera2VideoFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            cameraId = getArguments().getString(CAMERA_ID);
        }

    }

    public boolean ismIsRecordingVideo() {
        return mIsRecordingVideo;
    }

    public void setmIsRecordingVideo(boolean mIsRecordingVideo) {
        this.mIsRecordingVideo = mIsRecordingVideo;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera2_video, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        mTextureView = (AutoFitTextureView) view.findViewById(R.id.texture);
        ivPreview = view.findViewById(R.id.iv_preview);
//        if (cameraId.equals("3")) {
//            initDump();
//            mPreviewSize = new Size(1280,720);
//        }
        recorder = view.findViewById(R.id.video);
//        glSurfaceView = view.findViewById(R.id.glSurfaceview);
//        render = new CameraSurfaceRender(glSurfaceView, mPreviewSize.getWidth(), mPreviewSize.getHeight());
//        glSurfaceView.setEGLContextClientVersion(2);
//        glSurfaceView.setRenderer(render);
//        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        recorder.setOnClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();
        if (mTextureView.isAvailable()) {
            openCamera(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }

        if (glSurfaceView != null) {
            glSurfaceView.onResume();
        }
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause||cameraId =" + cameraId);
        super.onPause();
        if (glSurfaceView != null && render != null) {
            glSurfaceView.onPause();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy||cameraId =" + cameraId);
        closeCamera();
        stopBackgroundThread();
    }

    public void startRecorder() {
        mp4Writer = new Mp4Writer(getActivity(), mPreviewSize.getWidth(), mPreviewSize.getHeight(),
                30, getVideoPath(getActivity().getApplicationContext(), false));
        mp4Writer.startWrite();
//        mediaRecorderThread = new MediaRecorderThread(getActivity());
//        mediaRecorderThread.startEncode(mPreviewSize.getWidth(), mPreviewSize.getHeight(), getVideoPath(getActivity().getApplicationContext(), false));
        mIsRecordingVideo = true;
    }

    public void stopRecorder() {
        if (mediaRecorderThread != null) {
            mediaRecorderThread.stopAndRelease();
            mediaRecorderThread = null;
        }
        if (mp4Writer != null) {
            mp4Writer.endWrite();
            mp4Writer = null;
        }
        mIsRecordingVideo = false;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.video:
                if (mIsRecordingVideo) {
                    stopRecorder();
                    mIsRecordingVideo = false;
                    recorder.setText("录像");
                } else {
                    startRecorder();
                    recorder.setText("停止");
                    mIsRecordingVideo = true;
                }
                break;
        }
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground_"+cameraId);
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        try {
            mBackgroundThread.quitSafely();
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Tries to open a {@link CameraDevice}. The result is listened by `mStateCallback`.
     */
    @SuppressWarnings("MissingPermission")
    private void openCamera(int width, int height) {
        final Activity activity = getActivity();
        if (null == activity || activity.isFinishing()) {
            return;
        }
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            Log.d(TAG, "tryAcquire");
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map == null) {
                throw new RuntimeException("Cannot get available preview/video sizes");
            }
            fpsRange = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
            Log.d(TAG, "CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES == " + Arrays.toString(fpsRange));
            Log.d(TAG, "mPreviewSize width=" + mPreviewSize.getWidth() + ";height=" + mPreviewSize.getHeight());
            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mTextureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            } else {
                mTextureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
            }
            manager.openCamera(cameraId, mStateCallback, mBackgroundHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            closePreviewSession();

            if (null != mCameraDevice) {
                Log.i(TAG, "mCameraDevice=" + cameraId);
                mCameraDevice.close();
                mCameraDevice = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.");
        } finally {
            Log.i(TAG, "finally=" + cameraId);
            mCameraOpenCloseLock.release();
        }
    }

    /**
     * Start the camera preview.
     */
    private void startPreview() {
        if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            return;
        }
        try {
            closePreviewSession();

            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            List<Surface> list = new ArrayList<>();
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            Surface previewSurface = new Surface(texture);
            list.add(previewSurface);
            mPreviewBuilder.addTarget(previewSurface);
//            setupImageReader();
            //获取ImageReader的Surface
//            Surface imageReaderSurface = mImageReader.getSurface();
//            list.add(imageReaderSurface);
            //CaptureRequest添加imageReaderSurface，不加的话就会导致ImageReader的onImageAvailable()方法不会回调
//            mPreviewBuilder.addTarget(imageReaderSurface);
            //创建CaptureSession时加上imageReaderSurface，如下，这样预览数据就会同时输出到previewSurface和imageReaderSurface了
            mCameraDevice.createCaptureSession(list, new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            Log.d(TAG, "set CONTROL_AE_TARGET_FPS_RANGE  camera ID = " + cameraId);
                            mPreviewBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange[fpsRange.length - 1]);
                            mPreviewSession = session;
                            //设置反复捕获数据的请求，这样预览界面就会一直有数据显示
                            updatePreview();
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Activity activity = getActivity();
                            if (null != activity) {
                                Toast.makeText(activity, "Failed", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private byte[] cropYuv = null;

    private void setupImageReader() {
        //前三个参数分别是需要的尺寸和格式，最后一个参数代表每次最多获取几帧数据，本例的2代表ImageReader中最多可以获取两帧图像流
        mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(),
                ImageFormat.YUV_420_888, 2);

        //监听ImageReader的事件，当有图像流数据可用时会回调onImageAvailable方法，它的参数就是预览帧数据，可以对这帧数据进行处理
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {

            private byte[] bytes;
            private int fps, pCBCount, subfps = 0;
            private long lastFpsNanoseconds, sublastFpsNanoseconds, startTimeNs, endTimeNs = 0;

            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = reader.acquireLatestImage();
                //我们可以将这帧数据转成字节数组，类似于Camera1的PreviewCallback回调的预览帧数据
                if (image != null) {
                    bytes = CameraUtil.YUV_420_888toNV12(image);
                    byte [] glData = new byte[bytes.length];
                    System.arraycopy(bytes,0,glData,0,bytes.length);
                    if (mediaRecorderThread != null) {
//                        mediaRecorderThread.add(bytes);
                    }
                    if (mp4Writer != null){
                        mp4Writer.write(bytes);
                    }

                    if (render != null ) {
                        render.onImageReaderFrameCallBack(glData);
                    }
//                    dumpYUV(bytes);
//                    YuvImage image2 = new YuvImage(bytes,ImageFormat.NV21,mPreviewSize.getWidth(),mPreviewSize.getHeight(),null);
//                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
//                    image2.compressToJpeg(new Rect(0,0,mPreviewSize.getWidth(),mPreviewSize.getHeight()),80,stream);
//                    Bitmap bmp= BitmapFactory.decodeByteArray(stream.toByteArray(),0,stream.size());
//                    getActivity().runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            ivPreview.setImageBitmap(bmp);
//                        }
//                    });
                    endTimeNs = System.nanoTime();
                    pCBCount++;
                    if ((endTimeNs - startTimeNs) > 1000000000) {
                        startTimeNs = endTimeNs;
                        Log.d(TAG, "cameraID =  " + cameraId + " onImageAvailable fps = " + pCBCount);
                        pCBCount = 0;
                    }
                    image.close();
                }
            }
        }, mBackgroundHandler);
    }


    /**
     * Update the camera preview. {@link #startPreview()} needs to be called in advance.
     */
    private void updatePreview() {
        if (null == mCameraDevice) {
            return;
        }
        try {
            setUpCaptureRequestBuilder(mPreviewBuilder);
            mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setUpCaptureRequestBuilder(CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
    }

    private String getVideoFilePath(Context context) {
        final File dir = context.getExternalFilesDir(null);
        return (dir == null ? "" : (dir.getAbsolutePath() + "/"))
                + System.currentTimeMillis() + ".mp4";
    }

    private String getVideoPath(Context context, boolean isSub) {
        final File dir = context.getExternalFilesDir(null);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
        Date date = new Date(System.currentTimeMillis());
//        String formatString = simpleDateFormat.format(date);
        String formatString = "test";
        return (dir == null ? "" : (dir.getAbsolutePath() + "/"))
                + "Camera_" + cameraId.replace("/", "")
                + "_" + (isSub ? "sub" : "main") + "_"
                + formatString + ".mp4";
    }

    private void closePreviewSession() {
        if (mPreviewSession != null) {
            mPreviewSession.close();
            mPreviewSession = null;
        }
    }

    private void initDump() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
        String str = sdf.format(new Date());
        mFile = new File(getActivity().getExternalFilesDir(null) + "/" + str + ".yuv");
    }

    private void dumpYUV(byte[] data) {
        Log.d(TAG, "mFile =" + mFile);
        try {
            FileOutputStream out = new FileOutputStream(mFile, true);
            out.write(data);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
