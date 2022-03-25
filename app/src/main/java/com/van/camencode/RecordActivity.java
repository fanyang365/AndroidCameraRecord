package com.van.camencode;

import android.app.Activity;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import com.van.util.MediaRecordUtil;

import androidx.annotation.Nullable;

public class RecordActivity extends Activity implements View.OnClickListener {

    private boolean isRecording = false;
    private Button btnRecord;
    private MediaRecordUtil mediaRecordUtil;
    private SurfaceView     surface;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        findView();
        init();
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

    private void findView(){
        btnRecord   = findViewById(R.id.btnRecord);
        surface     = findViewById(R.id.surface);

        btnRecord.setOnClickListener(this);
    }

    private void init(){
//        mediaRecordUtil = new MediaRecordUtil(this, surface, 10);
    }
}
