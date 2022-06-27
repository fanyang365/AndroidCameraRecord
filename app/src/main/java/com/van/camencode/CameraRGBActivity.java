package com.van.camencode;

import android.app.Activity;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.van.android.hardcodec.AACEncoder;
import com.van.android.hardcodec.AVMuxer;
import com.van.android.hardcodec.AudioCapture;
import com.van.opengl.OpenGLAVCEncoder;
import com.van.opengl.RGBCameraRender;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

public class CameraRGBActivity extends Activity implements View.OnClickListener , AudioCapture.MyAudioRecordLinstener , AACEncoder.OnAACEncodeListener {

    private static final String TAG = "CameraRGBActivity";
    private GLSurfaceView glSurfaceView;
    private RGBCameraRender render;

    private Button btnCameraFacing;
    private Button          btnReset;
    private Button btnRecord;
    private boolean isRecording = false;
    private TextView        textFPS;
    private AVMuxer         avMuxer;
    private AudioCapture    audioCapture;
    private AACEncoder      aacEncoder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_rgbactivity);
        findView();
        init();
    }

    @Override
    protected void onDestroy() {
        render.release();
        super.onDestroy();
    }


    private void findView(){
        glSurfaceView = findViewById(R.id.glSurfaceView);
        textFPS       = findViewById(R.id.textFPS);
        btnReset = findViewById(R.id.btnReset);
        btnCameraFacing = findViewById(R.id.btnCameraFacing);
        btnRecord = findViewById(R.id.btnRecord);
        btnCameraFacing.setOnClickListener(this);

        btnCameraFacing.setOnClickListener(this);
        btnReset.setOnClickListener(this);
        btnRecord.setOnClickListener(this);

    }

    private void init(){
        render  = new RGBCameraRender(this, glSurfaceView);

        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setRenderer(render);
        glSurfaceView.setPreserveEGLContextOnPause(true);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        render.setRenderListener(new RGBCameraRender.RenderListener() {
            @Override
            public void onFPSUpdate(int fps) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textFPS.setText("FPS:"+fps);
                    }
                });
            }
        });

        render.setAVCEncoderLinstener(new OpenGLAVCEncoder.EncoderListener() {

            @Override
            public void onH264Data(ByteBuffer buffer, MediaCodec.BufferInfo info) {
                if (avMuxer == null)
                    return ;
                boolean ok = avMuxer.addMuxerData(new AVMuxer.MuxerData(AVMuxer.TRACK_VIDEO, buffer, info));
                Log.d(TAG, "添加视频="+ok + " info = "+info.presentationTimeUs);
            }

            @Override
            public void onMediaFormatChanged(MediaFormat mediaFormat) {
                if (avMuxer == null)
                    return ;
                avMuxer.addVideoTrack(mediaFormat);
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnCameraFacing:
                render.cameraFacingChanged();
                break;
            case R.id.btnRecord:
//                record();
                record2();
                break;
            case R.id.btnReset:
//                cameraSurface.stopRecord();
//                cameraSurface.startRecord();
                break;
        }
    }

    private void record(){
        if (isRecording){
            render.stopRecord();
            isRecording = false;
            btnRecord.setText("开始录像");
        }else{
            File file   = new File(Environment.getExternalStorageDirectory().toString()+"/111.mp4");
            render.startRecord(file);
            isRecording = true;
            btnRecord.setText("停止录像");
        }
    }

    private void record2(){
        if (!isRecording || avMuxer == null){
            avMuxer = new AVMuxer();
            File file   = new File(Environment.getExternalStorageDirectory().toString()+"/222.mp4");
            avMuxer.start(file);

            //添加音频轨道
            if (audioCapture == null){
                aacEncoder  = new AACEncoder();
                aacEncoder.setOnAACEncodeListener(this);
                aacEncoder.start();
                audioCapture = new AudioCapture(this);
                audioCapture.start();
                avMuxer.addAudioTrack(aacEncoder.getOutputFormat());
            }

            //添加视频轨道
            render.startEncode();
            isRecording = true;
            btnRecord.setText("停止录像");
        }else{
            avMuxer.stop();
            avMuxer = null;
            render.stopEncode();

            audioCapture.stopRecord();
            audioCapture = null;
            aacEncoder.stop();
            aacEncoder = null;

            isRecording = false;
            btnRecord.setText("开始录像");
        }
    }

    @Override
    public void OnAudioRecord(byte[] data) {
        aacEncoder.putAudioData(data);
    }

    @Override
    public void onFormatChanged(MediaFormat mediaFormat) {

    }

    @Override
    public void onEncodedFrame(byte[] data, MediaCodec.BufferInfo bufferInfo) {

    }

    @Override
    public void onEncodedFrame(ByteBuffer data, MediaCodec.BufferInfo bufferInfo) {
        if (avMuxer == null)
            return ;
        if (bufferInfo.presentationTimeUs <= 0 )
            return ;
        boolean ok = avMuxer.addMuxerData(new AVMuxer.MuxerData(AVMuxer.TRACK_AUDIO, data, bufferInfo));
        Log.d(TAG, "添加音频="+ok + " info = "+bufferInfo.presentationTimeUs);
    }
}