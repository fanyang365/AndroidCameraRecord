package com.van.opengl.filter;

import android.content.Context;

import com.van.camencode.R;

public class ScreenFilter extends AbstractFilter {
    public ScreenFilter(Context context) {
        super(context, R.raw.base_vert, R.raw.base_frag);
    }
}
