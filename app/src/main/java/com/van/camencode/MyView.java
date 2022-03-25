package com.van.camencode;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class MyView extends View implements View.OnTouchListener {

    private Bitmap      bitmap;
    private Paint       mPaint;

    private Path        strokePath;//存储当前动作绘画的path
    private List<Path>  recordPath; //存储所有绘画路径

    public MyView(Context context) {
        super(context);
        init();
    }

    public MyView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MyView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init(){
        recordPath  = new ArrayList<>();
        mPaint = new Paint();
        mPaint.setAntiAlias(true);//去锯齿
        mPaint.setDither(true);//线条柔和
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(5);
        mPaint.setColor(Color.rgb(255, 0, 0));
        mPaint.setTextSize(60);
        setOnTouchListener(this);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        return super.dispatchTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawBitmap(canvas);
        drawRecord(canvas);
    }

    private void drawRecord(Canvas canvas) {
        for(int i=0; i<recordPath.size(); i++){
            canvas.drawPath(recordPath.get(i), mPaint);
        }
    }

    private void drawBitmap(Canvas canvas){
        if (bitmap != null) {
            int right = getMeasuredWidth();
            int bottom = getMeasuredHeight();

            Rect destRect = new Rect(0, 0, right, bottom);
            canvas.drawBitmap(bitmap, null, destRect, null);
        }
    }

    public float downX;	//action_down的x点
    public float downY; //action_down的y点
    public float preX; //上一次X点
    public float preY;	//上一次Y点
    public float curX;	//当前X点
    public float curY;	//当前y点
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        curX = event.getX();
        curY = event.getY();

        int pointID = event.getPointerId(event.getActionIndex());

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                touch_down();
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                touch_move();
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                touch_up();
                invalidate();
                break;
        }

        preX = curX;
        preY = curY;
        return true;
    }

    private void touch_up() {
    }

    private void touch_move() {
        strokePath.quadTo(preX, preY, (curX + preX) / 2, (curY + preY) / 2);
    }


    private void touch_down() {
        downX = curX;
        downY = curY;

        strokePath = new Path();
        strokePath.moveTo(downX, downY);
        recordPath.add(strokePath);
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }


}
