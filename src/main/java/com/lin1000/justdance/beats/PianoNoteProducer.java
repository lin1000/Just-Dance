package com.lin1000.justdance.beats;

import com.lin1000.justdance.song.midi.MidiChart;

import java.util.ArrayList;
import java.util.List;

/**
 * Advances a {@link MidiChart}'s notes against the audio clock for piano mode — the
 * piano-mode equivalent of {@link ArrowsProducer}, but keyed by MIDI pitch (0-127) instead of
 * a fixed 0-3 lane.
 *
 * Phase 5 scope: rendering only — {@link #visibleNotes} answers "what's on screen right now,"
 * nothing more. Hit-testing incoming MIDI input against these notes is Phase 6's job.
 */
public class PianoNoteProducer {

    private final List<MidiChart.PianoNote> notes;
    private volatile boolean isStop = false;

    public PianoNoteProducer(MidiChart chart) {
        this.notes = chart.notes;
    }

    public void stop() { isStop = true; }

    /**
     * Notes whose time span overlaps [nowSec - pastSec, nowSec + aheadSec] — i.e. currently
     * relevant to draw, from just-passed to not-yet-arrived. {@code notes} is time-sorted
     * (guaranteed by {@link com.lin1000.justdance.song.midi.MidiChartLoader}), so this stops
     * scanning as soon as a note starts past the window instead of walking the whole chart
     * every frame.
     */
    public List<MidiChart.PianoNote> visibleNotes(double nowSec, double pastSec, double aheadSec) {
        List<MidiChart.PianoNote> visible = new ArrayList<>();
        if (isStop) return visible;
        double windowStart = nowSec - pastSec;
        double windowEnd = nowSec + aheadSec;
        for (MidiChart.PianoNote note : notes) {
            if (note.endSec < windowStart) continue;
            if (note.startSec > windowEnd) break;
            visible.add(note);
        }
        return visible;
    }
}
