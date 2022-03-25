package com.van.camencode;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;


import com.van.util.Camera2Helper;
import com.van.util.CameraHelper;
import com.van.util.MediaRecordUtil;

import androidx.annotation.Nullable;

public class SurfaceTextureActivity extends Activity implements TextureView.SurfaceTextureListener, View.OnClickListener {

    private static final String TAG = "SurfaceTextureActivity";
    private TextureView textureView;
    private Camera2Helper cameraHelper;

    private boolean isRecording = false;
    private Button btnRecord;
    private MediaRecordUtil mediaRecordUtil;
    private SurfaceView surface;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_surface_texture);
        findView();
        init();
    }

    private void findView(){
        textureView = findViewById(R.id.textureView);
        btnRecord   = findViewById(R.id.btnRecord);
        btnRecord.setOnClickListener(this);
    }

    private void init(){
        cameraHelper    = new Camera2Helper(this);
        textureView.setSurfaceTextureListener(this);

    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        cameraHelper.setSurfaceTexture(surface);
        cameraHelper.StartCamera();
        Surface surface2     = new Surface(surface);
        mediaRecordUtil = new MediaRecordUtil(this, surface2, 10);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "onSurfaceTextureSizeChanged width="+width);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.d(TAG, "onSurfaceTextureDestroyed");
        return false;
    }


    public long				m_last_time_stamp 	= System.currentTimeMillis();
    public int				m_preview_rate		= 0;
    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
//        Log.d(TAG, "onSurfaceTextureUpdated 11");
        long		current_time_stamp = System.currentTimeMillis();
        m_preview_rate++;
        if ((current_time_stamp-m_last_time_stamp) >= 1000)
        {
            Log.i("Test", "当前帧率="+m_preview_rate+",timestamp="+current_time_stamp+",时间差:"+(current_time_stamp-m_last_time_stamp));
            m_last_time_stamp 	= current_time_stamp;
            m_preview_rate		= 0;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnRecord:
                record();
                break;
        }
    }

    private void record(){
        if (isRecording){
            mediaRecordUtil.stopRecord();
            isRecording = false;
            btnRecord.setText("开始录像");
        }else{
            mediaRecordUtil.startRecord();
            isRecording = true;
            btnRecord.setText("停止录像");
        }
    }

}
