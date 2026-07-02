package com.lin1000.justdance.beats;

/**
 * A note. Its screen position is a pure function of the audio clock: an arrow knows the audio
 * time at which it should reach the judge line ({@code targetTimeSec}) and computes its Y each
 * frame from the current time. Holds/rolls additionally carry {@code targetEndTimeSec} — the
 * time their tail reaches the judge line — and render a body between head and tail.
 */
public class Arrow extends Object {
    public volatile int x;
    public volatile int y;
    public final int lane;                 // 0=Left 1=Down 2=Up 3=Right
    public final double targetTimeSec;     // audio time (s) when the head should hit the judge line
    public final double targetEndTimeSec;  // tail time for holds/rolls; == targetTimeSec for taps
    public volatile int yTail;             // screen Y of the tail (== y for taps)
    // Hold engaged: the head was hit and the player is holding the panel. While engaged the
    // head freezes at the judge line (classic DDR) until the tail time passes (OK) or the
    // panel is released early (break).
    public volatile boolean held = false;
    public volatile boolean triggered = false;

    public Arrow(int x_position, int lane, double targetTimeSec) {
        this(x_position, lane, targetTimeSec, targetTimeSec);
    }

    public Arrow(int x_position, int lane, double targetTimeSec, double targetEndTimeSec) {
        this.x = x_position;
        this.lane = lane;
        this.targetTimeSec = targetTimeSec;
        this.targetEndTimeSec = Math.max(targetTimeSec, targetEndTimeSec);
        this.y = Integer.MAX_VALUE / 2; // off-screen until the first update
        this.yTail = this.y;
    }

    public boolean isHold() { return targetEndTimeSec > targetTimeSec; }

    /**
     * Recompute screen Y from the current audio time. A future note sits below the judge line
     * (larger y); it reaches {@code judgeY} exactly at {@code targetTimeSec}, then rises past it.
     * An engaged hold's head stays pinned at the judge line while its tail keeps approaching.
     * Returns the new head y.
     */
    public int updateY(double nowSec, double pxPerSec, int judgeY) {
        int headY = judgeY + (int) Math.round((targetTimeSec - nowSec) * pxPerSec);
        yTail = judgeY + (int) Math.round((targetEndTimeSec - nowSec) * pxPerSec);
        if (held && headY < judgeY) headY = judgeY; // freeze at the receptors while held
        y = headY;
        return y;
    }
}
