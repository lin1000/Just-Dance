package com.lin1000.justdance.beats;

//真正的箭頭
public class Beat extends Object {
    public double time; // 節奏點發生的時間（秒）
    public int x;
    public int y;
    public boolean triggered = false;
    public BeatMapGenerator.Mode signalMode;
    public double singalStrength;
    public int frameIndex;

    public Beat(double time, int x_position, int y_position) {
        this.time = time;
        x = x_position;
        y = y_position;
    }

    public double getSingalStrength() {
        return singalStrength;
    }

    public void setSingalStrength(double singalStrength) {
        this.singalStrength = singalStrength;
    }

    public int getFrameIndex() {
        return frameIndex;
    }

    public void setFrameIndex(int frameIndex) {
        this.frameIndex = frameIndex;
    }

    public BeatMapGenerator.Mode getSignalMode() {
        return signalMode;
    }

    public void setSignalMode(BeatMapGenerator.Mode signalMode) {
        this.signalMode = signalMode;
    }
}
