package com.lin1000.justdance.controller;

import com.lin1000.justdance.gamepanel.Dance;

import java.util.TimerTask;

public class FPSTimerTask extends TimerTask {

    private long last;
    private long lastLongFramePositionSourceDataLine;

    private Dance mainTargetWindow;

    public FPSTimerTask(Dance MainTargetWindow) {
        this.mainTargetWindow = MainTargetWindow;
    }

    @Override
    public void run() {
        // Code to be executed by the timer
        // System.out.println("Timer task running on thread: " + Thread.currentThread().getName());
        // Add your specific task logic here
        // Calculating the delta time
        long now = System.nanoTime();
        double dt = (now - last) / 1_000_000_000.0;
        last = now;
        mainTargetWindow.setDeltaTime(dt);

        // Calculating the delta Frame
        long nowLongFramePositionSourceDataLine = mainTargetWindow.soundController.currentLongFramePositionSourceDataLine;
        long deltaFrame = (nowLongFramePositionSourceDataLine - lastLongFramePositionSourceDataLine);
        lastLongFramePositionSourceDataLine = nowLongFramePositionSourceDataLine;
        mainTargetWindow.setDeltaFrame(deltaFrame);

        mainTargetWindow.repaint();
    }
}
