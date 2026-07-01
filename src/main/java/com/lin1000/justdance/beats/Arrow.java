package com.lin1000.justdance.beats;

//真正的箭頭
public class Arrow extends Object {
    public volatile int x;
    public volatile int y;
    public volatile boolean triggered = false;

    // Exact sub-pixel vertical position. At 60 FPS a single frame moves an arrow
    // only a fraction of a pixel, so we accumulate movement here as a double and
    // expose the rounded value through the volatile int `y` that the renderer reads.
    // Only the simulation thread touches yExact, so it needs no synchronization.
    private double yExact;

    public Arrow(int x_position, int y_position) {
        x = x_position;
        y = y_position;
        yExact = y_position;
    }

    // Move upward by `dy` pixels (may be fractional). Returns the new integer y so
    // callers can detect when an arrow has scrolled off the top of the screen.
    public int move(double dy) {
        yExact -= dy; //movement
        y = (int) Math.round(yExact);
        return y;
    }
}
