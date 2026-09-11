package com.termux.app.vnc;

public class CursorInfo {
    public int width = 0;
    public int height = 0;
    public int xHot = 0;
    public int yHot = 0;

    public CursorInfo() {}

    public CursorInfo(int width, int height, int xHot, int yHot) {
        this.width = width;
        this.height = height;
        this.xHot = xHot;
        this.yHot = yHot;
    }

    public boolean isValid() {
        return width > 0 && height > 0;
    }
}