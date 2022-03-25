package com.van.util;

import android.content.Context;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.RequiresApi;

public class MediaRecordUtil {
    private static final String TAG = "cj";

    Context context;
    private android.media.MediaRecorder mediaRecorder;
    private boolean isRecording;
    private int time;
    private Surface surface;

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            Toast.makeText(context, "停止录像，并保存文件", Toast.LENGTH_SHORT).show();
        }
    };

    public MediaRecordUtil(Context context, Surface surface, int time) {
        this.context = context;
        this.surface    = surface;
        this.time = time;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void startRecord() {
        //准备名字
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmm");
        String format = dateFormat.format(date);
        String path = Environment.getExternalStorageDirectory().getAbsolutePath();
        String name = path + "/" + format + ".mp4";
        //准备好了

        try {
            File file = new File(name);
            if (file.exists()) {
                // 如果文件存在，删除它，演示代码保证设备上只有一个录音文件
                file.delete();
            }

            mediaRecorder = new android.media.MediaRecorder();
//			mediaRecorder.reset();

//            mediaRecorder.setCamera(Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT));
            // 设置音频录入源
            mediaRecorder.setAudioSource(android.media.MediaRecorder.AudioSource.MIC);
            // 设置视频图像的录入源
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            Log.e(TAG, "start: 输出格式");
            // 设置录入媒体的输出格式
            mediaRecorder.setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4);
            // 设置音频的编码格式
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            // 设置视频的编码格式
//			mediaRecorder.setVideoEncoder(android.media.MediaRecorder.VideoEncoder.MPEG_4_SP);
            mediaRecorder.setVideoEncoder(android.media.MediaRecorder.VideoEncoder.H264);
            // 设置视频的采样率，每秒4帧
            mediaRecorder.setVideoFrameRate(60);

            // 设置视频录制的分辨率。必须放在设置编码和格式的后面，否则报错
//			mediaRecorder.setVideoSize(240, 240);
            //一换这个6.0就走起来了
            CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);

            mediaRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
//			mediaRecorder.setVideoEncodingBitRate(640*480);
            // 设置录制视频文件的输出路径
            Log.e(TAG, "start: 输出路径");
            mediaRecorder.setOutputFile(file.getAbsolutePath());
            // 设置捕获视频图像的预览界面
//            mediaRecorder.setPreviewDisplay(surface);
//			mediaRecorder.setOrientationHint(180);// 视频旋转

            Log.e(TAG, "start: surfaceholder也有了");
            mediaRecorder.setOnErrorListener(new android.media.MediaRecorder.OnErrorListener() {

                @Override
                public void onError(android.media.MediaRecorder mr, int what, int extra) {
                    Log.e(TAG, "onError: 录制出错");
                    // 发生错误，停止录制
                    mediaRecorder.stop();
                    mediaRecorder.release();
                    mediaRecorder = null;
                    isRecording = false;

                    Toast.makeText(context, "录制出错", Toast.LENGTH_SHORT).show();
                }

            });

            // 准备、开始
            mediaRecorder.prepare();
            Log.e(TAG, "start: 不走了");
            mediaRecorder.start();
            Log.e(TAG, "start: 也开始了");

            isRecording = true;
            Toast.makeText(context, "开始录像", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "startRecord: "+e.toString() );
        }

        //录制5秒后自动停止
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                stopRecord();
            }
        },time);
    }

    public  void stopRecord(){
        if (isRecording) {
            // 如果正在录制，停止并释放资源
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording=false;

            new Thread(new Runnable() {
                @Override
                public void run() {
                    handler.sendEmptyMessage(0);
                }
            }).start();
        }else{
            Log.e(TAG, "已经停止录制" );
        }
    }

}
