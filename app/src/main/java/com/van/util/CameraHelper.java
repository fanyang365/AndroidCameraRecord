package com.van.util;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

import java.util.List;

/**
 * Camera 摄像头
 */
public class CameraHelper extends CameraAbstract{


    private static final String TAG = "CameraHelper";
    public static boolean		isUsed									= false;

    private Context mContext;

    private int				cameraCount;
    private int				currentCameraFacing;//当前摄像头是前置还是后置


    //摄像机相关
    List<Camera.Size> supportedPreviewSizes;	// 当前摄像机信息
    private Camera camera;

    private Camera.PreviewCallback previewCallback;

    public int getCameraCount() {
        return cameraCount;
    }
    public int getCurrentCameraFacing() {
        return currentCameraFacing;
    }

    public void setCurrentCameraFacing(int currentCameraFacing) {
        if (cameraCount <= currentCameraFacing) return ;
        if (currentCameraFacing < 0) return ;
        this.currentCameraFacing = currentCameraFacing;
    }

    public Camera.PreviewCallback getPreviewCallback() {
        return previewCallback;
    }

    public void setPreviewCallback(Camera.PreviewCallback previewCallback) {
        this.previewCallback = previewCallback;
    }

    /**
     * 根据码率获取对应分辨率
     * @param codeRate
     * @return
     */
    public static int getDefinitionByCodeRate(int codeRate){

        int definitioin		= VIDEO_DEFINITION_D1;
        //根据码率设置对应分辨率
        switch (codeRate) {
            case 128:
            case 256:
                definitioin		= VIDEO_DEFINITION_CIF;
                break;
            case 384:
            case 512:
                definitioin		= VIDEO_DEFINITION_D1;
                break;
            case 1024:
            case 768:
                definitioin		= VIDEO_DEFINITION_720P;
                break;
            case 1920:
                definitioin		= VIDEO_DEFINITION_1080P;
                break;
            default:
                definitioin		= VIDEO_DEFINITION_D1;
                break;
        }

        return definitioin;
    }

    public CameraHelper(Context context){
        mContext = context;
        //默认参数
        currentCodeRate			= 512;
        currentCameraDeflection	= 0;
        currentCameraFacing		= Camera.CameraInfo.CAMERA_FACING_FRONT;
        currentdefinition		= VIDEO_DEFINITION_720P;

        init(currentCodeRate, currentCameraDeflection, currentCameraFacing, currentdefinition);
    }

    /**
     * @param context
     * @param encodeType		编码类型：见VIDEO_HARDWARE_ENCODE和VIDEO_SOFT_ENCODE
     * @param defaultCodRate	默认码流：取值128~4096
     * @param CameraDeflection	摄像偏转角度：取值0~360
     * @param CameraFacing		哪个摄像头：见Camera.CameraInfo常量，前置或后置
     */
    public CameraHelper(Context context, int encodeType, int defaultCodRate, int CameraDeflection, int CameraFacing, int CameraDefinition) {
        mContext = context;
        init(defaultCodRate, CameraDeflection, CameraFacing, CameraDefinition);
    }


    private void init(int defaultCodRate, int cameraDeflection, int cameraFacing, int CameraDefinition){

        // 摄像头信息初始化
        if (defaultCodRate>4096) defaultCodRate=4096;
        if (defaultCodRate<128) defaultCodRate=512;
        if (cameraDeflection>360) cameraDeflection=360;
        if (cameraDeflection<0) cameraDeflection=0;
        //如果是USB摄像机
        if (cameraFacing == CAMERA_FACING_USB){

        }else{
            cameraCount			= Camera.getNumberOfCameras(); // get cameras number
            if (cameraCount > 1 && currentCameraFacing < cameraCount) {// 判断是否有前置摄像头
                currentCameraFacing	=  cameraFacing;
            }else {
                currentCameraFacing	= Camera.CameraInfo.CAMERA_FACING_BACK;
            }
        }

        currentCodeRate		= defaultCodRate;
        currentCameraFacing	= cameraFacing;
        currentCameraDeflection	= cameraDeflection;
        currentdefinition	= CameraDefinition;

    }



    //更换前置摄像头或后置摄像头
    public void cameraFacingChanged(){
        currentCameraFacing = currentCameraFacing == Camera.CameraInfo.CAMERA_FACING_BACK ? Camera.CameraInfo.CAMERA_FACING_FRONT
                : Camera.CameraInfo.CAMERA_FACING_BACK;

    }

