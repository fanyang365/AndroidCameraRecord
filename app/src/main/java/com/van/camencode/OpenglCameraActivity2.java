package com.van.camencode;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;


import java.io.File;
import java.util.concurrent.ArrayBlockingQueue;

import androidx.annotation.Nullable;

import com.van.android.hardcodec.AvcDecoder;
import com.van.opengl.CameraSurface;
import com.van.opengl.OpenGLAVCEncoder;

public class OpenglCameraActivity2 extends Activity implements View.OnClickListener, SurfaceHolder.Callback, OpenGLAVCEncoder.EncoderListener {

    private static final String TAG = "OpenglCameraActivity2";
    private RelativeLayout  mainRel;
    private CameraSurface cameraSurface;
    private Button          btnCameraFacing;
    private Button          btnReset;
    private Button btnRecord;
    private SurfaceView     decodeSurface;
    private AvcDecoder      avcDecoder;

    private boolean isRecording = false;

    private ArrayBlockingQueue<byte[]> dataQueue    = new ArrayBlockingQueue(100);

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
        decodeSurface   = findViewById(R.id.decodeSurface);
        btnCameraFacing = findViewById(R.id.btnCameraFacing);
        btnCameraFacing.setOnClickListener(this);
        btnReset.setOnClickListener(this);
        btnRecord   = findViewById(R.id.btnRecord);
        btnRecord.setOnClickListener(this);

        decodeSurface.getHolder().addCallback(this);
    }

    private void init(){
        cameraSurface = new CameraSurface(this);
        cameraSurface.setAVCEncoderLinstener(this);
        cameraSurface.create();
        avcDecoder      = new AvcDecoder(1080, 1920);
        mainRel.addView(cameraSurface);
//        mediaRecordUtil = new MediaRecordUtil(this, surface2, 10);
        new Thread(new DecodeThread()).start();
    }

    private volatile  boolean isStop  = false;

    private class DecodeThread implements Runnable{

        @Override
        public void run() {
            while (!isStop && !Thread.interrupted()){
                try {
                    byte[] data = dataQueue.take();
                    if (avcDecoder != null && isSurfaceCreated){
//                        Log.d(TAG, "DecodeThread 111 size = "+dataQueue.size());
                        avcDecoder.decode(data, 0, data.length, null, 0);
//                        Log.d(TAG, "DecodeThread 222 size = "+dataQueue.size());
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void stopDecode(){
        isStop  = true;
        dataQueue.clear();
    }

    @Override
    protected void onDestroy() {
        stopDecode();
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

    boolean isSurfaceCreated    = false;

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        avcDecoder.init(holder.getSurface());
        isSurfaceCreated    = true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopDecode();
    }

    @Override
    public void onH264Data(byte[] data) {
//        if (avcDecoder != null && isSurfaceCreated){
//            avcDecoder.decode(data, 0, data.length, null, 0);
//        }
        dataQueue.add(data);
    }
}
