package com.van.opengl;

import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.Image;
import android.media.ImageReader;
import android.opengl.EGL14;
import android.opengl.GLES10;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;

import com.van.ncnn.NcnnYoloFace;
import com.van.opencv.FaceTracker;
import com.van.opencv.YUVData;
import com.van.opengl.filter.BeautyFilter;
import com.van.opengl.filter.CameraFilter;
import com.van.opengl.filter.MosaicsFilter;
import com.van.opengl.filter.RecordFilter;
import com.van.opengl.filter.SoulFilter;
import com.van.opengl.filter.SplitFilter;
import com.van.opengl.filter.WaterFilter;
import com.van.util.Camera2Helper;
import com.van.util.CameraAbstract;
import com.van.util.CameraHelper;
import com.van.util.FileUtil;
import com.van.util.ICameraData;
import com.van.util.OSDBean;
import com.van.util.WriteFileUtil;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.glClear;

public class CameraSurface extends GLSurfaceView implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener , Camera.PreviewCallback {

    private static final String TAG = "CameraSurface";
    private Context      mContext;
    private ICameraData cameraDataLinstener;
    private CameraHelper cameraHelper;
//    private FaceTracker tracker;
    private NcnnYoloFace ncnnyoloface = new NcnnYoloFace();
    private  int[] textures;
    private SurfaceTexture mSurfaceTexture;
    private float[] mTransformMatrix = new float[16];
    private CameraFilter cameraFilter;
    /*显示filter*/
    private RecordFilter recordFilter;
    private WaterFilter waterFilter;
    private SoulFilter soulFilter;
    private SplitFilter splitFilter;
    private MosaicsFilter mosaicsFilter;
    private BeautyFilter beautyFilter;
    private OpenGLAVCEncoder glavcEncoder;
    private OpenGLMediaRecorder mediaRecorder;
    private boolean     isCreated;

    private boolean mosaicsEnable = true;

    private String xmlPath;
    private String binPath;

    private OpenGLAVCEncoder.EncoderListener encoderListener;

    public void setAVCEncoderLinstener(OpenGLAVCEncoder.EncoderListener encoderListener){
        this.encoderListener    = encoderListener;
    }

    public CameraSurface(Context context) {
        super(context);
        mContext    = context;

        init();

    }
    public CameraSurface(Context context, CameraHelper cameraHelper){
        super(context);
        mContext    = context;
        init();
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
        init();
    }

    private void init(){
        File file   = mContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        xmlPath = file.getAbsolutePath() + "/lbpcascade_frontalface.xml";
        binPath = file.getAbsolutePath() + "/seeta_fa_v1.1.bin";

        //拷贝 模型
        FileUtil.copyAssets2SdCard(mContext, "lbpcascade_frontalface_improved.xml",
                xmlPath);
        FileUtil.copyAssets2SdCard(mContext, "seeta_fa_v1.1.bin",
                binPath);

        //默认参数
        cameraHelper    = new CameraHelper(mContext
                , CameraHelper.VIDEO_HARDWARE_ENCODE
                , 1080
                , 0
                , Camera.CameraInfo.CAMERA_FACING_BACK
                , CameraAbstract.VIDEO_DEFINITION_1080P);
//        cameraHelper.setPreviewCallback(this);


//        cameraHelper    = new Camera2Helper((Activity) mContext);
//        cameraHelper.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
//            @Override
//            public void onImageAvailable(ImageReader reader) {
//                Log.d(TAG, "onImageAvailable ASDF");
//
//
//                Image image = reader.acquireNextImage();
//                if (image == null) {
//                    return;
//                }
//                byte[] rawData = Camera2Helper.getImageData(image, Camera2Helper.NV21);
//                if (ncnnyoloface != null){
////                    ncnnyoloface.detectorImage(reader);
//                    if (yuvData == null)
//                        yuvData = new YUVData();
//                    yuvData.setData(rawData);
//                    yuvData.setCameraFacing(0);
//                    yuvData.setCameraOrientation(cameraHelper.getCameraOrientation());
//                    yuvData.setWidth(640);
//                    yuvData.setHeight(480);
//                    ncnnyoloface.detector(yuvData);
//
//                }
//                image.close();
//            }
//        });


        reload();
    }

