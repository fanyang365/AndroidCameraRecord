package com.van.camencode;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.FileDataSourceImpl;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.tracks.AACTrackImpl;
import com.googlecode.mp4parser.authoring.tracks.h264.H264TrackImpl;
import com.van.android.hardcodec.AACEncoder;
import com.van.android.hardcodec.AudioCapture;
import com.van.android.hardcodec.AvcDecoder;
import com.van.android.hardcodec.AvcEncoder;
import com.van.util.ImageUtil;
import com.hjq.permissions.OnPermission;
import com.hjq.permissions.XXPermissions;
import com.van.util.WriteFileUtil;

public class MainActivity extends Activity implements SurfaceHolder.Callback, PreviewCallback, OnClickListener{
	public SurfaceView mPreview;
	public SurfaceHolder mHolder;

	public SurfaceView 		m_decodeView;
	public SurfaceHolder 	m_decodeViewHolder;
	public ReentrantLock 	nativeFunctionLock 		= new ReentrantLock();
	
	private int preview_width ;
	private int preview_height;
	private int preview_format ;
	private int currentFps;
	private int currentCameraFacing;
	private int currentCodeRate;
	private Camera mCamera;
	
	private AvcEncoder mEncoder ;
	private AvcDecoder mDecoder ;
	
	private byte[] buffer1 = new byte[1920*1088*3/2];
	private byte[] buffer2 = new byte[1920*1088*3/2];

	public long				m_last_time_stamp 	= System.currentTimeMillis();
	public int				m_preview_rate		= 0;
	
    private List<Size>		supportedPreviewSizes;	// 当前摄像机信息
    private List<Integer>	supportedFormats;	// 当前摄像机信息
	
	private Button				cameraFacingButton;
	
	private TextView			textFPS,
								textIsHardEncode,
								textIsHardDecode;
    
	private RelativeLayout		mainRel,
								settingRel;
	
	private Spinner				resolutionSpinner,
								fpsSpinner,
								codeRateSpinner,
								formatSpinner;
	
	private List<String>		resolutionList,
								fpsList,
								codeRateList;
	
	private List<CItem>			formatList;
	
	private ArrayAdapter<String> resolutionaAdapter,
								fpsAdapter,
								codeRateAdapter;
	
	private ArrayAdapter<CItem> formatAdapter;

	private Button				btnPhoto;
	private Button				btnRecord;
	private boolean				isRecording;
	private boolean				isIFrameWrote;	//录像是I帧是否已被写入

	/**
	 * 根据值, 设置spinner默认选中:
	 * @param spinner
	 * @param value
	 */
	public static void setSpinnerItemSelectedByValue(Spinner spinner,String value){
	    SpinnerAdapter apsAdapter= spinner.getAdapter(); //得到SpinnerAdapter对象
	    int k= apsAdapter.getCount();
	    for(int i=0;i<k;i++){
	        if(value.equals(apsAdapter.getItem(i).toString())){
	            spinner.setSelection(i,true);// 默认选中项
	            break;
	        }
	    }
	} 

	private void permissionInit(){
		//申请权限
		XXPermissions.with(this)
//				.constantRequest() //可设置被拒绝后继续申请，直到用户授权或者永久拒绝
//				.permission(Permission.SYSTEM_ALERT_WINDOW, Permission.REQUEST_INSTALL_PACKAGES) //支持请求6.0悬浮窗权限8.0请求安装权限
//                .permission(Permission.Group.STORAGE, Permission.Group.CALENDAR) //不指定权限则自动获取清单中的危险权限
				.request(new OnPermission() {

					@Override
					public void hasPermission(List<String> granted, boolean isAll) {

					}

					@Override
					public void noPermission(List<String> denied, boolean quick) {
//						ToastUtil.showToast(MainActivity.this, "请先授予APP权限:"+denied.get(0));
//						finish();
					}
				});
	}

