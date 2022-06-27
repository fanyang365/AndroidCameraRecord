package com.van.opengl;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;

import com.van.util.CameraAbstract;
import com.van.util.CameraHelper;
import com.van.opengl.filter.CameraYUVFilter2;
import com.van.util.OSDBean;
import com.van.opengl.filter.RecordFilter;
import com.van.opengl.filter.WaterFilter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glGenTextures;

public class YUVCameraRender2 implements GLSurfaceView.Renderer, Camera.PreviewCallback {

    private static final String TAG = "YUVCameraRender";
    private Context context;

    private CameraHelper cameraHelper;

    private GLSurfaceView mGLSurfaceView;

    private SurfaceTexture mSurfaceTexture;

    private byte[] mYUVBuffer;

    private Object object   = new Object();

    private CameraYUVFilter2 cameraYUVFilter;
    private WaterFilter     waterFilter;
    private RecordFilter    recordFilter;

    private OpenGLAVCEncoder glavcEncoder;

    private  int[] textures;

    public YUVCameraRender2(Context context, GLSurfaceView mGLSurfaceView) {
        this.context = context;
        this.mGLSurfaceView = mGLSurfaceView;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        cameraHelper    = new CameraHelper(context
                , CameraHelper.VIDEO_HARDWARE_ENCODE
                , 4096
                , 0
                , Camera.CameraInfo.CAMERA_FACING_FRONT
                , CameraAbstract.VIDEO_DEFINITION_1080P);
        cameraHelper.setPreviewCallback(this);

        //yuv预览摄像头
        cameraYUVFilter = new CameraYUVFilter2(mGLSurfaceView.getContext());
        OSDBean osdBean = new OSDBean();
        osdBean.setOSD1("阿斯顿发士大夫阿萨德f");
        //水印叠加
        waterFilter     = new WaterFilter(mGLSurfaceView.getContext(), osdBean);
        //屏幕显示
        recordFilter    = new RecordFilter(mGLSurfaceView.getContext());

        glavcEncoder = new OpenGLAVCEncoder(mGLSurfaceView.getContext(),
                EGL14.eglGetCurrentContext(),
                cameraHelper.getCamera_video_width(),
                cameraHelper.getCamera_video_height());

        //设置最大码率
        glavcEncoder.setmBitrate(8192);
        startEncode();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {

        recordFilter.setSize(width, height);

        width   = cameraHelper.getCamera_video_width();
        height  = cameraHelper.getCamera_video_height();
        cameraYUVFilter.setSize(width, height);
        Log.d(TAG, "当前摄像头宽度="+cameraHelper.getCamera_video_width() + ", 高度="+ cameraHelper.getCamera_video_height());

        waterFilter.setSize(width, height);

        //创建一个纹理
        textures    = createTexture(cameraHelper.getCamera_video_width(), cameraHelper.getCamera_video_height());
        mSurfaceTexture = new SurfaceTexture(textures[0]);
        cameraHelper.setSurfaceTexture(mSurfaceTexture);
        cameraHelper.StopCamera();
        cameraHelper.StartCamera();
        cameraYUVFilter.setDegrees(90);
        Log.d(TAG, "摄像头翻转="+cameraHelper.getCameraOrientation());
    }

    public long				m_last_time_stamp 	= System.currentTimeMillis();
    public int				m_preview_rate		= 0;

    @Override
    public void onDrawFrame(GL10 gl) {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glClearColor(0f, 0f, 0f, 1f);
        if (mYUVBuffer == null)
            return ;
        int id;
        synchronized (object) {
            if (mYUVBuffer == null)
                return ;
            id = cameraYUVFilter.draw(textures[0], false, mYUVBuffer);
        }
        id      = waterFilter.onDraw(id, false);
//        //拿到fbo引用，开始编码
        if (glavcEncoder != null)
            glavcEncoder.fireFrame(id,System.nanoTime());
//
        recordFilter.onDraw(id, false);

    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
//        Log.d(TAG, "onPreviewFrame 11 f="+cameraHelper.getCurrentCameraDeflection());

        synchronized (object) {

            if (mYUVBuffer == null){
                mYUVBuffer  = new byte[data.length];
                Log.d(TAG, "mYUVBuffer new byte ");
            }

            if (mYUVBuffer.length != data.length){
                mYUVBuffer  = new byte[data.length];
                Log.d(TAG, "mYUVBuffer new byte2 ");
            }
            System.arraycopy(data, 0, mYUVBuffer, 0, data.length);
        }
//
//        getJpg(data,  camera);
        mGLSurfaceView.requestRender();
        camera.addCallbackBuffer(data);

        long		current_time_stamp = System.currentTimeMillis();
        m_preview_rate++;
        if ((current_time_stamp-m_last_time_stamp) >= 1000)
        {
            Log.i(TAG, "当前帧率="+m_preview_rate+",timestamp="+current_time_stamp+",时间差:"+(current_time_stamp-m_last_time_stamp));
            m_last_time_stamp 	= current_time_stamp;
            m_preview_rate		= 0;
        }
    }

    private void getJpg(byte[] data, Camera camera){
        Camera.Size size = camera.getParameters().getPreviewSize();
        try {
            YuvImage image = new YuvImage(data, ImageFormat.NV21,size.width, size.height, null);
            if (image != null) {
                // 保存图片 ///
                File file = new File(Environment.getExternalStorageDirectory().getPath() + File.separator+"picture.jpg");
                FileOutputStream stream = new FileOutputStream(file);
                if (image.compressToJpeg(new Rect(0, 0, size.width, size.height), 80, stream)) {
                    stream.flush();
                    stream.close();
                }
                //
//                ByteArrayOutputStream stream = new ByteArrayOutputStream();
//                image.compressToJpeg(new Rect(0, 0, size.width, size.height), 80, stream);
//                Bitmap bmp=BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
//                stream.close();
                //
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    // 创建纹理
    private int[] createTexture(int width, int height )
    {
        int format =    GLES20.GL_RGBA;
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

    /**
     * 根据当前横竖屏状态，判断摄像头该偏转多少度。
     * @param activity
     * @param cameraId		前置或后置摄像头
     * @return 偏转的角度
     */
    private int getCameraDisplayOrientation (Activity activity, int cameraId) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo (cameraId , info);
        int rotation = activity.getWindowManager ().getDefaultDisplay ().getRotation ();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;   // compensate the mirror

        } else {
            // back-facing
            result = ( info.orientation - degrees + 360) % 360;
        }
        return result;
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

    public void release() {
        stopEncode();
        cameraHelper.StopCamera();
    }
}
