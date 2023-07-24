package com.quectel.openglyuv.display.utils;

import android.graphics.Bitmap;

public class BitmapOverlay {
    
    // 将Bitmap叠加在YUV图像上
    public static byte[] overlayBitmapOnYUV(byte[] yuvData, int width, int height, Bitmap overlayBitmap,int x ,int y) {
        // 将Bitmap转换为ARGB_8888格式
        Bitmap argbBitmap = overlayBitmap.copy(Bitmap.Config.ARGB_8888, true);
        int bitmapWidth = argbBitmap.getWidth();
        int bitmapHeight = argbBitmap.getHeight();
        
        // 将YUV数据转换为RGB数据
        int[] rgbData = new int[width * height];
        decodeYUV420SP(rgbData, yuvData, width, height);
        
        // 将Bitmap覆盖到RGB数据上
        int[] argbData = new int[bitmapWidth * bitmapHeight];
        argbBitmap.getPixels(argbData, 0, bitmapWidth, x, y, bitmapWidth, bitmapHeight);
        int argbIndex = 0;
        for (int i = 0; i < rgbData.length; i++) {
            int a = (argbData[argbIndex] >> 24) & 0xff;
            int r = (argbData[argbIndex] >> 16) & 0xff;
            int g = (argbData[argbIndex] >> 8) & 0xff;
            int b = argbData[argbIndex] & 0xff;
            rgbData[i] = (a << 24) | (r << 16) | (g << 8) | b;
            argbIndex++;
            if (argbIndex >= argbData.length) {
                argbIndex = 0;
            }
        }
        
        // 将RGB数据转换为YUV数据
        byte[] outputData = new byte[width * height * 3 / 2];
        encodeYUV420SP(outputData, width, height, rgbData);
        
        return outputData;
    }

    /**
     * 将YUV420数据解码为RGB8888数据
     * @param rgbData 存储解码后的RGB数据
     * @param yuv420 待解码的YUV420数据
     * @param width 图像宽度
     * @param height 图像高度
     */
    private static void decodeYUV420SP(int[] rgbData, byte[] yuv420, int width, int height) {
        final int frameSize = width * height;

        // YUV数据中Y分量的索引位置
        int yIndex = 0;
        // YUV数据中U分量的索引位置
        int uvIndex = frameSize;

        // YUV数据中UV分量的步长
        int uvStep = (width + 1) / 2;
        // 解码后的RGB数据中每一行数据的右边界
        int rowStride = width << 1;

        for (int j = 0; j < height; j++) {
            int rgbIndex = j * rowStride;
            int y = (yuv420[yIndex] & 0xff) - 16;
            if (y < 0) {
                y = 0;
            }
            for (int i = 0; i < width; i++) {
                int uvp = uvIndex + (i >> 1) / uvStep;
                int vwp = uvp + (uvIndex >> 1) / uvStep;
                int u = (yuv420[uvp] & 0xff) - 128;
                int v = (yuv420[vwp] & 0xff) - 128;
                int r = (int) (y + 1.402f * v);
                int g = (int) (y - 0.344f * u - 0.714f * v);
                int b = (int) (y + 1.772f * u);

                if (r < 0) {
                    r = 0;
                } else if (r > 255) {
                    r = 255;
                }
                if (g < 0) {
                    g = 0;
                } else if (g > 255) {
                    g = 255;
                }
                if (b < 0) {
                    b = 0;
                } else if (b > 255) {
                    b = 255;
                }
                rgbData[rgbIndex++] = 0xff000000 | (r << 16) | (g << 8) | b;
                yIndex++;
            }
        }
    }

    /**
     * 将RGB8888数据编码为YUV420数据
     * @param outputData 存储编码后的YUV数据
     * @param width 图像宽度
     * @param height 图像高度
     * @param rgbData 待编码的RGB数据
     */
    private static void encodeYUV420SP(byte[] outputData, int width, int height, int[] rgbData) {
        final int frameSize = width * height;

        // YUV数据中Y分量的索引位置
        int yIndex = 0;
        // YUV数据中UV分量的索引位置
        int uvIndex = frameSize;

        // YUV数据中UV分量的步长
        int uvStep = (width + 1) / 2;
        // RGB数据中每一行数据的右边界
        int rowStride = width << 1;

        for (int j = 0; j < height; j++) {
            int rgbIndex = j * rowStride;
            for (int i = 0; i < width; i++) {
                int rgb = rgbData[rgbIndex];
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;

                // 计算Y分量
                int y = (int) (0.299f * r + 0.587f * g + 0.114f * b);
                if (y < 0) {
                    y = 0;
                } else if (y > 255) {
                    y = 255;
                }
                outputData[yIndex++] = (byte) y;

                if (j % 2 == 0) {
                    if (i % 2 == 0) {
                        // 计算U分量
                        int u = (int) (-0.169f * r - 0.331f * g + 0.5f * b + 128);
                        if (u < 0) {
                            u = 0;
                        } else if (u > 255) {
                            u = 255;
                        }
                        outputData[uvIndex++] = (byte) u;
                    }
                } else {
                    if (i % 2 != 0) {
                        // 计算V分量
                        int v = (int) (0.5f * r - 0.419f * g - 0.081f * b + 128);
                        if (v < 0) {
                            v = 0;
                        } else if (v > 255) {
                            v = 255;
                        }
                        outputData[uvIndex++] = (byte) v;
                    }
                }
                rgbIndex++;
            }
        }
    }
}