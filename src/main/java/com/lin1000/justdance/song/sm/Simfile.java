package com.lin1000.justdance.song.sm;

import java.util.ArrayList;
import java.util.List;

/**
 * A parsed StepMania .sm simfile: header metadata, a tempo map, and one or more charts.
 * Notes are stored by (lane, beat); {@link Timing#beatToSeconds} converts a beat to the audio
 * time at which it should reach the receptor.
 *
 * Lane order matches both StepMania dance-single and this game's lanes: 0=Left 1=Down 2=Up 3=Right.
 */
public final class Simfile {

    public String title = "", subtitle = "", artist = "", music = "", banner = "", background = "";
    public double offsetSec = 0.0;
    public final Timing timing = new Timing();
    public final List<Chart> charts = new ArrayList<>();

    /** The chart to play: first dance-single chart, else the first chart, else null. */
    public Chart playableChart() {
        for (Chart c : charts) if ("dance-single".equalsIgnoreCase(c.stepsType)) return c;
        return charts.isEmpty() ? null : charts.get(0);
    }

    public static final class Note {
        public final int lane;     // 0=L 1=D 2=U 3=R
        public final double beat;
        public Note(int lane, double beat) { this.lane = lane; this.beat = beat; }
    }

    public static final class Chart {
        public String stepsType = "", difficulty = "";
        public int meter = 0;
        public final List<Note> notes = new ArrayList<>();
    }

    /** Tempo map → seconds. songTime(beat) = offsetSec + integral of 60/BPM over the beat span. */
    public final class Timing {
        private final List<double[]> seg = new ArrayList<>(); // each = {startBeat, bpm}, ascending

        void addBpm(double beat, double bpm) { seg.add(new double[]{beat, bpm}); }
        private void ensureDefault() { if (seg.isEmpty()) seg.add(new double[]{0.0, 120.0}); }

        public double firstBpm() { ensureDefault(); return seg.get(0)[1]; }

        public double beatToSeconds(double beat) {
            ensureDefault();
            double sec = 0.0;
            for (int i = 0; i < seg.size(); i++) {
                double startBeat = seg.get(i)[0];
                double bpm = seg.get(i)[1];
                double endBeat = (i + 1 < seg.size()) ? seg.get(i + 1)[0] : Double.MAX_VALUE;
                if (beat <= startBeat) break;
                double span = Math.min(beat, endBeat) - startBeat;
                if (span > 0) sec += span * 60.0 / bpm;
                if (beat <= endBeat) break;
            }
            return offsetSec + sec;
        }
    }
}
