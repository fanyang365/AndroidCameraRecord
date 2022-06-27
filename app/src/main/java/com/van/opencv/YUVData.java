package com.van.opencv;

/**
 * @program: AndroidCameraRecord
 * @description: YUV数据参数
 * @author: Van
 * @create: 2022-05-24 13:41
 **/
public class YUVData {

    private byte[] data;
    private int width;
    private int height;
    private int cameraFacing;
    private int cameraOrientation;

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getCameraFacing() {
        return cameraFacing;
    }

    public void setCameraFacing(int cameraFacing) {
        this.cameraFacing = cameraFacing;
    }

    public int getCameraOrientation() {
        return cameraOrientation;
    }

    public void setCameraOrientation(int cameraOrientation) {
        this.cameraOrientation = cameraOrientation;
    }
}
