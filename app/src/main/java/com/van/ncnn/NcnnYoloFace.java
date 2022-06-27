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

package com.van.ncnn;

import android.content.res.AssetManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.view.Surface;

import com.van.opencv.Face;
import com.van.opencv.YUVData;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class NcnnYoloFace
{

    private static final String TAG = "NcnnYoloFace";
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private long self;

    public native boolean loadModel(AssetManager mgr, int modelid, int cpugpu);
//    public native boolean openCamera(int facing);
//    public native boolean closeCamera();
    public native boolean setOutputWindow(Surface surface);
    private native Face putYUVData(byte[] data,int cameraFacing, int cameraOritation, int width, int height, ByteBuffer buffer);

    //结果
    public Face mFace;

    private int count ;
    private long time;
    private ByteBuffer buffer;

    public NcnnYoloFace() {
        mHandlerThread = new HandlerThread("track");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                //子线程 耗时再久 也不会对其他地方 (如：opengl绘制线程) 产生影响
//                synchronized (NcnnYoloFace.this) {
                    long time1  = System.currentTimeMillis();
                    if (msg.what == 11){
                        YUVData yuvData = (YUVData)msg.obj;
                        //定位 线程中检测
                        if (buffer == null)
                            buffer =  ByteBuffer.allocateDirect(yuvData.getWidth() * yuvData.getHeight() * 3).order(ByteOrder.nativeOrder());
                        mFace = putYUVData(yuvData.getData(), yuvData.getCameraFacing(), yuvData.getCameraOrientation(), yuvData.getWidth(),yuvData.getHeight(), buffer);
                        mFace.rgbBuffer = buffer;
//                    }
//
                    long time2  = System.currentTimeMillis();
                    time = (time2 - time1 + time);
                    count++;
//                    if ((time2 - time1) > 25){
//                        try {
//                            Thread.sleep(30);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                    Log.d(TAG, "opencv 检测 平均time =" + time / count);

                }
            }
        };
    }

    static {
        System.loadLibrary("ncnnyoloface");
    }

    public void stopTrack() {
        synchronized (this) {
            mHandlerThread.quitSafely();
            mHandler.removeCallbacksAndMessages(null);
//            native_stop(self);
            self = 0;
        }
    }

    private boolean useThread   = true;

    public void detector(YUVData yuvData) {
        if (useThread){
            //把积压的 11号任务移除掉
            mHandler.removeMessages(11);
            //加入新的11号任务
            Message message = mHandler.obtainMessage(11);
            message.obj = yuvData;
            mHandler.sendMessage(message);
        }else {
            if (buffer == null)
                buffer =  ByteBuffer.allocateDirect(yuvData.getWidth() * yuvData.getHeight() * 3).order(ByteOrder.nativeOrder());
            mFace = putYUVData(yuvData.getData(), yuvData.getCameraFacing(), yuvData.getCameraOrientation(), yuvData.getWidth(),yuvData.getHeight(), buffer);
            mFace.rgbBuffer = buffer;
        }
    }

    public Face detector2(YUVData yuvData){
        if (useThread){
            //把积压的 11号任务移除掉
            mHandler.removeMessages(11);
            //加入新的11号任务
            Message message = mHandler.obtainMessage(11);
            message.obj = yuvData;
            mHandler.sendMessage(message);
        }else {
            if (buffer == null)
                buffer =  ByteBuffer.allocateDirect(yuvData.getWidth() * yuvData.getHeight() * 3).order(ByteOrder.nativeOrder());
            mFace = putYUVData(yuvData.getData(), yuvData.getCameraFacing(), yuvData.getCameraOrientation(), yuvData.getWidth(),yuvData.getHeight(), buffer);
            mFace.rgbBuffer = buffer;
        }
        return mFace;
    }
}
