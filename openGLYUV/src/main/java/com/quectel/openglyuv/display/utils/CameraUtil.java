package com.quectel.openglyuv.display.utils;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.media.Image;
import android.util.Size;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @Author yocn
 * @Date 2019/8/2 11:26 AM
 * @ClassName CameraUtil
 */
public class CameraUtil {
    private static final String TAG = "CameraUtil";
    private static boolean VERBOSE = false;

    //选择sizeMap中大于并且最接近width和height的size
    public static Size getOptimalSize(Size[] sizeMap, int width, int height) {
        List<Size> sizeList = new ArrayList<>();
        for (Size option : sizeMap) {
            if (width > height) {
                if (option.getWidth() > width && option.getHeight() > height) {
                    sizeList.add(option);
                }
            } else {
                if (option.getWidth() > height && option.getHeight() > width) {
                    sizeList.add(option);
                }
            }
        }
        if (sizeList.size() > 0) {
            return Collections.min(sizeList, new Comparator<Size>() {
                @Override
                public int compare(Size lhs, Size rhs) {
                    return Long.signum(lhs.getWidth() * lhs.getHeight() - rhs.getWidth() * rhs.getHeight());
                }
            });
        }
        return sizeMap[0];
    }



    public static final int COLOR_FormatI420 = 1;
    public static final int COLOR_FormatNV21 = 2;

    private static boolean isImageFormatSupported(Image image) {
        int format = image.getFormat();
        switch (format) {
            case ImageFormat.YUV_420_888:
            case ImageFormat.NV21:
            case ImageFormat.YV12:
                return true;
            default:
        }
        return false;
    }


    public static byte[] getDataFromImage(Image image, int colorFormat) {
        if (colorFormat != COLOR_FormatI420 && colorFormat != COLOR_FormatNV21) {
            throw new IllegalArgumentException("only support COLOR_FormatI420 " + "and COLOR_FormatNV21");
        }
        if (!isImageFormatSupported(image)) {
            throw new RuntimeException("can't convert Image to byte array, format " + image.getFormat());
        }
        Rect crop = image.getCropRect();
        int format = image.getFormat();
        int width = crop.width();
        int height = crop.height();
        Image.Plane[] planes = image.getPlanes();
        byte[] data = new byte[width * height * ImageFormat.getBitsPerPixel(format) / 8];
        byte[] rowData = new byte[planes[0].getRowStride()];
        int channelOffset = 0;
        int outputStride = 1;
        for (int i = 0; i < planes.length; i++) {
            switch (i) {
                case 0:
                    channelOffset = 0;
                    outputStride = 1;
                    break;
                case 1:
                    if (colorFormat == COLOR_FormatI420) {
                        channelOffset = width * height;
                        outputStride = 1;
                    } else if (colorFormat == COLOR_FormatNV21) {
                        channelOffset = width * height + 1;
                        outputStride = 2;
                    }
                    break;
                case 2:
                    if (colorFormat == COLOR_FormatI420) {
                        channelOffset = (int) (width * height * 1.25);
                        outputStride = 1;
                    } else if (colorFormat == COLOR_FormatNV21) {
                        channelOffset = width * height;
                        outputStride = 2;
                    }
                    break;
                default:
            }
            ByteBuffer buffer = planes[i].getBuffer();
            int rowStride = planes[i].getRowStride();
            int pixelStride = planes[i].getPixelStride();
            int shift = (i == 0) ? 0 : 1;
            int w = width >> shift;
            int h = height >> shift;
            buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));
            for (int row = 0; row < h; row++) {
                int length;
                if (pixelStride == 1 && outputStride == 1) {
                    length = w;
                    buffer.get(data, channelOffset, length);
                    channelOffset += length;
                } else {
                    length = (w - 1) * pixelStride + 1;
                    buffer.get(rowData, 0, length);
                    for (int col = 0; col < w; col++) {
                        data[channelOffset] = rowData[col * pixelStride];
                        channelOffset += outputStride;
                    }
                }
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length);
                }
            }
        }
        return data;
    }

    private static ByteBuffer getBufferWithoutPadding(ByteBuffer buffer, int width, int rowStride, int times,boolean isVbuffer){
        if(width == rowStride) return buffer;  //没有buffer,不用处理。

        int bufferPos = buffer.position();
        int cap = buffer.capacity();
        byte []byteArray = new byte[times*width];
        int pos = 0;
        //对于y平面，要逐行赋值的次数就是height次。对于uv交替的平面，赋值的次数是height/2次
        for (int i=0;i<times;i++) {
            buffer.position(bufferPos);
            //part 1.1 对于u,v通道,会缺失最后一个像u值或者v值，因此需要特殊处理，否则会crash
            if(isVbuffer && i==times-1){
                width = width -1;
            }
            buffer.get(byteArray, pos, width);
            bufferPos+= rowStride;
            pos = pos+width;
        }

        //nv21数组转成buffer并返回
        ByteBuffer bufferWithoutPaddings=ByteBuffer.allocate(byteArray.length);
        // 数组放到buffer中
        bufferWithoutPaddings.put(byteArray);
        //重置 limit 和postion 值否则 buffer 读取数据不对
        bufferWithoutPaddings.flip();
        return bufferWithoutPaddings;
    }

