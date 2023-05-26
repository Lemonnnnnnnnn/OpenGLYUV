package com.quectel.openglyuv.display.Qencoder;


import static com.quectel.openglyuv.display.Qencoder.Constants.*;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.os.SystemClock;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.Log;


import com.quectel.openglyuv.display.Qencoder.database.SQHelper;
import com.quectel.openglyuv.display.utils.StorageUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;


public class QVideoEncoder {

    private static final String TAG = "QVideoEncoder";

    private final static String MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC;
    private static final long DEFAULT_TIMEOUT_US = 5 * 1000;

    private MediaCodec mEncoder;
    private MediaMuxer mMediaMuxer;
    private int mVideoTrackIndex;
    private boolean mStop = false;
    private int VIDEO_BIT_RATE = 1 * 1024 * 1024;
    private boolean shouldSwitchFile = false;
    private long currentFileSize = 0L;
    private Context mContext;
    private MediaFormat mMediaFormat;
    private boolean isFileSwitched = false;
    private SQHelper mSQHelper;
    private String currentFilePath;
    private String videoType;
    private String channelID;
    private String segmentationType;
    private int durationSeconds;
    private int sizeMB;
    private long startTime = 0L;
    private boolean isEncodeFirstStarted = true;
    private boolean isFirstEncode = true;

    public QVideoEncoder(Context context, MediaFormat mediaFormat) {
        mContext = context;
        //mMediaFormat = mediaFormat;

        mStop = false;
        mVideoTrackIndex = -1;
        Log.d(TAG, "QVideoEncoder constructed");
        initCodec(mediaFormat);
        initDataBase(context);
    }

    private void initDataBase(Context context) {
        mSQHelper = SQHelper.getInstance(context);
    }


    private void initMuxer(String path) {
        Log.d(TAG, "initMuxer||path =" + path);
        try {
            mMediaMuxer = new MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void initCodec(MediaFormat mediaFormat) {
        videoType = mediaFormat.getString(Constants.VIDEO_TYPE, Constants.VIDEO_TYPE_MAIN);
        channelID = mediaFormat.getString(Constants.CHANNEL_ID, "0");
        segmentationType = mediaFormat.getString(Constants.VIDEO_SEGMENTATION_TYPE, Constants.SEGMENTATION_BY_DURATION);
        if (TextUtils.equals(segmentationType, Constants.SEGMENTATION_BY_DURATION)) {
            durationSeconds = mediaFormat.getInteger(DURATION_SECONDS, 30);
        } else if (TextUtils.equals(segmentationType, SEGMENTATION_BY_SIZE)) {
            sizeMB = mediaFormat.getInteger(SIZE_MB, 50);
        }
        Log.d(TAG, "initCodec||segmentationType =" + segmentationType + "||durationSeconds ="
                + durationSeconds + "||sizeMB =" + sizeMB);

        String mime = mediaFormat.getString(MediaFormat.KEY_MIME, VIDEO_AVC);
        int width = mediaFormat.getInteger(MediaFormat.KEY_WIDTH, Constants.WIDTH_720P);
        int height = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT, Constants.HEIGHT_720P);
        int bitRate = mediaFormat.getInteger(MediaFormat.KEY_BIT_RATE, Constants.BIT_RATE_4_M);
        int colorFormat = mediaFormat.getInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
        int frmaeRate = mediaFormat.getInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE_30);
        int iInterval = mediaFormat.getInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL_2S);
        int bitRateMode = mediaFormat.getInteger(MediaFormat.KEY_BITRATE_MODE,
                MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);

