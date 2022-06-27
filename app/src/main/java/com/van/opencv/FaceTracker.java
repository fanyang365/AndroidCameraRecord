package com.van.opencv;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import com.van.util.CameraHelper;

public class FaceTracker {
    private static final String TAG = "FaceTracker";

    static {
        System.loadLibrary("native-lib");
    }

    private Handler mHandler;
    private HandlerThread mHandlerThread;

    private long self;
    //结果
    public Face mFace;

    public FaceTracker(String model, String seeta) {
        self = native_create(model, seeta);
        mHandlerThread = new HandlerThread("track");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                //子线程 耗时再久 也不会对其他地方 (如：opengl绘制线程) 产生影响
                synchronized (FaceTracker.this) {
//                    long time1  = System.currentTimeMillis();
                    YUVData yuvData = (YUVData)msg.obj;
                    //定位 线程中检测
                    mFace = native_detector(self, yuvData.getData(),
                            yuvData.getCameraFacing(), yuvData.getWidth(),yuvData.getHeight());
//                    long time2  = System.currentTimeMillis();
//                    Log.d(TAG, "opencv 检测 time =" + (time2-time1));

                }
            }
        };
    }


    public void startTrack() {
        native_start(self);
    }

    public void stopTrack() {
        synchronized (this) {
            mHandlerThread.quitSafely();
            mHandler.removeCallbacksAndMessages(null);
            native_stop(self);
            self = 0;
        }
    }

    public void detector(YUVData data) {
        //把积压的 11号任务移除掉
        mHandler.removeMessages(11);
        //加入新的11号任务
        Message message = mHandler.obtainMessage(11);
        message.obj = data;
        mHandler.sendMessage(message);
    }

    //传入模型文件， 创建人脸识别追踪器和人眼定位器
    private native long native_create(String model, String seeta);

    //开始追踪
    private native void native_start(long self);

    //停止追踪
    private native void native_stop(long self);

    //检测人脸
    private native Face native_detector(long self, byte[] data, int cameraId, int width, int
            height);


}
