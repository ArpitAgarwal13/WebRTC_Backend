package com.shivam.androidwebrtc.tutorial;

import android.content.Context;
import android.os.SystemClock;

import org.webrtc.*;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class CustomVideoCapturer implements VideoCapturer {
    private CapturerObserver capturerObserver;
    private int width = 1280;
    private int height = 720;
    private int fps = 30;

    private final Timer timer = new Timer();
    private final TimerTask tickTask = new TimerTask() {
        public void run() {
            CustomVideoCapturer.this.tick();
        }
    };

    @Override
    public void initialize(SurfaceTextureHelper surfaceTextureHelper, Context context, CapturerObserver capturerObserver) {
        this.capturerObserver = capturerObserver;
    }

    public CustomVideoCapturer(int width, int height, int fps) {
        this.width = width;
        this.height = height;
        this.fps = fps;
    }

    public void tick() {
        pushFrame(getNextFrame());
    }

    @Override
    public void startCapture(int width, int height, int framerate) {
        this.timer.schedule(this.tickTask, 0L, (long)(1000 / framerate));
    }

    @Override
    public void stopCapture() throws InterruptedException {
        this.timer.cancel();
    }

    @Override
    public void changeCaptureFormat(int width, int height, int framerate) {
        this.width = width;
        this.height = height;
        this.fps = framerate;
    }

    @Override
    public void dispose() {
//        isCapturing = false;
    }

    @Override
    public boolean isScreencast() {
        return false;
    }

    private void pushFrame(byte[] data) {
        if (capturerObserver == null) return;

        long captureTimeNs = TimeUnit.MILLISECONDS.toNanos(SystemClock.elapsedRealtime());

//        this.capturerObserver.onByteBufferFrameCaptured(data, width, height, fps, captureTimeNs);
    }

    private byte[] getNextFrame() {
        byte[] frameData = new byte[10]; // Replace with actual frame data
        return frameData;
    }
}

