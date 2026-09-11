package com.termux.app.vnc;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class VncInputView extends View {

    private VncClient mVncClient;
    private float mLastTouchX;
    private float mLastTouchY;
    private int mPointerId = -1;

    public VncInputView(Context context) {
        super(context);
        init();
    }

    public VncInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VncInputView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    public void initialize(VncClient vncClient) {
        mVncClient = vncClient;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mVncClient == null || !mVncClient.isConnected()) {
            return super.onTouchEvent(event);
        }

        int action = event.getActionMasked();
        int pointerIndex = event.getActionIndex();
        int pointerId = event.getPointerId(pointerIndex);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                mPointerId = pointerId;
                mLastTouchX = event.getX(pointerIndex);
                mLastTouchY = event.getY(pointerIndex);
                sendPointerEvent((int) mLastTouchX, (int) mLastTouchY, 1); // Button 1 = left click
                return true;

            case MotionEvent.ACTION_MOVE:
                // Find the pointer index for our tracked pointer
                int index = event.findPointerIndex(mPointerId);
                if (index >= 0) {
                    float x = event.getX(index);
                    float y = event.getY(index);
                    mVncClient.moveClientPointer((int) x, (int) y);
                    mLastTouchX = x;
                    mLastTouchY = y;
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                if (pointerId == mPointerId) {
                    sendPointerEvent((int) mLastTouchX, (int) mLastTouchY, 0); // Release button
                    mPointerId = -1;
                }
                return true;
        }

        return super.onTouchEvent(event);
    }

    private void sendPointerEvent(int x, int y, int mask) {
        if (mVncClient != null) {
            mVncClient.sendPointerEvent(x, y, mask);
        }
    }

    @Override
    public boolean onHoverEvent(MotionEvent event) {
        // Handle hover events for mouse-like input
        return onTouchEvent(event);
    }
}