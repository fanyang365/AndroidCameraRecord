package com.van.opengl.filter;

import android.content.Context;
import android.opengl.GLES20;

import com.van.camencode.R;

public class SoulFilter extends AbstractFboFilter {
    private int mixturePercent;
    private int scalePercent;

    public SoulFilter(Context context) {
        super(context, R.raw.base_vert, R.raw.soul_frag);
        mixturePercent = GLES20.glGetUniformLocation(program, "mixturePercent");
        scalePercent = GLES20.glGetUniformLocation(program, "scalePercent");
    }


    float mix = 0.0f; //透明度，越大越透明
    float scale = 0.0f; //缩放，越大就放的越大

    @Override
    public void beforeDraw( ) {
        super.beforeDraw( );
        GLES20.glUniform1f(mixturePercent, 1.0f - mix);
        GLES20.glUniform1f(scalePercent, scale + 1.0f);
        mix += 0.08f;
        scale += 0.08f;
        if (mix >= 1.0) {
            mix = 0.0f;
        }
        if (scale >= 1.0) {
            scale = 0.0f;
        }
    }

    @Override
    public int onDraw(int texture, boolean isOESTexture) {
        super.onDraw(texture, isOESTexture);
        GLES20.glUniform1f(mixturePercent, 1.0f - mix);
        GLES20.glUniform1f(scalePercent, scale + 1.0f);


        mix += 0.08f;
        scale += 0.08f;
        if (mix >= 1.0) {
            mix = 0.0f;
        }
        if (scale >= 1.0) {
            scale = 0.0f;
        }
        return texture;
    }

}
