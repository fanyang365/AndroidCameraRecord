precision mediump float;
varying  vec2 aCoord;  //待绘制坐标  0， 0
uniform sampler2D vTexture;
void main() {
    float y = aCoord.y;

    if(y<0.5)
    {
        y+=0.25;
    }else{
        y -= 0.25;
    }
//    采样的坐标
    gl_FragColor= texture2D(vTexture, vec2(aCoord.x, y));

}