	private void sdf(CItem cItem){
		cItem.setID(1);
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		CItem cItem = new CItem();
		Log.d("main", "item = "+ cItem.GetID());
		sdf(cItem);
		Log.d("main", "item = "+ cItem.GetID());
		permissionInit();
		init();

		preview_width = 1920;
		preview_height = 1080;
		currentFps		= 30;
		currentCodeRate = 10000;
		currentCameraFacing = Camera.CameraInfo.CAMERA_FACING_BACK;
		preview_format = ImageFormat.NV21 ;
		mPreview = (SurfaceView) this.findViewById(R.id.preview);
		mHolder = mPreview.getHolder();
		mHolder.setFixedSize(preview_width, preview_height); // 预览大小設置
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		mHolder.addCallback(this);

		m_decodeView 		= (SurfaceView) this.findViewById(R.id.SurfaceView01);
		m_decodeViewHolder	= m_decodeView.getHolder();
		m_decodeViewHolder.setFixedSize(getPreviewWidth(),getPreviewHeight()); // 预览大小設置
		m_decodeViewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		m_decodeViewHolder.addCallback(this);

		//初始化编解码类
	    nativeFunctionLock.lock();
		mEncoder = new AvcEncoder(getPreviewWidth(),getPreviewHeight(),currentFps,currentCodeRate*1024);
		//mDecoder = new AvcDecoder(getPreviewWidth(),getPreviewHeight());
		mDecoder = new AvcDecoder(1920,1080);
		nativeFunctionLock.unlock();

		//是否支持硬件编解码：
		if (AvcEncoder.IsSupportH264HardEncode() == true) {
			textIsHardEncode.setTextColor(Color.GREEN);
			textIsHardEncode.setText("硬编码：支持");
		}else {
			textIsHardEncode.setTextColor(Color.RED);
			textIsHardEncode.setText("硬编码：不支持");
		}

		if (AvcEncoder.IsSupportH264HardDecode() == true) {
			textIsHardDecode.setTextColor(Color.GREEN);
			textIsHardDecode.setText("硬解码：支持");
		}else {
			textIsHardDecode.setTextColor(Color.RED);
			textIsHardDecode.setText("硬解码：不支持");
		}

	}
	
	@Override
	protected void onDestroy() {
		nativeFunctionLock.lock();
		mEncoder.close();
		nativeFunctionLock.unlock();
		super.onDestroy();
	}
	
	private void init(){
		textFPS				= (TextView) findViewById(R.id.textFPS);
		btnPhoto				= (Button) findViewById(R.id.btnPhoto);
		btnRecord				= (Button) findViewById(R.id.btnRecord);
		textIsHardEncode	= (TextView) findViewById(R.id.textIsHardEncode);
		textIsHardDecode	= (TextView) findViewById(R.id.textIsHardDecode);
		cameraFacingButton	= (Button) findViewById(R.id.cameraFacingButton);
		mainRel				= (RelativeLayout) findViewById(R.id.mainRel);
		settingRel			= (RelativeLayout) findViewById(R.id.settingRel);
		
		cameraFacingButton.setOnClickListener(this);
		mainRel.setOnClickListener(this);
		btnPhoto.setOnClickListener(this);
		btnRecord.setOnClickListener(this);

		//只有一个摄像头
		if (Camera.getNumberOfCameras() < 2) {
			cameraFacingButton.setVisibility(View.GONE);
		}
		
		//分辨率
		resolutionSpinner	= (Spinner) findViewById(R.id.resolutionSpinner);
		resolutionList 		= new ArrayList<String>();
		resolutionaAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, resolutionList);
		resolutionaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		resolutionSpinner.setAdapter(resolutionaAdapter);
		resolutionSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				String text = (String) resolutionSpinner.getSelectedItem();
				String[] array= text.split("X");
				preview_width	= Integer.parseInt(array[0]);
				preview_height	= Integer.parseInt(array[1]);
				//Toast.makeText(MainActivity.this, "改变了"+preview_width, Toast.LENGTH_SHORT).show();
				mEncoder.widthChanged(preview_width, preview_height);
				StopCamera();
				StartCamera();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// TODO Auto-generated method stub
				
			}
		});
		
		//码率
		codeRateSpinner = (Spinner) findViewById(R.id.codeRateSpinner);
		codeRateList = new ArrayList<String>();
		codeRateList.add("128kbps");
		codeRateList.add("256kbps");
		codeRateList.add("512kbps");
		codeRateList.add("1024kbps");
		codeRateList.add("2048kbps");
		codeRateList.add("4096kbps");

		codeRateAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, codeRateList);
		codeRateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		codeRateSpinner.setAdapter(codeRateAdapter);
		codeRateSpinner.setSelection(3, true);
		codeRateSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				String[] array		= codeRateList.get(position).split("kbps");
				currentCodeRate		= Integer.parseInt(array[0]);
				//Toast.makeText(MainActivity.this, "改变了", Toast.LENGTH_SHORT).show();
				mEncoder.bitRateChanged(currentCodeRate*1024);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// TODO Auto-generated method stub

			}
		});
		
		
		
		//帧率
		fpsSpinner	= (Spinner) findViewById(R.id.fpsSpinner);
		fpsList 		= new ArrayList<String>();
		fpsList.add("5fps");
		fpsList.add("10fps");
		fpsList.add("15fps");
		fpsList.add("20fps");
		fpsList.add("25fps");
		fpsList.add("30fps");
		fpsList.add("60fps");
		fpsAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, fpsList);
		fpsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		fpsSpinner.setAdapter(fpsAdapter);
		fpsSpinner.setSelection(5, true);
		fpsSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				String[] array = fpsList.get(position).split("fps");
				currentFps		= Integer.parseInt(array[0]);
				mEncoder.fpsChanged(currentFps);
			}
			
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// TODO Auto-generated method stub
				
			}
		});
		
		
		//格式
		formatSpinner	= (Spinner) findViewById(R.id.formatSpinner);
		formatList 		= new ArrayList<CItem>();
		formatAdapter = new ArrayAdapter<CItem>(this, android.R.layout.simple_spinner_item, formatList);
		formatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		formatSpinner.setAdapter(formatAdapter);
		formatSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
