// Tencent is pleased to support the open source community by making ncnn available.
//
// Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
//
// Licensed under the BSD 3-Clause License (the "License"); you may not use this file except
// in compliance with the License. You may obtain a copy of the License at
//
// https://opensource.org/licenses/BSD-3-Clause
//
// Unless required by applicable law or agreed to in writing, software distributed
// under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
// CONDITIONS OF ANY KIND, either express or implied. See the License for the
// specific language governing permissions and limitations under the License.

#include <android/asset_manager_jni.h>
#include <android/native_window_jni.h>
#include <android/native_window.h>

#include <android/log.h>
#include <stdio.h>

#include <jni.h>
#include <sys/time.h>

#include <string>
#include <vector>

#include <platform.h>
#include <benchmark.h>

#include "yoloface.h"

#include "ndkcamera.h"

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/imgcodecs.hpp>

#if __ARM_NEON
#include <arm_neon.h>
#endif // __ARM_NEON

#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, "native-lib", __VA_ARGS__)


static int draw_unsupported(cv::Mat& rgb)
{
    return 0;
}

static int draw_fps(cv::Mat& rgb)
{
    return 0;
}

static YoloFace* g_yoloface = 0;
static ncnn::Mutex lock;

class MyNdkCamera : public NdkCamera
{
public:
    virtual void on_image_render(cv::Mat& rgb) const;
};

void MyNdkCamera::on_image_render(cv::Mat& rgb) const
{

}

static MyNdkCamera* g_camera = 0;

extern "C" {

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "JNI_OnLoad");

    g_camera = new MyNdkCamera;

    return JNI_VERSION_1_4;
}

JNIEXPORT void JNI_OnUnload(JavaVM* vm, void* reserved)
{
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "JNI_OnUnload");

    {
        ncnn::MutexLockGuard g(lock);

        delete g_yoloface;
        g_yoloface = 0;
    }

    delete g_camera;
    g_camera = 0;
}

// public native boolean loadModel(AssetManager mgr, int modelid, int cpugpu);
JNIEXPORT jboolean JNICALL Java_com_van_ncnn_NcnnYoloFace_loadModel(JNIEnv* env, jobject thiz, jobject assetManager, jint modelid, jint cpugpu)
{
    if (modelid < 0 || modelid > 6 || cpugpu < 0 || cpugpu > 1)
    {
        return JNI_FALSE;
    }
    LOGD("loadModel modlid=%d, cpugpu=%d", modelid, cpugpu);
    AAssetManager* mgr = AAssetManager_fromJava(env, assetManager);

    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "loadModel %p", mgr);

    const char* modeltypes[] =
    {
        "yolov5n-0.5",
    };

    const int target_sizes[] =
    {
        320,
    };

    const float mean_vals[][3] =
    {
        {127.f, 127.f, 127.f},
    };

    const float norm_vals[][3] =
    {
        {1 / 255.f, 1 / 255.f, 1 / 255.f},
    };

    const char* modeltype = modeltypes[(int)modelid];
    int target_size = target_sizes[(int)modelid];
    bool use_gpu = (int) cpugpu == 1;

    // reload
    {
        ncnn::MutexLockGuard g(lock);

        if (use_gpu && ncnn::get_gpu_count() == 0)
        {
            // no gpu
            delete g_yoloface;
            g_yoloface = 0;
            LOGD("人脸识别使用gpu");
        }
        else
        {
            if (!g_yoloface)
                g_yoloface = new YoloFace;
            g_yoloface->load(mgr, modeltype, target_size, mean_vals[(int)modelid], norm_vals[(int)modelid], use_gpu);
            LOGD("人脸识别使用cpu, use_gpu=%d", use_gpu);
        }
    }

    return JNI_TRUE;
}

