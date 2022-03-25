attribute vec4 vPosition;
attribute vec2 vCoord;

uniform mat4 uMVPMatrix;

varying vec2 aCoord;

void main() {
    gl_Position =  uMVPMatrix * vPosition;
    aCoord = vCoord;
}