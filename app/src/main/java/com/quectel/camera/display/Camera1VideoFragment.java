package com.quectel.camera.display;

import android.content.Context;
import android.media.Image;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.app.Fragment;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.quectel.openglyuv.display.encoder.Mp4Writer;
import com.quectel.openglyuv.display.opengl.CameraSurfaceRender;
import com.quectel.openglyuv.display.utils.Camera2Helper;
import com.quectel.openglyuv.display.utils.CameraUtil;
import com.quectel.openglyuv.display.utils.LibYUVUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Camera1VideoFragment extends BaseFragment {

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
        mIsRecordingVideo = true;
    }

    public void stopRecorder() {
        if (mp4Writer != null) {
            mp4Writer.endWrite();
            mp4Writer = null;
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
            byte[] bytes = CameraUtil.YUV_420_888toNV21(image);
            if (mp4Writer != null){
                mp4Writer.write(bytes);
            }
            if (render != null) {
                render.onImageReaderFrameCallBack(bytes);
            }
            image.close();
        }
    };
}