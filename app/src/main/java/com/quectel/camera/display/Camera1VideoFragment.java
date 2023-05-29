package com.quectel.camera.display;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.Image;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.app.Fragment;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import com.quectel.openglyuv.display.Qencoder.Constants;
import com.quectel.openglyuv.display.Qencoder.QVideoEncoder;
import com.quectel.openglyuv.display.encoder.EncoderParams;
import com.quectel.openglyuv.display.encoder.MediaRecorderThread;
import com.quectel.openglyuv.display.encoder.Mp4Writer;
import com.quectel.openglyuv.display.encoder.VideoRecorder;
import com.quectel.openglyuv.display.encoder.YUVTools;
import com.quectel.openglyuv.display.opengl.CameraSurfaceRender;
import com.quectel.openglyuv.display.utils.Camera1Helper;
import com.quectel.openglyuv.display.utils.Camera2Helper;
import com.quectel.openglyuv.display.utils.CameraUtil;

import java.io.File;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Camera1VideoFragment extends Fragment {

    private final String TAG = Camera1VideoFragment.class.getSimpleName();
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private Size mPreviewSize = new Size(1920, 1080);
    private Mp4Writer mp4Writer;
    private boolean mIsRecordingVideo;
    private Camera2Helper helper;
    private GLSurfaceView glSurfaceView;
    private CameraSurfaceRender render;
    private QVideoEncoder encoder;
    private EncodeThread encodeThread = null;

    // TODO: Rename and change types of parameters
    private String cameraID;

    public Camera1VideoFragment() {
        // Required empty public constructor
    }

    public boolean ismIsRecordingVideo() {
        return mIsRecordingVideo;
    }

    // TODO: Rename and change types and number of parameters
    public static Camera1VideoFragment newInstance(String param1) {
        Camera1VideoFragment fragment = new Camera1VideoFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            cameraID = getArguments().getString(ARG_PARAM1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_camera1_video, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        glSurfaceView = view.findViewById(R.id.glSurfaceview);
        render = new CameraSurfaceRender(glSurfaceView, mPreviewSize.getWidth(), mPreviewSize.getHeight());
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setRenderer(render);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        helper = new Camera2Helper(cameraID, getActivity(), false);
        helper.setPreviewSize(mPreviewSize);
        helper.setPreviewFrame(onPreviewFrame);
        helper.initCamera();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public void startRecorder() {
        mp4Writer = new Mp4Writer(getActivity(), mPreviewSize.getWidth(), mPreviewSize.getHeight(),
                30, getVideoPath(getActivity().getApplicationContext(), false));
        mp4Writer.startWrite();
//        MediaFormat encodeMediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC,
//                mPreviewSize.getWidth(), mPreviewSize.getHeight());
//        encodeMediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
//        encodeMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 1*1024*1024);
//        encodeMediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
//        encodeMediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10);
//        encodeMediaFormat.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR);
//        encodeMediaFormat.setString(Constants.CHANNEL_ID,cameraID);

//        encoder = new QVideoEncoder(getActivity(),encodeMediaFormat);
//        encodeThread = new EncodeThread();
//        encodeThread.startThread();
//        encodeThread.start();
        mIsRecordingVideo = true;
    }

    public void stopRecorder() {
        if (mp4Writer != null) {
            mp4Writer.endWrite();
            mp4Writer = null;
        }
        if (encodeThread != null){
            encodeThread.stopThread();
            try {
                encodeThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (encoder != null){
            encoder.release();
            encoder = null;
        }
        mIsRecordingVideo = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (helper != null){
            helper.stopPreview();
        }
    }

    private String getVideoPath(Context context, boolean isSub) {
        final File dir = context.getExternalFilesDir(null);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
        Date date = new Date(System.currentTimeMillis());
//        String formatString = simpleDateFormat.format(date);
        String formatString = "test";
        return (dir == null ? "" : (dir.getAbsolutePath() + "/"))
                + "Camera_" + cameraID.replace("/", "")
                + "_" + (isSub ? "sub" : "main") + "_"
                + formatString + ".mp4";
    }

    private Camera2Helper.onPreviewFrame onPreviewFrame = new Camera2Helper.onPreviewFrame() {
        @Override
        public void previewFrameCallback(Image image) {
            byte[] bytes = CameraUtil.YUV_420_888toNV12(image);
            byte[] glData = new byte[bytes.length];
            System.arraycopy(bytes, 0, glData, 0, bytes.length);
            if (mp4Writer != null){
                mp4Writer.write(bytes);
            }
            if (encodeThread != null){
                encodeThread.setData(bytes);
            }
            if (encoder != null){
                encoder.encode(bytes,System.nanoTime() / 1000L);
            }
            if (render != null) {
                render.onImageReaderFrameCallBack(glData);
            }
            image.close();
        }
    };
    private class EncodeThread extends Thread {
        private byte[] data;
        private boolean isStop = false;

        public void setData(byte[] data) {
            this.data = data;
        }

        public void stopThread() {
            isStop = true;
        }

        public void startThread() {
            isStop = false;
        }

        @Override
        public void run() {
            super.run();
            while (!isStop && null != encoder) {
                encoder.encode(data, System.nanoTime() / 1000L);
            }
        }
    }
}