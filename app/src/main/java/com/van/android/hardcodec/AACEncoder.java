package com.van.android.hardcodec;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingDeque;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

public class AACEncoder {


    private static final String TAG = "AACEncoder";

    public static final int DEFAULT_BIT_RATE = 128 * 1024; //128kb

    public static final int DEFAULT_SIMPLE_RATE = 16000; //44100Hz

    public static final int DEFAULT_CHANNEL_COUNTS = 1;

    public static final int DEFAULT_MAX_INPUT_SIZE = 16384; //16k

    private MediaCodec mediaCodec;

    private MediaFormat mediaFormat;

    private OnAACEncodeListener onAACEncodeListener;

    private boolean isAsyMode = true;

    private boolean isAddDTS  = false;

    private volatile boolean isStart = false;

    private Thread encoderThread;

    private DataOutputStream dataOutputStream;

    /*存放待编码的pcm音频*/
    private LinkedBlockingDeque<byte[]> bufferAudioList;

    private HandlerThread mBackgroundThread;
    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler mBackgroundHandler;

    public AACEncoder() {
        bufferAudioList = new LinkedBlockingDeque<>();
    }

    public void setDataOutputStream(DataOutputStream dataOutputStream) {
        this.dataOutputStream = dataOutputStream;
    }

    public void setOnAACEncodeListener(OnAACEncodeListener onAACEncodeListener) {
        this.onAACEncodeListener = onAACEncodeListener;
    }

    public void setAsyMode(boolean asyMode) {
        isAsyMode = asyMode;
    }

