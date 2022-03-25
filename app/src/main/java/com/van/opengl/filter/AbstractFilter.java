package com.van.opengl.filter;

import android.content.Context;
import android.opengl.GLES10;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;


import com.van.opengl.OpenGLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_TEXTURE0;

public class AbstractFilter {
    //    顶点着色器
    //    片元着色器
    public int program;
    //句柄  gpu中  vPosition
    protected   int vPosition;
    FloatBuffer textureBuffer; // 纹理坐标
    protected    int vCoord;
    private   int vTexture;
    private   int vMatrix;
    protected int mWidth;
    protected int mHeight;
    private float[] mtx;
    //gpu顶点缓冲区
    FloatBuffer vertexBuffer; //顶点坐标缓存区
    //世界坐标
    float[] VERTEX = {
            -1.0f,-1.0f, //左上角
            1.0f, -1.0f, //右上角
            -1.0f,1.0f,  //左下角
            1.0f, 1.0f   //右下角
    };
    //android纹理坐标
    float[] TEXTURE = {
            0.0f, 0.0f, //左上
            1.0f, 0.0f, //右上
            0.0f, 1.0f, //左下
            1.0f, 1.0f, //右下
    };

    public AbstractFilter(Context context, int vertexShaderId, int fragmentShaderId) {
        //初始化顶点缓冲区
        vertexBuffer    = ByteBuffer.allocateDirect(VERTEX.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        vertexBuffer.clear();
        vertexBuffer.put(VERTEX);

        textureBuffer = ByteBuffer.allocateDirect(TEXTURE.length * 4).order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        textureBuffer.clear();
        textureBuffer.put(TEXTURE);

        //
        String vertexSharder = OpenGLUtils.readRawTextFile(context, vertexShaderId);
//  先编译    再链接   再运行  程序
        String fragSharder = OpenGLUtils.readRawTextFile(context, fragmentShaderId);
//cpu 1   没有用  索引     program gpu
        program = OpenGLUtils.loadProgram(vertexSharder, fragSharder);

        vPosition = GLES20.glGetAttribLocation(program, "vPosition");//0
        //接收纹理坐标，接收采样器采样图片的坐标
        vCoord = GLES20.glGetAttribLocation(program, "vCoord");//1
        //采样点的坐标
        vTexture = GLES20.glGetUniformLocation(program, "vTexture");
    }

    public void setSize(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    public void setTransformMatrix(float[] mtx) {
        this.mtx = mtx;
    }

    /**
     * @param texture       纹理句柄
     * @param isOESTexture  是否是外部扩展纹理，camera预览时必须使用外部扩展纹理 OES
     * @return
     */
    //摄像头数据  渲染   摄像  开始渲染
    public int onDraw(int texture, boolean isOESTexture) {
        GLES20.glEnable(GLES10.GL_MULTISAMPLE);
        GLES20.glEnable(GLES10.GL_POINT_SMOOTH);
        GLES20.glEnable(GLES10.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
//        opengl  fbo  有 1   没有  2
//View 的大小
        GLES20.glViewport(0, 0, mWidth, mHeight);
        Log.d("fileter", "width="+mWidth + ", height="+mHeight);
//        使用程序
        GLES20.glUseProgram(program);
//        从索引位0的地方读
        vertexBuffer.position(0);
        //     index   指定要修改的通用顶点属性的索引。
//     size  指定每个通用顶点属性的组件数。
        //        type  指定数组中每个组件的数据类型。
        //        接受符号常量GL_FLOAT  GL_BYTE，GL_UNSIGNED_BYTE，GL_SHORT，GL_UNSIGNED_SHORT或GL_FIXED。 初始值为GL_FLOAT。
//      normalized    指定在访问定点数据值时是应将其标准化（GL_TRUE）还是直接转换为定点值（GL_FALSE）。

        GLES20.glVertexAttribPointer(vPosition,2,GL_FLOAT, false,0,vertexBuffer);
//        生效
        GLES20.glEnableVertexAttribArray(vPosition);


        textureBuffer.position(0);
        GLES20.glVertexAttribPointer(vCoord, 2, GLES20.GL_FLOAT,
                false, 0, textureBuffer);
        //CPU传数据到GPU，默认情况下着色器无法读取到这个数据。 需要我们启用一下才可以读取
        GLES20.glEnableVertexAttribArray(vCoord);

//        形状就确定了

//         32  数据
//gpu    获取读取    //使用第几个图层
        GLES20.glActiveTexture(GL_TEXTURE0);

//生成一个采样
        if (isOESTexture){
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,texture);
        }else{
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
        }

        GLES20.glUniform1i(vTexture, 0);
//        模板方法
        beforeDraw();
//通知 渲染画面 画画
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);
        drawEnd();
        if (isOESTexture){
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,0);
        }else{
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,0);
        }
        return texture;
    }
    public void beforeDraw() {
    }
    public void drawEnd(){

    }

    public void release(){
        GLES20.glDeleteProgram(program);
    }
}
