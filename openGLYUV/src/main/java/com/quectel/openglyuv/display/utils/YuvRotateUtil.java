package com.quectel.openglyuv.display.utils;

public class YuvRotateUtil {

    //旋转摄像头采集的NV21画面，得到推流画面，顺时针为正方向
    public static void rotateNV21(int face, byte[] src, byte[] reversedBytes, byte[] rotatedBytes, int width, int height, int rotation) {
        if (rotation == 90)
            rotateNV21_90(src, rotatedBytes, width, height);
        else if (rotation == 180)
            rotateNV21_180(src, rotatedBytes, width, height);
        else if (rotation == 270)
            rotateNV21_270(src, rotatedBytes, width, height);
        else
            rotateNV21_0(src, rotatedBytes, width, height);
    }

    //将NV21画面原样保留
    public static void rotateNV21_0(byte[] src, byte[] dst, int width, int height) {
        System.arraycopy(src, 0, dst, 0, src.length);
    }

    //将NV21画面顺时针旋转90角度
    public static void rotateNV21_90(byte[] src, byte[] dst, int width, int height) {

        //旋转90度后的像素排列：
        //新的行数=原来的列数
        //新的列数=原来的高度-1-原来的行数

        //每相邻的四个Y分量，共享一组VU分量
        //旋转前VU分量在左上角，旋转后VU分量在右上角

        int index = 0;
        //旋转Y分量，放入dst数组
        for (int y = 0; y < width; y++)
            for (int x = 0; x < height; x++) {
                int oldY = (height - 1) - x;
                int oldX = y;
                int oldIndex = oldY * width + oldX;
                dst[index++] = src[oldIndex];
            }
        //每四个点采集一组VU分量，共享右上角像素的VU分量
        //根据Y分量，找到对应的VU分量，放入dst数组
        for (int y = 0; y < width; y += 2)
            for (int x = 0; x < height; x += 2) {
                int oldY = (height - 1) - (x + 1);
                int oldX = y;
                int vuY = height + oldY / 2; //根据Y分量计算VU分量所在行
                int vuX = oldX;
                int vuIndex = vuY * width + vuX;
                dst[index++] = src[vuIndex];
                dst[index++] = src[vuIndex + 1];
            }
    }

    //将NV21画面顺时针旋转180角度
    public static void rotateNV21_180(byte[] src, byte[] dst, int width, int height) {

        //旋转180度后的像素排列：
        //新的行数=原来的高度-1-原来的行数
        //新的列数=原来的宽度-1-原来的列数

        //每相邻的四个Y分量，共享一组VU分量
        //旋转前VU分量在左上角，旋转后VU分量在右下角

        int index = 0;
        //旋转Y分量，放入dst数组
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++) {
                int oldY = (height - 1) - y;
                int oldX = (width - 1) - x;
                int oldIndex = oldY * width + oldX;
                dst[index++] = src[oldIndex];
            }
        //每四个点采集一组VU分量，共享右下角像素的VU分量
        //根据Y分量，找到对应的VU分量，放入dst数组
        for (int y = 0; y < height; y += 2)
            for (int x = 0; x < width; x += 2) {
                int oldY = (height - 1) - (y + 1);
                int oldX = (width - 1) - (x + 1);
                int vuY = height + oldY / 2; //根据Y分量计算VU分量所在行
                int vuX = oldX;
                int vuIndex = vuY * width + vuX;
                dst[index++] = src[vuIndex];
                dst[index++] = src[vuIndex + 1];
            }
    }

    //将NV21画面顺时针旋转270角度
    public static void rotateNV21_270(byte[] src, byte[] dst, int width, int height) {

        //旋转270度后的像素排列：
        //新的行数=原来的宽度-1-原来的列数
        //新的列数=原来的行数

        //每相邻的四个Y分量，共享一组VU分量
        //旋转前VU分量在左上角，旋转后VU分量在左下角

        int index = 0;
        //旋转Y分量，放入dst数组
        for (int y = 0; y < width; y++)
            for (int x = 0; x < height; x++) {
                int oldY = x;
                int oldX = width - 1 - y;
                int oldIndex = oldY * width + oldX;
                dst[index++] = src[oldIndex];
            }
        //每四个点采集一组VU分量，共享左下角像素的VU分量
        //根据Y分量，找到对应的VU分量，放入dst数组
        for (int y = 0; y < width; y += 2)
            for (int x = 0; x < height; x += 2) {
                int oldY = x;
                int oldX = width - 1 - (y + 1);
                int vuY = height + oldY / 2; //根据Y分量计算VU分量所在行
                int vuX = oldX;
                int vuIndex = vuY * width + vuX;
                dst[index++] = src[vuIndex];
                dst[index++] = src[vuIndex + 1];
            }
    }

    //将NV21画面水平翻转
    public static void reverseNV21_H(byte[] src, byte[] dst, int width, int height) {

        //水平翻转的像素排列：
        //新的行数=原来的行数
        //新的列数=原来的宽度-1-原来的列数

        //每相邻的四个Y分量，共享一组VU分量
        //翻转前VU分量在左上角，旋转后VU分量在右上角

        int index = 0;
        //旋转Y分量，放入dst数组
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++) {
                int oldY = y;
                int oldX = width - 1 - x;
                int oldIndex = oldY * width + oldX;
                dst[index++] = src[oldIndex];
            }
        //每四个点采集一组VU分量，共享右上角像素的VU分量
        //根据Y分量，找到对应的VU分量，放入dst数组
        for (int y = 0; y < height; y += 2)
            for (int x = 0; x < width; x += 2) {
                int oldY = y;
                int oldX = width - 1 - (x + 1);
                int vuY = height + oldY / 2; //根据Y分量计算VU分量所在行
                int vuX = oldX;
                int vuIndex = vuY * width + vuX;
                dst[index++] = src[vuIndex];
                dst[index++] = src[vuIndex + 1];
            }
    }

    //将NV21画面竖直翻转
    public static void reverseNV21_V(byte[] src, byte[] dst, int width, int height) {

        //竖直翻转的像素排列：
        //新的行数=原来的高度-1-原来的行数
        //新的列数=原来的列数

        //每相邻的四个Y分量，共享一组VU分量
        //翻转前VU分量在左上角，旋转后VU分量在左下角

        int index = 0;
        //旋转Y分量，放入dst数组
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++) {
                int oldY = height - 1 - y;
                int oldX = x;
                int oldIndex = oldY * width + oldX;
                dst[index++] = src[oldIndex];
            }
        //每四个点采集一组VU分量，共享左下角像素的VU分量
        //根据Y分量，找到对应的VU分量，放入dst数组
        for (int y = 0; y < height; y += 2)
            for (int x = 0; x < width; x += 2) {
                int oldY = height - 1 - (y + 1);
                int oldX = x;
                int vuY = height + oldY / 2; //根据Y分量计算VU分量所在行
                int vuX = oldX;
                int vuIndex = vuY * width + vuX;
                dst[index++] = src[vuIndex];
                dst[index++] = src[vuIndex + 1];
            }
    }


}
