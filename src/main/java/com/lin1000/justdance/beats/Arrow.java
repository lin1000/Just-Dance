package com.lin1000.justdance.beats;

/**
 * A note. Its screen position is a pure function of the audio clock: an arrow knows the audio
 * time at which it should reach the judge line ({@code targetTimeSec}) and computes its Y each
 * frame from the current time. This is the beat-based model — no per-frame drift accumulation.
 */
public class Arrow extends Object {
    public volatile int x;
    public volatile int y;
    public final int lane;              // 0=Left 1=Down 2=Up 3=Right
    public final double targetTimeSec;  // audio time (s) when this note should hit the judge line
    public volatile boolean triggered = false;

    public Arrow(int x_position, int lane, double targetTimeSec) {
        this.x = x_position;
        this.lane = lane;
        this.targetTimeSec = targetTimeSec;
        this.y = Integer.MAX_VALUE / 2; // off-screen until the first update
    }

    /**
     * Recompute screen Y from the current audio time. A future note sits below the judge line
     * (larger y); it reaches {@code judgeY} exactly at {@code targetTimeSec}, then rises past it.
     * Returns the new y.
     */
    public int updateY(double nowSec, double pxPerSec, int judgeY) {
        y = judgeY + (int) Math.round((targetTimeSec - nowSec) * pxPerSec);
        return y;
    }
}
