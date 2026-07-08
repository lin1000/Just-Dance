package com.lin1000.justdance.beats;

import com.lin1000.justdance.controller.ConditionController;
import com.lin1000.justdance.song.midi.MidiChart;

import java.util.ArrayList;
import java.util.List;

/**
 * Advances a {@link MidiChart}'s notes against the audio clock for piano mode — the
 * piano-mode equivalent of {@link ArrowsProducer}, but keyed by MIDI pitch (0-127) instead of
 * a fixed 0-3 lane.
 *
 * {@link #visibleNotes} answers "what's on screen right now" for rendering. {@link #tryHit} and
 * {@link #sweepMisses} are the judgment half (Phase 6): a direct time-based tolerance window
 * around each note's {@code startSec} takes the place of arrow mode's pixel-Y judge line, since
 * piano has no per-difficulty scroll speed to make Y-position and time equivalent.
 */
public class PianoNoteProducer {

    // Tolerance windows in seconds, tunable. Mirrors arrow mode's PERFECT/GOOD tiers
    // (DanceAction.judgeLine) but expressed directly in time since piano has no scroll speed.
    private static final double PERFECT_SEC = 0.080;
    private static final double GOOD_SEC = 0.180;

    /** Mutable per-note judgment state, parallel to the chart's immutable note list. */
    private static final class PendingNote {
        final MidiChart.PianoNote note;
        boolean resolved;
        PendingNote(MidiChart.PianoNote note) { this.note = note; }
    }

    private final List<MidiChart.PianoNote> notes;
    private final List<PendingNote> pending;
    private volatile boolean isStop = false;

    public PianoNoteProducer(MidiChart chart) {
        this.notes = chart.notes;
        this.pending = new ArrayList<>(notes.size());
        for (MidiChart.PianoNote note : notes) {
            pending.add(new PendingNote(note));
        }
    }

    public void stop() { isStop = true; }

    /**
     * Hit-tests a MIDI NOTE_ON against unresolved notes of the same pitch within {@link
     * #GOOD_SEC} of {@code nowSec}; the nearest in time wins. Marks it resolved so it can't be
     * hit or missed again. Returns the {@link ConditionController#setCondition} code to fire
     * (0=PERFECT, 1=GOOD), or -1 if nothing matched — the caller should silently ignore a -1,
     * same as arrow mode ignores a press that lands on no arrow.
     */
    public int tryHit(int pitch, double nowSec) {
        if (isStop) return -1;
        PendingNote best = null;
        double bestDelta = Double.MAX_VALUE;
        for (PendingNote p : pending) {
            if (p.resolved || p.note.pitch != pitch) continue;
            double delta = Math.abs(p.note.startSec - nowSec);
            if (delta <= GOOD_SEC && delta < bestDelta) {
                best = p;
                bestDelta = delta;
            }
        }
        if (best == null) return -1;
        best.resolved = true;
        return bestDelta <= PERFECT_SEC ? 0 : 1;
    }

    /**
     * Called every tick from {@link com.lin1000.justdance.gamepanel.PianoDance#tick()}: any
     * unresolved note whose hit window has fully closed (nowSec has passed startSec by more
     * than GOOD_SEC with no {@link #tryHit}) becomes an unrecoverable MISS, fired once.
     */
    public void sweepMisses(double nowSec, ConditionController conditionControl) {
        if (isStop) return;
        for (PendingNote p : pending) {
            if (p.resolved) continue;
            if (nowSec - p.note.startSec > GOOD_SEC) {
                p.resolved = true;
                conditionControl.setCondition(3); // MISS
            }
        }
    }

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