    private void reload()
    {
        int current_model = 0;
        int current_cpugpu = 0;
        boolean ret_init = ncnnyoloface.loadModel(mContext.getAssets(), current_model, current_cpugpu);
        if (!ret_init)
        {
            Log.e("MainActivity", "ncnnyoloface loadModel failed");
        }
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

//        if (tracker != null){
//            tracker.stopTrack();
//            tracker = null;
//        }

        if (ncnnyoloface != null){
            ncnnyoloface.stopTrack();
            ncnnyoloface = null;
        }
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
        osdBean.setOSD1("水印水印，我是水印");
        //创建camera fbo
        cameraFilter = new CameraFilter(getContext());
        recordFilter = new RecordFilter(getContext());
        waterFilter = new WaterFilter(getContext(), osdBean);
        splitFilter = new SplitFilter(getContext());
        mosaicsFilter = new MosaicsFilter(getContext());
        soulFilter  = new SoulFilter(getContext());
        beautyFilter = new BeautyFilter(getContext());
        //是否要关闭相机？？？
        cameraHelper.setSurfaceTexture(mSurfaceTexture);
        ncnnyoloface.setOutputWindow(new Surface(mSurfaceTexture));
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
        soulFilter.setSize(width, height);
        waterFilter.setSize(cameraHelper.getCamera_video_width(), cameraHelper.getCamera_video_height());

        splitFilter.setSize(width,height);
        mosaicsFilter.setSize(width,height);

//        tracker = new FaceTracker(xmlPath, binPath);
//        tracker.startTrack();
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
        //                获取对应的数据
        long time1  = System.currentTimeMillis();
        //更新摄像头数据给GPU
        mSurfaceTexture.updateTexImage();
        //获取并设置摄像头矩阵
        mSurfaceTexture.getTransformMatrix(mTransformMatrix);
        cameraFilter.setTransformMatrix(mTransformMatrix);
//id     FBO所在的图层   纹理
        int id  = cameraFilter.onDraw(textures[0], false);
//        id = splitFilter.onDraw(id);
//        id      = soulFilter.onDraw(id, false);

        //绘制马赛克
//        if (mosaicsEnable && mosaicsFilter != null){
//            mosaicsFilter.setFace(ncnnyoloface.mFace);
//            id = mosaicsFilter.onDraw(id, false);
//        }

//        id      = waterFilter.onDraw(id, false);
//        id      = beautyFilter.onDraw(id, false);+
        long timeSlamp  = System.nanoTime();
        //拿到fbo引用，开始编码
        if (glavcEncoder != null)
            glavcEncoder.fireFrame(id,timeSlamp);
//        if (mediaRecorder != null)
//            mediaRecorder.fireFrame(id, timeSlamp);
        id      = recordFilter.onDraw(id, false);

        long time2  = System.currentTimeMillis();
        Log.d(TAG, "渲染耗时 = " + (time2 - time1));
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
        cameraHelper.StopCamera();
        cameraHelper.cameraFacingChanged();
        cameraHelper.StartCamera();
        onResume();
    }

    public boolean cameraFacingChanged(int cameraFacing){
        if (cameraHelper == null)
            return false;

        return true;
    }


    private YUVData yuvData;
    private byte[] tmpData;

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (tmpData == null || tmpData.length != data.length)
            tmpData = new byte[data.length];

        System.arraycopy(data, 0, tmpData, 0, tmpData.length);
        camera.addCallbackBuffer(data);

        if (bTakeYUV){
            saveYUV(tmpData);
            bTakeYUV    = false;
        }


        if (ncnnyoloface != null){
            if (yuvData == null)
                yuvData = new YUVData();
            yuvData.setData(tmpData);
            int facing  = cameraHelper.getCurrentCameraFacing() == 1 ? 0 : 1; //ndk层是相反的
            yuvData.setCameraFacing(facing);
            yuvData.setCameraOrientation(cameraHelper.getCameraOrientation());
            yuvData.setWidth(cameraHelper.getCamera_video_width());
            yuvData.setHeight(cameraHelper.getCamera_video_height());
            ncnnyoloface.detector(yuvData);
        }

//        if (tracker != null && (mosaicsEnable)) {
//            if (yuvData == null)
//                yuvData = new YUVData();
//            yuvData.setData(tmpData);
//            yuvData.setCameraFacing(cameraHelper.getCurrentCameraFacing());
//            yuvData.setWidth(cameraHelper.getCamera_video_width());
//            yuvData.setHeight(cameraHelper.getCamera_video_height());
//            tracker.detector(yuvData);
//        }

    }

    boolean bTakeYUV;
    public void takeYUV() {
        bTakeYUV    = true;
    }

    private void saveYUV(byte[] data){
        String path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/1.yuv";
        WriteFileUtil writeFileUtil = new WriteFileUtil(path);
        writeFileUtil.createfile();
        writeFileUtil.writeFile(data);
        writeFileUtil.stopStream();
    }
}
