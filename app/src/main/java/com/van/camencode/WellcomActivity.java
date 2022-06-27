package com.van.camencode;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.hjq.permissions.OnPermission;
import com.hjq.permissions.XXPermissions;

import java.util.List;

import androidx.annotation.Nullable;

public class WellcomActivity  extends Activity implements View.OnClickListener {

    private Button      btnCamera;
    private Button      btnCamera2;
    private Button      btnCameraX;
    private Button      btnCameraOpenGL;
    private Button      btnCameraOpenGL2;
    private Button      btnCameraTextureView;
    private Button      btnRecord;
    private Button      btnCameraYUV;
    private Button      btnCameraRGB;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wellcome);

        btnCamera   = findViewById(R.id.btnCamera);
        btnCameraTextureView   = findViewById(R.id.btnCameraTextureView);
        btnCamera2   = findViewById(R.id.btnCamera2);
        btnCameraX   = findViewById(R.id.btnCameraX);
        btnCameraYUV   = findViewById(R.id.btnCameraYUV);
        btnCameraOpenGL   = findViewById(R.id.btnCameraOpenGL);
        btnCameraOpenGL2   = findViewById(R.id.btnCameraOpenGL2);
        btnCameraRGB   = findViewById(R.id.btnCameraRGB);
        btnRecord   = findViewById(R.id.btnRecord);
        btnCamera.setOnClickListener(this);
        btnCameraTextureView.setOnClickListener(this);
        btnCamera2.setOnClickListener(this);
        btnCameraX.setOnClickListener(this);
        btnCameraOpenGL.setOnClickListener(this);
        btnCameraOpenGL2.setOnClickListener(this);
        btnRecord.setOnClickListener(this);
        btnCameraYUV.setOnClickListener(this);
        btnCameraRGB.setOnClickListener(this);
        permissionInit();
//        btnCamera2.setVisibility(View.GONE);
//        btnCameraX.setVisibility(View.GONE);
    }

    @Override
    public void onClick(View v) {
        Intent intent   = new Intent();
        switch (v.getId()){
            case R.id.btnCamera:
                intent.setClass(WellcomActivity.this, MainActivity.class);
                break;
            case R.id.btnCamera2:
                intent.setClass(WellcomActivity.this, Camera2Activity.class);
                break;
            case R.id.btnCameraX:
                intent.setClass(WellcomActivity.this, CameraXActivity.class);
                break;
            case R.id.btnCameraOpenGL:
                intent.setClass(WellcomActivity.this, OpenglCameraActivity.class);
                break;
            case R.id.btnCameraOpenGL2:
                intent.setClass(WellcomActivity.this, OpenglCameraActivity2.class);
                break;
            case R.id.btnCameraTextureView:
                intent.setClass(WellcomActivity.this, SurfaceTextureActivity.class);
                break;
            case R.id.btnRecord:
                intent.setClass(WellcomActivity.this, RecordActivity.class);
                break;
            case R.id.btnCameraYUV:
                intent.setClass(WellcomActivity.this, CameraYUVOpenGL.class);
                break;
            case R.id.btnCameraRGB:
                intent.setClass(WellcomActivity.this, CameraRGBActivity.class);
                break;
        }
        startActivity(intent);
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
}
