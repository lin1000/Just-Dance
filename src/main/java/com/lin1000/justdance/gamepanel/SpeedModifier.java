package com.lin1000.justdance.gamepanel;

/**
 * DDR / StepMania style scroll-speed modifier.
 *
 * The note field is a time axis (distance = speed x time), so scroll speed is defined
 * from a musically meaningful quantity instead of a raw pixel step. Two modes, exactly
 * like the arcade games:
 *
 *  - XMOD ("x1.5", "x2.0", ...): speed scales with the song's BPM times a player
 *    multiplier. On-screen distance per beat is constant (= BASE_PX_PER_BEAT x multiplier),
 *    so note spacing reflects the rhythm and faster songs scroll faster.
 *  - CMOD ("C300", "C450", ...): speed is constant px/s, treating the whole song as a
 *    fixed target BPM regardless of its real tempo — a constant reading speed.
 *
 * The player cycles multiplier / target and toggles mode live (see DanceAction), and the
 * current setting is shown on the HUD.
 */
public class SpeedModifier {

    public enum Mode { XMOD, CMOD }

    // On-screen note travel per musical beat at multiplier x1.0 — the reference scale that
    // maps beats to pixels. Field height (spawn->judge) is ~660px, so at x1 the field shows
    // ~6.6 beats, at x2 ~3.3 beats, etc.
    private static final double BASE_PX_PER_BEAT = 100.0;

    // XMOD multiplier bounds/step (arcade-style x0.5 .. x8.0 in 0.5 increments).
    private static final double MULT_MIN = 0.5, MULT_MAX = 8.0, MULT_STEP = 0.5;
    // CMOD target-BPM bounds/step.
    private static final double CBPM_MIN = 100, CBPM_MAX = 1000, CBPM_STEP = 50;

    private Mode mode = Mode.XMOD;
    private double multiplier = 1.5;   // XMOD reading-speed multiplier
    private double constantBpm = 300;  // CMOD target BPM

    /** Scroll speed in pixels/second for a song of the given BPM under the current setting. */
    public double pixelsPerSecond(double songBpm) {
        if (mode == Mode.CMOD) {
            return BASE_PX_PER_BEAT * (constantBpm / 60.0);
        }
        return BASE_PX_PER_BEAT * multiplier * (songBpm / 60.0);
    }

    /** Bump the active knob up (multiplier in XMOD, target BPM in CMOD). */
    public void increase() {
        if (mode == Mode.XMOD) multiplier = Math.min(MULT_MAX, multiplier + MULT_STEP);
        else                   constantBpm = Math.min(CBPM_MAX, constantBpm + CBPM_STEP);
    }

    /** Bump the active knob down. */
    public void decrease() {
        if (mode == Mode.XMOD) multiplier = Math.max(MULT_MIN, multiplier - MULT_STEP);
        else                   constantBpm = Math.max(CBPM_MIN, constantBpm - CBPM_STEP);
    }

    /** Switch between XMOD and CMOD. */
    public void toggleMode() {
        mode = (mode == Mode.XMOD) ? Mode.CMOD : Mode.XMOD;
    }

    /** Short HUD label, e.g. "x1.5" or "C300". */
    public String label() {
        return (mode == Mode.XMOD)
                ? String.format("x%.1f", multiplier)
                : "C" + (int) constantBpm;
    }

    public Mode getMode() { return mode; }
    public double getMultiplier() { return multiplier; }
    public double getConstantBpm() { return constantBpm; }
}
