package com.van.opengl.filter;

import static android.opengl.GLES20.GL_CLAMP_TO_EDGE;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_LUMINANCE;
import static android.opengl.GLES20.GL_LUMINANCE_ALPHA;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE1;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_S;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_T;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.GL_UNSIGNED_BYTE;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindFramebuffer;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glTexImage2D;
import static android.opengl.GLES20.glTexParameterf;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glVertexAttribPointer;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.van.camencode.R;
import com.van.opengl.OpenGLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class CameraRGBFilter extends AbstractFboFilter{

    private static final String TAG = "CameraYUVFilter2";
    private int degrees = 0;
    private int waterTextureId;
    private final int mMVPMatrixLocation;
    private float[] mMatrix = new float[16];

    public CameraRGBFilter(Context context) {
        super(context, R.raw.yuv_vertex2, R.raw.base_frag);
        mMVPMatrixLocation = glGetUniformLocation(program, "uMVPMatrix");
        //android纹理坐标
        float[] TEXTURE = {
                1.0f, 0.0f, //右上
                0.0f, 0.0f, //左上
                1.0f, 1.0f, //右下
                0.0f, 1.0f, //左下
        };

        textureBuffer.clear();
        textureBuffer.put(TEXTURE);
    }

    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);

    }


    @Override
    public void beforeDraw() {
        super.beforeDraw();
    }

    @Override
    public void drawEnd() {
        super.drawEnd();
    }

    public int draw(int texture, boolean isOESTexture, ByteBuffer rgbBuffer) {
        super.onDraw(texture, isOESTexture);
        glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer[0]);

        textureBuffer.position(0);
        glVertexAttribPointer(vCoord, 2, GL_FLOAT, false, 0, textureBuffer);
        glEnableVertexAttribArray(vCoord);

        if (waterTextureId <= 0){
            int[] mTextureId = new int[1];
            OpenGLUtils.glGenTextures(mTextureId);
            waterTextureId  = mTextureId[0];
        }


        Matrix.setIdentityM(mMatrix, 0);
//        Log.d(TAG, "旋转角度="+degrees);
        Matrix.rotateM(mMatrix, 0, 180, 0.0f, 0.0f, 1.0f);
//        Matrix.translateM(mMatrix, 0, 0.0f, 0.0f, 0.0f);
//        Matrix.scaleM(mMatrix, 0, 1.0f, 1.0f, 1.0f);

        GLES20.glUniformMatrix4fv(mMVPMatrixLocation, 1, false, mMatrix, 0);

        rgbBuffer.clear();
        //表示后续的操作 就是作用于这个纹理上
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, waterTextureId);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGB, mHeight, mWidth, 0, GLES20.GL_RGB, GLES20.GL_UNSIGNED_BYTE,
                rgbBuffer);

        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);  //
        return frameTextures[0];
    }

    public int getDegrees() {
        return degrees;
    }

    public void setDegrees(int degrees) {
        this.degrees = degrees;
    }
}
