package com.lin1000.justdance.controller;

import com.lin1000.justdance.gamepanel.Dance;

import java.util.TimerTask;

public class FPSTimerTask extends TimerTask {

    private Dance mainTargetWindow;

    public FPSTimerTask(Dance MainTargetWindow) {
        this.mainTargetWindow = MainTargetWindow;
    }

    @Override
    public void run() {
        // Code to be executed by the timer
        //System.out.println("Timer task running on thread: " + Thread.currentThread().getName());
        // Add your specific task logic here
        mainTargetWindow.repaint();
    }
}
