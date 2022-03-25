package com.van.opengl;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.util.Log;

import java.io.IOException;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glViewport;

public class GLRender implements GLSurfaceView.Renderer , SurfaceTexture.OnFrameAvailableListener{

    private Camera mCamera;
    private int mSurfaceTextureId;
    private SurfaceTexture  mSurfaceTexture;
    private GLSurfaceView mGLSurfaceView;
    private float[] mTransformMatrix = new float[16];
    private OESProgram mProgram;

    public GLRender(GLSurfaceView glSurfaceView) {
        mGLSurfaceView = glSurfaceView;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mCamera = Camera.open(0);
    }

    int cameraWidth = 1920;
    int cameraHeight    = 1080;

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        glViewport(0, 0, width, height);
        int[] textures    =new int[1];
        glGenTextures(1, textures, 0);
        mSurfaceTextureId = textures[0];
        //新建一个surface纹理
        mSurfaceTexture = new SurfaceTexture(textures[0]);
        mSurfaceTexture.setOnFrameAvailableListener(this);
        mProgram = new OESProgram(mGLSurfaceView.getContext(), cameraWidth, cameraHeight);

        try {
            Camera.Parameters params    = mCamera.getParameters();
            List<Camera.Size>  supportedPreviewSizes = params.getSupportedPreviewSizes();
            //设置帧率
            List<int[]> fpsList = params.getSupportedPreviewFpsRange();
            if(fpsList != null && fpsList.size() > 0) {
                int[] maxFps = fpsList.get(0);
                for (int[] fps: fpsList) {
                    if(maxFps[0] * maxFps[1] < fps[0] * fps[1]) {
                        maxFps = fps;
                    }
                }
                Log.d("Test", "设置帧率 = "+maxFps[0] + " ~ " + maxFps[1]);
                //注意setPreviewFpsRange的输入参数是帧率*1000，如30帧/s则参数为30*1000
                params.setPreviewFpsRange(maxFps[0] , maxFps[1]);
                //setPreviewFrameRate的参数是实际的帧率
                params.setPreviewFrameRate(30);
            }
            params.setPreviewSize(cameraWidth, cameraHeight);
            mCamera.setDisplayOrientation(90);
            mCamera.setPreviewTexture(mSurfaceTexture);
        } catch (IOException e) {
            e.printStackTrace();
        }

//        int bufferSize = cameraWidth * cameraHeight * ImageFormat.getBitsPerPixel(ImageFormat.NV21) / 8;
//        mCamera.setPreviewCallbackWithBuffer(this);
//        for (int i = 0; i < 2; i++) {
//            byte[] callbackBuffer = new byte[bufferSize];
//            mCamera.addCallbackBuffer(callbackBuffer);
//        }

        mCamera.startPreview();
    }

    //    @Override
//    public void onPreviewFrame(byte[] data, Camera camera) {
//        mGLSurfaceView.requestRender();
//        mCamera.addCallbackBuffer(data);
//
//    }

    @Override
    public void onDrawFrame(GL10 gl) {
        //更新摄像头数据给GPU
        mSurfaceTexture.updateTexImage();
        mSurfaceTexture.getTransformMatrix(mTransformMatrix);
//        绘制摄像头数据
        mProgram.draw(mSurfaceTextureId, mTransformMatrix);
    }


    public long				m_last_time_stamp 	= System.currentTimeMillis();
    public int				m_preview_rate		= 0;


    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        mGLSurfaceView.requestRender();

        long		current_time_stamp = System.currentTimeMillis();
        m_preview_rate++;
        if ((current_time_stamp-m_last_time_stamp) >= 1000)
        {
            Log.i("Test", "当前帧率="+m_preview_rate+" ,timestamp="+current_time_stamp+",时间差:"+(current_time_stamp-m_last_time_stamp));
            m_last_time_stamp 	= current_time_stamp;
            m_preview_rate		= 0;
        }
    }
}
