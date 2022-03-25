package com.van.opengl.filter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.opengl.GLES20;
import android.util.Log;


import com.van.camencode.R;
import com.van.opengl.OpenGLUtils;
import com.van.util.OSDBean;

import java.nio.ByteBuffer;


public class WaterFilter extends AbstractFboFilter{

    private static final String TAG = "WaterFilter";
    private volatile ByteBuffer bitmapBuffer;
    private Object object   = new Object();

    private int waterTextureId;
    private OSDBean osdBean;


    public WaterFilter(Context context, OSDBean osdBean) {
        super(context, R.raw.base_vert, R.raw.base_frag);
        this.osdBean    = osdBean;
        initWater();
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
        onDrawWater();
        super.drawEnd();
    }

    @Override
    public int onDraw(int texture, boolean isOESTexture) {

//        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
//        //设置背景颜色
//        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        super.onDraw(texture, isOESTexture);
        return frameTextures[0];
    }

    /*创建水印图片*/
    public static Bitmap createTextImage(OSDBean osdBean, boolean isRotate){
        if (osdBean == null)
            return null;
        Paint paint = new Paint();
        paint.setColor(Color.parseColor(osdBean.getTextColor()));
        paint.setTextSize(osdBean.getTextSize());
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);

        int padding = osdBean.getTextPadding();
        //取宽度最大值
        float width     = 0;
        float height    = 0;
        float lineHeight    = paint.getFontMetrics().bottom - paint.getFontMetrics().top;
        int   heightOffset    = (int) (padding+lineHeight);
        if (osdBean.getOSD1() != null && !osdBean.getOSD1().equals("")){
            width   = paint.measureText(osdBean.getOSD1());
            height  += (lineHeight+padding);
        }

        if (osdBean.getOSD2() != null && !osdBean.getOSD2().equals("")){
            width   = Math.max(width, paint.measureText(osdBean.getOSD2()));
            height  += (lineHeight+padding);
        }

        if (osdBean.getOSD3() != null && !osdBean.getOSD3().equals("")){
            width   = Math.max(width, paint.measureText(osdBean.getOSD3()));
            height  += (lineHeight+padding);
        }

        if (osdBean.getOSD4() != null && !osdBean.getOSD4().equals("")){
            width   = Math.max(width, paint.measureText(osdBean.getOSD4()));
            height  += (lineHeight+padding);
        }

        Bitmap bm   = Bitmap.createBitmap((int) (width + padding), (int)height, Bitmap.Config.ARGB_8888);
        Canvas canvas   = new Canvas(bm);
        canvas.drawColor(Color.parseColor(osdBean.getBgColor()));
        if (osdBean.getOSD1() != null && !osdBean.getOSD1().equals("")){
            canvas.drawText(osdBean.getOSD1(), padding, heightOffset, paint);
            heightOffset += (lineHeight + padding);
        }
        if (osdBean.getOSD2() != null && !osdBean.getOSD2().equals("")){
            canvas.drawText(osdBean.getOSD2(), padding, heightOffset, paint);
            heightOffset += (lineHeight + padding);
        }
        if (osdBean.getOSD3() != null && !osdBean.getOSD3().equals("")){
            canvas.drawText(osdBean.getOSD3(), padding, heightOffset, paint);
            heightOffset += (lineHeight + padding);
        }
        if (osdBean.getOSD4() != null && !osdBean.getOSD4().equals("")){
            canvas.drawText(osdBean.getOSD4(), padding, heightOffset, paint);
            heightOffset += (lineHeight + padding);
        }
        if (isRotate){
            return ConvertBitmap(bm, bm.getWidth(), bm.getHeight());
        }else {
            return bm;
        }


    }

    private void initWater() {
//        Bitmap bitmap = createTextImage("我是水印\n2021年4月28日14:03:56", 20, "#fff000", "#000000FF", 0);
        Bitmap bitmap = createTextImage(osdBean, true);
        osdWidth    = bitmap.getWidth();
        osdHeight   = bitmap.getHeight();
        synchronized (object){
            bitmapBuffer    = ByteBuffer.allocate(osdWidth * osdHeight * 4);
            Log.d(TAG, "updateOSD" + bitmapBuffer.limit());
            bitmap.copyPixelsToBuffer(bitmapBuffer);
            bitmapBuffer.flip();
        }
    }

    private boolean isUpdateOSD = false;
    private int osdWidth        = 0;
    private int osdHeight       = 0;

    public void updateOSD(OSDBean osdBean){
//        long time1      = System.currentTimeMillis();
        Bitmap bitmap   = createTextImage(osdBean, true);

//        int limitSize   = bitmap.getWidth() * bitmap.getHeight() * 4;
        synchronized (object){
            //新的图片较大, 重新生成缓冲区
//            if (limitSize >= bitmapBuffer.limit()){
                osdWidth    = bitmap.getWidth();
                osdHeight   = bitmap.getHeight();
                bitmapBuffer    = ByteBuffer.allocate(osdWidth * osdHeight * 4);
//            }
            bitmapBuffer.clear();
            bitmap.copyPixelsToBuffer(bitmapBuffer);
            bitmapBuffer.flip();
            isUpdateOSD = true;
        }
        bitmap.recycle();
//        long time2      = System.currentTimeMillis();
//        Log.d(TAG, "update OSD time = "+ (time2 - time1));
    }


    private void onDrawWater() {
        //        updateBitmap();
        //帖纸画上去
        //开启混合模式 ： 将多张图片进行混合(贴图)
        GLES20.glEnable(GLES20.GL_BLEND);
        //设置贴图模式
        // 1：src 源图因子 ： 要画的是源  (耳朵)
        // 2: dst : 已经画好的是目标  (从其他filter来的图像)
        //画耳朵的时候  GL_ONE:就直接使用耳朵的所有像素 原本是什么样子 我就画什么样子
        // 表示用1.0减去源颜色的alpha值来作为因子
        //  耳朵不透明 (0,0 （全透明）- 1.0（不透明）) 目标图对应位置的像素就被融合掉了 不见了
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        //画画
        //不是画全屏 定位到相应的位置
        //设置显示窗口
        //起始的位置
        float x = 20;
        float y = 20;
        GLES20.glViewport((int) x, (int) y,
                osdWidth,
                osdHeight);

        if (waterTextureId <= 0){
            int[] mTextureId = new int[1];
            OpenGLUtils.glGenTextures(mTextureId);
            waterTextureId  = mTextureId[0];
            //表示后续的操作 就是作用于这个纹理上
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, waterTextureId);
            // 将 Bitmap与纹理id 绑定起来
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, osdWidth, osdHeight,
                    0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, bitmapBuffer);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        }


            //表示后续的操作 就是作用于这个纹理上
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, waterTextureId);
        //如果更新了OSD，则重新绑定Image2d
        if (isUpdateOSD){
            synchronized (object){
                // 将 Bitmap与纹理id 绑定起来
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, osdWidth, osdHeight,
                        0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, bitmapBuffer);
                isUpdateOSD = false;
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            }
        }else{
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

    }


    /*翻转图片*/
    public static Bitmap ConvertBitmap(Bitmap a, int width, int height)
    {
        Matrix m = new Matrix();
//        m.postScale(1, -1);   //镜像垂直翻转
        m.postScale(-1, 1);   //镜像水平翻转
        m.postRotate(180);  //旋转180度
        Bitmap new2 = Bitmap.createBitmap(a, 0, 0, width, height, m, true);
        return new2;
    }


}
