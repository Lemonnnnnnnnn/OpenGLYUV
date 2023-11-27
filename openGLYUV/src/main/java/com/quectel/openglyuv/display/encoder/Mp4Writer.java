package com.quectel.openglyuv.display.encoder;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Message;
import android.util.Log;

import com.quectel.openglyuv.display.utils.BaseMessageLoop;
import com.quectel.openglyuv.display.utils.MediaCodecUtil;

import java.io.IOException;
import java.nio.ByteBuffer;

public class Mp4Writer {
    private String TAG = "Mp4Writer";
    private final BaseMessageLoop mThread;
    private static final int TYPE_BEGIN = 0;
    private static final int TYPE_WRITE = 1;
    private static final int TYPE_END = 2;
    private int encodeVideoTrackIndex;
    private volatile boolean isRunning = false;
    private MediaCodec encodeCodec;
    private MediaMuxer mMediaMuxer;
    private int encodeIndex = 0;
    private int encodeFrameRate = 0;
    boolean isSub = false;
    int encode_num = 0;
    long start_time = 0L;
    private int HEIGHT;
    private int WIDTH;

//    private int colorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible;
    private int colorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;
    private int bitRate = 2*1024*1024;

    public Mp4Writer(Context context, final int width, final int height, int fps, final String saveMp4Path) {
        isSub = saveMp4Path.contains("sub");
        Log.d(TAG,"Mp4Writer constructed,saveMp4Path =" + saveMp4Path + "||isSub =" + isSub);
        encodeFrameRate = fps;
        WIDTH = width;
        HEIGHT = height;
        String name = isSub ? "subWrite":"write";
        mThread = new BaseMessageLoop(context, name) {
            @Override
            protected boolean recvHandleMessage(Message msg) {
//                Log.d(TAG, "recvHandleMessage:" + msg.what);
                switch (msg.what) {
                    case TYPE_BEGIN:
                        try {
                            encodeIndex = 0;
                            init(width, height, saveMp4Path);
                            isRunning = true;
                        } catch (IOException e) {
                            e.printStackTrace();
//                            stopAndRelease();
                            Quit();
                        }
                        break;
                    case TYPE_WRITE:
                        if (isRunning) {
                            byte[] frameData = (byte[]) msg.obj;
                            encodeData(frameData, encodeIndex++, false);
                        }
                        break;
                    case TYPE_END:
                        if (isRunning) {
                            stopAndRelease();
                            isRunning = false;
                            Quit();
                        }
                        break;
                    default:
                }
                return false;
            }
        };
        mThread.Run();
    }

    private void init(int width, int height, String savePath) throws IOException {
        String codecName = MediaCodecUtil.getExpectedEncodeCodec(MediaFormat.MIMETYPE_VIDEO_AVC, colorFormat);
        MediaFormat encodeMediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
        encodeMediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);
        encodeMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        encodeMediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, encodeFrameRate);
        encodeMediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
//        encodeMediaFormat.setInteger(MediaFormat.KEY_PROFILE,MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline);
//        encodeMediaFormat.setInteger(MediaFormat.KEY_LEVEL,MediaCodecInfo.CodecProfileLevel.AVCLevel4);
        encodeMediaFormat.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR);