        mMediaFormat = MediaFormat.createVideoFormat(mime, width, height);
        mMediaFormat.setString(Constants.VIDEO_TYPE, videoType);
        mMediaFormat.setString(Constants.CHANNEL_ID, channelID);
        mMediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);
        mMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        mMediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iInterval);
        mMediaFormat.setInteger(MediaFormat.KEY_BITRATE_MODE, bitRateMode);
        mMediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frmaeRate);

        try {
            mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mEncoder.configure(mMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mEncoder.start();
    }

    public void release() {
        Log.d(TAG, "released");
        mStop = true;
        insertDataBase(currentFilePath, new Date().getTime());
        if (mEncoder != null) {
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        }
        if (mMediaMuxer != null) {
            mMediaMuxer.release();
            mMediaMuxer = null;
        }
    }

    public void encode(byte[] yuv, long presentationTimeUs) {
        while (!mStop) {
            if (mEncoder == null) {
                //Log.e(TAG, "mEncoder is null");
                return;
            }
            if (yuv == null) {
                //Log.e(TAG, "input yuv data is null");
                return;
            }

            int inputBufferIndex = mEncoder.dequeueInputBuffer(DEFAULT_TIMEOUT_US);
            //Log.d(TAG, "inputBufferIndex: " + inputBufferIndex);
            if (inputBufferIndex == -1) {
                //Log.e(TAG, "no valid buffer available");
                return;
            }

            ByteBuffer inputBuffer = mEncoder.getInputBuffer(inputBufferIndex);
            if (inputBuffer != null) {
                inputBuffer.put(yuv);
            }
            mEncoder.queueInputBuffer(inputBufferIndex, 0, yuv.length, presentationTimeUs, 0);

            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex = mEncoder.dequeueOutputBuffer(bufferInfo, DEFAULT_TIMEOUT_US);
            //Log.d(TAG, "outputBufferIndex: " + outputBufferIndex);
            if (outputBufferIndex >= 0) {
                ByteBuffer outputBuffer = mEncoder.getOutputBuffer(outputBufferIndex);
                if (isFirstEncode) {
                    Log.d(TAG, "isFirstEncode");
                    isFirstEncode = false;
                    currentFilePath = StorageUtil.getNextVideoFilePath(mContext, videoType, channelID);
                    initMuxer(currentFilePath);
                }
                // write head info
                if (mVideoTrackIndex == -1) {
                    Log.d(TAG, "this is first frame, call writeHeadInfo first");
                    mVideoTrackIndex = writeHeadInfo(outputBuffer, bufferInfo);
                }

                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                    //Log.d(TAG, "write outputBuffer");
                    if (outputBuffer != null) {
                        if (isEncodeFirstStarted) {
                            isEncodeFirstStarted = false;
                            startTime = SystemClock.elapsedRealtime();
                            Log.d(TAG, "encode First Started");
                        } else {
                            if (isFileSwitched) {
                                isFileSwitched = false;
                                startTime = SystemClock.elapsedRealtime();
                                Log.d(TAG, "startTime refreshed");
                            }
                        }

                        mMediaMuxer.writeSampleData(mVideoTrackIndex, outputBuffer, bufferInfo);
                        if (TextUtils.equals(segmentationType, SEGMENTATION_BY_SIZE)) {
                            currentFileSize += bufferInfo.size;
                            Log.d(TAG, "currentFileSize =" + currentFileSize);
                            if (currentFileSize >= sizeMB * 1024 * 1024) {
                                mStop = true;
//                            mSQHelper.insertRecord(currentFilePath, new Date().getTime());
//                                StorageUtil.getRsqliteHelper(mContext).insertRecorder(currentFilePath, new Date().getTime());
                                insertDataBase(currentFilePath, new Date().getTime());
                                switchFile();
                            }
                        } else if (TextUtils.equals(segmentationType, SEGMENTATION_BY_DURATION)) {
                            long duration = SystemClock.elapsedRealtime() - startTime;
                            if (duration >= durationSeconds * 1000) {
                                Log.d(TAG, "duration =" + duration);
                                mStop = true;
                                //StorageUtil.getRsqliteHelper(mContext).insertRecorder(currentFilePath, new Date().getTime());
                                insertDataBase(currentFilePath, new Date().getTime());
                                switchFile();
                            }
                        }
                    }
                }
                mEncoder.releaseOutputBuffer(outputBufferIndex, false);
                break; // 跳出循环
            }
        }

    }

    private int writeHeadInfo(ByteBuffer outputBuffer, MediaCodec.BufferInfo bufferInfo) {
        byte[] csd = new byte[bufferInfo.size];
        outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
        outputBuffer.position(bufferInfo.offset);
        outputBuffer.get(csd);
        ByteBuffer sps = null;
        ByteBuffer pps = null;
        for (int i = bufferInfo.size - 1; i > 3; i--) {
            if (csd[i] == 1 && csd[i - 1] == 0 && csd[i - 2] == 0 && csd[i - 3] == 0) {
                sps = ByteBuffer.allocate(i - 3);
                pps = ByteBuffer.allocate(bufferInfo.size - (i - 3));
                sps.put(csd, 0, i - 3).position(0);
                pps.put(csd, i - 3, bufferInfo.size - (i - 3)).position(0);
            }
        }
        MediaFormat outputFormat = mEncoder.getOutputFormat();
        if (sps != null) {
            outputFormat.setByteBuffer("csd-0", sps);
            outputFormat.setByteBuffer("csd-1", pps);
        }
        int videoTrackIndex = mMediaMuxer.addTrack(outputFormat);
        Log.d(TAG, "videoTrackIndex: " + videoTrackIndex);
        mMediaMuxer.start();
        return videoTrackIndex;
    }


    private void switchFile() {
        mMediaMuxer.release();
        mMediaMuxer = null;
        currentFilePath = StorageUtil.getNextVideoFilePath(mContext, videoType, channelID);
        Log.d(TAG, "switchFile||nextFile =" + currentFilePath);
        if (!TextUtils.isEmpty(currentFilePath)) {
            initMuxer(currentFilePath);
        }
        currentFileSize = 0L;
        isFileSwitched = true;
        mVideoTrackIndex = -1;
        mStop = false;
    }

    private void insertDataBase(String filePath, long insertTime) {
        Log.d(TAG, "insertDataBase||filePath =" + filePath + "||insertTime =" + insertTime);
        StorageUtil.getRsqliteHelper(mContext).insertRecorder(currentFilePath, new Date().getTime());
    }

}