// public native boolean openCamera(int facing);
JNIEXPORT jboolean JNICALL Java_com_van_ncnn_NcnnYoloFace_openCamera(JNIEnv* env, jobject thiz, jint facing)
{
    if (facing < 0 || facing > 1)
        return JNI_FALSE;

    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "openCamera %d", facing);

    g_camera->open((int)facing);

    return JNI_TRUE;
}

// public native boolean closeCamera();
JNIEXPORT jboolean JNICALL Java_com_van_ncnn_NcnnYoloFace_closeCamera(JNIEnv* env, jobject thiz)
{
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "closeCamera");

    g_camera->close();

    return JNI_TRUE;
}

// public native boolean setOutputWindow(Surface surface);
JNIEXPORT jboolean JNICALL Java_com_van_ncnn_NcnnYoloFace_setOutputWindow(JNIEnv* env, jobject thiz, jobject surface)
{
    ANativeWindow* win = ANativeWindow_fromSurface(env, surface);

    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "setOutputWindow %p", win);

//    g_camera->set_window(win);

    return JNI_TRUE;
}

}
int count ;
double  timeP;

extern "C"
JNIEXPORT jobject JNICALL
Java_com_van_ncnn_NcnnYoloFace_putYUVData(JNIEnv *env, jobject thiz, jbyteArray data_, jint cameraFacing_, jint cameraOrientation_, jint imgWidth,
                                          jint imgHeight, jobject buffer_) {
    if (g_camera == NULL)
        return NULL;

    jbyte * data    = env->GetByteArrayElements(data_, JNI_FALSE);
//    cv::Mat src(imgHeight + imgHeight / 2, imgWidth, CV_8UC1, data);
    g_camera->camera_facing         = cameraFacing_;
    g_camera->camera_orientation    = cameraOrientation_;
//    if (cameraFacing_ == 1) {
//        //前置摄像头，需要逆时针旋转90度
//        cv::rotate(src, src, cv::ROTATE_90_COUNTERCLOCKWISE);
//        //水平翻转 镜像
//        flip(src, src, 1);
//    } else {
//        //顺时针旋转90度
//        rotate(src, src, cv::ROTATE_90_CLOCKWISE);
//    }
//    LOGD("摄像头facing=%d, 偏转角度=%d, imgWidth=%d, imgHeight=%d", cameraFacing_, cameraOrientation_ , imgWidth, imgHeight);
    cv::Mat rgb = g_camera->on_image((unsigned char *)data, imgWidth, imgHeight);
    env->ReleaseByteArrayElements(data_, data, 0);
//    cv::Mat rgb(imgHeight/2, imgWidth/2, CV_8UC3);
//    ncnn::yuv420sp2rgb((unsigned char *)data, imgWidth, imgHeight, rgb.data);
//    ncnn::yuv420sp2rgb_half((unsigned char *)data, imgWidth, imgHeight, rgb.data);
//    FILE * file;
//    file = fopen("/sdcard/1111/ttt.yuv","w+");
//    if (file != NULL){
//        cv::imwrite("/sdcard/1111/ttt.png", rgb);
//        fwrite(data, imgWidth*imgHeight*3/2, 1 , file);
//        fclose(file);
//    }
    // nanodet
//    cv::Mat rgb = cv::imread("/sdcard/1111/rrr.jpg", 1);

    {
//        ncnn::MutexLockGuard g(lock);
        if (g_yoloface)
        {
            std::vector<Object> objects;
//            double tt1  = ncnn::get_current_time();
            g_yoloface->detect(rgb, objects);
//            double tt2  = ncnn::get_current_time();
//            timeP = (tt2 - tt1 + timeP);
//            count++;
//            LOGD("转换平均时间=%.0lf , 当前转换时间=%.0lf", (timeP/count), (tt2-tt1));

//                LOGD("11使用时间=%.2lf,rgb cols=%d , rgb rows=%d， 一共有%d个人脸, 人脸1的x=%f, y=%f, faceWidth=%f, faceHeight=%f", time,rgb.cols, rgb.rows, objects.size(), objects[0].rect.x, objects[0].rect.y, objects[0].rect.width, objects[0].rect.height);
//                LOGD("1脸宽=%d, 脸高=%d, x=%d， y=%d", objects[0].rect.width, objects[0].rect.height, objects[0].rect.x, objects[0].rect.y);

            jclass clazz = env->FindClass("com/van/opencv/Face");
            jmethodID costruct = env->GetMethodID(clazz, "<init>", "(I[F[F[F[FII)V");
            int size = objects.size();
            //创建java 的float 数组
            jfloatArray floatArrayFaceX = env->NewFloatArray(size);
            jfloatArray floatArrayFaceY = env->NewFloatArray(size);
            jfloatArray floatArrayFaceWidth = env->NewFloatArray(size);
            jfloatArray floatArrayFaceHeight = env->NewFloatArray(size);
            int rgbSize                 = rgb.cols * rgb.rows * 3;
//            jbyteArray byteArrayRGB     = env->NewByteArray(rgbSize);


            float faceX[size];
            float faceY[size];
            float faceWidth[size];
            float faceHeight[size];

            for (int j = 0; j < size; j++) {
                faceX[j]    = objects[j].rect.x;
                faceY[j]    = objects[j].rect.y;
                faceWidth[j] = objects[j].rect.width;
                faceHeight[j] = objects[j].rect.height;
//                    LOGD("2脸宽=%d, 脸高=%d, x=%d, y=%d", faceWidth, faceHeight, x, y);
            }

            env->SetFloatArrayRegion(floatArrayFaceX, 0, size, faceX);
            env->SetFloatArrayRegion(floatArrayFaceY, 0, size, faceY);
            env->SetFloatArrayRegion(floatArrayFaceWidth, 0, size, faceWidth);
            env->SetFloatArrayRegion(floatArrayFaceHeight, 0, size, faceHeight);

//            env->SetByteArrayRegion(byteArrayRGB, 0, rgbSize,
//                                    reinterpret_cast<const jbyte *>(rgb.data));


            jobject face = env->NewObject(clazz, costruct, size,  floatArrayFaceX, floatArrayFaceY, floatArrayFaceWidth, floatArrayFaceHeight, rgb.cols, rgb.rows);
//            jfieldID  rgbFieldid = env->GetFieldID(clazz, "rgbBuffer", "Ljava/nio/ByteBuffer;");
//            jobject rgbBuffer = env->GetObjectField(face, rgbFieldid);

//            char * dattt = new char[rgbSize];
//            jobject dataobj = env->NewDirectByteBuffer(dattt, rgbSize);
//            double tttt1 = ncnn::get_current_time();
//            char * ddd = static_cast<char *>(env->GetDirectBufferAddress(rgbBuffer));
//            char * ddd2 = static_cast<char *>(env->GetDirectBufferAddress(dataobj));
//            memcpy(ddd, rgb.data, rgbSize);
//            double tttt2 = ncnn::get_current_time();
//            LOGD("1访问java用时%.0lf, dattt = %p, ddd2 = %p", (tttt2-tttt1), dattt, ddd2);
//            delete[] dattt;

//            double tttt3 = ncnn::get_current_time();
            char * dddd = static_cast<char *>(env->GetDirectBufferAddress(buffer_));
            memcpy(dddd, rgb.data, rgbSize);
//            double tttt4 = ncnn::get_current_time();
//            LOGD("2访问java用时%.0lf", (tttt4-tttt3));

//            env->ReleaseByteArrayElements(rgb_obj, pData, 0);
            env->DeleteLocalRef(floatArrayFaceX);
            env->DeleteLocalRef(floatArrayFaceY);
            env->DeleteLocalRef(floatArrayFaceWidth);
            env->DeleteLocalRef(floatArrayFaceHeight);

            return face;
        }
    }
    return NULL;
}