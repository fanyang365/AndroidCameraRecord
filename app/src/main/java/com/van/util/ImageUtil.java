package com.van.util;

public class ImageUtil {

    /**
     * 翻转NV21数据
     * @param src
     * @param width
     * @param height
     * @param dst
     * @param rotation
     */
    public static void rotate(byte[] src, int width, int height, byte[] dst,  int rotation){

        if (rotation == 90){
            capture_video_process90(src, width, height, dst);
            nv12_uv_video_process90(src, width, height, dst);
        }else if(rotation == 180){
            capture_video_process180(src, width, height, dst);
            nv12_video_process180(src, width, height, dst);
        }else if(rotation == 270){
            capture_video_process270(src, width, height, dst);
            nv12_video_process270(src, width, height, dst);
        }else {
            nv12_video_process0(src, width, height, dst);
        }

    }

    // 90度顺时针旋转
    public static void nv12_video_process0(byte[] src, int width, int height, byte[] dst)
    {
//        Log.d("test", "nv12_video_process0 111");
        int		i, j, uv_index, offset;
        int 	uv_width	= width>>1;
        int     uv_height	= height>>1;

        offset      = width * height;
        uv_index    = 0;

        // copy y data
        System.arraycopy(src, 0, dst, 0, offset);
        for (i=0; i<uv_height; i++)
        {
            for (j=0; j<uv_width; j++)
            {
                dst[offset+uv_index]    = src[offset+uv_index+1];
                dst[offset+uv_index+1]  = src[offset+uv_index];
                uv_index ++;
                uv_index ++;
            }
        }
    }
    
    // 90度顺时针旋转
    public static void nv12_uv_video_process90(byte[] src, int width, int height, byte[] dst)
    {
        int		i, j, src_size, dst_size, offset;
        int 	uv_width	= width>>1;
        int     uv_height	= height>>1;
        offset  = width * height;
        src_size = 0;
        dst_size = 0;
        for (i=0; i<uv_height; i++)
        {
            dst_size = uv_height-i-1;
            dst_size = dst_size+dst_size;//两个字节

            for (j=0; j<uv_width; j++)
            {
                //dst[dst_size]	= src[src_size+1];
                //dst[dst_size+1] = src[src_size];
                dst[offset+dst_size]    = src[offset+src_size+1];
                dst[offset+dst_size+1]  = src[offset+src_size];

                dst_size += uv_height;
                dst_size += uv_height;
                src_size ++;
                src_size ++;
            }
        }
    }

    // 90度顺时针旋转
    public static void capture_video_process90(byte[] src, int width, int height, byte[] des)
    {
        int		i, j, src_size, dst_size;

        src_size = 0;
        dst_size = 0;
        for (i=0; i<height; i++)
        {
            dst_size = height-i-1;
            for (j=0; j<width; j++)
            {
                des[dst_size] = src[src_size];

                dst_size += height;
                src_size ++;
            }
        }
    }


    // 180度顺时针旋转
    public static void nv12_video_process180(byte[] src, int width, int height, byte[] dst)
    {
        int		i, j, src_size, dst_size, offset;
        offset   = width * height;
        int 	uv_width	= width>>1;
        int     uv_height	= height>>1;
        src_size = 0;
        dst_size = uv_width*uv_height-1;
        dst_size = dst_size+dst_size;

        for (i=0; i<uv_height; i++)
        {
            for (j=0; j<uv_width; j++)
            {
                //dst[dst_size]	= src[src_size+1];
                //dst[dst_size+1] = src[src_size];
                dst[offset+dst_size]	= src[offset+src_size+1];
                dst[offset+dst_size+1] = src[offset+src_size];

                dst_size --;
                dst_size --;
                src_size ++;
                src_size ++;
            }
        }
    }

    // 180度顺时针旋转
    public static void capture_video_process180(byte[] src, int width, int height, byte[] dst)
    {
        int		i, j, src_size, dst_size;

        src_size = 0;
        dst_size = width*height-1;

        for (i=0; i<height; i++)
        {
            for (j=0; j<width; j++)
            {
                dst[dst_size] = src[src_size];

                dst_size --;
                src_size ++;
            }
        }
    }


    // 270度逆时间旋转,并左右颠倒
    public static void nv12_video_process270(byte[] src, int width, int height, byte[] dst)
    {
        int		i, j, src_size, dst_size, dst_bottom_size, offset;
        offset   = width * height;
        int 	uv_width	= width>>1;
        int     uv_height	= height>>1;
        src_size 		= 0;
        dst_size 		= 0;
        dst_bottom_size = (uv_width-1)*uv_height;
        dst_bottom_size = dst_bottom_size;

        for (i=0; i<uv_height; i++)
        {
            dst_size = dst_bottom_size+i;
            dst_size += dst_size;
            for (j=0; j<uv_width; j++)
            {
                //dst[dst_size]	= src[src_size+1];
                //dst[dst_size+1] = src[src_size];
                dst[offset+dst_size]	= src[offset+src_size+1];
                dst[offset+dst_size+1] = src[offset+src_size];

                dst_size -= uv_height+uv_height;
                src_size += 2;
            }
        }
    }

    // 270度逆时间旋转,并左右颠倒
    public static void capture_video_process270(byte[] src, int width, int height, byte[] dst)
    {
        int		i, j, src_size, dst_size, dst_bottom_size;

        src_size 		= 0;
        dst_size 		= 0;
        dst_bottom_size = (width-1)*height;
        for (i=0; i<height; i++)
        {
            dst_size = dst_bottom_size+i;
            for (j=0; j<width; j++)
            {
                dst[dst_size] = src[src_size];

                dst_size -= height;
                src_size ++;
            }
        }
    }

}
