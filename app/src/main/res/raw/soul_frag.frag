//纹理坐标系  1  世界坐标系  2
//   纹理   当前上色点的坐标  aCoord  对应起来
varying highp vec2 aCoord;
uniform   sampler2D  vTexture; //有值 1  没有  2
//不断的变化    告诉你条件    cpu  传进来     scalePercent  >1      增加
uniform highp float scalePercent;
//混合 透明度    ----》 有大变小   规律
uniform lowp float mixturePercent;
void main() {
    //采样器 vTexture 工具  颜色 [r  , g ,b  ,a ]
    //    lowp  vec4 textureColor  =  texture2D(vTexture,aCoord);
    //中心点
    highp vec2 center=vec2(0.5,0.5);
    //临时变量   [0.6,0.6]  textureCoordinateToUse  会1  不会2
    highp vec2 textureCoordinateToUse = aCoord;
    //  [0.6,0.6]  -  [0.5, 0.5]   =    [0.1, 0.1]
    textureCoordinateToUse-=center;
    //采样点 一定比     需要渲染 的坐标点要小     y 轴

    // [0,-0.1]  /  1.1   =  [0, -0.09]
    //    textureCoordinateToUse 增大 1  减小  2
    textureCoordinateToUse=textureCoordinateToUse/scalePercent;

    //[0, -0.09]   +  [0.5,0.5] =[0.5,0.41]    ,0.41    实际是变大
    textureCoordinateToUse+=center;
    //    [0.5,0.6]
    //    [0.5,0.6]
    //   gl_FragColor= texture2D(vTexture,aCoord);
    ////[0.5,0.59]   //原来绘制颜色
    lowp vec4 textureColor = texture2D(vTexture, aCoord);
    //      新采样颜色
    lowp vec4 textureColor2= texture2D(vTexture,textureCoordinateToUse);

    //mixturePercent   1  --->0
    gl_FragColor= mix(textureColor,textureColor2,mixturePercent);

}