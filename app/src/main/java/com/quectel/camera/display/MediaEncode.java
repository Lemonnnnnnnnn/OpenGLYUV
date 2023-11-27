package com.quectel.camera.display;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import java.nio.ByteBuffer;

/**
 *
 */

public class MediaEncode {

    private MediaCodec mMediaCodec;
    private IEncoderListener encoderListener;

    private int videoW;
    private int videoH;
    private int videoBitrate;
    private int videoFrameRate;

    private static final String TAG = "Encode";
    private static final String MIME = "Video/AVC";
    //    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo(); //for sps pps head
    private int mSleepTime =15000;
    private byte[] mSpsPps = null;

    public MediaEncode(int videoW, int videoH, int videoBitrate, int videoFrameRate, IEncoderListener encoderListener) {
        this.videoW = videoW;
        this.videoH = videoH;
        this.videoBitrate = videoBitrate;
        this.videoFrameRate = videoFrameRate;
        this.encoderListener = encoderListener;

        initMediaCodec();
    }

    public void initMediaCodec() {
        try {
            mMediaCodec = MediaCodec.createEncoderByType(MIME);

            MediaFormat format = MediaFormat.createVideoFormat(MIME, videoW, videoH);
            format.setInteger(MediaFormat.KEY_BIT_RATE, videoBitrate);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, videoFrameRate);

            //当前项目高通AIS输出的相机yuv数据格式仅仅支持nv12或nv21,
            // 高通该平台硬件仅仅支持color_format19(即yuv420sp也叫nv12)
            //因此，ais只能选择nv12的方式
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            format.setInteger(MediaFormat.KEY_PROFILE,MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline);
            format.setInteger(MediaFormat.KEY_LEVEL,MediaCodecInfo.CodecProfileLevel.AVCLevel31);
            format.setInteger(
                    MediaFormat.KEY_BITRATE_MODE,
                    MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR
            );
            mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mMediaCodec.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }





    public boolean encoderYUV420Ex(ByteBuffer input) {
        long l = System.currentTimeMillis();
        long pts = System.nanoTime()/1000L;
        try {
            //输入待编码数据
            int inputBufferIndex = mMediaCodec.dequeueInputBuffer(0);
            //Log.i(TAG, "inputBufferIndex : " + inputBufferIndex);
            if (inputBufferIndex >= 0) {//输入队列有可用缓冲区
                //获取编码器传入数据ByteBuffer
                ByteBuffer inputBuffer = mMediaCodec.getInputBuffer(inputBufferIndex);
                inputBuffer.clear();//清除以前数据
                inputBuffer.put(input.array(), input.arrayOffset(), input.capacity());//需要编码器处理数据
                // Log.d(TAG, "encoderYUV420Ex :inputBuffer.position="+inputBuffer.position()+"inputBuffer.limit="+inputBuffer.limit()+"inputBuffer.capacity="+inputBuffer.capacity());
                mMediaCodec.queueInputBuffer(inputBufferIndex, 0, input.capacity(), pts/*微秒*/, 0);//通知编码器 数据放入
            }
            long l1 = System.currentTimeMillis();
           // mMediaCodec.queueInputBuffer(inputBufferIndex, 0, input.capacity(), pts/*微秒*/, 0);

            Log.e(TAG, "encoderYUV420Ex:111: "+(l1-l) );
            //取出编码数据
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 10000);//获取解码数据
            long l2 = System.currentTimeMillis();
            Log.e(TAG, "encoderYUV420Ex:222: "+(l2-l1) );


            Log.i(TAG, "outputBufferIndex : " + outputBufferIndex);
            while (outputBufferIndex >= 0) {//输出队列有可用缓冲区
                //Log.i(TAG, "outputBufferIndex : " + outputBufferIndex);
              //  ByteBuffer outputBuffer = mMediaCodec.getOutputBuffers()[outputBufferIndex];
                long l3 = System.currentTimeMillis();

                ByteBuffer outputBuffer = mMediaCodec.getOutputBuffer(outputBufferIndex);//获取编码数据
                byte[] outData = new byte[bufferInfo.size];
                outputBuffer.get(outData);
                switch (bufferInfo.flags) {
                    case MediaCodec.BUFFER_FLAG_CODEC_CONFIG://sps pps
                        mSpsPps = new byte[bufferInfo.size];
                        mSpsPps = outData;
                        long l4 = System.currentTimeMillis();

                        Log.e(TAG, "encoderYUV420Ex:l4: "+(l4-l3) );

                        break;
                    case MediaCodec.BUFFER_FLAG_KEY_FRAME://I

                        int type = outData[4] & 0x1F;
                        if (type == 5) {
                            byte[] keyframe = new byte[bufferInfo.size + mSpsPps.length];
                            System.arraycopy(mSpsPps, 0, keyframe, 0, mSpsPps.length);
                            System.arraycopy(outData, 0, keyframe, mSpsPps.length, outData.length);
//                          mBufferedOutputStream.write(keyframe, 0, keyframe.length);
                            if (encoderListener != null) {
                                encoderListener.onH264(keyframe, true);
                            }
                        } else {
//                          mBufferedOutputStream.write(outData, 0, outData.length);
                            if (encoderListener != null) {
                                encoderListener.onH264(outData, false);
                            }
                        }
                        long l5 = System.currentTimeMillis();

                        Log.e(TAG, "encoderYUV420Ex:l5: "+(l5-l3) );

                        break;
                    default:
//                        mBufferedOutputStream.write(outData, 0, outData.length);
                        if (encoderListener != null) {
                            encoderListener.onH264(outData, false);
                        }
                        break;
                }
                mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);//告诉编码器数据处理完成
                long pts1 = System.nanoTime();
                outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 2000);//可能一次放入的数据处理会输出多个数据
                long pts2 = System.nanoTime();
                Log.e(TAG, "encoderYUV420Ex:nanoTime: "+(pts2-pts1)/1000000 );

            }
            long l4 = System.currentTimeMillis();
            Log.e(TAG, "encoderYUV420Ex:nanoTime_: "+(l4-l) );

        } catch (Throwable t) {
            t.printStackTrace();
        }

        return true;
    }

    public void releaseMediaCodec() {
        if (mMediaCodec != null) {
            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaCodec = null;
        }
    }

    public interface IEncoderListener {
        void onH264(byte[] data, boolean isKeyFrame);
    }

}