//    public static byte[] YUV_420_888toNV21(Image image) {
//        int width =  image.getWidth();
//        int height = image.getHeight();
//        //part1 获得真正的消除padding的ybuffer和ubuffer
//        ByteBuffer yBuffer = getBufferWithoutPadding(image.getPlanes()[0].getBuffer(), image.getWidth(), image.getPlanes()[0].getRowStride(),image.getHeight(),false);
//        ByteBuffer vBuffer = getBufferWithoutPadding(image.getPlanes()[2].getBuffer(), image.getWidth(), image.getPlanes()[2].getRowStride(),image.getHeight()/2,true);
//
//        //part2 将y数据和uv的交替数据（除去最后一个v值）赋值给nv21
//        int ySize = yBuffer.remaining();
//        int vSize = vBuffer.remaining();
//        byte[] nv21;
//        int byteSize = width*height*3/2;
//        nv21 = new byte[byteSize];
//        yBuffer.get(nv21, 0, ySize);
//        vBuffer.get(nv21, ySize, vSize);
//
//        //part3 最后一个像素值的u值是缺失的，因此需要从u平面取一下。
//        ByteBuffer uPlane = image.getPlanes()[1].getBuffer();
//        byte lastValue = uPlane.get(uPlane.capacity() - 1);
//        nv21[byteSize - 1] = lastValue;
//        return nv21;
//    }

