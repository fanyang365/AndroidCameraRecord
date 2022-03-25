package com.van.util;

import android.media.MediaCodec;

import com.van.opengl.CameraSurface;

import java.nio.ByteBuffer;

public interface ICameraData {

    /**
     * 视频格式改变，录像时addTrack使用。
     */
    void onFormatChanged(CameraSurface cameraSurface);

    /**
     * camera初始化打开完成
     */
    void onCameraStart(CameraSurface cameraSurface);

    /**
     * camera关闭
     */
    void onCameraStop(CameraSurface cameraSurface);

    /**
     * 回调H264数据
     * @param data
     * @param bufferInfo
     */
    void onCameraH264Data(ByteBuffer data, MediaCodec.BufferInfo bufferInfo);
}