    public boolean cameraFacingChanged(int cameraFacing){
        if (cameraFacing == currentCameraFacing)
            return false;
        if (cameraFacing != Camera.CameraInfo.CAMERA_FACING_FRONT && cameraFacing!= Camera.CameraInfo.CAMERA_FACING_BACK) {
            return false;
        }
        currentCameraFacing = cameraFacing;
        return true;
    }

    /**
     * 根据清晰度找到并设置对应分辨率。
     * VIDEO_DEFINITION_CIF=>352*288
     * VIDEO_DEFINITION_D1=>640*480
     * VIDEO_DEFINITION_720P=>1280*720
     * VIDEO_DEFINITION_1080P=>1920*1080
     * @param definitioin 清晰度， 见本类常量 VIDEO_DEFINITION_
     */
    private void findResolution(int definitioin){

        int width 	= 640;
        int	height	= 480;
        int i		= 0;
        int surportWidth	= 0;
        int	surportHeight 	= 0;

        switch (definitioin) {
            case VIDEO_DEFINITION_CIF:
                width = 352;
                height = 288;
                break;
            case VIDEO_DEFINITION_MID:
                width	= 480;
                height	= 360;
                break;
            case VIDEO_DEFINITION_D1:
                width = 640;
                height = 480;
                break;
            case VIDEO_DEFINITION_720P:
                width = 1280;
                height = 720;
                break;
            case VIDEO_DEFINITION_1080P:
                width = 1920;
                height = 1080;
                break;
            default:
                width = 1920;
                height = 1080;
                break;
        }

        camera_video_width	= 0;
        camera_video_height = 0;
        //找到摄像机支持的分辨率
        for (i = 0; i < supportedPreviewSizes.size(); i++)
        {
            surportWidth = supportedPreviewSizes.get(i).width;
            surportHeight= supportedPreviewSizes.get(i).height;

            // 初始状态赋值第一个分辨率,以防止不支持的分辨率出现
            if ((camera_video_width == 0) || (camera_video_height == 0))
            {
                camera_video_width	= surportWidth;
                camera_video_height	= surportHeight;
            }

            if ((surportWidth==width) && (surportHeight==height))
            {
                camera_video_height = surportHeight;
                camera_video_width = surportWidth;
                break;
            }
            //如果没找到352x288，320x240也可
            else if ((currentCodeRate == 128) || (currentCodeRate == 256))
            {
                if ((surportWidth==320) && (surportHeight==240))
                {
                    camera_video_height = surportHeight;
                    camera_video_width = surportWidth;
                    break;
                }
            }
        }

    }


