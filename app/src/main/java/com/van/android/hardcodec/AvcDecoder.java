package com.van.android.hardcodec;

import java.nio.ByteBuffer;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

@SuppressLint("NewApi")
public class AvcDecoder {

	public AvcDecoder(int width, int height) {
		mWidth = width;
		mHeight = height;
		m_init_ok = 0;
	}
	
	private MediaCodec mMediaCodec;
	private int mWidth ;
	private int mHeight ;
	private int m_init_ok = 0;
	private MediaCodec.BufferInfo bufferInfo;
	private boolean m_is_surface_view = false;
	public void init(Surface surfaceVivew)
	{
		try 
		{
			if (surfaceVivew != null)
			{
				m_is_surface_view = true;
			}
			mMediaCodec = MediaCodec.createDecoderByType("video/avc");
			MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", mWidth, mHeight);
		    Log.i("Test", "init....1,surfaceVivew="+surfaceVivew);
		    mMediaCodec.configure(mediaFormat, surfaceVivew, null, 0);
		    Log.i("Test", "init....2");
		    mMediaCodec.start();		    
		    bufferInfo = new MediaCodec.BufferInfo();
		    m_init_ok = 1;
		}
		catch (Exception e) 
		{
			e.printStackTrace();
			Log.i("Test", "打开解码器失败!");
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
	
	public int decode(byte[] input , int offset , int count , byte[] output , int out_offset) 
	{
		if (m_init_ok == 0)
			return 0;
		
		int len = 0;
		ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
	    ByteBuffer[] outputBuffers = mMediaCodec.getOutputBuffers();
	    
	    int inputBufferIndex = mMediaCodec.dequeueInputBuffer(-1);
	    if (inputBufferIndex >= 0) {
	        ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
	        inputBuffer.clear();
	        inputBuffer.put(input, offset, count);
	        mMediaCodec.queueInputBuffer(inputBufferIndex, 0, count, 0, 0);
	    }
	    int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo,1000000l);  // 1000000 us timeout , one second
	    if (outputBufferIndex >= 0) {
	    	if (m_is_surface_view == false)
	    	{
		        ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
		        if (output != null)
		        	outputBuffer.get(output, out_offset, bufferInfo.size);
		        len = bufferInfo.size ;
		        mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
	    	}
	    	else
	    	{	    		
	    		mMediaCodec.releaseOutputBuffer(outputBufferIndex, true);
	    		
	    		MediaFormat mediaFormat = mMediaCodec.getOutputFormat();
	    		int 		width = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);  
	    		int 		height = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
	    		
	    		//Log.i("Test", "Decoder width="+width+",height="+height);

	    	}
	    }
	    return len;
	}

}
