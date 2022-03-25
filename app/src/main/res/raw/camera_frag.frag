#extension GL_OES_EGL_image_external : require
precision mediump float; // 数据精度
//必须 写的 固定的  意思   用采样器
//所有float类型数据的精度是lowp
varying vec2 aCoord;
//采样器  uniform static
uniform samplerExternalOES vTexture;
void main(){
    //Opengl 自带函数
    vec4 rgba = texture2D(vTexture,aCoord);
    gl_FragColor=vec4(rgba.r,rgba.g,rgba.b,rgba.a);
}