    @Override
    public boolean StartCamera() {
        boolean result 	= false;

        Log.i("Test", "StartCamera...1");
        try
        {
            if (camera == null)
            {
                Log.i("Test", "StartCamera...2");
                int camera_number = Camera.getNumberOfCameras();

                Log.i("Test", "StartCamera...3");
                Log.i("Test", "StartCamera...currentCameraFacing="+currentCameraFacing);
                if (camera_number == 1) {
                    currentCameraFacing = Camera.CameraInfo.CAMERA_FACING_BACK;
                }
                camera = Camera.open(currentCameraFacing);
                //Log.i("Test", "StartCamera...4");
                if (camera != null)
                {
                    //Log.i("Test", "StartCamera...5");
                    //设置显示holder
//                    camera.setPreviewDisplay(this.getHolder());
                    //Log.i("Test", "StartCamera...6");
                    //获取当前的摄像机参数
                    Camera.Parameters params = camera.getParameters();
                    //Log.i("Test", "StartCamera...7");
                    supportedPreviewSizes = params.getSupportedPreviewSizes();
                    //Log.i("Test", "StartCamera...8");
                    int 			i;
                    int 			surportWidth;
                    int 			surportHeight;
                    List<Integer> supportedFormats;	// 当前摄像机信息
                    int				currentFormat;

                    supportedFormats = params.getSupportedPreviewFormats();
                    for (i = 0; i < supportedPreviewSizes.size(); i++)
                    {
                        surportWidth = supportedPreviewSizes.get(i).width;
                        surportHeight= supportedPreviewSizes.get(i).height;
                        Log.i("Test", i+".分辨统率:"+surportWidth+"X"+surportHeight);
                    }
                    for (i = 0; i < supportedFormats.size(); i++)
                    {
                        currentFormat = supportedFormats.get(i).intValue();
                        Log.i("Test", i+".格式:"+currentFormat);
                    }
                    ////////////////////////
                    findResolution(currentdefinition);
                    Log.i("Test", "1.当前选择的分辨率:camera_video_width="+camera_video_width+", camera_video_height="+camera_video_height+", currentdefinition="+currentdefinition);
                    params.setPreviewSize(camera_video_width, camera_video_height);
                    params.setPreviewFormat(ImageFormat.NV21);
                    params.setPreviewFrameRate(frameRate);
                    //设置自动对焦
                    List<String> focusModes = params.getSupportedFocusModes();
                    if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                    camera.setParameters(params);

                    Log.i("test", "当前最大码率限制"+currentCodeRate);
                    if (surfaceTexture != null){
                        camera.setPreviewTexture(surfaceTexture);
                    }
                    if (previewCallback != null){
                        previewBufCallbackInit(camera);
                    }

                    camera.startPreview() ;

                    int displayOrientation = getCameraDisplayOrientation(mContext, currentCameraFacing);
                    //加上偏转的角度
                    displayOrientation = (displayOrientation+currentCameraDeflection) % 360;
//                    displayOrientation  = 0;
                    camera.setDisplayOrientation(displayOrientation);
                    Log.d(TAG, "预览偏转="+displayOrientation);
                    if (currentCameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                        //如果是前置摄像头，传递给服务器的数据需要翻转一下。
                        cameraOrientation	= (360 - displayOrientation) % 360;
                    }else {
                        cameraOrientation	= displayOrientation;
                    }

                    Camera.Size CurpreSize = params.getPreviewSize();
                    if (cameraOrientation == 90 || cameraOrientation == 270){
                        camera_video_width	= CurpreSize.height;
                        camera_video_height	= CurpreSize.width;
                    }else{
                        camera_video_width	= CurpreSize.width;
                        camera_video_height	= CurpreSize.height;
                    }
                    Log.i("Test", "2.当前选择的分辨率:camera_video_width="+camera_video_width+", camera_video_height="+camera_video_height);

                    Log.i("Test", "设置偏转角度："+cameraOrientation);
                    result = true;
                    Log.i("Test", "StartCamera...100");
                    isUsed	= true;
                    if (iCameraStatusLinstener != null){
                        iCameraStatusLinstener.OnCameraStat(this);
                    }
//                    if (cameraDataLinstener != null)
//                        cameraDataLinstener.onCameraStart(this);
                }
            }
            else
            {
                Log.i("Test", "打开摄像机失败!");
            }
        }
        catch(Exception e)
        {
            Log.i("Test", "打开摄像机异常!");
            e.printStackTrace();
        }

        return result;
    }


    private PixelFormat pixelFormat = new PixelFormat();
    private final static int CACHE_BUFFER_NUM = 5;
    private byte[][] mPreviewCallbackBuffers = new byte[CACHE_BUFFER_NUM][];
    /*使用3个缓冲区接受数据，提升帧率*/
    private void previewBufCallbackInit(Camera camera) {
        PixelFormat.getPixelFormatInfo(ImageFormat.NV21, pixelFormat);
        int bufSize = camera_video_width * camera_video_height * pixelFormat.bitsPerPixel / 8;
        for (int i = 0; i < mPreviewCallbackBuffers.length; i++) {
            if (mPreviewCallbackBuffers[i] == null) {
                mPreviewCallbackBuffers[i] = new byte[bufSize];
            }
            camera.addCallbackBuffer(mPreviewCallbackBuffers[i]);
        }
        camera.setPreviewCallbackWithBuffer(previewCallback);
    }

    @Override
    public void StopCamera() {
        Log.i("Test","StopCamera..1");
        if (camera != null)
        {
            Log.i("Test","StopCamera..1.1");
            camera.stopPreview();
            camera.release() ;
            Log.i("Test","StopCamera..1.2");
            if (iCameraStatusLinstener != null){
                iCameraStatusLinstener.OnCameraStop(this);
            }
        }
        camera = null;
        Log.i("Test", "StopCamera..2");
        isUsed	= false;

    }

    /**
     * 根据当前横竖屏状态，判断摄像头该偏转多少度。
     * @param context
     * @param cameraId		前置或后置摄像头
     * @return 偏转的角度
     */
    private int getCameraDisplayOrientation (Context context, int cameraId) {
        if (context == null)
            return 0;
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo (cameraId , info);
        int rotation = ((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
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

}
