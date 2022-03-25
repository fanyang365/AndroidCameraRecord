package com.van.opengl.filter;

import android.content.Context;

import com.van.camencode.R;

public class RecordFilter extends AbstractFilter{
//    输出屏幕
    public RecordFilter(Context context){
        super(context, R.raw.base_vert, R.raw.base_frag);
    }

}
