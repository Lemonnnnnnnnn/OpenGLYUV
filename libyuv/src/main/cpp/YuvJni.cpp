#include <jni.h>
#include <string>
#include "libyuv.h"
#include <android/log.h>

#define TAG "YuvJni"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

//分别用来存储1420，1420缩放，I420旋转和镜像的数据
static jbyte *Src_i420_data;
static jbyte *Src_i420_data_scale;
static jbyte *Src_i420_data_rotate;

JNIEXPORT void JNI_OnUnload(JavaVM *jvm, void *reserved) {
    //进行释放
    free(Src_i420_data);
    free(Src_i420_data_scale);
    free(Src_i420_data_rotate);
}

void scaleI420(jbyte *src_i420_data, jint width, jint height, jbyte *dst_i420_data, jint dst_width,
               jint dst_height, jint mode) {

    jint src_i420_y_size = width * height;
    jint src_i420_u_size = (width >> 1) * (height >> 1);
    jbyte *src_i420_y_data = src_i420_data;
    jbyte *src_i420_u_data = src_i420_data + src_i420_y_size;
    jbyte *src_i420_v_data = src_i420_data + src_i420_y_size + src_i420_u_size;

    jint dst_i420_y_size = dst_width * dst_height;
    jint dst_i420_u_size = (dst_width >> 1) * (dst_height >> 1);
    jbyte *dst_i420_y_data = dst_i420_data;
    jbyte *dst_i420_u_data = dst_i420_data + dst_i420_y_size;
    jbyte *dst_i420_v_data = dst_i420_data + dst_i420_y_size + dst_i420_u_size;

    libyuv::I420Scale((const uint8 *) src_i420_y_data, width,
                      (const uint8 *) src_i420_u_data, width >> 1,
                      (const uint8 *) src_i420_v_data, width >> 1,
                      width, height,
                      (uint8 *) dst_i420_y_data, dst_width,
                      (uint8 *) dst_i420_u_data, dst_width >> 1,
                      (uint8 *) dst_i420_v_data, dst_width >> 1,
                      dst_width, dst_height,
                      (libyuv::FilterMode) mode);
}

void rotateI420(jbyte *src_i420_data, jint width, jint height, jbyte *dst_i420_data, jint degree) {
    jint src_i420_y_size = width * height;
    jint src_i420_u_size = (width >> 1) * (height >> 1);

    jbyte *src_i420_y_data = src_i420_data;
    jbyte *src_i420_u_data = src_i420_data + src_i420_y_size;
    jbyte *src_i420_v_data = src_i420_data + src_i420_y_size + src_i420_u_size;

    jbyte *dst_i420_y_data = dst_i420_data;
    jbyte *dst_i420_u_data = dst_i420_data + src_i420_y_size;
    jbyte *dst_i420_v_data = dst_i420_data + src_i420_y_size + src_i420_u_size;

    //要注意这里的width和height在旋转之后是相反的
    if (degree == libyuv::kRotate90 || degree == libyuv::kRotate270) {
        libyuv::I420Rotate((const uint8 *) src_i420_y_data, width,
                           (const uint8 *) src_i420_u_data, width >> 1,
                           (const uint8 *) src_i420_v_data, width >> 1,
                           (uint8 *) dst_i420_y_data, height,
                           (uint8 *) dst_i420_u_data, height >> 1,
                           (uint8 *) dst_i420_v_data, height >> 1,
                           width, height,
                           (libyuv::RotationMode) degree);
    }
}

void mirrorI420(jbyte *src_i420_data, jint width, jint height, jbyte *dst_i420_data) {
    jint src_i420_y_size = width * height;
    jint src_i420_u_size = (width >> 1) * (height >> 1);

    jbyte *src_i420_y_data = src_i420_data;
    jbyte *src_i420_u_data = src_i420_data + src_i420_y_size;
    jbyte *src_i420_v_data = src_i420_data + src_i420_y_size + src_i420_u_size;

    jbyte *dst_i420_y_data = dst_i420_data;
    jbyte *dst_i420_u_data = dst_i420_data + src_i420_y_size;
    jbyte *dst_i420_v_data = dst_i420_data + src_i420_y_size + src_i420_u_size;

    libyuv::I420Mirror((const uint8 *) src_i420_y_data, width,
                       (const uint8 *) src_i420_u_data, width >> 1,
                       (const uint8 *) src_i420_v_data, width >> 1,
                       (uint8 *) dst_i420_y_data, width,
                       (uint8 *) dst_i420_u_data, width >> 1,
                       (uint8 *) dst_i420_v_data, width >> 1,
                       width, height);
}