//				preview_format = ((CItem) formatSpinner.getSelectedItem()).GetID();
//				mEncoder.setColorFomart(preview_format);
//				mEncoder.bitRateChanged(currentCodeRate*1024);
//				StopCamera();
//				StartCamera();

			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// TODO Auto-generated method stub
				
			}
		});

	}

	@Override
	public void onClick(View view) {
		
		switch (view.getId()) {
		case R.id.mainRel:
			
			if (settingRel.getVisibility() == View.VISIBLE) {
				settingRel.setVisibility(View.GONE);
			}else {
				settingRel.setVisibility(View.VISIBLE);
			}
			
			break;
		case R.id.cameraFacingButton:

			if (currentCameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
				currentCameraFacing = Camera.CameraInfo.CAMERA_FACING_BACK;
				cameraFacingButton.setText("后置摄像头");
			}else {
				currentCameraFacing = Camera.CameraInfo.CAMERA_FACING_FRONT;
				cameraFacingButton.setText("前置摄像头");
			}
			StopCamera();
			StartCamera();
			break;
		case R.id.btnPhoto:
			takePicture();
			break;
		case R.id.btnRecord:
			if (isRecording){
				stopRecord();
			}else{
				startRecord();
			}
			break;
		}
		
	}

	private AudioCapture	audioCapture;
	private AACEncoder		mAACEncoder;

	@RequiresApi(api = Build.VERSION_CODES.M)
	public synchronized void audioInit(){
		if (mAACEncoder == null){
			mAACEncoder	= new AACEncoder();
			mAACEncoder.setAddDTS(true);
			mAACEncoder.setOnAACEncodeListener(new AACEncoder.OnAACEncodeListener() {
				@Override
				public void onFormatChanged(MediaFormat mediaFormat) {

				}

				@Override
				public void onEncodedFrame(byte[] data, MediaCodec.BufferInfo bufferInfo) {
					if (aacInputStream != null){
						aacInputStream.writeFile(data, 0, data.length);
					}
				}

				@Override
				public void onEncodedFrame(ByteBuffer data, MediaCodec.BufferInfo bufferInfo) {

				}
			});
			mAACEncoder.start();
		}

		if (audioCapture == null){
			audioCapture = new AudioCapture(new AudioCapture.MyAudioRecordLinstener() {
				@Override
				public void OnAudioRecord(byte[] data) {
					if (mAACEncoder != null){
						mAACEncoder.putAudioData(data);
					}
				}
			});
			audioCapture.start();
		}
	}

	public synchronized void audioUnit(){
		if (audioCapture != null){
			audioCapture.stopRecord();
			audioCapture	= null;
		}

		if (mAACEncoder != null) {
			mAACEncoder.stop();
			mAACEncoder = null;
		}
	}

	private WriteFileUtil h264InputStream;
	private WriteFileUtil aacInputStream;
	private String recordFileName	= "test";
	private String recordH264Path  = Environment.getExternalStorageDirectory().toString() + "/test.h264";
	private String recordAACPath  = Environment.getExternalStorageDirectory().toString() + "/test.aac";
	private synchronized void startRecord(){
		if (isRecording)
			return;
		isIFrameWrote		= false;

		h264InputStream		= new WriteFileUtil(recordH264Path);
		h264InputStream.createfile();
		if (sps_data != null){
			h264InputStream.writeFile(sps_data);
		}
		aacInputStream		= new WriteFileUtil(recordAACPath);
		aacInputStream.createfile();
		audioInit();

		isRecording	= true;
		btnRecord.setText("停止录像");
	}

	private synchronized void stopRecord(){

		audioUnit();

		if (h264InputStream != null){
			h264InputStream.stopStream();
			h264InputStream = null;
		}

		if (aacInputStream != null){
			aacInputStream.stopStream();
			aacInputStream = null;
		}

//		if (isRecording){
//			Log.d("CreateMP4", "停止录像，path="+recordH264Path);
//			Thread mp4Thread  = new CreateMp4(recordH264Path, recordAACPath, recordFileName);
//			mp4Thread.start();
//			isRecording     = false;
//		}
		isRecording		= false;
		btnRecord.setText("开始录像");
	}

	private void takePicture(){
		if (mCamera == null)
			return ;

		mCamera.takePicture(null, null, new Camera.PictureCallback() {
			@Override
			public void onPictureTaken(byte[] data, Camera camera) {
				Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
				int displayOrientation = getCameraDisplayOrientation(MainActivity.this, currentCameraFacing);
				Configuration mConfiguration = MainActivity.this.getResources().getConfiguration(); //获取设置的配置信息
				int ori = mConfiguration.orientation; //获取屏幕方向
				if (ori == mConfiguration.ORIENTATION_PORTRAIT) {
					//竖屏
					if (currentCameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
						displayOrientation = 270;
					}
				}

				//加上偏转的角度
//				displayOrientation = (displayOrientation+currentCameraDeflection) % 360;
				//旋转缩放图片
                Matrix matrix = new Matrix();
                matrix.preRotate(displayOrientation);
                matrix.postScale(0.3f, 0.3f);//缩放
                bitmap = Bitmap.createBitmap(bitmap ,0,0, bitmap .getWidth(), bitmap.getHeight(),matrix,true);

				//重启预览
				camera.stopPreview();
				camera.startPreview();
				showBitmap(bitmap);
			}
		});
	}

	private void showBitmap(final Bitmap bitmap){
		runOnUiThread(new Runnable() {
			@Override
			public void run() {

			}
		});
        Toast.makeText(MainActivity.this, "你好帅呀！涂鸦试试！", Toast.LENGTH_LONG).show();
        PhotoPop	photoPop	= new PhotoPop(MainActivity.this, 800, 600);
        photoPop.showAtLocation(btnPhoto, Gravity.CENTER, 0, 0);
        photoPop.showBitmap(bitmap);
	}

	/**
	 * 根据当前横竖屏状态，判断摄像头该偏转多少度。
	 * @param activity
	 * @param cameraId		前置或后置摄像头
	 * @return 偏转的角度
	 */
	private int getCameraDisplayOrientation (Activity activity, int cameraId) {
		Camera.CameraInfo info = new Camera.CameraInfo();
		Camera.getCameraInfo (cameraId , info);
		int rotation = activity.getWindowManager ().getDefaultDisplay ().getRotation ();
		int degrees = 0;
		switch (rotation) {
			case Surface.ROTATION_0:
				degrees = 0;
				break;
			case Surface.ROTATION_90:
				degrees = 90;
				break;
			case Surface.ROTATION_180:
				degrees = 180;
				break;
			case Surface.ROTATION_270:
				degrees = 270;
				break;
		}
		int result;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			result = (info.orientation + degrees) % 360;
			result = (360 - result) % 360;   // compensate the mirror

		} else {
			// back-facing
			result = ( info.orientation - degrees + 360) % 360;
		}
		return result;
	}

	private void StartCamera(){
		try 
		{
			mCamera = Camera.open(0);
//			mCamera.setDisplayOrientation(90);
			mCamera.setPreviewDisplay(mHolder);
			Camera.Parameters parameters = mCamera.getParameters();
					            
            //////////////////////// Test
            int 			i;
            
            //parameters.getSupportedPreviewFrameRates()
            supportedPreviewSizes 	= parameters.getSupportedPreviewSizes();
            supportedFormats 		= parameters.getSupportedPreviewFormats();
            
            resolutionList.clear();
    		for (i = 0; i < supportedPreviewSizes.size(); i++)
    		{
        		resolutionList.add(supportedPreviewSizes.get(i).width+"X"+supportedPreviewSizes.get(i).height);
    		}

    		resolutionaAdapter.notifyDataSetChanged();
    		setSpinnerItemSelectedByValue(resolutionSpinner, preview_width+"X"+preview_height);
    		
    		formatList.clear();
    		for (i = supportedFormats.size()-1; i >=0 ; i--)
    		{
    			CItem cItem = new CItem(supportedFormats.get(i).intValue(), getFormat(supportedFormats.get(i).intValue()));
    			formatList.add(cItem);
    		}
    		formatAdapter.notifyDataSetChanged();

			parameters.setPreviewSize(preview_width, preview_height);
			parameters.setPreviewFormat(preview_format);
			mEncoder.setColorFomart(preview_format);
			//设置帧率
			List<int[]> fpsList = parameters.getSupportedPreviewFpsRange();
			if(fpsList != null && fpsList.size() > 0) {
				int[] maxFps = fpsList.get(0);
				for (int[] fps: fpsList) {
					if(maxFps[0] * maxFps[1] < fps[0] * fps[1]) {
						maxFps = fps;
					}
				}
				Log.d("Test", "设置帧率 = "+maxFps[0] + " ~ " + maxFps[1]);
				//注意setPreviewFpsRange的输入参数是帧率*1000，如30帧/s则参数为30*1000
				parameters.setPreviewFrameRate(30);
				parameters.setPreviewFpsRange(maxFps[0] , maxFps[1]);
				//setPreviewFrameRate的参数是实际的帧率

			}
//			parameters.setPreviewFrameRate(5);
			mCamera.setParameters(parameters);
//            mCamera.setPreviewCallbackWithBuffer(this);
            previewBufCallbackInit();
			mCamera.startPreview();
			Log.i("Test", "打开摄像机成功!");
			
		}
		catch (Exception e) 
		{
			e.printStackTrace();
			Log.i("Test", "打开摄像机失败!");
		}
	}


    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
