package com.quectel.camera.display;

import static com.quectel.camera.display.ImagerReaderUtil.getIrARGB;
import static com.quectel.camera.display.ImagerReaderUtil.transformRAW10ToRAW16;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CameraRawFragment extends BaseFragment implements View.OnClickListener {

    private final static String TAG = "PreviewFragment";
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String CAMERA_ID = "cameraId";
    private static final String WIDTH = "width";
    private static final String HEIGHT = "height";

    private static int PREVIEW_WIDTH_NOW = 1280;
    private static int PREVIEW_HEIGHT_NOW = 720;

    int realID = 0;//0;
    private CameraManager manager;

    private CameraDevice mCameraDevice;
    private TextureView mTextureView;
    private CaptureRequest.Builder mPreviewBuilder;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private CameraCaptureSession mPreviewSession;

    private ImageReader rawImageReader;
    private File mFile;
    private Size largestRaw;
    private Surface previewSurface;
    private SparseIntArray ORIENTATIONS;
    private SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();

    private Button buttonPicRaw;
    private ImageView imageViewRaw;

    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @RequiresApi(api = Build.VERSION_CODES.P)
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            Log.d(TAG, "onOpened");
            mCameraDevice = cameraDevice;
            startPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {

        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {

        }
    };

    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
            Log.d(TAG, "onSurfaceTextureAvailable||realID =" + realID);
            openCamera(realID + "");
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {

        }
    };
    private ImageSaver imageSaver;

    public static CameraRawFragment getInstance() {
        return new CameraRawFragment();
    }

    public static CameraRawFragment newInstance(int cameraId, int width, int height) {
        CameraRawFragment fragment = new CameraRawFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(CAMERA_ID, cameraId);
        bundle.putInt(WIDTH, width);
        bundle.putInt(HEIGHT, height);
        fragment.setArguments(bundle);
        return fragment;
    }

    public static CameraRawFragment newInstance(String param1, String param2) {
        CameraRawFragment fragment = new CameraRawFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            realID = getArguments().getInt(CAMERA_ID);
            PREVIEW_WIDTH_NOW = getArguments().getInt(WIDTH);
            PREVIEW_HEIGHT_NOW = getArguments().getInt(HEIGHT);
        }
        manager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_preview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mTextureView = view.findViewById(R.id.textureview);
        buttonPicRaw = view.findViewById(R.id.pic_raw);
        imageViewRaw = view.findViewById(R.id.iv_raw);

        buttonPicRaw.setOnClickListener(this);

        mFile = new File(getActivity().
                getExternalFilesDir(null) + "/" + "image.raw");

        ORIENTATIONS = new SparseIntArray();
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);

        INVERSE_ORIENTATIONS = new SparseIntArray();
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0);


        imageSaver = new ImageSaver(getActivity(), realID);
    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();
        if (mTextureView.isAvailable()) {
            openCamera(realID + "");
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        closeCamera();
    }

    @SuppressLint("MissingPermission")
    private void openCamera(String cameraId) {
        final Activity activity = getActivity();
        if (null == activity || activity.isFinishing()) {
            return;
        }
        Log.d(TAG, "openCamera||cameraId =" + cameraId);
//        configureTransform();

        try {
//            if ((ActivityCompat.checkSelfPermission(getActivity(),
//                    (Manifest.permission.CAMERA)) != PackageManager.PERMISSION_GRANTED)
//                    || (ActivityCompat.checkSelfPermission(getActivity(),
//                    (Manifest.permission.RECORD_AUDIO)) != PackageManager.PERMISSION_GRANTED)) {
//                return;
//            }
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size[] sizes = map.getOutputSizes(MediaCodec.class);
            if (sizes != null) {
                for (Size size : sizes) {
                    Log.d(TAG, "support size width = " + size.getWidth() + " height = " + size.getHeight());
                }
            }
            String[] list = manager.getCameraIdList();
            for (String s : list) {
                if (cameraId.equals(s)) {
                    manager.openCamera(cameraId, mStateCallback, null);
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void startPreview() {
        SurfaceTexture mSurfaceTexture = mTextureView.getSurfaceTexture();
        assert mSurfaceTexture != null;
        Log.d(TAG, "startPreview||PREVIEW_WIDTH_NOW =" + PREVIEW_WIDTH_NOW);
        mSurfaceTexture.setDefaultBufferSize(PREVIEW_WIDTH_NOW, PREVIEW_HEIGHT_NOW);
        try {
            setUpCameraOutputs();
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            List<Surface> list = new ArrayList<>();

            if (realID == 0){
                previewSurface = new Surface(mSurfaceTexture);
                list.add(previewSurface);
                mPreviewBuilder.addTarget(previewSurface);
            }


            Surface rawSurface = rawImageReader.getSurface();
            mPreviewBuilder.addTarget(rawSurface);
            list.add(rawSurface);
            mCameraDevice.createCaptureSession(list, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    mPreviewSession = cameraCaptureSession;
                    updatePreview();
                    Log.d(TAG, "harrison||onConfigured");

                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void updatePreview() {
        if (null == mCameraDevice) {
            return;
        }
        try {
            setUpCaptureRequestBuilder(mPreviewBuilder);
            HandlerThread thread = new HandlerThread("CameraPreview");
            thread.start();
            mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setUpCaptureRequestBuilder(CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
    }


    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void configureTransform() {
        Log.d(TAG, "configureTransform||PREVIEW_WIDTH_NOW =" + PREVIEW_WIDTH_NOW);
        int viewWidth = PREVIEW_WIDTH_NOW;
        int viewHeight = PREVIEW_HEIGHT_NOW;
        Activity activity = getActivity();

        assert activity != null;
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, viewHeight, viewWidth);
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / viewHeight,
                    (float) viewWidth / viewWidth);
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }


    private void closeCamera() {
        closePreviewSession();
//        stopBackgroundThread();
        if (null != mCameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    private void closePreviewSession() {
        if (mPreviewSession != null) {
            mPreviewSession.close();
            mPreviewSession = null;
        }
    }

    public void takeRawPic() {
        if (imageSaver != null) {
            imageSaver.setTakePic(true);
            Log.d(TAG, "take Raw success");
        } else {
            Log.d(TAG, "take Raw failed, imageSaver == null");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.pic_raw:
                if (imageSaver != null) {
                    imageSaver.setTakePic(true);
                }
                break;
        }
    }

    private void setUpImageReader(Size rawSize) {
            rawImageReader = ImageReader.newInstance
                    (rawSize.getWidth(), rawSize.getHeight(), ImageFormat.RAW10, 2);
        rawImageReader.setOnImageAvailableListener(imageReader -> {
            Image image = imageReader.acquireLatestImage();
            if (image != null) {
                if (imageSaver.isTakePic) {
                    imageSaver.mImage = image;
                    mBackgroundHandler.post(imageSaver);
                } else {
//                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
//                    byte[] data = new byte[buffer.remaining()];
//                    buffer.get(data);
//                    data = transformRAW10ToRAW16(data, rawSize.getWidth(), rawSize.getHeight());
////                    Bitmap mIrBitmap = RawToBitMap.convert8bit(data,rawSize.getWidth(),rawSize.getHeight());
//                    int[] irArgbData = getIrARGB(data, rawSize.getWidth(), rawSize.getHeight());
//                    Bitmap mIrBitmap = Bitmap.createBitmap(rawSize.getWidth(), rawSize.getHeight(), Bitmap.Config.ARGB_8888);
//                    mIrBitmap.setPixels(irArgbData, 0, rawSize.getWidth(), 0, 0, rawSize.getWidth(), rawSize.getHeight());
//                    getActivity().runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            imageViewRaw.setImageBitmap(mIrBitmap);
//                        }
//                    });
                    Log.d(TAG, "camera id = " + realID + " getRawStream width = " + image.getWidth() + " height = " + image.getHeight());
                    image.close();
                }
            }
        }, mBackgroundHandler);
    }

    private void setUpCameraOutputs() {
        try {
            // Find a CameraDevice that supports RAW captures, and configure state.
//            for (String cameraId : manager.getCameraIdList()) {
            CameraCharacteristics characteristics
                    = manager.getCameraCharacteristics(realID + "");

            // We only use a camera that supports RAW in this sample.
            if (!contains(characteristics.get(
                            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES),
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW)) {
                return;
            }

            StreamConfigurationMap map = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            largestRaw = Collections.min(
                    Arrays.asList(map.getOutputSizes(ImageFormat.RAW_SENSOR)),
                    new CompareSizesByArea());
            Size[] rawSize = map.getOutputSizes(ImageFormat.RAW_SENSOR);
            Log.d(TAG, "rawSize.size = " + rawSize.length + "  rawSize == " + Arrays.toString(rawSize));
            Log.d(TAG, "raw.width =" + largestRaw.getWidth() + "||height =" + largestRaw.getHeight());
            setUpImageReader(largestRaw);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private static boolean contains(int[] modes, int mode) {
        if (modes == null) {
            return false;
        }
        for (int i : modes) {
            if (i == mode) {
                return true;
            }
        }
        return false;
    }

    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }


    public static class ImageSaver implements Runnable {
        public Image mImage;
        private File mImageFile;
        private Context mContext;
        public boolean isTakePic = false;

        public ImageSaver(Context context, int cameraID) {
            mContext = context;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
            String dateStr = sdf.format(new Date());
            mImageFile = new File(mContext.getExternalFilesDir(null) +
                    "/cameraSwitch_" + cameraID + "_" + dateStr + ".raw");
            Log.d(TAG, "ImageSaver||mImageFile =" + mImageFile.getPath());
        }

        public ImageSaver(Image image, Context context) {
            mImage = image;
            mContext = context;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
            String dateStr = sdf.format(new Date());
            mImageFile = new File(mContext.getExternalFilesDir(null) +
                    "/cameraSwitch_" + dateStr + ".raw");
            Log.d(TAG, "ImageSaver||mImageFile =" + mImageFile.getPath());
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
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);

            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(mImageFile);
                fos.write(data, 0, data.length);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                isTakePic = false;
                mImage.close();
                if (fos != null) {
                    try {
                        fos.close();
                        fos = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
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