//    public static byte[] YUV_420_888toNV21(Image image) {
//        byte[] nv21;
//        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
//        ByteBuffer vuBuffer = image.getPlanes()[2].getBuffer();
//
//        int ySize = yBuffer.remaining();
//        int vuSize = vuBuffer.remaining();
//
//        nv21 = new byte[ySize + vuSize];
//
//        yBuffer.get(nv21, 0, ySize);
//        vuBuffer.get(nv21, ySize, vuSize);
//
//        return nv21;
//    }
    public static byte[] YUV_420_888toNV21(Image image) {
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
//        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

        int ySize = yBuffer.capacity();
//        int uSize = uBuffer.capacity();
        int vSize = vBuffer.capacity();

        int width = image.getWidth();
        int height = image.getHeight();
        // 申请最终结果nv21数组
        byte[] nv21 = new byte[width * height * 3 / 2];
        // 先取y通道数据，直接拷贝即可
        yBuffer.get(nv21, 0, ySize);
        // vuvuvuvu
        vBuffer.get(nv21, ySize, vSize);
//        uBuffer.get(nv21, ySize, uSize);

//        return YuvUtil.NV21toI420SemiPlanar(nv21,width,height);
//        return NV21toI420SemiPlanar(nv21,width,height);
        return nv21;
    }

    public static byte[] YUV_420_888toNV12(Image image) {
        byte[] nv12;
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

        int ySize = yBuffer.capacity();
        int uSize = uBuffer.capacity();
        int vSize = vBuffer.capacity();
//        Log.i(TAG, "YUV_420_888toNV12 total size is " + (ySize + uSize + vSize) + " ySize " + ySize + " uSize " + uSize + " vSize " + vSize);

        int width = image.getWidth();
        int height = image.getHeight();
        // 申请最终结果nv21数组
        nv12 = new byte[width * height * 3 / 2];
        // 先取y通道数据，直接拷贝即可
        yBuffer.get(nv12, 0, ySize);
        // vuvuvuvu
        uBuffer.get(nv12, ySize, uSize);
//        uBuffer.get(nv21, ySize + vSize, uSize);

        return nv12;
    }

    public static ByteBuffer YUV_420_888toByteBuffer(Image image){
        ByteBuffer nv12ByteBuffer ;
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();
        int width = image.getWidth();
        int height = image.getHeight();
        nv12ByteBuffer = ByteBuffer.allocateDirect(width * height * 3 / 2)
                .order(ByteOrder.nativeOrder());
        nv12ByteBuffer.position(0);
        nv12ByteBuffer.put(yBuffer);
        nv12ByteBuffer.put(uBuffer);
        return nv12ByteBuffer;
    }

    public static byte[] getNV21FromImage(Image image) {
        long time1 = System.currentTimeMillis();
        int w = image.getWidth(), h = image.getHeight();
        int i420Size = w * h * 3 / 2;
        int picel1 = ImageFormat.getBitsPerPixel(ImageFormat.NV21);
        int picel2 = ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888);

        Image.Plane[] planes = image.getPlanes();
        //remaining0 = rowStride*(h-1)+w => 27632= 192*143+176
        int remaining0 = planes[0].getBuffer().remaining();
        int remaining1 = planes[1].getBuffer().remaining();
        //remaining2 = rowStride*(h/2-1)+w-1 =>  13807=  192*71+176-1
        int remaining2 = planes[2].getBuffer().remaining();
        //获取pixelStride，可能跟width相等，可能不相等
        int pixelStride = planes[2].getPixelStride();
        int rowOffest = planes[2].getRowStride();
        byte[] nv21 = new byte[i420Size];
        byte[] yRawSrcBytes = new byte[remaining0];
        byte[] uRawSrcBytes = new byte[remaining1];
        byte[] vRawSrcBytes = new byte[remaining2];
        planes[0].getBuffer().get(yRawSrcBytes);
        planes[1].getBuffer().get(uRawSrcBytes);
        planes[2].getBuffer().get(vRawSrcBytes);
        if (pixelStride == w) {
            //两者相等，说明每个YUV块紧密相连，可以直接拷贝
            System.arraycopy(yRawSrcBytes, 0, nv21, 0, rowOffest * h);
            System.arraycopy(vRawSrcBytes, 0, nv21, rowOffest * h, rowOffest * h / 2 - 1);
        } else {
//            Log.d(TAG, "pixelStride else");
            byte[] ySrcBytes = new byte[w * h];
            byte[] uSrcBytes = new byte[w * h / 2 - 1];
            byte[] vSrcBytes = new byte[w * h / 2 - 1];
            for (int row = 0; row < h; row++) {
                //源数组每隔 rowOffest 个bytes 拷贝 w 个bytes到目标数组
                System.arraycopy(yRawSrcBytes, rowOffest * row, ySrcBytes, w * row, w);

                //y执行两次，uv执行一次
                if (row % 2 == 0) {
                    //最后一行需要减一
                    if (row == h - 2) {
                        System.arraycopy(vRawSrcBytes, rowOffest * row / 2, vSrcBytes, w * row / 2, w - 1);
                    } else {
                        System.arraycopy(vRawSrcBytes, rowOffest * row / 2, vSrcBytes, w * row / 2, w);
                    }
                }
            }
            System.arraycopy(ySrcBytes, 0, nv21, 0, w * h);
            System.arraycopy(vSrcBytes, 0, nv21, w * h, w * h / 2 - 1);
        }
        return nv21;
    }

    public static byte[] NV21toI420SemiPlanar(byte[] nv21bytes, int width, int height) {
        byte[] i420bytes = new byte[width * height * 3 / 2];
        System.arraycopy(nv21bytes, 0, i420bytes, 0, width * height);
        for (int i = width * height; i < nv21bytes.length; i += 2) {
            i420bytes[i] = nv21bytes[i + 1];
            i420bytes[i + 1] = nv21bytes[i];
        }
        return i420bytes;
    }

}