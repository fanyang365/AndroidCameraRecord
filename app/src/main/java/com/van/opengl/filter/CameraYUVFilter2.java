package com.van.opengl.filter;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.van.camencode.R;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

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

public class CameraYUVFilter2 extends AbstractFboFilter{

    private static final String TAG = "CameraYUVFilter2";
    private final int mMVPMatrixLocation;
    protected final int mUniformYTextureLocation;
    protected final int mUniformUVTextureLocation;
    private ByteBuffer mYBuffer, mUVBuffer;
    public int mYTestureId, mUVTextureId;
    private float[] mMatrix = new float[16];
    private int degrees = 0;

    public CameraYUVFilter2(Context context) {
        super(context, R.raw.yuv_vertex2, R.raw.yuv_fragment2);

        //android纹理坐标
        float[] TEXTURE = {
                1.0f, 0.0f, //右上
                0.0f, 0.0f, //左上
                1.0f, 1.0f, //右下
                0.0f, 1.0f, //左下
        };

        textureBuffer.clear();
        textureBuffer.put(TEXTURE);

        mUniformYTextureLocation = glGetUniformLocation(program, "y_texture");
        mUniformUVTextureLocation = glGetUniformLocation(program, "uv_texture");
        mMVPMatrixLocation = glGetUniformLocation(program, "uMVPMatrix");

        int[] textures = new int[2];
        glGenTextures(2, textures, 0);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textures[0]);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        mYTestureId = textures[0];

        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, textures[1]);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        mUVTextureId = textures[1];
    }

    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);

        mYBuffer = ByteBuffer.allocateDirect(mWidth * mHeight)
                .order(ByteOrder.nativeOrder());

        mUVBuffer = ByteBuffer.allocateDirect(mWidth * mHeight / 2)
                .order(ByteOrder.nativeOrder());

//        Log.d(TAG, "width="+mWidth+", height="+mHeight);
    }


    @Override
    public void beforeDraw() {
        super.beforeDraw();
    }

    @Override
    public void drawEnd() {
        super.drawEnd();
    }

    public int draw(int texture, boolean isOESTexture, byte[] data) {
        super.onDraw(texture, isOESTexture);
        glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer[0]);
        mYBuffer.position(0);
        mYBuffer.put(data, 0, mWidth * mHeight);

        mUVBuffer.position(0);
        mUVBuffer.put(data, mWidth * mHeight, mWidth * mHeight / 2);

        mYBuffer.position(0);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, mYTestureId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE, mWidth, mHeight,
                0, GL_LUMINANCE, GL_UNSIGNED_BYTE, mYBuffer);
        glUniform1i(mUniformYTextureLocation, 0);

        mUVBuffer.position(0);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, mUVTextureId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE_ALPHA, mWidth / 2, mHeight / 2,
                0, GL_LUMINANCE_ALPHA, GL_UNSIGNED_BYTE, mUVBuffer);
        glUniform1i(mUniformUVTextureLocation, 1);

        Matrix.setIdentityM(mMatrix, 0);
        Log.d(TAG, "旋转角度="+degrees);
        Matrix.rotateM(mMatrix, 0, degrees, 0.0f, 0.0f, 1.0f);

        GLES20.glUniformMatrix4fv(mMVPMatrixLocation, 1, false, mMatrix, 0);

        vertexBuffer.position(0);
        glVertexAttribPointer(vPosition, 2, GL_FLOAT, false, 0, vertexBuffer);
        glEnableVertexAttribArray(vPosition);

        textureBuffer.position(0);
        glVertexAttribPointer(vCoord, 2, GL_FLOAT, false, 0, textureBuffer);
        glEnableVertexAttribArray(vCoord);

        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
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
