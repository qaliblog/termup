package com.termux.app.vnc;

import android.util.Log;

import java.io.IOException;
import java.security.cert.X509Certificate;

public class VncClient {

    private static final String TAG = "VncClient";

    public interface Observer {
        String getVncPassword();
        UserCredential getVncCredentials();
        boolean verifyVncServerCertificate(X509Certificate certificate);
        void onCutTextReceived(String text);
        void onFramebufferUpdated();
        void onFramebufferSizeChanged(int width, int height);
        void onPointerMoved(int x, int y);
        void onBell();
    }

    public static class CursorInfo {
        public int width;
        public int height;
        public int xHot;
        public int yHot;

        public boolean isValid() {
            return width > 0 && height > 0;
        }
    }

    private long mNativePtr;
    private final Observer mObserver;
    private volatile boolean mConnected = false;
    private boolean mDestroyed = false;
    public CursorInfo cursorInfo = new CursorInfo();

    public VncClient(Observer observer) {
        mObserver = observer;
        mNativePtr = nativeClientCreate();
        if (mNativePtr == 0) {
            throw new RuntimeException("Could not create native rfbClient!");
        }
    }

    public void configure(int securityType, boolean useLocalCursor, int imageQuality, boolean useRawEncoding) {
        if (!mConnected && !mDestroyed) {
            nativeConfigure(mNativePtr, securityType, useLocalCursor, imageQuality, useRawEncoding);
        }
    }

    public void connect(String host, int port) throws IOException {
        if (mConnected) {
            throw new IllegalStateException("Already connected");
        }
        if (mDestroyed) {
            throw new IllegalStateException("Client has been destroyed");
        }

        if (!nativeInit(mNativePtr, host, port)) {
            throw new IOException(nativeGetLastErrorStr());
        }
        mConnected = true;
    }

    public boolean processServerMessage() throws IOException {
        if (!mConnected || mDestroyed) {
            return false;
        }

        if (nativeProcessServerMessage(mNativePtr)) {
            return true;
        }

        mConnected = false;
        throw new IOException(nativeGetLastErrorStr());
    }

    public String getDesktopName() {
        if (mConnected && !mDestroyed) {
            return nativeGetDesktopName(mNativePtr);
        }
        return "";
    }

    public void sendKeyEvent(int keySym, int xtCode, boolean isDown) {
        if (mConnected && !mDestroyed) {
            nativeSendKeyEvent(mNativePtr, keySym, xtCode, isDown);
        }
    }

    public void sendPointerEvent(int x, int y, int mask) {
        if (mConnected && !mDestroyed) {
            nativeSendPointerEvent(mNativePtr, x, y, mask);
        }
    }

    public void moveClientPointer(int x, int y) {
        if (mConnected && !mDestroyed) {
            nativeMoveClientPointer(mNativePtr, x, y);
            if (mObserver != null) {
                mObserver.onPointerMoved(x, y);
            }
        }
    }

    public void sendCutText(String text) {
        if (mConnected && !mDestroyed) {
            nativeSendCutText(mNativePtr, text);
        }
    }

    public void setDesktopSize(int width, int height) {
        if (mConnected && !mDestroyed && width > 0 && height > 0) {
            nativeSetDesktopSize(mNativePtr, width, height);
        }
    }

    public void refreshFrameBuffer() {
        if (mConnected && !mDestroyed) {
            nativeRefreshFrameBuffer(mNativePtr);
        }
    }

    public void setFrameBufferUpdatesPaused(boolean pause) {
        if (mDestroyed) return;
        nativePauseFramebufferUpdates(mNativePtr, pause);
        if (!pause) {
            refreshFrameBuffer();
        }
    }

    public void uploadFrameTexture() {
        if (mConnected && !mDestroyed) {
            nativeUploadFrameTexture(mNativePtr);
        }
    }

    public void uploadCursorTexture() {
        if (mConnected && !mDestroyed) {
            nativeUploadCursorTexture(mNativePtr);
        }
    }

    public void cleanup() {
        if (!mDestroyed) {
            nativeCleanup(mNativePtr);
            mConnected = false;
            mDestroyed = true;
        }
    }

    public boolean isConnected() {
        return mConnected && !mDestroyed;
    }

    public int getFramebufferWidth() {
        if (mConnected && !mDestroyed) {
            return nativeGetWidth(mNativePtr);
        }
        return 0;
    }

    public int getFramebufferHeight() {
        if (mConnected && !mDestroyed) {
            return nativeGetHeight(mNativePtr);
        }
        return 0;
    }

    // Native methods
    private native long nativeClientCreate();
    private native void nativeConfigure(long clientPtr, int securityType, boolean useLocalCursor, int imageQuality, boolean useRawEncoding);
    private native boolean nativeInit(long clientPtr, String host, int port);
    private native boolean nativeProcessServerMessage(long clientPtr);
    private native void nativeSendKeyEvent(long clientPtr, int keySym, int xtCode, boolean isDown);
    private native void nativeSendPointerEvent(long clientPtr, int x, int y, int mask);
    private native void nativeMoveClientPointer(long clientPtr, int x, int y);
    private native void nativeSendCutText(long clientPtr, String text);
    private native void nativeSetDesktopSize(long clientPtr, int width, int height);
    private native void nativeRefreshFrameBuffer(long clientPtr);
    private native void nativePauseFramebufferUpdates(long clientPtr, boolean pause);
    private native String nativeGetDesktopName(long clientPtr);
    private native int nativeGetWidth(long clientPtr);
    private native int nativeGetHeight(long clientPtr);
    private native String nativeGetLastErrorStr();
    private native void nativeCleanup(long clientPtr);
    private native void nativeUploadFrameTexture(long clientPtr);
    private native void nativeUploadCursorTexture(long clientPtr);

    static {
        System.loadLibrary("native-vnc");
    }

    // Callback methods called from native code
    @SuppressWarnings("unused")
    private String cbGetPassword() {
        return mObserver != null ? mObserver.getVncPassword() : "";
    }

    @SuppressWarnings("unused")
    private UserCredential cbGetCredential() {
        return mObserver != null ? mObserver.getVncCredentials() : null;
    }

    @SuppressWarnings("unused")
    private boolean cbVerifyServerCertificate(byte[] der) {
        if (mObserver != null) {
            try {
                java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X.509");
                java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(der);
                X509Certificate cert = (X509Certificate) cf.generateCertificate(bis);
                return mObserver.verifyVncServerCertificate(cert);
            } catch (Exception e) {
                Log.e(TAG, "Certificate verification error", e);
            }
        }
        return false;
    }

    @SuppressWarnings("unused")
    private void cbGotXCutText(byte[] bytes, boolean isUTF8) {
        if (mObserver != null) {
            String cutText = isUTF8 ? new String(bytes, java.nio.charset.StandardCharsets.UTF_8)
                    : new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1);
            mObserver.onCutTextReceived(cutText);
        }
    }

    @SuppressWarnings("unused")
    private void cbFinishedFrameBufferUpdate() {
        if (mObserver != null) {
            mObserver.onFramebufferUpdated();
        }
    }

    @SuppressWarnings("unused")
    private void cbFramebufferSizeChanged(int w, int h) {
        if (mObserver != null) {
            mObserver.onFramebufferSizeChanged(w, h);
        }
    }

    @SuppressWarnings("unused")
    private void cbBell() {
        if (mObserver != null) {
            mObserver.onBell();
        }
    }

    @SuppressWarnings("unused")
    private void cbHandleCursorPos(int x, int y) {
        if (mObserver != null) {
            mObserver.onPointerMoved(x, y);
        }
    }
}