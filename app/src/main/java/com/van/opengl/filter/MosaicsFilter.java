package com.van.opengl.filter;

import static android.opengl.GLES20.glBindFramebuffer;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.van.opencv.Face;
import com.van.camencode.R;


/**
 * @program: CameraGLRender
 * @description: 马赛克filter
 * @author: Van
 * @create: 2022-05-20 15:16
 **/
public class MosaicsFilter extends AbstractFboFilter{

    private static final String TAG = "MosaicsFilter";
    private Face mFace;
    private int[] mTextureId;

    /*gl中人脸位置*/
    private int faceX;
    private int faceY;
    private int faceWidth;
    private int faceHeight;
    private int faceCount;

    public MosaicsFilter(Context mContext) {
        super(mContext, R.raw.base_vert, R.raw.mosaics_frag);

        faceX = GLES20.glGetUniformLocation(program, "faceX");
        faceY = GLES20.glGetUniformLocation(program, "faceY");
        faceWidth = GLES20.glGetUniformLocation(program, "faceWidth");
        faceHeight = GLES20.glGetUniformLocation(program, "faceHeight");
        faceCount = GLES20.glGetUniformLocation(program, "faceCount");
    }

    public void setFace(Face face) {
        this.mFace = face;
    }

    @Override
    public void beforeDraw() {
        super.beforeDraw();
    }

    @Override
    public int onDraw(int texture, boolean isOESTexture) {
        if (null == mFace){
            return texture;
        }
        glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer[0]);
        //开启混合模式
        GLES20.glEnable(GLES20.GL_BLEND);


        int count   = mFace.count;
        if (count > 50)
            count   = 50;

        GLES20.glUniform1i(faceCount, count);

        float gl_x_arr[] = new float[50];
        float gl_y_arr[] = new float[50];
        float gl_width_arr[] = new float[50];
        float gl_height_arr[] = new float[50];


        for (int i=0; i< count; i++){
            //这里的坐标是相对于 传入opencv识别的图像的像素，需要转换为在屏幕的位置
            float gl_x  = getGlFloat(mFace.faceX[i], mFace.imgWidth);
            float gl_y  = getGlFloat(mFace.faceY[i], mFace.imgHeight);
            float gl_width  =  (getGlFloat(mFace.faceX[i] + mFace.faceWidth[i], mFace.imgWidth));
            float gl_height =  (getGlFloat(mFace.faceY[i] + mFace.faceHeight[i], mFace.imgHeight));

            gl_x_arr[i]         = gl_x;
            gl_y_arr[i]         = gl_y;
            gl_width_arr[i]     = gl_width;
            gl_height_arr[i]     = gl_height;
//            Log.d(TAG, "左="+gl_x+", 右="+gl_width + ", 上=" + gl_y + ", 下="+gl_height);
        }

//        x = x / mFace.imgWidth * mWidth;
//        y = y / mFace.imgHeight * mHeight;
//        Log.d(TAG, "face x="+x + ", y="+y + ", face="+mFace.toString());
//        Log.d(TAG, "face 左边=" + x + ", 下边 = " + y);
//        Log.d(TAG, "face 宽度=" + mFace.width + ", height=" + mFace.height);

//        Log.d(TAG, "左="+gl_x+", 右="+gl_width);
//        gl_x    = 0.5f;
//        gl_y    = 0.1f;
//        gl_width = 1.0f;
//        gl_height   = 1.0f;

        //设置马赛克位置
        GLES20.glUseProgram(program);
        GLES20.glUniform1fv(faceX, 50, gl_x_arr, 0);
        GLES20.glUniform1fv(faceY, 50, gl_y_arr, 0);
        GLES20.glUniform1fv(faceWidth, 50, gl_width_arr, 0);
        GLES20.glUniform1fv(faceHeight, 50, gl_height_arr, 0);

//        vertexBuffer.position(0);
//        glVertexAttribPointer(vPosition, 2, GL_FLOAT, false, 0, vertexBuffer);
//        glEnableVertexAttribArray(vPosition);
//
//        textureBuffer.position(0);
//        glVertexAttribPointer(vCoord, 2, GL_FLOAT, false, 0, textureBuffer);
//        glEnableVertexAttribArray(vCoord);
//
//        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);  //
        return super.onDraw(texture, isOESTexture);

    }

    //获取gl中的坐标，0~1
    private float getGlFloat(float p, float total){
        return ( p / total);
    }

}
