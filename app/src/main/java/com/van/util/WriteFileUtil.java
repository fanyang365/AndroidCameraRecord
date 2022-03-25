package com.van.util;

import android.content.Context;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class WriteFileUtil {

    private static final String TAG = "WriteFileUtil";
    private String path = "";
    private BufferedOutputStream bufferedOutputStream;
    private OutputStream outputStream;

    public WriteFileUtil(String path) {
        this.path = path;
    }

    public boolean createfile(){
        File file = new File(path);
        Log.d(TAG, "创建文件路径="+file.getAbsolutePath());
        try {
            if(file.exists()){
                file.delete();
            }else{
                file.createNewFile();
            }
            bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(file));
            return true;
        } catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }


    public void stopStream(){
        if (bufferedOutputStream != null) {
            try {
                bufferedOutputStream.flush();
                bufferedOutputStream.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        if (outputStream != null){
            try {
                outputStream.close();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    public void writeFile(byte[] data){
        writeFile(data, 0, data.length);
    }

    public void writeFile(byte[] data, int offset, int len){
        try {
            if (bufferedOutputStream != null){
                bufferedOutputStream.write(data, offset, len);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