    private MediaFormat createMediaFormat() {
        MediaFormat mediaFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC,
                DEFAULT_SIMPLE_RATE, DEFAULT_CHANNEL_COUNTS);
        mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, DEFAULT_BIT_RATE);
        mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, DEFAULT_MAX_INPUT_SIZE);
        return mediaFormat;
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }


    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        if (mBackgroundThread == null || mBackgroundHandler == null)
            return ;

        mBackgroundThread.interrupt();
        mBackgroundThread = null;
        mBackgroundHandler = null;
    }



    @RequiresApi(api = Build.VERSION_CODES.M)
    private void configure() {
        mediaCodec = createMediaCodec();
        if (mediaCodec == null) {
            throw new IllegalStateException("该设备不支持AAC编码器");
        }
        if (isAsyMode) {
            startBackgroundThread();
            mediaCodec.setCallback(new AsyEncodeCallback(), mBackgroundHandler);
        } else {
            encoderThread = new Thread(new SynchronousEncodeRunnable());
        }
        mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private MediaCodec createMediaCodec() {
        mediaFormat = createMediaFormat();
        MediaCodecList mediaCodecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
        String name = mediaCodecList.findEncoderForFormat(mediaFormat);
        if (name != null) {
            try {
                return MediaCodec.createByCodecName(name);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public MediaFormat getOutputFormat(){
        if (mediaCodec != null)
            return mediaCodec.getOutputFormat();
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void start() {
        configure();
        mediaCodec.start();
        isStart = true;
        if (!isAsyMode) {
            encoderThread.start();
        }
        Log.d(TAG, "采取模式是否为异步="+isAsyMode);
    }


    public synchronized void putAudioData(byte data[]){
        try {
            if(bufferAudioList != null)
                bufferAudioList.put(data);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private class AsyEncodeCallback extends MediaCodec.Callback {

        @Override
        public  void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
//            Log.d(TAG, "onInputBufferAvailable 开始 index="+index);
            if (!isStart) {
                try {
                    innerStop();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }


            try {
                if (!Thread.interrupted()){
                    byte[] data = bufferAudioList.take();
                    ByteBuffer byteBuffer = codec.getInputBuffer(index);
                    byteBuffer.clear();
                    byteBuffer.put(data);
                    mediaCodec.queueInputBuffer(index, 0, data.length, System.nanoTime() / 1000,
                            0);
                }

            } catch (InterruptedException e) {

            }

        }

        @Override
        public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
//            Log.d(TAG, "onOutputBufferAvailable 结束 index="+index);
            if (!isStart) {
                try {
                    innerStop();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }

            ByteBuffer byteBuffer = mediaCodec.getOutputBuffer(index);
            byteBuffer.position(info.offset);
            byteBuffer.limit(info.offset + info.size);
//            byte[] frame = new byte[info.size];
//            byteBuffer.get(frame, 0, info.size);
//
//            byte[] packetWithADTS = new byte[frame.length + 7];
//            System.arraycopy(frame, 0, packetWithADTS, 7, frame.length);
//            addADTStoPacket(packetWithADTS, packetWithADTS.length);



            if (onAACEncodeListener != null){
                if (isAddDTS){
                    byte[] packetWithADTS = new byte[info.size + 7];
                    byteBuffer.get(packetWithADTS, 7, info.size);
                    addADTStoPacket(packetWithADTS, packetWithADTS.length);
                    onAACEncodeListener.onEncodedFrame(packetWithADTS, info);
                }else{
                    onAACEncodeListener.onEncodedFrame(byteBuffer, info);
                }
//

            }
            mediaCodec.releaseOutputBuffer(index, false);
        }

        @Override
        public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
            Log.d(TAG, "onError ="+e.getDiagnosticInfo());
        }

        @Override
        public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
            if (onAACEncodeListener != null){
                onAACEncodeListener.onFormatChanged(format);
            }
            Log.d(TAG, "onOutputFormatChanged");
        }
    }

    private class SynchronousEncodeRunnable implements Runnable {

        @Override
        public void run() {
            while (isStart && !Thread.interrupted()) {
                queryEncodedData();
            }
            try {
                innerStop();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void queryEncodedData() {
        if (mediaCodec == null) {
            return;
        }
//        Log.d(TAG, "queryEncodedData  1");
        byte[] data;
        try {
            data = bufferAudioList.take();
        }catch (InterruptedException e){
            e.printStackTrace();
            return ;
        }
//        Log.d(TAG, "queryEncodedData  1.1");
        int bufferIndexId = mediaCodec.dequeueInputBuffer(-1);
        if (bufferIndexId >= 0) {
            ByteBuffer inputBuffer = mediaCodec.getInputBuffer(bufferIndexId);
            inputBuffer.clear();
            inputBuffer.put(data);
            mediaCodec.queueInputBuffer(bufferIndexId, 0, data.length, System.nanoTime() / 1000,
                    0);
        }

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndexId = mediaCodec.dequeueOutputBuffer(bufferInfo, 100);
        if(outputBufferIndexId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED)
        {
            // 编码器输出缓存区格式改变，通常在存储数据之前且只会改变一次
            // 这里设置混合器视频轨道，如果音频已经添加则启动混合器（保证音视频同步）
            if (onAACEncodeListener != null){
                onAACEncodeListener.onFormatChanged(mediaCodec.getOutputFormat());
            }

        }
        if (outputBufferIndexId >= 0) {
            //获取缓存信息的长度
            //拿到输出Buffer
            ByteBuffer byteBuffer = mediaCodec.getOutputBuffer(outputBufferIndexId);
            byteBuffer.position(bufferInfo.offset);
            byteBuffer.limit(bufferInfo.offset + bufferInfo.size);

            if (onAACEncodeListener != null){
                if (isAddDTS){
                    byte[] packetWithADTS = new byte[bufferInfo.size + 7];
                    byteBuffer.get(packetWithADTS, 7, bufferInfo.size);
                    addADTStoPacket(packetWithADTS, packetWithADTS.length);
                    onAACEncodeListener.onEncodedFrame(packetWithADTS, bufferInfo);
                }else{
                    onAACEncodeListener.onEncodedFrame(byteBuffer, bufferInfo);
                }
//

            }
//            Log.d(TAG, "queryEncodedData  2");

            mediaCodec.releaseOutputBuffer(outputBufferIndexId, false);
//            Log.d(TAG, "queryEncodedData  4");
            return ;
        }
//        Log.d(TAG, "queryEncodedData  5");
    }

    public synchronized void stop() {
        if (!isStart) {
            return;
        }
        if (bufferAudioList != null){
            bufferAudioList.clear();
        }

        stopBackgroundThread();
        isStart = false;

    }

    public interface OnAACEncodeListener {
        void onFormatChanged(MediaFormat mediaFormat);
        void onEncodedFrame(byte[] data, MediaCodec.BufferInfo bufferInfo);
        void onEncodedFrame(ByteBuffer data, MediaCodec.BufferInfo bufferInfo);
    }

    private void writeToFile(byte[] frame) {
        byte[] packetWithADTS = new byte[frame.length + 7];
        System.arraycopy(frame, 0, packetWithADTS, 7, frame.length);
        addADTStoPacket(packetWithADTS, packetWithADTS.length);
        if (dataOutputStream != null) {
            try {
                dataOutputStream.write(packetWithADTS, 0, packetWithADTS.length);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void addADTStoPacket(byte[] packet, int packetLen) {
        int profile = 2;  //AAC LC，MediaCodecInfo.CodecProfileLevel.AACObjectLC;
        int freqIdx = 8;  //44100, 见后面注释avpriv_mpeg4audio_sample_rates中32000对应的数组下标，来自ffmpeg源码
        int chanCfg = DEFAULT_CHANNEL_COUNTS;  //见后面注释channel_configuration，Stero双声道立体声

        /*int avpriv_mpeg4audio_sample_rates[] = {
            96000, 88200, 64000, 48000, 44100, 32000,
                    24000, 22050, 16000, 12000, 11025, 8000, 7350
        };
        channel_configuration: 表示声道数chanCfg
        0: Defined in AOT Specifc Config
        1: 1 channel: front-center
        2: 2 channels: front-left, front-right
        3: 3 channels: front-center, front-left, front-right
        4: 4 channels: front-center, front-left, front-right, back-center
        5: 5 channels: front-center, front-left, front-right, back-left, back-right
        6: 6 channels: front-center, front-left, front-right, back-left, back-right, LFE-channel
        7: 8 channels: front-center, front-left, front-right, side-left, side-right, back-left, back-right, LFE-channel
        8-15: Reserved
        */

        // fill in ADTS data
        packet[0] = (byte) 0xFF;
        packet[1] = (byte)0xF9;
//        packet[1] = (byte) 0xF1;
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }



    public boolean isAddDTS() {
        return isAddDTS;
    }

    public void setAddDTS(boolean addDTS) {
        isAddDTS = addDTS;
    }

    private void innerStop() throws IOException {
        Log.d(TAG, "innerStop 111");
        if (mediaCodec != null) {
            mediaCodec.stop();
            mediaCodec.release();
            mediaCodec = null;
        }
        if (dataOutputStream != null) {
            dataOutputStream.close();
            dataOutputStream = null;
        }
        bufferAudioList.clear();

    }
}