void nv21ToI420(jbyte *src_nv21_data, jint width, jint height, jbyte *src_i420_data) {
    jint src_y_size = width * height;
    jint src_u_size = (width >> 1) * (height >> 1);

    jbyte *src_nv21_y_data = src_nv21_data;
    jbyte *src_nv21_vu_data = src_nv21_data + src_y_size;

    jbyte *src_i420_y_data = src_i420_data;
    jbyte *src_i420_u_data = src_i420_data + src_y_size;
    jbyte *src_i420_v_data = src_i420_data + src_y_size + src_u_size;


    libyuv::NV21ToI422((const uint8 *) src_nv21_y_data, width,
                       (const uint8 *) src_nv21_vu_data, width,
                       (uint8 *) src_i420_y_data, width,
                       (uint8 *) src_i420_u_data, width >> 1,
                       (uint8 *) src_i420_v_data, width >> 1,
                       width, height);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_libyuv_util_YuvUtil_init(JNIEnv *env, jclass type, jint width, jint height, jint dst_width,
                                  jint dst_height) {
    Src_i420_data = (jbyte *) malloc(sizeof(jbyte) * width * height * 3 / 2);
    Src_i420_data_scale = (jbyte *) malloc(sizeof(jbyte) * dst_width * dst_height * 3 / 2);
    Src_i420_data_rotate = (jbyte *) malloc(sizeof(jbyte) * dst_width * dst_height * 3 / 2);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_libyuv_util_YuvUtil_compressYUV(JNIEnv *env, jclass type,
                                         jbyteArray src_, jint width,
                                         jint height, jbyteArray dst_,
                                         jint dst_width, jint dst_height,
                                         jint mode, jint degree,
                                         jboolean isMirror) {
//    LOGD("%s called @line %d started\n", __func__, __LINE__);
    jbyte *Src_data = env->GetByteArrayElements(src_, NULL);
    jbyte *Dst_data = env->GetByteArrayElements(dst_, NULL);
    //nv21转化为i420
    nv21ToI420(Src_data, width, height, Src_i420_data);
    //进行缩放的操作
    scaleI420(Src_i420_data, width, height, Src_i420_data_scale, dst_width, dst_height, mode);
    if (isMirror) {
        //进行旋转的操作
        rotateI420(Src_i420_data_scale, dst_width, dst_height, Src_i420_data_rotate, degree);
        //因为旋转的角度都是90和270，那后面的数据width和height是相反的
        mirrorI420(Src_i420_data_rotate, dst_height, dst_width, Dst_data);
    } else {
        rotateI420(Src_i420_data_scale, dst_width, dst_height, Dst_data, degree);
    }
    env->ReleaseByteArrayElements(dst_, Dst_data, 0);
//    LOGD("%s called @line %d done\n", __func__, __LINE__);
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_libyuv_util_YuvUtil_NV21toI420SemiPlanar(JNIEnv *env, jclass type, jbyteArray inputArray,
                                                  jint width, jint height) {
    jbyte *input = env->GetByteArrayElements(inputArray, NULL);


    int framesize = width * height;
//    int i = 0, j = 0;
//    jbyte temp;
//
//    for (j = 0; j < framesize / 2; j += 2) {
//        temp = input[j + framesize - 1];
//        input[framesize + j - 1] = input[j + framesize];
//        input[j + framesize] = temp;
//    }

    jbyteArray it = env->NewByteArray(framesize * 3 / 2);
    jbyte *out = env ->GetByteArrayElements(it,NULL);

    nv21ToI420(input,width,height,out);
    env->SetByteArrayRegion(it, 0, framesize * 3 / 2, out);
    env->ReleaseByteArrayElements(inputArray, input, 0);
    return it;

}

extern "C"
JNIEXPORT void JNICALL
Java_com_libyuv_util_YuvUtil_cropYUV(JNIEnv *env, jclass type, jbyteArray src_, jint width,
                                     jint height, jbyteArray dst_, jint dst_width, jint dst_height,
                                     jint left, jint top) {
    //裁剪的区域大小不对
    if (left + dst_width > width || top + dst_height > height) {
        return;
    }

    //left和top必须为偶数，否则显示会有问题
    if (left % 2 != 0 || top % 2 != 0) {
        return;
    }

    jint src_length = env->GetArrayLength(src_);
    jbyte *src_i420_data = env->GetByteArrayElements(src_, NULL);
    jbyte *dst_i420_data = env->GetByteArrayElements(dst_, NULL);


    jint dst_i420_y_size = dst_width * dst_height;
    jint dst_i420_u_size = (dst_width >> 1) * (dst_height >> 1);

    jbyte *dst_i420_y_data = dst_i420_data;
    jbyte *dst_i420_u_data = dst_i420_data + dst_i420_y_size;
    jbyte *dst_i420_v_data = dst_i420_data + dst_i420_y_size + dst_i420_u_size;

    libyuv::ConvertToI420((const uint8 *) src_i420_data, src_length,
                          (uint8 *) dst_i420_y_data, dst_width,
                          (uint8 *) dst_i420_u_data, dst_width >> 1,
                          (uint8 *) dst_i420_v_data, dst_width >> 1,
                          left, top,
                          width, height,
                          dst_width, dst_height,
                          libyuv::kRotate0, libyuv::FOURCC_I420);

    env->ReleaseByteArrayElements(dst_, dst_i420_data, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_libyuv_util_YuvUtil_yuvI420ToNV21(JNIEnv *env, jclass type, jbyteArray i420Src,
                                           jbyteArray nv21Src,
                                           jint width, jint height) {

    jbyte *src_i420_data = env->GetByteArrayElements(i420Src, NULL);
    jbyte *src_nv21_data = env->GetByteArrayElements(nv21Src, NULL);

    jint src_y_size = width * height;
    jint src_u_size = (width >> 1) * (height >> 1);

    jbyte *src_i420_y_data = src_i420_data;
    jbyte *src_i420_u_data = src_i420_data + src_y_size;
    jbyte *src_i420_v_data = src_i420_data + src_y_size + src_u_size;

    jbyte *src_nv21_y_data = src_nv21_data;
    jbyte *src_nv21_vu_data = src_nv21_data + src_y_size;


    libyuv::I420ToNV21(
            (const uint8 *) src_i420_y_data, width,
            (const uint8 *) src_i420_u_data, width >> 1,
            (const uint8 *) src_i420_v_data, width >> 1,
            (uint8 *) src_nv21_y_data, width,
            (uint8 *) src_nv21_vu_data, width,
            width, height);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_libyuv_util_YuvUtil_Yuv420Scale(JNIEnv *env, jclass type,
                                         jbyteArray _src, jint _yuvFormat, jint Image_Width,
                                         jint Image_Height, jbyteArray _dst, jint DST_Width,
                                         jint DST_Height) {

    jbyte *srcBytes, *dstBytes;
    srcBytes = env->GetByteArrayElements(_src, 0);

    dstBytes = env->GetByteArrayElements(_dst, 0);

    //clock_t t1 = clock();
    const int y_length = Image_Width * Image_Height;
    const int uv_stride = (Image_Width >> 1) * (Image_Height >> 1);
    unsigned char *YUV_Data = (unsigned char *) malloc(Image_Width * Image_Height * 3 / 2);
    unsigned char *Y_Data = YUV_Data;
    unsigned char *U_Data = Y_Data + y_length;
    unsigned char *V_Data = U_Data + uv_stride;

    const int y_length_DST = DST_Width * DST_Height;
    const int uv2_stride = (DST_Width >> 1) * (DST_Height >> 1);
    unsigned char *Dst_Data = (unsigned char *) malloc(DST_Width * DST_Height * 3 / 2);
    unsigned char *YDst_Data = Dst_Data;
    unsigned char *UDst_Data = YDst_Data + y_length_DST;
    unsigned char *VDst_Data = UDst_Data + uv2_stride;

    unsigned char *NV12_Data = (unsigned char *) srcBytes;
    unsigned char *Y_NV12_Data = NV12_Data;
    unsigned char *UV_NV12_Data = Y_NV12_Data + y_length;

    unsigned char *Y_NV12Dst_Data = (unsigned char *) dstBytes;
    unsigned char *UV_NV12Dst_Data = Y_NV12Dst_Data + y_length_DST;
    int filter = 0;

    if (_yuvFormat == 0) {
        libyuv::NV12ToI420(Y_NV12_Data, Image_Width,
                           UV_NV12_Data, Image_Width,
                           Y_Data, Image_Width,
                           U_Data, Image_Width >> 1,
                           V_Data, Image_Width >> 1,
                           Image_Width, Image_Height);
    } else {
        libyuv::NV21ToI420(Y_NV12_Data, Image_Width,
                           UV_NV12_Data, Image_Width,
                           Y_Data, Image_Width,
                           U_Data, Image_Width >> 1,
                           V_Data, Image_Width >> 1,
                           Image_Width, Image_Height);
    }

    libyuv::I420Scale(Y_Data, Image_Width,
                      U_Data, Image_Width >> 1,
                      V_Data, Image_Width >> 1,
                      Image_Width, Image_Height,
                      YDst_Data, DST_Width,
                      UDst_Data, DST_Width >> 1,
                      VDst_Data, DST_Width >> 1,
                      DST_Width, DST_Height,
                      static_cast<libyuv::FilterMode>(filter));

    if (_yuvFormat == 0) {
        libyuv::I420ToNV12(YDst_Data, DST_Width,
                           UDst_Data, DST_Width >> 1,
                           VDst_Data, DST_Width >> 1,
                           Y_NV12Dst_Data, DST_Width,
                           UV_NV12Dst_Data, DST_Width,
                           DST_Width, DST_Height);
    } else {
        libyuv::I420ToNV21(YDst_Data, DST_Width,
                           UDst_Data, DST_Width >> 1,
                           VDst_Data, DST_Width >> 1,
                           Y_NV12Dst_Data, DST_Width,
                           UV_NV12Dst_Data, DST_Width,
                           DST_Width, DST_Height);
    }
    free(YUV_Data);
    free(Dst_Data);
#ifdef LIBYUV_DEBUG
    clock_t t2 = clock();
    double endtime = (double)(t2 - t1) / CLOCKS_PER_SEC;
    std::cout << "TotalTime:" << endtime << "s" << std::endl;
    FILE* fpp = fopen("/sdcard/scale_vga.yuv", "w");
    fwrite(_dst, 1, DST_Width * DST_Height * 3 / 2, fpp);
    fclose(fpp);
#endif

}





