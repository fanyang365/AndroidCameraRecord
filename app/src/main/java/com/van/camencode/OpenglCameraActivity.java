package com.van.camencode;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;

import com.hjq.permissions.OnPermission;
import com.hjq.permissions.XXPermissions;
import com.van.opengl.GLRender;
import com.van.opengl.MyGlSurfaceView;

import java.util.List;

import androidx.annotation.Nullable;

public class OpenglCameraActivity extends Activity {

    private MyGlSurfaceView myGlSurfaceView;
    private GLRender glRender;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opengl_camera);
        permissionInit();
        findView();
        init();
    }

    private void findView(){
        myGlSurfaceView = findViewById(R.id.myGlSurfaceView);
    }

    private void init(){
        myGlSurfaceView.setEGLContextClientVersion(2);
        glRender    = new GLRender(myGlSurfaceView);
        myGlSurfaceView.setRenderer(glRender);
        myGlSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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

    @Override
    protected void onPause() {
        super.onPause();
        if (myGlSurfaceView != null)
            myGlSurfaceView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (myGlSurfaceView != null)
            myGlSurfaceView.onResume();
    }

}
