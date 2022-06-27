package com.van.android.hardcodec;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class AVMuxer {

    public static final String TAG                      = "AVMuxer";
    public static final int TRACK_VIDEO = 0;
    public static final int TRACK_AUDIO = 1;

    private MediaMuxer  mediaMuxer;
    /** 视频轨道 */
    private int videoTrackIndex = -1;
    /** 音频轨道 */
    private int audioTrackIndex = -1;
    private boolean             isVideoAdd              = false;
    private boolean             isAudioAdd              = false;
    /** 是否正在混合 */
    private boolean             isMediaMuxerStart       = false;
    /** 混合线程是否启动 */
    private volatile boolean    isLooper                = false;

    public AVMuxer() {

    }

    public boolean start(String path){
        File file   = new File(path);
        return start(file);
    }

    public synchronized boolean start(File outputFile){
        if (outputFile == null ){
            throw new IllegalArgumentException("非法的存储路径");
        }

        if (isMediaMuxerStart || mediaMuxer != null){
            throw new RuntimeException("录像已经在进行！");
        }

        //初始化混合器
        try{
            mediaMuxer = new MediaMuxer(outputFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        }catch (IOException e){
            e.printStackTrace();
            return false;
        }

        isLooper    = true;
        return true;
    }

    public synchronized void stop(){
        Log.d(TAG, "====停止媒体混合线程=====");
        stopMediaMuxer();
        isLooper    = false;
    }

    /** 开起混合器 */
    private synchronized void startMuxer(){
        if (isMediaMuxerStart)
            return;

        if (isAudioAdd && isVideoAdd) {
            Log.d(TAG, "====启动媒体混合器=====");
            mediaMuxer.start();
            isMediaMuxerStart = true;
        }
    }

    /**关闭混合器*/
    private synchronized void stopMediaMuxer(){
        if (!isMediaMuxerStart)
            return;
        if (mediaMuxer != null){
            mediaMuxer.stop();
            mediaMuxer.release();
            mediaMuxer = null;
        }
        Log.d(TAG, "====停止媒体混合器=====");
        isMediaMuxerStart = false;
    }

    /**
     * 添加视频轨道
     * @param mediaFormat
     * @return
     */
    public synchronized boolean addVideoTrack(MediaFormat mediaFormat){
        if (mediaMuxer == null || !isLooper){
            throw new RuntimeException("混合器未初始化完成。");
        }
        videoTrackIndex = mediaMuxer.addTrack(mediaFormat);
        Log.d(TAG, "====addVideoTrack===== index="+videoTrackIndex);
        if (videoTrackIndex >= 0){
            isVideoAdd      = true;
            startMuxer();
            return true;
        }

        return false;
    }

    /**
     * 添加音频轨道
     * @param mediaFormat
     * @return
     */
    public synchronized boolean addAudioTrack(MediaFormat mediaFormat){
        if (mediaMuxer == null || !isLooper){
            throw new RuntimeException("混合器未初始化完成。");
        }
        audioTrackIndex = mediaMuxer.addTrack(mediaFormat);
        Log.d(TAG, "====addAudioTrack===== index="+audioTrackIndex);
        if (audioTrackIndex >= 0){
            isAudioAdd      = true;
            startMuxer();
            return true;
        }
        return false;
    }

    public synchronized boolean addMuxerData(MuxerData data){
        if (mediaMuxer == null || !isLooper){
            return false;
//            throw new RuntimeException("混合器未初始化完成。");
        }
        if (!isAudioAdd || !isVideoAdd)
            return false;
        int track = -1;
        if (data.trackIndex == TRACK_VIDEO) {
            track = videoTrackIndex;
        } else if(data.trackIndex == TRACK_AUDIO){
            track = audioTrackIndex;
        }
//        Log.d(TAG, "===track: "+track+"    时间戳= " + data.bufferInfo.presentationTimeUs);
        //添加数据
        mediaMuxer.writeSampleData(track, data.byteBuf, data.bufferInfo);
        return true;
    }

    /**
     * 封装需要传输的数据类型
     */
    public static class MuxerData {
        int trackIndex;
        ByteBuffer byteBuf;
        MediaCodec.BufferInfo bufferInfo;

        public MuxerData(int trackIndex, ByteBuffer byteBuf, MediaCodec.BufferInfo bufferInfo) {
            this.trackIndex = trackIndex;
            this.byteBuf = byteBuf;
            this.bufferInfo = bufferInfo;
        }
    }

    public boolean isMediaMuxerStart() {
        return isMediaMuxerStart;
    }

    public boolean isLooper() {
        return isLooper;
    }
}
