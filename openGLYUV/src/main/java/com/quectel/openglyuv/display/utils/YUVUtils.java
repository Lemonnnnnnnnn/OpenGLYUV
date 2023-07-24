package com.quectel.openglyuv.display.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class YUVUtils {
    private final static String TAG = "QWatermarkUtils";

    public static void overlayBitmapOnYuv(byte[] yuvData, int width, int height, Bitmap bitmap, int x, int y) {
        // 将yuv数据转换为RGB格式
        int[] pixels = new int[width * height];
        YuvImage yuvImage = new YuvImage(yuvData, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, outputStream);
        byte[] jpegData = outputStream.toByteArray();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap rgbBitmap = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length, options);
        rgbBitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        rgbBitmap = rgbBitmap.copy(Bitmap.Config.ARGB_8888,true);
        // 在RGB数据上绘制半透明的Bitmap
        Canvas canvas = new Canvas(rgbBitmap);
        Paint paint = new Paint();
        paint.setAlpha(128);
        canvas.drawBitmap(bitmap, x, y, paint);

        // 将RGB数据转换为yuv格式
        int[] yuv = new int[width * height];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int pixel = pixels[i * width + j];
                int r = (pixel >> 16) & 0xff;
                int g = (pixel >> 8) & 0xff;
                int b = pixel & 0xff;
                yuv[i * width + j] = ((66 * r + 129 * g + 25 * b + 128) >> 8) + 16;
                if (j % 2 == 0 && i % 2 == 0) {
                    int u = ((-38 * r - 74 * g + 112 * b + 128) >> 8) + 128;
                    int v = ((112 * r - 94 * g - 18 * b + 128) >> 8) + 128;
                    yuv[width * height + (i / 2) * (width / 2) + (j / 2)] = v;
                    yuv[width * height + width * height / 4 + (i / 2) * (width / 2) + (j / 2)] = u;
                }
            }
        }

        // 将叠加了Bitmap的yuv数据写回原始yuv数据
        System.arraycopy(yuv, 0, yuvData, 0, yuvData.length);
    }
    //缩小图片到制定长宽
    public static Bitmap scaleImage(Bitmap bm, int newWidth, int newHeight) {
        if (bm == null) {
            return null;
        }
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap newbm = Bitmap.createBitmap(bm, 0, 0, width, height, matrix,
                true);
        if (!bm.isRecycled()) {
            bm.recycle();
        }
        return newbm;
    }
    public static byte[] getYUVByBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int size = width * height;

        int[] pixels = new int[size];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        return rgb2YCbCr420(pixels, width, height);
    }

    public static byte[] rgb2YCbCr420(int[] pixels, int width, int height) {
        int len = width * height;
        // yuv格式数组大小，y亮度占len长度，u,v各占len/4长度。
        byte[] yuv = new byte[len * 3 / 2];
        int y, u, v;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                // 屏蔽ARGB的透明度值
                int a = pixels[i * width + j] >>> 24;
                int rgb = pixels[i * width + j] & 0x00FFFFFF;
                // 像素的颜色顺序为bgr，移位运算。
                int r = rgb & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = (rgb >> 16) & 0xFF;
                // 套用公式
                y = ((66 * r + 129 * g + 25 * b + 128) >> 8) + 16;
                u = ((-38 * r - 74 * g + 112 * b + 128) >> 8) + 128;
                v = ((112 * r - 94 * g - 18 * b + 128) >> 8) + 128;
                // 赋值
                if (a == 0){
                    yuv[i * width + j] = 0;
                    yuv[len + (i >> 1) * width + (j & ~1)] = 0;
                    yuv[len + (i >> 1) * width + (j & ~1) + 1] = 0;
                } else {
                    yuv[i * width + j] = (byte) y;
                    yuv[len + (i >> 1) * width + (j & ~1)] = (byte) u;
                    yuv[len + (i >> 1) * width + (j & ~1) + 1] = (byte) v;
                }

            }
        }
        return yuv;
    }
    public Bitmap rawByteArray2RGBABitmap2(byte[] data, int width, int height) {
        int frameSize = width * height;
        int[] rgba = new int[frameSize];
        for (int i = 0; i < height; i++)
            for (int j = 0; j < width; j++) {
                int y = (0xff & ((int) data[i * width + j]));
                int u = (0xff & ((int) data[frameSize + (i >> 1) * width + (j & ~1) + 0]));
                int v = (0xff & ((int) data[frameSize + (i >> 1) * width + (j & ~1) + 1]));
                y = y < 16 ? 16 : y;
                int r = Math.round(1.164f * (y - 16) + 1.596f * (v - 128));
                int g = Math.round(1.164f * (y - 16) - 0.813f * (v - 128) - 0.391f * (u - 128));
                int b = Math.round(1.164f * (y - 16) + 2.018f * (u - 128));
                r = r < 0 ? 0 : (r > 255 ? 255 : r);
                g = g < 0 ? 0 : (g > 255 ? 255 : g);
                b = b < 0 ? 0 : (b > 255 ? 255 : b);
                rgba[i * width + j] = 0xff000000 + (b << 16) + (g << 8) + r;
            }
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bmp.setPixels(rgba, 0 , width, 0, 0, width, height);
        return bmp;
    }


    public static Bitmap text2bitmap(String text) {
        Bitmap bitmap = Bitmap.createBitmap(300, 50, Bitmap.Config.ARGB_8888);
//        bitmap.eraseColor(Color.RED);
        //创建画布对象
        Canvas canvas = new Canvas(bitmap);
        //绘制文字
        Paint paint = new Paint();
        // 防锯齿
        paint.setAntiAlias(true);
        paint.setColor(Color.RED);
//        paint.setAlpha(128);
        //设置文本的对齐方式
        paint.setTextAlign(Paint.Align.LEFT);
        int sp = 10;
        //设置文本大小，单位是 px，这个和我们平时使用的字体单位 sp 不同，所以最好进行转换。
        paint.setTextSize(sp);
        canvas.drawText(text, 10, 10, paint);
        return bitmap;
    }

    public static Bitmap text2bitmap(String text, int size) {
        Paint paint = new Paint();
        // 防锯齿
        paint.setAntiAlias(true);
        paint.setColor(Color.WHITE);
        //设置文本的对齐方式
        paint.setTextAlign(Paint.Align.LEFT);
        //设置文本大小，单位是 px，这个和我们平时使用的字体单位 sp 不同，所以最好进行转换。
        paint.setTextSize(size);
//        int bitmapWidth = Math.round(paint.measureText(text));
//        Log.d(TAG,"bitmapWidth =" + bitmapWidth);
        Bitmap bitmap = Bitmap.createBitmap(500, 60, Bitmap.Config.ARGB_8888);
//        bitmap.eraseColor(Color.RED);
        //创建画布对象
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.RED);
        //绘制文字
        canvas.drawText(text, 10, 40, paint);
        return bitmap;
    }

    public static byte[] getNv21FromImage(Image image) {
        int w = image.getWidth();
        int h = image.getHeight();
        int ySize = w * h;
        int uvSize = w * h / 2;
        byte[] yByte = new byte[ySize];
        byte[] uvByte = new byte[uvSize];
        byte[] nv21Byte = new byte[uvSize + ySize];

        // 01 get y && uv buffer
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        ByteBuffer buffer2 = image.getPlanes()[2].getBuffer();

        // 02 get y && uv bytes
        buffer.get(yByte, 0, w * h);
        buffer2.get(uvByte, 0, w * h / 2 - 1);

        // 03 merge yuv
        System.arraycopy(yByte, 0, nv21Byte, 0, ySize); /* y */
        System.arraycopy(uvByte, 0, nv21Byte, ySize, uvSize); /* uv */
        return nv21Byte;
    }

    /**
     * 叠图
     * 调用完成后改方法后，直接使用传入的nv21 数据即可。
     *
     * @param nv21_src      叠图最下面的图的nv21数据，大小要比被叠图的nv21数据大
     * @param width         最下面叠图的nv21数据的宽
     * @param height        最下面叠图的nv21数据的高
     * @param left          叠图起始左边位置
     * @param top           叠图起始的上边位置
     * @param overlayNv21   小图的nv21数据
     * @param overlayWidth  小图的宽
     * @param overlayHeight 小图的高
     */
    public static void overlayNV21(byte[] nv21_src, int width, int height, int left, int top, byte[] overlayNv21, int overlayWidth, int overlayHeight) {
        if (nv21_src.length != width * height * 3 / 2) {
            return;
        }
        if (overlayNv21.length != overlayWidth * overlayHeight * 3 / 2) {
            return;
        }
        for (int i = 0; i < overlayWidth; i++) {
            for (int j = 0; j < overlayHeight; j++) {
                byte b = overlayNv21[j * overlayWidth + i];
                if (b == 0x0) {
                    continue;
                }
                nv21_src[(j + top) * width + (i + left)] = b;
//                if (nv21_src[(j + top) * width + i + left] >= 0 && nv21_src[(j + top) * width + (i + top)] <= 0x70) {
//                    nv21_src[(j + top) * width + i + left] = (byte) 0xff;
//                } else {
//                    nv21_src[(j + top) * width + i + left] = 0;
//                }
            }
        }
        for (int i = 0; i < overlayWidth; i++) {
            for (int j = 0; j < (overlayHeight / 2); j++) {
                byte b = overlayNv21[j * overlayWidth + i + overlayWidth * overlayHeight];
                if (b == (byte) 0x0) {
                    continue;
                }
                nv21_src[(j + top / 2) * width + (i + left) + width * height] = b;
            }
        }
    }


    public static void overlayNV21New(byte[] nv21_src, int src_width, int src_height, int left, int top, byte[] overlayNv21, int overlayWidth, int overlayHeight) {
        if (nv21_src.length != src_width * src_height * 3 / 2) {
            return;
        }
        if (overlayNv21.length != overlayWidth * overlayHeight * 3 / 2) {
            return;
        }
        int originalOverlayWidth = overlayWidth;
        int originalOverlayHeight = overlayHeight;
        if (overlayWidth + left > src_width) {
            //不符合要求，进行二次剪裁
            overlayWidth = src_width - left;
        }
        if (overlayHeight + top > src_height) {
            //不符合要求，进行二次剪裁
            overlayHeight = src_height - top;
        }
        //确保为偶数
        left &= ~1;
        top &= ~1;
        overlayWidth &= ~1;
        overlayHeight &= ~1;

//        for (int i = 0; i < overlayWidth; i++) {
//            for (int j = 0; j < overlayHeight; j++) {
//                byte b = overlayNv21[j * overlayWidth + i];
//                if (b == 0x10) {
//                    continue;
//                }
//                if (nv21_src[j * src_width + i] >= 0 && nv21_src[j * src_width + i] <= 0x60) {
//                    nv21_src[j * src_width + i] = (byte) 0xff;
//                } else {
//                    nv21_src[j * src_width + i] = 0;
//                }
//            }
//        }
        for (int i = 0; i < overlayWidth; i++) {
            for (int j = 0; j < overlayHeight / 2; j++) {
                byte b = overlayNv21[j * overlayWidth + i + overlayWidth * overlayHeight];
                if (b == (byte) 0x80 || b == (byte) 0x10 || b == (byte) 0xeb) {
                    continue;
                }
                nv21_src[j * src_width + i + src_width * src_height] = b;
            }
        }
    }

}