//        encodeMediaFormat.setInteger("qti-ext-enc-ipbqprange-minIQP",25);
//        encodeMediaFormat.setInteger("qti-ext-enc-ipbqprange-maxIQP",30);
//        encodeMediaFormat.setInteger("qti-ext-enc-ipbqprange-minPQP",25);
//        encodeMediaFormat.setInteger("qti-ext-enc-ipbqprange-maxPQP",30);
//        encodeMediaFormat.setInteger("qti-ext-enc-ipbqprange-minBQP",25);
//        encodeMediaFormat.setInteger("qti-ext-enc-ipbqprange-maxBQP",30);
//        encodeMediaFormat.setInteger("vendor.qti-ext-enc-frame-qp",25);
//        encodeMediaFormat.setInteger("qti-ext-enc-quantization.Iqp",35);
//        encodeMediaFormat.setInteger("qti-ext-enc-quantization.Pqp",40);
//        encodeMediaFormat.setInteger("qti-ext-enc-quantization.Bqp",40);
        encodeCodec = MediaCodec.createByCodecName(codecName);
        encodeCodec.configure(encodeMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        encodeCodec.start();
        mMediaMuxer = new MediaMuxer(savePath, 5);
    }

    private void stopAndRelease() {
        mMediaMuxer.stop();
        mMediaMuxer.release();
        encodeCodec.stop();
        encodeCodec.release();
    }

    private void encodeData(byte[] yuvBytes, int encodeIndex, boolean isVideoEOS) {
//        Log.d(MediaCodecUtil.TAG, "video encodeData");
        long presentationTimeUs = computePresentationTime();
        MediaCodec.BufferInfo encodeOutputBufferInfo = new MediaCodec.BufferInfo();
        MediaCodec.BufferInfo muxerOutputBufferInfo = new MediaCodec.BufferInfo();
        int inputBufferIndex = encodeCodec.dequeueInputBuffer(5000);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = encodeCodec.getInputBuffer(inputBufferIndex);
            inputBuffer.put(yuvBytes);
            encodeCodec.queueInputBuffer(inputBufferIndex, 0, yuvBytes.length, presentationTimeUs, 0);
        } else {
            Log.d(MediaCodecUtil.TAG, "video dequeueInputBuffer failed currentIndex =" + inputBufferIndex);
        }

        if ((encodeOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            Log.v(MediaCodecUtil.TAG, " encode  buffer stream end");
        }

        int outputBufferIndex = encodeCodec.dequeueOutputBuffer(encodeOutputBufferInfo, 5000);
        switch (outputBufferIndex) {
            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                MediaFormat newFormat = encodeCodec.getOutputFormat();
                encodeVideoTrackIndex = mMediaMuxer.addTrack(newFormat);
                mMediaMuxer.start();
                break;
            case MediaCodec.INFO_TRY_AGAIN_LATER:
                break;
            default:
                ByteBuffer outputBuffer = encodeCodec.getOutputBuffer(outputBufferIndex);
                byte[] outData = new byte[encodeOutputBufferInfo.size];
                outputBuffer.get(outData);
                if (encodeOutputBufferInfo.size != 0) {
                    encodeOutputBufferInfo.presentationTimeUs = presentationTimeUs;
                    //写到mp4
                    //根据偏移定位
                    outputBuffer.position(encodeOutputBufferInfo.offset);
                    //ByteBuffer 可读写总长度
                    outputBuffer.limit(encodeOutputBufferInfo.offset + encodeOutputBufferInfo.size);
                }
                if (encodeVideoTrackIndex == -1){
                    encodeVideoTrackIndex = writeHeadInfo(outputBuffer,encodeOutputBufferInfo);
                }
                mMediaMuxer.writeSampleData(encodeVideoTrackIndex, outputBuffer, encodeOutputBufferInfo);
                encodeCodec.releaseOutputBuffer(outputBufferIndex, false);
//                Log.d(MediaCodecUtil.TAG, "video encode frame success");
                break;
        }
    }

    /**
     * 开始初始化编码器
     */
    public void startWrite() {
        mThread.sendEmptyMessage(TYPE_BEGIN);
    }

    /**
     * 写入数据
     * @param frame
     */
    public void write(byte[] frame) {
        mThread.sendMessage(TYPE_WRITE, 0, 0, frame);
    }

    /**
     * 结束录制
     */
    public void endWrite() {
        if (isRunning) {
            encodeData(new byte[0], encodeIndex++, false);
        }
        mThread.removeMessages(TYPE_WRITE);
        mThread.sendEmptyMessage(TYPE_END);
    }

    private long computePresentationTime() {
        return System.nanoTime() / 1000L;
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
        MediaFormat outputFormat = encodeCodec.getOutputFormat();
        if (sps != null) {
            outputFormat.setByteBuffer("csd-0", sps);
            outputFormat.setByteBuffer("csd-1", pps);
        }
        int videoTrackIndex = mMediaMuxer.addTrack(outputFormat);
        Log.d(TAG, "videoTrackIndex: " + videoTrackIndex);
        mMediaMuxer.start();
        return videoTrackIndex;
    }

}