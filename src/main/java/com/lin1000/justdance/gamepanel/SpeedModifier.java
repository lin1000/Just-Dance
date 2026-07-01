package com.lin1000.justdance.gamepanel;

import java.awt.Color;

/**
 * Named difficulty presets that set the arrow scroll speed.
 *
 * Each level fixes a constant scroll speed in pixels/second, so a given level plays
 * equally hard on every song regardless of its BPM (unlike a BPM-scaled multiplier).
 * Higher level = faster scroll = less reaction time = harder. The note field from spawn
 * (y=730) to the judge line (~70) is ~660px, so the approach time a player gets to read
 * a note is 660 / pxPerSec.
 *
 * The player picks the level with LEFT/RIGHT in the song-selection screen; the choice is
 * carried into gameplay where {@link com.lin1000.justdance.gamepanel.Dance#tick} reads
 * {@link #pixelsPerSecond()} every frame.
 */
public class SpeedModifier {

    public enum Difficulty {
        EASY  ("EASY",   264, new Color(0x35C759)),  // ~2.5s to read each note
        NORMAL("NORMAL", 440, new Color(0xFFD60A)),  // ~1.5s
        HARD  ("HARD",   660, new Color(0xFF9F0A)),  // ~1.0s
        EXPERT("EXPERT", 880, new Color(0xFF453A));  // ~0.75s

        public final String label;
        public final double pxPerSec;
        public final Color color;

        Difficulty(String label, double pxPerSec, Color color) {
            this.label = label;
            this.pxPerSec = pxPerSec;
            this.color = color;
        }
    }

    private Difficulty difficulty = Difficulty.NORMAL;

    /** Constant scroll speed (px/s) for the selected level. */
    public double pixelsPerSecond() { return difficulty.pxPerSec; }

    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty d) { if (d != null) difficulty = d; }

    /** Short HUD label for the selected level, e.g. "NORMAL". */
    public String label() { return difficulty.label; }

    /** Step one level harder / easier, clamped at the ends (no wrap-around). */
    public void harder() {
        Difficulty[] v = Difficulty.values();
        difficulty = v[Math.min(v.length - 1, difficulty.ordinal() + 1)];
    }
    public void easier() {
        Difficulty[] v = Difficulty.values();
        difficulty = v[Math.max(0, difficulty.ordinal() - 1)];
    }
}
