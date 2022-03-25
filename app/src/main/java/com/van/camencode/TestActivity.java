package com.van.camencode;

import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;

import com.van.android.hardcodec.AvcDecoder;
import com.van.android.hardcodec.AvcEncoder;

public class TestActivity extends MainActivity 
{	
	public long				m_last_time_stamp 	= System.currentTimeMillis();
	public int				m_preview_rate		= 0;
	
	@Override
	public void onPreviewFrame(final byte[] data, Camera camera) {
		
		//Log.i("Test", "onPreviewFrame...");
		doCodec(data);
		
		long		current_time_stamp = System.currentTimeMillis();		
		m_preview_rate++;		
		if ((current_time_stamp-m_last_time_stamp) >= 1000)
		{	
			Log.i("Test", "当前帧率="+m_preview_rate+",timestamp="+current_time_stamp+",时间差:"+(current_time_stamp-m_last_time_stamp));
			m_last_time_stamp 	= current_time_stamp;
			m_preview_rate		= 0;			
		}
	}
	
	private void doCodec(byte[] data){
		
		nativeFunctionLock.lock();
		if (mEncoder != null)
		{
			int et = mEncoder.encode(data , 0 , data.length, buffer1, 0);			
			int dt = mDecoder.decode(buffer1, 0 , et , buffer2 , 0);
		}
		nativeFunctionLock.unlock();
	}
	
	private byte[] buffer1 = new byte[1920*1088*3/2];
	private byte[] buffer2 = new byte[1920*1088*3/2];

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		int framerate = 25;
	    int bitrate = 1250000;
	    
	    nativeFunctionLock.lock();
		mEncoder = new AvcEncoder(getPreviewWidth(),getPreviewHeight(),framerate,bitrate);
		//mDecoder = new AvcDecoder(getPreviewWidth(),getPreviewHeight());		
		mDecoder = new AvcDecoder(1920,1080);
		nativeFunctionLock.unlock();
	}


	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		nativeFunctionLock.lock();
		mEncoder.close();
		nativeFunctionLock.unlock();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		super.surfaceCreated(holder);
		if (m_decodeViewHolder == holder)
		{
			Log.i("Test", "m_decodeViewHolder surfaceCreated...1");
			mDecoder.init(m_decodeViewHolder.getSurface());
			Log.i("Test", "m_decodeViewHolder surfaceCreated...2");
		}		
	}

	private AvcEncoder mEncoder ;
	private AvcDecoder mDecoder ;

}
