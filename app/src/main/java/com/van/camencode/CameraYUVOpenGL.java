package com.van.camencode;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;

import androidx.annotation.Nullable;

import com.van.opengl.YUVCameraRender2;

public class CameraYUVOpenGL extends Activity implements View.OnClickListener{

    private static final String TAG = "CameraYUVOpenGL";
    private TextureView textureView;
    private GLSurfaceView   glSurfaceView;

    private YUVCameraRender2 render;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_yuv_opengl);
        findView();
        init();
    }

    @Override
    protected void onDestroy() {
        render.release();
        super.onDestroy();
    }

    private void findView(){
        textureView = findViewById(R.id.textureView);
        glSurfaceView = findViewById(R.id.glSurfaceView);

    }

    private void init(){
        render  = new YUVCameraRender2(this, glSurfaceView);

        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setRenderer(render);
        glSurfaceView.setPreserveEGLContextOnPause(true);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){

        }
    }

}
