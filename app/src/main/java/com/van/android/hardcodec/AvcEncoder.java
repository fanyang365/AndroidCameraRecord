package com.van.android.hardcodec;

import java.nio.ByteBuffer;

import android.annotation.SuppressLint;
import android.graphics.ImageFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

@SuppressLint("NewApi")
public class AvcEncoder {

	public AvcEncoder(int width, int height, int framerate, int bitrate) {
		mWidth = width;
		mHeight = height;
		mFramerate = framerate;
		mBitrate = bitrate;
		init();
		yuv420	= new byte[width*height*3];
	}
	
	private MediaCodec mMediaCodec;
	private int mWidth ;
	private int mHeight ;
	private int mFramerate;
	private int mBitrate;
	private int colorFomart	= ImageFormat.NV21;
	private byte[] yuv420;
	byte[] m_info = null;
	
	public int getColorFomart() {
		return colorFomart;
	}

	public void setColorFomart(int colorFomart) {
		this.colorFomart = colorFomart;
	}

	private static void PrintfSupportMediaCodecInfo()
	{
		Log.i("Test", "Build.VERSION.SDK_INT="+Build.VERSION.SDK_INT);
		if(Build.VERSION.SDK_INT>=18)
		{  
			for(int j = MediaCodecList.getCodecCount() - 1; j >= 0; j--)
			{
				MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(j);
				String[] types = codecInfo.getSupportedTypes();
				
				Log.i("Test", "code name:"+codecInfo.getName()+",isEncoder="+codecInfo.isEncoder()+",type.length="+types.length);
				for (int i = 0; i < types.length; i++)
				{
					Log.i("Test", "types["+i+"]="+types[i]);
				}  
			}  
		}
	}
	
	public static boolean IsSupportH264HardEncode()
	{
		boolean	is_support = false;
		
		if(Build.VERSION.SDK_INT>=18)
		{  
			for(int j = MediaCodecList.getCodecCount() - 1; j >= 0; j--)
			{
				MediaCodecInfo 	codecInfo	= MediaCodecList.getCodecInfoAt(j);
				String[] 		types 		= codecInfo.getSupportedTypes();
				
				if (codecInfo.isEncoder())
				{
					for (int i = 0; i < types.length; i++)
					{
						if(types[i].startsWith("video/avc"))
						{
							is_support = true;
							break;
						}
					} 
				}
				if (is_support)
					break;
			}
		}
		
		return is_support;
	}
	
	public static boolean IsSupportH264HardDecode()
	{
		boolean	is_support = false;
		
		if(Build.VERSION.SDK_INT>=18)
		{  
			for(int j = MediaCodecList.getCodecCount() - 1; j >= 0; j--)
			{
				MediaCodecInfo 	codecInfo	= MediaCodecList.getCodecInfoAt(j);
				String[] 		types 		= codecInfo.getSupportedTypes();
				
				if (codecInfo.isEncoder() == false)
				{
					for (int i = 0; i < types.length; i++)
					{
						if(types[i].startsWith("video/avc"))
						{
							is_support = true;
							break;
						}
					} 
				}
				if (is_support)
					break;
			}
		}
		
		return is_support;
	}
	
	private void init()
	{
		PrintfSupportMediaCodecInfo();
		if (IsSupportH264HardEncode())
		{
			Log.i("Test", "支持H264硬编码!");
		}
		
		if (IsSupportH264HardDecode())
		{
			Log.i("Test", "支持H264硬解码!");
		}

		try {
			mMediaCodec = MediaCodec.createEncoderByType("video/avc");
			MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", mWidth, mHeight);
			mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mBitrate);
			mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mFramerate);
			if(colorFomart == ImageFormat.NV21){
				mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
			}else{
				mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
			}
			mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 3);
			mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
			mMediaCodec.start();
		}catch (Exception e){
			e.printStackTrace();
		}


	}
	
	public void close(){
		try {
			mMediaCodec.stop();
			mMediaCodec.release();
			mMediaCodec=null;
	    } catch (Exception e){ 
	        e.printStackTrace();
	    }
	}
	
	
	public void bitRateChanged(int mBitrate){
		close();
		this.mBitrate = mBitrate;
		try {
			mMediaCodec = MediaCodec.createEncoderByType("video/avc");
			MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", mWidth, mHeight);
			mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mBitrate);
			mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mFramerate);
			if(colorFomart == ImageFormat.NV21){
				mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
			}else{
				mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
			}
			mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
			mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
			mMediaCodec.start();
		}catch (Exception e){

		}

	}
	
	public void widthChanged(int mWidth, int mHeight){
		close();
		yuv420	= new byte[mWidth*mHeight*3/2];
		this.mWidth = mWidth;
		this.mHeight = mHeight;
		try {
			mMediaCodec = MediaCodec.createEncoderByType("video/avc");
			MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", mWidth, mHeight);
			mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mBitrate);
			mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mFramerate);
			if(colorFomart == ImageFormat.NV21){
				mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
			}else{
				mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
			}
			mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
			mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
			mMediaCodec.start();
		}catch (Exception e){

		}

	    
	    
	}
	
	public void fpsChanged(int framerate){
		Log.i("fff", "FPS值改变："+framerate);
		close();
		this.mFramerate = framerate;
		try {
			mMediaCodec = MediaCodec.createEncoderByType("video/avc");
			MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", mWidth, mHeight);
			mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mBitrate);
			mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mFramerate);
			if(colorFomart == ImageFormat.NV21){
				mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
			}else{
				mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
			}
			mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
			mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
			mMediaCodec.start();
		}catch (Exception e){

		}
	}
	
	public int encode(byte[] input , int offset , int count , byte[] output , int out_offset) 
	{	
		int len = 0;
//		if (colorFomart == ImageFormat.YV12) {
//			Log.d("AvcEncoder", "set colorfomart = "+Integer.toHexString(colorFomart));
//			swapYV12toI420(input, yuv420, mWidth, mHeight);
//		}else {
////			nv21ToI420(input, yuv420, mWidth, mHeight);
//			ImageUtil.rotate(input, mWidth, mHeight, yuv420, 0);
//		}
		ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
	    ByteBuffer[] outputBuffers = mMediaCodec.getOutputBuffers();
	    int inputBufferIndex = mMediaCodec.dequeueInputBuffer(-1);
	    if (inputBufferIndex >= 0) {
	        ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
	        inputBuffer.clear();
	        inputBuffer.put(input, offset, count);
	        mMediaCodec.queueInputBuffer(inputBufferIndex, 0, count, System.nanoTime() / 1000L, 0);
	    }
	    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
	    int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo,1000000l);  // 1000000 us timeout , one second
	    if (outputBufferIndex >= 0) {
	        ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
	        outputBuffer.get(output, out_offset, bufferInfo.size);
	        len = bufferInfo.size ;
	        mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
	    }else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
			format = mMediaCodec.getOutputFormat();
		}
		return len;
	}

	MediaFormat format;

	public MediaFormat getFormat(){
		return format;
	}
	
	private void swapYV12toI420(byte[] yv12bytes, byte[] i420bytes, int width, int height)
	{
		System.arraycopy(yv12bytes, 0, i420bytes, 0,width*height);
		System.arraycopy(yv12bytes, width*height+width*height/4, i420bytes, width*height,width*height/4);
		System.arraycopy(yv12bytes, width*height, i420bytes, width*height+width*height/4,width*height/4);
	}

}
