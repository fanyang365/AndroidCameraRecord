package com.van.camencode;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupWindow;

public class PhotoPop extends PopupWindow {

    private Context mContext;
    private View			contentView;
    private MyView          myView;

    public PhotoPop(Context context, int width, int height){
        this.mContext	= context;
        //设置可以获得焦点
        setFocusable(true);
        //设置弹窗内可点击
        setTouchable(true);
        //设置弹窗外可点击
        setOutsideTouchable(true);

        //设置弹窗的宽度和高度
        setWidth(width);
        setHeight(height);

        //设置弹窗的布局界面
        contentView = LayoutInflater.from(mContext).inflate(R.layout.pop_photo, null);
//		contentView.getBackground().setAlpha(200);
        contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        setContentView(contentView);

        findView();
        init();
    }

    private void findView(){
        myView  = contentView.findViewById(R.id.myView);
    }

    private void init() {

    }


    public void showBitmap(Bitmap bitmap){
        myView.setBitmap(bitmap);
        myView.invalidate();
    }


    
    
}
