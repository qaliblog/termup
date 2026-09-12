package com.termux.app.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.vnc.VncClient;
import com.termux.app.vnc.VncFrameView;
import com.termux.app.vnc.VncInputView;
import com.termux.app.vnc.UserCredential;
import com.termux.app.vnc.VncClient.CursorInfo;

import java.security.cert.X509Certificate;

public class DesktopFragment extends Fragment implements VncClient.Observer {

    private static final String TAG = "DesktopFragment";
    private static final String VNC_HOST = "127.0.0.1";
    private static final int VNC_PORT = 5901;

    private TermuxActivity mActivity;
    private View mRootView;
    private VncFrameView mFrameView;
    private VncInputView mInputView;
    private View mLoadingContainer;
    private TextView mLoadingText;
    private VncClient mVncClient;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private Runnable mConnectionRetryRunnable;
    private boolean mIsConnecting = false;
    private int mRetryCount = 0;
    private static final int MAX_RETRIES = 10;
    private static final long RETRY_DELAY_MS = 2000;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_desktop, container, false);
        mActivity = (TermuxActivity) requireActivity();

        mFrameView = mRootView.findViewById(R.id.vnc_frame_view);
        mInputView = mRootView.findViewById(R.id.vnc_input_view);
        mLoadingContainer = mRootView.findViewById(R.id.vnc_loading_container);
        mLoadingText = mRootView.findViewById(R.id.vnc_loading_text);

        // Initialize VNC views
        initializeVncViews();

        // Auto-connect to VNC server
        connectToVnc();

        return mRootView;
    }

    private void initializeVncViews() {
        mVncClient = new VncClient(this);
        mVncClient.configure(1, true, 9, false); // Security type 1 (None), local cursor, quality 9, no raw encoding

        if (mFrameView != null) {
            mFrameView.initialize(mVncClient);
        }

        if (mInputView != null) {
            mInputView.initialize(mVncClient);
        }
    }

    private void connectToVnc() {
        if (mIsConnecting) return;

        mIsConnecting = true;
        showLoading("Starting desktop environment...");

        // First, start the VNC server in the Ubuntu proot
        startVncServer(() -> {
            // After VNC server starts, connect to it
            new Thread(() -> {
                try {
                    mVncClient.connect(VNC_HOST, VNC_PORT);
                    mHandler.post(() -> {
                        mRetryCount = 0;
                        hideLoading();
                        startMessageProcessingLoop();
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Failed to connect to VNC server", e);
                    mHandler.post(() -> {
                        handleConnectionFailed(e.getMessage());
                    });
                }
            }).start();
        });
    }

    private void startVncServer(Runnable onSuccess) {
        showLoading("Starting VNC server in Ubuntu...");

        new Thread(() -> {
            try {
                // Get the Termux app data directory
                String appDataDir = mActivity.getApplicationInfo().dataDir;
                String prootDir = appDataDir + "/files/proot-distro/installed-rootfs/ubuntu";
                
                // Check if Ubuntu is installed
                java.io.File prootFile = new java.io.File(prootDir);
                if (!prootFile.exists()) {
                    mHandler.post(() -> {
                        showLoading("Ubuntu not installed. Please install it first from the terminal.");
                    });
                    return;
                }

                // Start VNC server using proot-distro
                ProcessBuilder pb = new ProcessBuilder(
                    "proot-distro", "login", "ubuntu", "--user", "root", "--",
                    "bash", "-c",
                    "export DISPLAY=:1 && vncserver :1 -geometry 1920x1080 -depth 24 -localhost no -SecurityTypes None 2>&1"
                );
                pb.directory(new java.io.File(appDataDir));
                pb.redirectErrorStream(true);
                
                Process process = pb.start();
                
                // Read output
                java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    Log.d(TAG, "VNC startup: " + line);
                }
                
                int exitCode = process.waitFor();
                Log.d(TAG, "VNC server startup exited with code: " + exitCode);
                
                // Wait a bit for VNC server to be ready
                Thread.sleep(3000);
                
                mHandler.post(onSuccess);
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to start VNC server", e);
                mHandler.post(() -> {
                    handleConnectionFailed("Failed to start VNC server: " + e.getMessage());
                });
            }
        }).start();
    }

    private void startMessageProcessingLoop() {
        new Thread(() -> {
            while (mVncClient != null && mVncClient.isConnected() && !Thread.currentThread().isInterrupted()) {
                try {
                    mVncClient.processServerMessage();
                } catch (Exception e) {
                    Log.e(TAG, "VNC message processing error", e);
                    mHandler.post(() -> {
                        handleDisconnected(e.getMessage());
                    });
                    break;
                }
            }
        }).start();
    }

    private void handleConnectionFailed(String error) {
        mIsConnecting = false;
        mRetryCount++;

        if (mRetryCount < MAX_RETRIES) {
            showLoading("Connection failed. Retrying... (" + mRetryCount + "/" + MAX_RETRIES + ")");
            mConnectionRetryRunnable = this::connectToVnc;
            mHandler.postDelayed(mConnectionRetryRunnable, RETRY_DELAY_MS);
        } else {
            showLoading("Failed to connect to desktop. Please ensure the VNC server is running.");
        }
    }

    private void handleDisconnected(String error) {
        if (mVncClient != null) {
            mVncClient.cleanup();
        }
        mIsConnecting = false;
        showLoading("Disconnected: " + error + "\nRetrying...");

        mConnectionRetryRunnable = this::connectToVnc;
        mHandler.postDelayed(mConnectionRetryRunnable, RETRY_DELAY_MS);
    }

    private void showLoading(String message) {
        if (mLoadingContainer != null) {
            mLoadingContainer.setVisibility(View.VISIBLE);
        }
        if (mLoadingText != null) {
            mLoadingText.setText(message);
        }
        if (mFrameView != null) {
            mFrameView.setVisibility(View.GONE);
        }
    }

    private void hideLoading() {
        if (mLoadingContainer != null) {
            mLoadingContainer.setVisibility(View.GONE);
        }
        if (mFrameView != null) {
            mFrameView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mVncClient != null && !mVncClient.isConnected() && !mIsConnecting) {
            connectToVnc();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mConnectionRetryRunnable != null) {
            mHandler.removeCallbacks(mConnectionRetryRunnable);
        }
        if (mVncClient != null) {
            mVncClient.setFrameBufferUpdatesPaused(true);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mConnectionRetryRunnable != null) {
            mHandler.removeCallbacks(mConnectionRetryRunnable);
        }
        if (mVncClient != null) {
            mVncClient.cleanup();
            mVncClient = null;
        }
    }

    // VncClient.Observer implementation
    @Override
    public String getVncPassword() {
        return ""; // No password for local VNC
    }

    @Override
    public UserCredential getVncCredentials() {
        return UserCredential.empty();
    }

    @Override
    public boolean verifyVncServerCertificate(X509Certificate certificate) {
        return true; // Accept all certificates for local connection
    }

    @Override
    public void onCutTextReceived(String text) {
        // Handle clipboard text from remote
    }

    @Override
    public void onFramebufferUpdated() {
        if (mFrameView != null) {
            mFrameView.onFramebufferUpdated();
        }
    }

    @Override
    public void onFramebufferSizeChanged(int width, int height) {
        Log.d(TAG, "Framebuffer size changed: " + width + "x" + height);
        if (mFrameView != null) {
            mFrameView.onFramebufferSizeChanged(width, height);
        }
    }

    @Override
    public void onPointerMoved(int x, int y) {
        // Pointer moved by server
    }

    @Override
    public void onBell() {
        // Bell notification from server
    }
}