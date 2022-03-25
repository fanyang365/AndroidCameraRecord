package com.van.opengl;

import android.content.Context;
import android.media.MediaRecorder;
import android.opengl.EGLContext;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import androidx.annotation.RequiresApi;

public class OpenGLMediaRecorder  {

    private static final String TAG = "OpenGLMediaRecorder";
    private Context     mContext;
    private EGLContext  mGlContext;
    private int         mWidth;
    private int         mHeight;
    private MediaRecorder   mMediaRecorder;
    private EGLEnv      eglEnv;
    private Handler     mHandler;
    private boolean isStart;
    private Surface mSurface;

    public OpenGLMediaRecorder(Context context, EGLContext glContext, int width, int
            height) {
        mContext = context.getApplicationContext();
        mWidth = width;
        mHeight = height;
        mGlContext = glContext;
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public boolean startRecord(File file){
        try {
            mMediaRecorder  = new MediaRecorder();
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mMediaRecorder.setOutputFile(file.getAbsolutePath());
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mMediaRecorder.setVideoEncodingBitRate(4096*1024);
            mMediaRecorder.setVideoSize(mWidth, mHeight);
            mMediaRecorder.setVideoFrameRate(30);
            mMediaRecorder.setOrientationHint(0);
            // 准备、开始
            mMediaRecorder.prepare();
            Log.e(TAG, "开始录像1");
            mMediaRecorder.start();
            Log.e(TAG, "开始录像 2");
            mSurface    = mMediaRecorder.getSurface();

            Log.e(TAG, "开始录像 3");
            //創建OpenGL 的 環境
            HandlerThread handlerThread = new HandlerThread("recorder-gl");
            handlerThread.start();
            mHandler = new Handler(handlerThread.getLooper());

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    eglEnv = new EGLEnv(mContext,mGlContext, mSurface,mWidth, mHeight);
                    isStart = true;
                }
            });
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void reset(){
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mMediaRecorder.stop();
                mMediaRecorder.reset();
                //准备名字
                Date date = new Date();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmm");
                String format = dateFormat.format(date);
        format = "1";
                String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/hcvsdb";
                String name = path + "/" + format + ".mp4";
                try {
                    File file = new File(name);
                    if (file.exists()) {
                        file.delete();
                    }
                    mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                    mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
                    mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                    mMediaRecorder.setOutputFile(file.getAbsolutePath());
                    mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                    mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
                    mMediaRecorder.setVideoEncodingBitRate(4096 * 1024);
                    mMediaRecorder.setVideoSize(mWidth, mHeight);
                    mMediaRecorder.setVideoFrameRate(30);
                    mMediaRecorder.setOrientationHint(0);
                    // 准备、开始
                    mMediaRecorder.prepare();
                    Log.e(TAG, "开始录像1");
                    mMediaRecorder.start();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        });
    }


    public void stopRecord(){
        Log.d(TAG, "停止录像！");
        isStart = false;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mMediaRecorder.stop();
                mMediaRecorder.release();
                mMediaRecorder = null;
                eglEnv.release();
                eglEnv = null;
                mSurface = null;
                mHandler.getLooper().quitSafely();
                mHandler = null;
            }
        });
    }

    //    编码   textureId数据  并且编码
//byte[]
    public void fireFrame(final int textureId, final long timestamp) {
//        主动拉去openglfbo数据
        if (!isStart) {
            return;
        }
        //录制用的opengl已经和handler的线程绑定了 ，所以需要在这个线程中使用录制的opengl
        mHandler.post(new Runnable() {
            public void run() {
//                opengl   能 1  不能2  draw  ---》surface
                eglEnv.draw(textureId,timestamp);
            }
        });

    }
}
