package com.lin1000.justdance.gamepanel;

import java.awt.Color;

/**
 * Which gameplay engine a round uses: arrow-based (StepMania-style, {@link Dance}) or
 * piano-based (MIDI note highway, a future {@code PianoDance}).
 *
 * The player toggles the mode with a button in the song-selection screen; the choice is read
 * by {@link com.lin1000.justdance.Project#run()} *before* constructing gameplay, since (unlike
 * {@link SpeedModifier}, which only tunes behavior inside an already-decided class) this
 * choice decides which concrete class gets constructed.
 */
public class GameMode {

    public enum Mode {
        ARROW("ARROW", new Color(0x5fe6ff)),
        PIANO("PIANO", new Color(0xff9f0a));

        public final String label;
        public final Color color;

        Mode(String label, Color color) {
            this.label = label;
            this.color = color;
        }
    }

    private Mode mode = Mode.ARROW;

    public Mode getMode() { return mode; }
    public void setMode(Mode m) { if (m != null) mode = m; }

    /** Short HUD label for the selected mode, e.g. "PIANO". */
    public String label() { return mode.label; }

    /** Cycle to the other mode (only two values, so toggle and cycle are the same thing). */
    public void toggle() {
        Mode[] v = Mode.values();
        mode = v[(mode.ordinal() + 1) % v.length];
    }
}
