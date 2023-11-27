package com.quectel.camera.display;

public class ImagerReaderUtil {
    /**
     * 非安卓标准格式的RAW10转RAW16，补充数据在末尾
     * @param src
     * @param width
     * @param height
     * @return 大端格式的raw16数据
     */
    public static byte[] transformRAW10ToRAW16(byte[] src, int width, int height) {
        if (src == null || src.length <= 0 || width <= 0 || height <= 0) {
            return null;
        }
        byte[] dst = new byte[width * height * 2];
        int rowStride = src.length / height;
        for (int i = 0; i < height; i++) {
            int k = 0, n = 0, dstStartIndex, srcStartIndex, fillByteStartIndex;
            for (int j = 0; j < width; j += 4) {
                srcStartIndex = i * width + j;
                dstStartIndex = (i * width + k) * 2;
                fillByteStartIndex = i * (rowStride - width) + n;
                if (dstStartIndex >= 0 && dstStartIndex + 7 < dst.length
                        && fillByteStartIndex >= 0 && fillByteStartIndex < src.length) {
                    /** Big_Endian ① */
                    dst[dstStartIndex] = src[srcStartIndex];
                    dst[dstStartIndex + 1] = (byte) ((src[fillByteStartIndex] << 6) & 0xc0);
                    dst[dstStartIndex + 2] = src[srcStartIndex + 1];
                    dst[dstStartIndex + 3] = (byte) ((src[fillByteStartIndex] << 4) & 0xc0);
                    dst[dstStartIndex + 4] = src[srcStartIndex + 2];
                    dst[dstStartIndex + 5] = (byte) ((src[fillByteStartIndex] << 2) & 0xc0);
                    dst[dstStartIndex + 6] = src[srcStartIndex + 3];
                    dst[dstStartIndex + 7] = (byte) (src[fillByteStartIndex] & 0xc0);
                }
                k+=4;
                n++;
            }

        }

        return dst;
    }

    // add by zzh 16位无符号灰度图数据，转化成argb数据
    public static int[] getIrARGB(byte[] irBytes, int irWidth, int irHeight) {
        int[] argbData = new int[irWidth * irHeight];
        for (int i = 0; i < irWidth * irHeight; i++) {
            byte[] shortBytes = new byte[]{irBytes[i * 2], irBytes[i * 2 + 1]};
            // 0-65535的灰度数据
            int shortData = getShort(shortBytes, false);
            // 0-255范围的灰度数据
            int grayData = ((shortData * 255) / 65535) & 0x000000ff;
            // R G B A
            argbData[i] = grayData | grayData << 8 | grayData << 16 | 0xff000000;
        }
        return argbData;
    }

    /**大端、小端方式，两字节数据转short值(有符号)**/
    public static short getShort(byte[] b, boolean isBigEdian) {
        if (b == null || b.length <= 0 || b.length > 2) {
            return 0;
        }
        if (isBigEdian) {
            return (short) (((b[1] << 8) | b[0] & 0xff));
        } else {
            return (short) (((b[1] & 0xff) | b[0] << 8));
        }
    }
}
