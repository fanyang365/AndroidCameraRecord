package com.van.opengl;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.opengl.EGLContext;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;

import com.van.util.WriteFileUtil;

import java.io.IOException;
import java.nio.ByteBuffer;

/*Opengl h264 编码器*/
public class OpenGLAVCEncoder {

    private static final String TAG = "OpenGLAVCEncoder";
    private Context     mContext;
    private EGLContext  mGlContext;
    private int         mWidth;
    private int         mHeight;
    /*码率*/
    private int         mBitrate    = 2480;
    /*I帧间隔*/
    private int         mIFrameInterval    = 3;
    /*帧率*/
    private int         mKeyFramRate    = 25;
    private EGLEnv      eglEnv;
    private MediaCodec  mMediaCodec;
    private Surface     mSurface;
    private Handler     mHandler;
    private boolean isStart;
    private long startTime;

    private boolean     isWriteFile = false;

    private EncoderListener encoderListener;

    public interface EncoderListener{
        void onH264Data(ByteBuffer buffer, MediaCodec.BufferInfo info);
        void onMediaFormatChanged(MediaFormat mediaFormat);
    }

    public EncoderListener getEncoderListener() {
        return encoderListener;
    }

    public void setEncoderListener(EncoderListener encoderListener) {
        this.encoderListener = encoderListener;
    }

    public OpenGLAVCEncoder(Context context, EGLContext glContext, int width, int
            height) {
        mContext = context.getApplicationContext();
        mWidth = width;
        mHeight = height;
        mGlContext = glContext;
    }

    public void start(float speed) throws IOException {
        MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC,
                mWidth, mHeight);
        //颜色空间 从 surface当中获得
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities
                .COLOR_FormatSurface);
        //码率
        format.setInteger(MediaFormat.KEY_BIT_RATE, mBitrate * 1000);
        //帧率
        format.setInteger(MediaFormat.KEY_FRAME_RATE, mKeyFramRate);
        //关键帧间隔
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, mIFrameInterval);
        //创建编码器
        mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        //配置编码器
        mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
//输入数据     byte[]    gpu  mediaprojection

        mSurface= mMediaCodec.createInputSurface();

//        视频  编码一个可以播放的视频
        //混合器 (复用器) 将编码的h.264封装为mp4
        //开启编码
        mMediaCodec.start();
//        重点    opengl   gpu里面的数据画面   肯定要调用   opengl 函数
//线程
        //創建OpenGL 的 環境
        HandlerThread handlerThread = new HandlerThread("codec-gl");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                eglEnv = new EGLEnv(mContext,mGlContext, mSurface,mWidth, mHeight);
                isStart = true;
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
//                获取对应的数据
//                long time1  = System.currentTimeMillis();
                codec(false);
//                long time2  = System.currentTimeMillis();
//                Log.d(TAG, "编码耗时 = " + (time2 - time1));
            }
        });

    }

    private WriteFileUtil h264InputStream;
    private String recordH264Path  = Environment.getExternalStorageDirectory().toString() + "/1122.h264";

    private void codec(boolean endOfStream) {
//        数据什么时候
//        编码
        //给个结束信号
        if (endOfStream) {
            mMediaCodec.signalEndOfInputStream();
        }

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int index = mMediaCodec.dequeueOutputBuffer(bufferInfo, 11000);
        if (index >= 0) {
            ByteBuffer buffer = mMediaCodec.getOutputBuffer(index);
//            byte[] outData = new byte[bufferInfo.size];
//            buffer.get(outData);
            if (startTime == 0) {
                // 微妙转为毫秒
                startTime = bufferInfo.presentationTimeUs / 1000;
            }

//            if (isWriteFile){
//                if (h264InputStream == null){
//                    h264InputStream = new WriteFileUtil(recordH264Path);
//                    h264InputStream.createfile();
//                }
//                h264InputStream.writeFile(outData);
//            }

            if (encoderListener != null){
                encoderListener.onH264Data(buffer, bufferInfo);
            }

//                包含   分隔符
            mMediaCodec.releaseOutputBuffer(index, false);
        }else if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
            MediaFormat mediaFormat = mMediaCodec.getOutputFormat();
            if (encoderListener != null){
                encoderListener.onMediaFormatChanged(mediaFormat);
            }
            Log.i(TAG, "mediaFormat: " + mediaFormat.toString());
        }

    }
    public void stop() {
        // 释放
        isStart = false;
        if (mHandler == null)
            return ;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (h264InputStream != null){
                    h264InputStream.stopStream();
                }
                codec(true);
                mMediaCodec.stop();
                mMediaCodec.release();
                mMediaCodec = null;
                eglEnv.release();
                eglEnv = null;
                mSurface = null;
                mHandler.getLooper().quitSafely();
                mHandler = null;
            }
        });
    }

    public MediaFormat getOutputMediaFormat(){
        if (mMediaCodec == null)
            return null;

        return mMediaCodec.getOutputFormat();
    }

    public int getmBitrate() {
        return mBitrate;
    }

    public void setmBitrate(int mBitrate) {
        this.mBitrate = mBitrate;
    }

    public int getmIFrameInterval() {
        return mIFrameInterval;
    }

    public void setmIFrameInterval(int mIFrameInterval) {
        this.mIFrameInterval = mIFrameInterval;
    }

    public int getmKeyFramRate() {
        return mKeyFramRate;
    }

    public void setmKeyFramRate(int mKeyFramRate) {
        this.mKeyFramRate = mKeyFramRate;
    }
}
