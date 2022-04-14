package com.van.opengl;

import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.EGL14;
import android.opengl.GLES10;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.van.opengl.filter.BeautyFilter;
import com.van.opengl.filter.CameraFilter;
import com.van.opengl.filter.RecordFilter;
import com.van.opengl.filter.SoulFilter;
import com.van.opengl.filter.SplitFilter;
import com.van.opengl.filter.WaterFilter;
import com.van.util.Camera2Helper;
import com.van.util.CameraAbstract;
import com.van.util.CameraHelper;
import com.van.util.ICameraData;
import com.van.util.OSDBean;

import java.io.File;
import java.io.IOException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.glClear;

public class CameraSurface extends GLSurfaceView implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    private static final String TAG = "CameraSurface";
    private Context      mContext;
    private ICameraData cameraDataLinstener;
    private Camera2Helper cameraHelper;
    private  int[] textures;
    private SurfaceTexture mSurfaceTexture;
    private float[] mTransformMatrix = new float[16];
    private CameraFilter cameraFilter;
    /*显示filter*/
    private RecordFilter recordFilter;
    private WaterFilter waterFilter;
    private SoulFilter soulFilter;
    private SplitFilter splitFilter;
    private BeautyFilter beautyFilter;
    private OpenGLAVCEncoder glavcEncoder;
    private OpenGLMediaRecorder mediaRecorder;
    private boolean     isCreated;

    private OpenGLAVCEncoder.EncoderListener encoderListener;

    public void setAVCEncoderLinstener(OpenGLAVCEncoder.EncoderListener encoderListener){
        this.encoderListener    = encoderListener;
    }

    public CameraSurface(Context context) {
        super(context);
        mContext    = context;
        //默认参数
//        cameraHelper    = new CameraHelper(mContext
//                , CameraHelper.VIDEO_HARDWARE_ENCODE
//                , 1080
//                , 0
//                , Camera.CameraInfo.CAMERA_FACING_BACK
//                , CameraAbstract.VIDEO_DEFINITION_1080P);
        cameraHelper    = new Camera2Helper((Activity) context);

    }
    public CameraSurface(Context context, CameraHelper cameraHelper){
        super(context);
        mContext    = context;
//        this.cameraHelper   = cameraHelper;
    }

    public CameraSurface(Context context, int encodeType, int defaultCodRate, int CameraDeflection, int CameraFacing, int CameraDefinition) {
        super(context);
//        cameraHelper    = new CameraHelper(mContext
//                , encodeType
//                , defaultCodRate
//                , CameraDeflection
//                , CameraFacing
//                , CameraDefinition);
    }

    public synchronized void create(){
        if (isCreated)
            return ;
        setEGLContextClientVersion(2);
        setRenderer(this);
        setPreserveEGLContextOnPause(true);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        isCreated   = true;
    }

    public synchronized void destory() {
        stopEncode();
        if (cameraHelper != null){
            cameraHelper.StopCamera();
        }
        isCreated   = false;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        gl.glEnable(GLES10.GL_MULTISAMPLE);
        gl.glShadeModel(GLES10.GL_FLAT);
        gl.glEnable(GLES10.GL_POINT_SMOOTH);
        gl.glEnable(GLES10.GL_BLEND);
        gl.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        textures    = createTextureID();
        //新建一个surface纹理
        mSurfaceTexture = new SurfaceTexture(textures[0]);
//        mSurfaceTexture.attachToGLContext(textures[0]);
        mSurfaceTexture.setOnFrameAvailableListener(this);
        OSDBean osdBean = new OSDBean();
        osdBean.setOSD1("阿斯顿发士大夫阿萨德f");
        //创建camera fbo
        cameraFilter = new CameraFilter(getContext());
        recordFilter = new RecordFilter(getContext());
        waterFilter = new WaterFilter(getContext(), osdBean);
        splitFilter = new SplitFilter(getContext());
        soulFilter  = new SoulFilter(getContext());
        beautyFilter = new BeautyFilter(getContext());
        //是否要关闭相机？？？
        cameraHelper.setSurfaceTexture(mSurfaceTexture);
//        cameraHelper.setCameraOrientation(90);
        cameraHelper.StopCamera();
        cameraHelper.StartCamera();
        //先关闭之前的编码器
        stopEncode();
        //初始化opengl编码器
        glavcEncoder = new OpenGLAVCEncoder(getContext(),
                EGL14.eglGetCurrentContext(),
                cameraHelper.getCamera_video_width(),
                cameraHelper.getCamera_video_height());
        if (encoderListener != null){
            glavcEncoder.setEncoderListener(encoderListener);
        }
        //设置最大码率
        glavcEncoder.setmBitrate(4096);
        startEncode();
        Log.d("CameraGLSurface", "当前EGL上下文=" + EGL14.eglGetCurrentContext());
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d(TAG, "onSurfaceChanged width="+width+", height="+height);
        cameraFilter.setSize(cameraHelper.getCamera_video_width(), cameraHelper.getCamera_video_height());
        recordFilter.setSize(width, height);
//        beautyFilter.setSize(width, height);
//        soulFilter.setSize(width, height);
        waterFilter.setSize(cameraHelper.getCamera_video_width(), cameraHelper.getCamera_video_height());


//        splitFilter.setSize(width,height);
    }

    public long				m_last_time_stamp 	= System.currentTimeMillis();
    public int				m_preview_rate		= 0;
    @Override
    public void onDrawFrame(GL10 gl) {
        gl.glEnable(GLES10.GL_MULTISAMPLE);
        gl.glEnable(GLES10.GL_POINT_SMOOTH);
        gl.glEnable(GLES10.GL_BLEND);
        gl.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        //更新摄像头数据给GPU
        mSurfaceTexture.updateTexImage();
        //获取并设置摄像头矩阵
        mSurfaceTexture.getTransformMatrix(mTransformMatrix);
        cameraFilter.setTransformMatrix(mTransformMatrix);
//id     FBO所在的图层   纹理
        int id  = cameraFilter.onDraw(textures[0], false);

//        id = splitFilter.onDraw(id);
//        id      = soulFilter.onDraw(id, false);
//        id      = waterFilter.onDraw(id, false);
//        id      = beautyFilter.onDraw(id, false);+
        long timeSlamp  = System.nanoTime();
        //拿到fbo引用，开始编码
        if (glavcEncoder != null)
            glavcEncoder.fireFrame(id,timeSlamp);
//        if (mediaRecorder != null)
//            mediaRecorder.fireFrame(id, timeSlamp);
        id      = recordFilter.onDraw(id, false);


    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        //刷新数据
        requestRender();

//        long		current_time_stamp = System.currentTimeMillis();
//        m_preview_rate++;
//        if ((current_time_stamp-m_last_time_stamp) >= 1000)
//        {
//            Log.i("Test", "当前帧率="+m_preview_rate+" ,timestamp="+current_time_stamp+",时间差:"+(current_time_stamp-m_last_time_stamp));
//            m_last_time_stamp 	= current_time_stamp;
//            m_preview_rate		= 0;
//        }
    }


    private int[] createTextureID() {
        int[] texture = new int[1];
        GLES20.glGenTextures(1, texture, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MIN_FILTER,GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        // 解绑扩展纹理
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        return texture;
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

    public void startRecord(File file){
        this.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (mediaRecorder == null){
                    //开始录像
                    mediaRecorder   = new OpenGLMediaRecorder(getContext(), EGL14.eglGetCurrentContext(), cameraHelper.getCamera_video_width(), cameraHelper.getCamera_video_height());
                    mediaRecorder.startRecord(file);
                }
            }
        });
    }

    public void stopRecord(){
        this.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (mediaRecorder != null){
                    mediaRecorder.stopRecord();
                    mediaRecorder   = null;
                }
            }
        });
    }


    public ICameraData getCameraDataLinstener() {
        return cameraDataLinstener;
    }

    public void setCameraDataLinstener(ICameraData cameraDataLinstener) {
        this.cameraDataLinstener = cameraDataLinstener;
    }

    //更换前置摄像头或后置摄像头
    public void cameraFacingChanged(){
        if (cameraHelper == null)
            return ;
        onPause();
//        cameraHelper.cameraFacingChanged();
        onResume();
    }

    public boolean cameraFacingChanged(int cameraFacing){
        if (cameraHelper == null)
            return false;

        return true;
    }
}
