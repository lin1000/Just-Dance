package com.lin1000.justdance.song.midi;

import java.util.List;

/**
 * A parsed MIDI file, used as piano mode's "chart" — the piano-mode equivalent of
 * {@link com.lin1000.justdance.song.sm.Simfile} for arrow mode. Loaded once by
 * {@link MidiChartLoader} and otherwise immutable.
 */
public final class MidiChart {

    /** One played note: a MIDI pitch held from startSec to endSec. */
    public static final class PianoNote {
        public final int pitch;      // MIDI note number, 0-127
        public final double startSec;
        public final double endSec;
        public final int velocity;

        public PianoNote(int pitch, double startSec, double endSec, int velocity) {
            this.pitch = pitch;
            this.startSec = startSec;
            this.endSec = endSec;
            this.velocity = velocity;
        }
    }

    public final List<PianoNote> notes;

    public MidiChart(List<PianoNote> notes) {
        this.notes = notes;
    }
}
