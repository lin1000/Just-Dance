package com.lin1000.justdance.song.midi;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads a standard MIDI file (.mid/.midi) into a {@link MidiChart} — the piano-mode equivalent
 * of {@link com.lin1000.justdance.song.sm.SmParser} parsing a .sm file for arrow mode.
 *
 * MVP simplification, documented explicitly: tempo is read once from the file's first tempo
 * meta-event (defaulting to 120 BPM if none is present) and treated as constant for the whole
 * file. Fine for short pieces without mid-song tempo changes; a file that genuinely changes
 * tempo partway through would need per-event tempo tracking, which this loader does not do.
 */
public final class MidiChartLoader {

    private MidiChartLoader() {}

    private static final int DEFAULT_TEMPO_MPQN = 500_000; // 120 BPM, in microseconds per quarter note
    private static final int META_TEMPO_TYPE = 0x51;

    public static MidiChart load(File midiFile) throws Exception {
        Sequence sequence = MidiSystem.getSequence(midiFile);
        if (sequence.getDivisionType() != Sequence.PPQ) {
            throw new UnsupportedOperationException("Only PPQ-resolution MIDI files are supported: " + midiFile);
        }
        int ppq = sequence.getResolution();
        double secondsPerTick = findFirstTempoMpqn(sequence) / 1_000_000.0 / ppq;

        List<MidiChart.PianoNote> notes = new ArrayList<>();
        // One open-note tracker per (channel, pitch), reset per track: pairs each NOTE_ON with
        // its matching NOTE_OFF (or a zero-velocity NOTE_ON, the standard alternate note-off).
        Map<Integer, long[]> openNotes = new HashMap<>(); // key: channel*128+pitch -> {startTick, velocity}

        for (Track track : sequence.getTracks()) {
            openNotes.clear();
            for (int i = 0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                if (!(event.getMessage() instanceof ShortMessage sm)) continue;

                int key = sm.getChannel() * 128 + sm.getData1();
                boolean isNoteOn = sm.getCommand() == ShortMessage.NOTE_ON && sm.getData2() > 0;
                boolean isNoteOff = sm.getCommand() == ShortMessage.NOTE_OFF
                        || (sm.getCommand() == ShortMessage.NOTE_ON && sm.getData2() == 0);

                if (isNoteOn) {
                    openNotes.put(key, new long[]{event.getTick(), sm.getData2()});
                } else if (isNoteOff) {
                    long[] open = openNotes.remove(key);
                    if (open == null) continue; // stray NOTE_OFF with no matching NOTE_ON
                    double startSec = open[0] * secondsPerTick;
                    double endSec = event.getTick() * secondsPerTick;
                    notes.add(new MidiChart.PianoNote(sm.getData1(), startSec, endSec, (int) open[1]));
                }
            }
        }

        notes.sort(Comparator.comparingDouble(n -> n.startSec));
        return new MidiChart(notes);
    }

    private static int findFirstTempoMpqn(Sequence sequence) {
        for (Track track : sequence.getTracks()) {
            for (int i = 0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                if (event.getMessage() instanceof MetaMessage mm && mm.getType() == META_TEMPO_TYPE) {
                    byte[] d = mm.getData();
                    return ((d[0] & 0xFF) << 16) | ((d[1] & 0xFF) << 8) | (d[2] & 0xFF);
                }
            }
        }
        return DEFAULT_TEMPO_MPQN;
    }
}
