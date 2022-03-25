package com.van.opengl;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.van.util.CameraAbstract;
import com.van.util.CameraHelper;
import com.van.opengl.filter.CameraYUVFilter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glViewport;

public class YUVCameraRender implements GLSurfaceView.Renderer, Camera.PreviewCallback {

    private static final String TAG = "YUVCameraRender";
    private Context context;

    private CameraHelper cameraHelper;

    private GLSurfaceView mGLSurfaceView;

    private SurfaceTexture mSurfaceTexture;

    private ByteBuffer mYUVBuffer;

    private CameraYUVFilter cameraYUVFilter;

    private OpenGLAVCEncoder glavcEncoder;

    private  int[] textures;

    public YUVCameraRender(Context context, GLSurfaceView mGLSurfaceView) {
        this.context = context;
        this.mGLSurfaceView = mGLSurfaceView;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        cameraHelper    = new CameraHelper(context
                , CameraHelper.VIDEO_HARDWARE_ENCODE
                , 4096
                , 0
                , Camera.CameraInfo.CAMERA_FACING_BACK
                , CameraAbstract.VIDEO_DEFINITION_1080P);
        cameraHelper.setPreviewCallback(this);
        cameraYUVFilter = new CameraYUVFilter(context, cameraHelper.getCamera_video_width(), cameraHelper.getCamera_video_height());
        //初始化opengl编码器
        glavcEncoder = new OpenGLAVCEncoder(mGLSurfaceView.getContext(),
                EGL14.eglGetCurrentContext(),
                cameraHelper.getCamera_video_width(),
                cameraHelper.getCamera_video_height());
        //设置最大码率
        glavcEncoder.setmBitrate(8192);
        startEncode();
    }


    /*开始编码*/
    private void startEncode() {
        if (glavcEncoder == null)
            return ;
        try {
            glavcEncoder.start(1.0f);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*停止编码*/
    private void stopEncode() {
        if (glavcEncoder == null)
            return ;
        glavcEncoder.stop();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {

        glViewport(0, 0, width, height);
        width   = cameraHelper.getCamera_video_width();
        height  = cameraHelper.getCamera_video_height();
        int bufferSize = width * height * ImageFormat.getBitsPerPixel(ImageFormat.NV21) / 8;
        mYUVBuffer = ByteBuffer.allocateDirect(bufferSize)
                .order(ByteOrder.nativeOrder());
        int[] textures = new int[1];
        glGenTextures(1, textures, 0);
        mSurfaceTexture = new SurfaceTexture(textures[0]);
        cameraHelper.setSurfaceTexture(mSurfaceTexture);
        cameraHelper.StopCamera();
        cameraHelper.StartCamera();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glClearColor(0f, 0f, 1f, 1f);
        synchronized (mYUVBuffer) {
            cameraYUVFilter.draw(mYUVBuffer.array());
            //拿到fbo引用，开始编码
            if (glavcEncoder != null)
                glavcEncoder.fireFrame(cameraYUVFilter.mYTestureId,System.nanoTime());
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Log.d(TAG, "onPreviewFrame 11 f="+cameraHelper.getCurrentCameraDeflection());
        synchronized (mYUVBuffer) {
            mYUVBuffer.position(0);
            mYUVBuffer.put(data);
        }
        mGLSurfaceView.requestRender();
        camera.addCallbackBuffer(data);
    }


    // 创建纹理
    private int[] createTexture(int width, int height, int format)
    {
        int[] texture = new int[1];
        //创建纹理
        GLES20.glGenTextures(1, texture, 0);
        //绑定纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]);
        //设置纹理属性
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, format, width, height, 0, format, GLES20.GL_UNSIGNED_BYTE, null);
        return texture;
    }

    public void release() {
        cameraHelper.StopCamera();
        stopEncode();
    }
}
