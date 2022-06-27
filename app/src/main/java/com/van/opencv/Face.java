package com.van.opencv;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class Face {

    //人脸总数
    public int count;

    //保存人脸x， y坐标
    public float[] faceX;
    public float[] faceY;
    // 保存人脸的宽、高
    public float[] faceWidth;
    public float[] faceHeight;

    //送去检测图片的宽、高
    public int imgWidth;
    public int imgHeight;

    //识别后的RGB图片
    public ByteBuffer rgbBuffer;

    public Face(int count, float[] faceX, float[] faceY, float[] faceWidth, float[] faceHeight, int imgWidth, int imgHeight) {
        this.count = count;
        this.faceX = faceX;
        this.faceY = faceY;
        this.faceWidth = faceWidth;
        this.faceHeight = faceHeight;
        this.imgWidth = imgWidth;
        this.imgHeight = imgHeight;
        rgbBuffer = ByteBuffer.allocateDirect(imgWidth * imgHeight * 3).order(ByteOrder.nativeOrder());
    }
}