//		mCamera.addCallbackBuffer(yuvData);
        // Log.i("Test", "onPreviewFrame...");
//		boolean ok		= isOnMainThread();
		doCodec(data);
        long		current_time_stamp = System.currentTimeMillis();
        m_preview_rate++;
        if ((current_time_stamp-m_last_time_stamp) >= 1000)
        {
            Log.i("Test", "当前帧率="+m_preview_rate+" ,timestamp="+current_time_stamp+",时间差:"+(current_time_stamp-m_last_time_stamp));
            textFPS.setText("FPS:"+m_preview_rate);
            m_last_time_stamp 	= current_time_stamp;
            m_preview_rate		= 0;
        }
        camera.addCallbackBuffer(data);
    }

    private PixelFormat pixelFormat = new PixelFormat();
    private final static int CACHE_BUFFER_NUM = 5;
    private byte[][] mPreviewCallbackBuffers = new byte[CACHE_BUFFER_NUM][];
    /*使用3个缓冲区接受数据，提升帧率*/
    private void previewBufCallbackInit() {
        PixelFormat.getPixelFormatInfo(ImageFormat.NV21, pixelFormat);
        int bufSize = preview_width * preview_height * pixelFormat.bitsPerPixel / 8;
        for (int i = 0; i < mPreviewCallbackBuffers.length; i++) {
            if (mPreviewCallbackBuffers[i] == null) {
                mPreviewCallbackBuffers[i] = new byte[bufSize];
            }
            mCamera.addCallbackBuffer(mPreviewCallbackBuffers[i]);
        }
        mCamera.setPreviewCallbackWithBuffer(this);
    }
	
	private void StopCamera()
    {  
		try{
			mCamera.setPreviewCallback(null); //！！这个必须在前，不然退出出错
			mCamera.stopPreview(); 
			mCamera.release();
			mCamera = null;
		}catch(Exception ex){
			
		}

    }

	/*旋转后的摄像头NV21data*/
	private byte[]			cameraOrientationdata;
	private int				m_camera_preview_size		= 0;
	private byte[]			sps_data;
	
	private void doCodec(byte[] data){
		if (cameraOrientationdata == null){
			m_camera_preview_size	= preview_width*preview_height*3/2;
			cameraOrientationdata	= new byte[m_camera_preview_size];
		}
		nativeFunctionLock.lock();
		if (mEncoder != null)
		{
			ImageUtil.rotate(data, preview_width, preview_height, cameraOrientationdata, 0);
			int et = mEncoder.encode(cameraOrientationdata , 0 , m_camera_preview_size, buffer1, 0);
//			//缓存sps和pps
			if ((buffer1[4] & 0x1F) == 7){
				sps_data	= new byte[et];
				System.arraycopy(buffer1, 0, sps_data, 0, et);
			}
			if (h264InputStream != null){
				if (isIFrameWrote){//第一帧必须是I帧
					h264InputStream.writeFile(buffer1, 0, et);
				}else {
					if ((buffer1[4] & 0x1F) == 5){
						h264InputStream.writeFile(buffer1, 0, et);
						isIFrameWrote	= true;
					}
				}

			}
			int dt = mDecoder.decode(buffer1, 0 , et , buffer2 , 0);
		}
		nativeFunctionLock.unlock();
	}
	
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (mHolder == holder)
		{			
			StartCamera();
		}
		
		if (m_decodeViewHolder == holder)
		{
			Log.i("Test", "m_decodeViewHolder surfaceCreated...1");
			mDecoder.init(m_decodeViewHolder.getSurface());
			Log.i("Test", "m_decodeViewHolder surfaceCreated...2");
		}		
		
	}
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if (mHolder == holder)
		{			
			StopCamera();
		}
	}

	/**
	 * 判断是否在当前主线程
	 * @return
	 */
	public static boolean isOnMainThread(){
		return Thread.currentThread() == Looper.getMainLooper().getThread();
	}

	
	public int getPreviewWidth()
	{
		return this.preview_width;
	}
	
	public int getPreviewHeight(){
		return this.preview_height;
	}

	public int getPreviewFormat()
	{
		return preview_format ;
	}
	
	private String getFormat(int format){

		switch (format) {
		case ImageFormat.JPEG:
			return "JPEG";
		case ImageFormat.NV16:
			return "NV16";
		case ImageFormat.NV21:
			return "NV21";
		case ImageFormat.RGB_565:
			return "RGB_565";
		case ImageFormat.UNKNOWN:
			return "UNKNOWN";
		case ImageFormat.YUV_420_888:
			return "YUV_420_888";
		case ImageFormat.YUY2:
			return "YUY2";
		case ImageFormat.YV12:
			return "YV12";
		default:
			return "UNKNOWN";
		}
		
	}


	public class CreateMp4 extends Thread{

		private String h264Path;
		private String aacPath;
		private String fileName;

		public CreateMp4(String h264Path,  String aacPath, String fileName){
			this.h264Path   = h264Path;
			this.aacPath    = aacPath;
			this.fileName   = fileName;
		}

		@Override
		public void run() {
			Log.d("CreateMp4", "开始生成MP4...");
			if (aacPath == null || h264Path == null)
				return ;
			try {
				H264TrackImpl h264Track = new H264TrackImpl(new FileDataSourceImpl(h264Path));
				AACTrackImpl aacTrack = new AACTrackImpl(new FileDataSourceImpl(aacPath));
				Movie movie = new Movie();
				movie.addTrack(h264Track);
				movie.addTrack(aacTrack);
				Container mp4file = new DefaultMp4Builder().build(movie);
				String outputMP4FilePath    = Environment.getExternalStorageDirectory().toString()+"/"+fileName+".mp4";
				Log.d("CreateMp4", "生成MP4路径="+outputMP4FilePath);
				FileChannel fc = new FileOutputStream(new File(outputMP4FilePath)).getChannel();
				mp4file.writeContainer(fc);
				fc.close();

				delete264File(h264Path, aacPath);
			}catch (Exception e){
				e.printStackTrace();
				Log.d("CreateMp4", "生成MP4失败！"+e.getMessage());
				File file   = new File(h264Path);
				if (file.exists() && file.length() == 0){
					file.delete();
				}
			}
		}
	}

	private void delete264File(String h264Path, String aacPath){
		if (aacPath == null || h264Path == null)
			return ;
		File h264File   = new File(h264Path);
		if (h264File != null && h264File.exists()){
			Log.d("CreateMp4", "删除文件,path="+h264File.getAbsolutePath());
			h264File.delete();
		}


		File aacFile    = new File(aacPath);
		if (aacFile != null && aacFile.exists())
			aacFile.delete();
	}

}
