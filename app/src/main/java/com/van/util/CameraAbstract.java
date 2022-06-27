package com.van.util;


import android.graphics.SurfaceTexture;

/**
 * 摄像头基类
 */
public abstract class CameraAbstract {

    public static final int		VIDEO_HARDWARE_ENCODE						= 1;		//硬编码
    public static final int		VIDEO_SOFT_ENCODE							= 0;		//软编码

    public static final int		CAMERA_FACING_USB							= 3;

    /**cif清晰度	 */
    public static final int		VIDEO_DEFINITION_CIF					= 0;
    /**D1清晰度	 */
    public static final int		VIDEO_DEFINITION_D1						= 1;
    /**720P清晰度	 */
    public static final int		VIDEO_DEFINITION_720P					= 2;
    /**10800P清晰度	 */
    public static final int		VIDEO_DEFINITION_1080P					= 3;

    public static final int		VIDEO_DEFINITION_MID					= 4;

    protected volatile int				camera_video_width = 1920;//默认宽度
    protected volatile int				camera_video_height = 1080;//默认高度
    /*视频显示的texture*/
    protected SurfaceTexture surfaceTexture;
    protected int				frameRate			= 26;
    protected int				currentCameraDeflection;//本地摄像头偏转角度
    protected int				cameraOrientation;//传输给服务器的摄像头旋转参数，摄像头编码数据时用。
    protected int				currentdefinition;			//当前清晰度
    protected int 			    currentCodeRate;//当前码率

    protected ICameraStatus     iCameraStatusLinstener;

    public int getCurrentCodeRate() {
        return currentCodeRate;
    }

    public void setCurrentCodeRate(int currentCodeRate) {
        if (currentCodeRate>8192) currentCodeRate=8192;
        if (currentCodeRate<128) currentCodeRate=128;
        this.currentCodeRate = currentCodeRate;
    }

    public int getCamera_video_width() {
        return camera_video_width;
    }

    public void setCamera_video_width(int camera_video_width) {
        this.camera_video_width = camera_video_width;
    }

    public int getCamera_video_height() {
        return camera_video_height;
    }

    public void setCamera_video_height(int camera_video_height) {
        this.camera_video_height = camera_video_height;
    }

    public int getCurrentCameraDeflection() {
        return currentCameraDeflection;
    }

    public void setCurrentCameraDeflection(int currentCameraDeflection) {
        if (currentCameraDeflection>360) currentCameraDeflection=360;
        if (currentCameraDeflection<0) currentCameraDeflection=0;
        this.currentCameraDeflection = currentCameraDeflection;
    }

    public int getCameraOrientation() {
        return cameraOrientation;
    }

    public void setCameraOrientation(int cameraOrientation) {
        this.cameraOrientation = cameraOrientation;
    }

    public int getCurrentdefinition() {
        return currentdefinition;
    }
    public void setCurrentdefinition(int currentdefinition) {
        this.currentdefinition = currentdefinition;
    }

    public ICameraStatus getiCameraStatusLinstener() {
        return iCameraStatusLinstener;
    }

    public void setiCameraStatusLinstener(ICameraStatus iCameraStatusLinstener) {
        this.iCameraStatusLinstener = iCameraStatusLinstener;
    }


    public SurfaceTexture getSurfaceTexture() {
        return surfaceTexture;
    }

    public void setSurfaceTexture(SurfaceTexture surfaceTexture) {
        this.surfaceTexture = surfaceTexture;
    }


    /**
     * 打开摄像机
     * @return
     */
    abstract boolean    StartCamera();

    /*关闭摄像机*/
    abstract void       StopCamera();



}
