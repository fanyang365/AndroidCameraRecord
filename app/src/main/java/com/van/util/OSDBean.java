package com.van.util;

public class OSDBean implements Cloneable {
    private int     textSize    = 30;
    private int     textPadding = 0;

    private String OSD1;
    private String OSD2;
    private String OSD3;
    private String OSD4;
    private String textColor    = "#FFFFFF";
    private String bgColor      = "#000000FF";

    public String getOSD1() {
        return OSD1;
    }

    public void setOSD1(String OSD1) {
        this.OSD1 = OSD1;
    }

    public String getOSD2() {
        return OSD2;
    }

    public void setOSD2(String OSD2) {
        this.OSD2 = OSD2;
    }

    public String getOSD3() {
        return OSD3;
    }

    public void setOSD3(String OSD3) {
        this.OSD3 = OSD3;
    }

    public int getTextSize() {
        return textSize;
    }

    public void setTextSize(int textSize) {
        this.textSize = textSize;
    }

    public int getTextPadding() {
        return textPadding;
    }

    public void setTextPadding(int textPadding) {
        this.textPadding = textPadding;
    }

    public String getTextColor() {
        return textColor;
    }

    public void setTextColor(String textColor) {
        this.textColor = textColor;
    }

    public String getBgColor() {
        return bgColor;
    }

    public void setBgColor(String bgColor) {
        this.bgColor = bgColor;
    }

    public String getOSD4() {
        return OSD4;
    }

    public void setOSD4(String OSD4) {
        this.OSD4 = OSD4;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
