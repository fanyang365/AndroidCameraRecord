package com.van.camencode;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;

import com.van.android.hardcodec.AvcDecoder;
import com.van.android.hardcodec.AvcEncoder;
import com.hjq.permissions.OnPermission;
import com.hjq.permissions.XXPermissions;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import androidx.annotation.NonNull;

public class Camera2Activity extends Activity {
    /**
     * Tag for the {@link Log}.
     */
    private static final String TAG = "Camera2BasicFragment";

    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;
    private int mWidth  = 1920;
    private int mHeight = 1080;
    Context ctx;
    boolean mFlashSupported;
    String mCameraId;
    CameraDevice mCameraDevice;
    CaptureRequest.Builder mPreviewRequestBuilder;
    SurfaceView surfaceView;
    SurfaceHolder mSurfaceHolder;
    CameraCaptureSession mCaptureSession;
    CaptureRequest mPreviewRequest;
    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread mBackgroundThread;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler mBackgroundHandler;

    private ImageReader mImageReader;

    private AvcEncoder mEncoder ;
    private AvcDecoder mDecoder ;
    private byte[] buffer1 = new byte[mWidth*1080*3/2];
    private byte[] buffer2 = new byte[mWidth*1080*3/2];
    private SurfaceView surfaceView01;
    /**
     * An {@link AutoFitTextureView} for camera preview.
     */
    private AutoFitTextureView mTextureView;
    /**
     * Orientation of the camera sensor
     */
    private int mSensorOrientation;
    /**
     * The {@link android.util.Size} of camera preview.
     */
    private Size mPreviewSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        permissionInit();
        startBackgroundThread();
        ctx = this;
        findView();
        init();
        cameraInit();
    }

    @Override
    protected void onDestroy() {
        stopBackgroundThread();
        mEncoder.close();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mTextureView.isAvailable()) {
            openCamera();
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    private void findView() {
        surfaceView = findViewById(R.id.preview);
        surfaceView01 = findViewById(R.id.SurfaceView01);
        mSurfaceHolder = surfaceView.getHolder();
        mTextureView = (AutoFitTextureView) findViewById(R.id.texture);
        mTextureView.setVisibility(View.VISIBLE);
        surfaceView.setVisibility(View.GONE);
        surfaceView01.setVisibility(View.VISIBLE);
    }

    private void doCodec(byte[] data){

        if (mEncoder != null)
        {
            int et = mEncoder.encode(data , 0 , data.length, buffer1, 0);
            int dt = mDecoder.decode(buffer1, 0 , et , buffer2 , 0);
        }
    }


    private void doCodec2(byte[] data, int offset, int length){

        if (mEncoder != null)
        {
            int et = mEncoder.encode(data , offset , length, buffer1, 0);
            int dt = mDecoder.decode(buffer1, 0 , et , buffer2 , 0);
        }
    }



    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     */
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }

    };

    private void init() {

        //初始化编解码类
        mEncoder = new AvcEncoder(mWidth,mHeight,30,4096*1024);
        //mDecoder = new AvcDecoder(getPreviewWidth(),getPreviewHeight());
        mDecoder = new AvcDecoder(mWidth,mHeight);


//        mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
//            @Override
//            public void surfaceCreated(SurfaceHolder holder) {
//                openCamera();
//            }
//
//            @Override
//            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//
//            }
//
//            @Override
//            public void surfaceDestroyed(SurfaceHolder holder) {
//
//            }
//        });
//
        surfaceView01.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                mDecoder.init(surfaceView01.getHolder().getSurface());
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });

    }

    Range<Integer>[] fpsRanges;
    /**
     * Sets up member variables related to camera.
     *
     * @param width  The width of available size for camera preview
     * @param height The height of available size for camera preview
     */
    @SuppressWarnings("SuspiciousNameCombination")
    private void setUpCameraOutputs(int width, int height) {
        Activity activity = this;
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics
                        = manager.getCameraCharacteristics(cameraId);

                // We don't use a front facing camera in this sample.
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }

                for (Range<Integer> fpsRange : map.getHighSpeedVideoFpsRanges()) {
                    Log.d(TAG, "openCamera: [width, height] = "+ fpsRange.toString());
                }

                android.util.Size[] sizes = map.getOutputSizes(ImageFormat.YUV_420_888);
                // For still image captures, we use the largest available size.
                Size largest = sizes[0];

                // Find out if we need to swap dimension to get the preview size relative to sensor
                // coordinate.
                int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
                //get fps
                fpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);

                //noinspection ConstantConditions
                mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                boolean swappedDimensions = false;
                switch (displayRotation) {
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_180:
                        if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                            swappedDimensions = true;
                        }
                        break;
                    case Surface.ROTATION_90:
                    case Surface.ROTATION_270:
                        if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                            swappedDimensions = true;
                        }
                        break;
                    default:
                        Log.e(TAG, "Display rotation is invalid: " + displayRotation);
                }

                Point displaySize = new Point();
                activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
                int rotatedPreviewWidth = width;
                int rotatedPreviewHeight = height;
                int maxPreviewWidth = displaySize.x;
                int maxPreviewHeight = displaySize.y;

                if (swappedDimensions) {
                    rotatedPreviewWidth = height;
                    rotatedPreviewHeight = width;
                    maxPreviewWidth = displaySize.y;
                    maxPreviewHeight = displaySize.x;
                }

                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                    maxPreviewWidth = MAX_PREVIEW_WIDTH;
                }

                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                    maxPreviewHeight = MAX_PREVIEW_HEIGHT;
                }

                // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
                // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                // garbage capture data.
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                        rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                        maxPreviewHeight, largest);
                mPreviewSize    = largest;

                // We fit the aspect ratio of TextureView to the size of preview we picked.
                int orientation = getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    mTextureView.setAspectRatio(
                            mPreviewSize.getWidth(), mPreviewSize.getHeight());
                } else {
                    mTextureView.setAspectRatio(
                            mPreviewSize.getHeight(), mPreviewSize.getWidth());
                }

                mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(),
                        ImageFormat.YUV_420_888, /*maxImages*/2);
                mImageReader.setOnImageAvailableListener(
                        mOnImageAvailableListener, mBackgroundHandler);
                // Check if the flash is supported.
                Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                mFlashSupported = available == null ? false : available;

                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.

        }
    }


    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     *                          class
     * @param textureViewWidth  The width of the texture view relative to sensor coordinate
     * @param textureViewHeight The height of the texture view relative to sensor coordinate
     * @param maxWidth          The maximum width that can be chosen
     * @param maxHeight         The maximum height that can be chosen
     * @param aspectRatio       The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                          int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }


    private void cameraInit() {
        CameraManager manager = (CameraManager) ctx.getSystemService(Context.CAMERA_SERVICE);
//        mImageReader = ImageReader.newInstance(mWidth, mHeight, ImageFormat.YUV_420_888, 2);
//        mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);
        try {
            //获取可用摄像头列表
            for (String cameraId : manager.getCameraIdList()) {
                //获取相机的相关参数
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                // 不使用前置摄像头。
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }
                // 检查闪光灯是否支持。
                Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                mFlashSupported = available == null ? false : available;
                mCameraId = cameraId;
                Log.e(TAG, " 相机可用 ");
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            //不支持Camera2API
        }
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
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void openCamera() {
        CameraManager manager = (CameraManager) ctx.getSystemService(Context.CAMERA_SERVICE);
        setUpCameraOutputs(1920, 1080);
        try {
            //打开相机预览
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    Activity#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for Activity#requestPermissions for more details.
                return;
            }

            manager.openCamera("0", mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera(){
        CameraManager manager = (CameraManager) ctx.getSystemService(Context.CAMERA_SERVICE);

    }

    /**
     * This a callback object for the {@link ImageReader}. "onImageAvailable" will be called when a
     * still image is ready to be saved.
     */
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        public long				m_last_time_stamp 	= System.currentTimeMillis();
        public int				m_preview_rate		= 0;
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireLatestImage();
//            Log.d("asdf", "onImageAvailable size=" + image.getWidth());
            if (image == null) {
                return;
            }
//            int width   = image.getWidth();
//            int height  = image.getHeight();
//            ByteBuffer byteBuffer   = image.getPlanes()[0].getBuffer();
//            ByteBuffer byteBuffer2  = image.getPlanes()[1].getBuffer();
//            int p1  = image.getPlanes()[0].getPixelStride();
//            int p2  = image.getPlanes()[1].getPixelStride();
//            int p3  = image.getPlanes()[2].getPixelStride();
//            Log.d(TAG, "p1="+p1+", p2="+p2+", p3="+p3);
//            ByteBuffer byteBuffer3  = image.getPlanes()[2].getBuffer();
//            byte[] bytes = new byte[width*height*3/2];
//            byteBuffer.get(bytes, 0, byteBuffer.limit());
//            byteBuffer2.get(bytes, byteBuffer.limit(), byteBuffer2.limit());
////            byteBuffer3.get(bytes, byteBuffer.limit() + byteBuffer2.limit(), byteBuffer3.limit());
//            doCodec2(bytes, 0, byteBuffer.limit()+byteBuffer2.limit());

//            mBackgroundHandler.post(new ImageSaver(reader.acquireNextImage(), mFile));
            long		current_time_stamp = System.currentTimeMillis();
            m_preview_rate++;
            if ((current_time_stamp-m_last_time_stamp) >= 1000)
            {
                Log.i("Test", "当前帧率="+m_preview_rate+",timestamp="+current_time_stamp+",时间差:"+(current_time_stamp-m_last_time_stamp) + "width="+image.getWidth()+ ", height="+image.getHeight());
                m_last_time_stamp 	= current_time_stamp;
                m_preview_rate		= 0;
            }
            image.close();
        }
    };

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

    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            //创建CameraPreviewSession
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            cameraDevice.close();
            mCameraDevice = null;
        }

    };


    /**
     * Creates a new {@link CameraCaptureSession} for camera preview.
     */
    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            // This is the output Surface we need to start preview.
            Surface surface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);
            mPreviewRequestBuilder.addTarget(mImageReader.getSurface());
            // 设置预览画面的帧率 视实际情况而定选择一个帧率范围
            int i = fpsRanges.length-1;
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRanges[i]);
            Log.i("Test", "设置帧率范围 = " + fpsRanges[i].getLower() +" ~ "+fpsRanges[i].getUpper());

            //创建高帧率预览

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            mCaptureSession = cameraCaptureSession;
                            try {
                                // Auto focus should be continuous for camera preview.
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                // Flash is automatically enabled when necessary.
//                                setAutoFlash(mPreviewRequestBuilder);

                                // Finally, we start displaying the camera preview.
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                mCaptureSession.setRepeatingRequest(mPreviewRequest,
                                        null, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {

                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


//
//    /**
//     * 为相机预览创建新的CameraCaptureSession
//     */
//    private void createCameraPreviewSession() {
//        if (mCameraDevice == null)
//            return ;
//        try {
//            //设置了一个具有输出Surface的CaptureRequest.Builder。
//            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
////            mPreviewRequestBuilder.addTarget(mSurfaceHolder.getSurface());
//            SurfaceTexture texture = mTextureView.getSurfaceTexture();
//            assert texture != null;
//
//            // This is the output Surface we need to start preview.
//            Surface surface = new Surface(texture);
//            mPreviewRequestBuilder.addTarget(surface);
//            mPreviewRequestBuilder.addTarget(mImageReader.getSurface());
//            //创建一个CameraCaptureSession来进行相机预览。
//            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
//                    new CameraCaptureSession.StateCallback() {
//
//                        @Override
//                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
//                            // 相机已经关闭
//                            if (null == mCameraDevice) {
//                                return;
//                            }
//                            // 会话准备好后，我们开始显示预览
//                            mCaptureSession = cameraCaptureSession;
//                            try {
//                                // 自动对焦应
//                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
//                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
//                                // 闪光灯
////                                setAutoFlash(mPreviewRequestBuilder);
//                                // 开启相机预览并添加事件
//                                mPreviewRequest = mPreviewRequestBuilder.build();
//                                //发送请求
//                                mCaptureSession.setRepeatingRequest(mPreviewRequest,
//                                        null, mBackgroundHandler);
//                                Log.e(TAG," 开启相机预览并添加事件");
//                            } catch (CameraAccessException e) {
//                                e.printStackTrace();
//                            }
//                        }
//
//                        @Override
//                        public void onConfigureFailed(
//                                @NonNull CameraCaptureSession cameraCaptureSession) {
//                            Log.e(TAG," onConfigureFailed 开启预览失败");
//                        }
//                    }, null);
//        } catch (CameraAccessException e) {
//            Log.e(TAG," CameraAccessException 开启预览失败");
//            e.printStackTrace();
//        }









}
