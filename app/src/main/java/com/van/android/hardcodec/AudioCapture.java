package com.van.android.hardcodec;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import java.nio.ByteBuffer;

public class AudioCapture extends Thread{

    private ByteBuffer  audio_buffer;

    public interface MyAudioRecordLinstener{
        void OnAudioRecord(byte[] data);
    }

    private static String TAG = "HCVSAudioRecord";

    private final int kSampleRate = 16000;
    private final int kChannelMode = AudioFormat.CHANNEL_IN_MONO;
    private final int kEncodeFormat = AudioFormat.ENCODING_PCM_16BIT;

    private MyAudioRecordLinstener myAudioRecordLinstener;
    AudioRecord mRecord = null;
    boolean mReqStop = false;

    public  int kFrameSize = 1280;

    public AudioCapture(MyAudioRecordLinstener myAudioRecordLinstener){
        this.myAudioRecordLinstener = myAudioRecordLinstener;
        init();
    }

    private void audioInit(){

    }

    private void audioUnit(){

    }

    private void init() {
        audioInit();
        kFrameSize = AudioRecord.getMinBufferSize(kSampleRate, kChannelMode,
                kEncodeFormat);
        audio_buffer    = ByteBuffer.allocateDirect(kFrameSize);
        mRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                kSampleRate, kChannelMode, kEncodeFormat, kFrameSize );
    }

    private void recordAndPlay() {
        mRecord.startRecording();

        byte[] buffer = new byte[kFrameSize];
        int num = 0;
        while (!mReqStop) {
            num = mRecord.read(buffer, 0, kFrameSize);
            if (num <= 0)
            {
                try {
                    Thread.sleep(10);
//                    Log.d(TAG, "音频线程暂停10ms");
                    continue;
                }catch (InterruptedException e){

                }
            }
            if (myAudioRecordLinstener != null){
                myAudioRecordLinstener.OnAudioRecord(buffer);
            }
            audio_buffer.clear();
            audio_buffer.put(buffer);
//            Log.d(TAG, "buffer = " + buffer.toString() + ", num = " + num);
        }
//        Log.d(TAG, "clean up");
    }

    public void stopRecord() {
        mReqStop = true;
        if (mRecord != null){
            mRecord.stop();
            mRecord.release();
        }
        mRecord = null;
        audioUnit();
    }

    @Override
    public void run() {
        recordAndPlay();
    }
}
