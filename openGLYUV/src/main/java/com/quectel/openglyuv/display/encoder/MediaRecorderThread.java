package com.quectel.openglyuv.display.encoder;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import com.quectel.openglyuv.display.utils.MediaCodecUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Vector;

public class MediaRecorderThread extends Thread{
    boolean isRun = false;
    private MediaCodec encodeCodec;
    private MediaMuxer mMediaMuxer;
    private int colorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible;
    private int bitRate = 1*1024*1024;
    private int encodeFrameRate = 30;
    private final Vector<byte[]> frameBytes;
    private int encodeVideoTrackIndex;
    private static final int CACHE_SIZE = 100;
    private Context context;

    public MediaRecorderThread(Context context) {
        this.context = context;
        frameBytes = new Vector<byte[]>();
    }

    public void setColorFormat(int colorFormat) {
        this.colorFormat = colorFormat;
    }

    public void setBitRate(int bitRate) {
        this.bitRate = bitRate;
    }

    public void setEncodeFrameRate(int encodeFrameRate) {
        this.encodeFrameRate = encodeFrameRate;
    }

    public void startEncode(int width, int height, String savePath){
        try {
            String codecName = MediaCodecUtil.getExpectedEncodeCodec(MediaFormat.MIMETYPE_VIDEO_AVC, colorFormat);
            MediaFormat encodeMediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
            encodeMediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);
            encodeMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
            encodeMediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, encodeFrameRate);
            encodeMediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10);
            encodeMediaFormat.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR);
            encodeCodec = MediaCodec.createByCodecName(codecName);
            encodeCodec.configure(encodeMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            encodeCodec.start();
            mMediaMuxer = new MediaMuxer(savePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            isRun = true;
            if (!(this.isAlive())){
                start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void stopAndRelease() {
        isRun = false;
        if (mMediaMuxer != null){
            mMediaMuxer.stop();
            mMediaMuxer.release();
        }

        if (encodeCodec != null){
            encodeCodec.stop();
            encodeCodec.release();
        }
    }
    public void add(byte[] data) {
        if (!isRun) return;
        if (frameBytes.size() > CACHE_SIZE) {
            frameBytes.remove(0);
        }
//        Log.d(MediaCodecUtil.TAG,"video frameBytes.size ==  "+frameBytes.size());
        frameBytes.add(data);
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
        Log.d(MediaCodecUtil.TAG, "videoTrackIndex: " + videoTrackIndex);
        mMediaMuxer.start();
        return videoTrackIndex;
    }

    @Override
    public void run() {
        while (isRun){
            if (frameBytes.isEmpty()){
                continue;
            }
            try {
                byte[] yuvBytes = frameBytes.remove(0);
//                Log.d(MediaCodecUtil.TAG, "video encodeData");
                long presentationTimeUs = computePresentationTime();
                MediaCodec.BufferInfo encodeOutputBufferInfo = new MediaCodec.BufferInfo();
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
                        break;
                }
            }catch (Exception e){
                e.printStackTrace();
            }

        }
    }
}
