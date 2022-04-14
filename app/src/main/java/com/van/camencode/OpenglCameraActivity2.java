package com.van.camencode;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;


import java.io.File;

import androidx.annotation.Nullable;

import com.van.opengl.CameraSurface;

public class OpenglCameraActivity2 extends Activity implements View.OnClickListener {

    private RelativeLayout  mainRel;
    private CameraSurface cameraSurface;
    private Button          btnCameraFacing;
    private Button          btnReset;
    private Button btnRecord;

    private boolean isRecording = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opengl2_camera);
        findView();
        init();
    }

    private void findView(){
        mainRel = findViewById(R.id.mainRel);
        btnReset = findViewById(R.id.btnReset);
        btnCameraFacing = findViewById(R.id.btnCameraFacing);
        btnCameraFacing.setOnClickListener(this);
        btnReset.setOnClickListener(this);
        btnRecord   = findViewById(R.id.btnRecord);
        btnRecord.setOnClickListener(this);
    }

    private void init(){
        cameraSurface = new CameraSurface(this);
        cameraSurface.create();
        mainRel.addView(cameraSurface);
//        mediaRecordUtil = new MediaRecordUtil(this, surface2, 10);
    }

    @Override
    protected void onDestroy() {
//        cameraSurface.destory();
        super.onDestroy();
    }



    @Override
    protected void onPause() {
        super.onPause();
//        if (cameraSurface != null)
//            cameraSurface.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
//        if (cameraSurface != null)
//            cameraSurface.onResume();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnCameraFacing:
                cameraSurface.cameraFacingChanged();
//                Intent intent = new Intent(OpenglCameraActivity2.this, WellcomActivity.class);
//                startActivity(intent);
                break;
            case R.id.btnRecord:
                record();
                break;
            case R.id.btnReset:
//                cameraSurface.stopRecord();
//                cameraSurface.startRecord();
                break;
        }
    }


    private void record(){
        if (isRecording){
            cameraSurface.stopRecord();
            isRecording = false;
            btnRecord.setText("开始录像");
        }else{
            File file   = new File(Environment.getExternalStorageDirectory().toString()+"/111.mp4");
            cameraSurface.startRecord(file);
            isRecording = true;
            btnRecord.setText("停止录像");
        }
    }
}
