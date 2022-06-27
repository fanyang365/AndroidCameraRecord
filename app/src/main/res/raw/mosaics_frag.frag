precision mediump float;
varying vec2 aCoord;
uniform sampler2D vTexture;

// 假定纹理的大小
const vec2 TexSize = vec2(400.0, 400.0);
// 马赛克的大小
const vec2 mosaicSize = vec2(8.0, 8.0);

uniform highp int  faceCount;
uniform highp float faceX[50];
uniform highp float faceY[50];
uniform highp float faceWidth[50];
uniform highp float faceHeight[50];


void main()
{
    vec4 color;

    for (int i=0; i<50; i++){


        //纹理坐标和屏幕左边Y轴是相反的，所以需要使用1.0减去Y值
        if(faceWidth[i] > 0.0 && faceHeight[i] > 0.0
            && aCoord.x > faceX[i] && aCoord.x < faceWidth[i] && (1.0 - aCoord.y )> faceY[i] && (1.0 - aCoord.y )< faceHeight[i]
        ){
            // 纹理坐标是0～1， 先将纹理坐标扩大假定纹理大小
            vec2 intXY = vec2(aCoord.x*TexSize.x, aCoord.y*TexSize.y);
            // 计算得到假定纹理大小下当前纹理坐标所处色块的起始点位置
            vec2 XYMosaic = vec2(floor(intXY.x/mosaicSize.x)*mosaicSize.x, floor(intXY.y/mosaicSize.y)*mosaicSize.y);
            // 在将起始点位置换算成标准0～1的范围
            vec2 UVMosaic = vec2(XYMosaic.x/TexSize.x, XYMosaic.y/TexSize.y);
            color = texture2D(vTexture, UVMosaic);
            gl_FragColor = color;
            return ;
        }
    }

    //采样原图
    color  =texture2D(vTexture, aCoord);
    gl_FragColor = color;

}