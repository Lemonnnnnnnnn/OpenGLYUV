package com.quectel.camera.display;


import android.app.Fragment;

public abstract class BaseFragment extends Fragment {
    protected abstract void startRecorder();
    protected abstract void stopRecorder();
    protected abstract boolean ismIsRecordingVideo();
}
