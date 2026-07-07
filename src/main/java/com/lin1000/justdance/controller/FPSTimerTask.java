package com.lin1000.justdance.controller;

import com.lin1000.justdance.gamepanel.GameplayScreen;

import javax.swing.SwingUtilities;
import java.util.TimerTask;

public class FPSTimerTask extends TimerTask {

    private long last;
    private long lastLongFramePositionSourceDataLine;

    private GameplayScreen mainTargetWindow;
    private SoundController soundController;

    public FPSTimerTask(GameplayScreen mainTargetWindow, SoundController soundController) {
        this.mainTargetWindow = mainTargetWindow;
        this.soundController = soundController;
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
        long nowLongFramePositionSourceDataLine = soundController.currentLongFramePositionSourceDataLine;
        long deltaFrame = (nowLongFramePositionSourceDataLine - lastLongFramePositionSourceDataLine);
        lastLongFramePositionSourceDataLine = nowLongFramePositionSourceDataLine;
        mainTargetWindow.setDeltaFrame(deltaFrame);

        // Run the simulation step and the repaint together on the EDT, in that order.
        // This keeps all game-state mutation (controller polling, arrow movement, MISS
        // scoring) on the same thread as the keyboard handlers, so they never race, while
        // paint() remains a pure render. Movement itself is audio-slaved inside tick(), so
        // if the EDT falls behind and a frame is skipped the next tick catches up exactly.
        SwingUtilities.invokeLater(() -> {
            mainTargetWindow.tick();
            mainTargetWindow.repaint();
        });
    }
}
