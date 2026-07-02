package com.lin1000.justdance.song;

import com.lin1000.justdance.song.sm.Simfile;
import com.lin1000.justdance.song.sm.SmParser;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The song catalog, built by scanning StepMania-style song folders under songs/ and reading
 * each folder's .sm header. This is the single source of truth: title/artist/audio/banner/bpm
 * all come from the .sm — adding a song means dropping a folder in, no code or index file.
 *
 * Audio must be WAV (the engine's audio loop decodes WAV); a .sm whose #MUSIC points at a .wav
 * is fully StepMania-standard.
 */
public final class SongLibrary {

    private static final String SONGS_DIR = "songs";

    /** One catalog entry: the authored metadata plus the fully-parsed simfile (notes + timing). */
    public static final class Entry {
        public final SongMeta meta;
        public final Simfile simfile;
        Entry(SongMeta meta, Simfile simfile) { this.meta = meta; this.simfile = simfile; }
    }

    private static final List<Entry> ENTRIES = scan();
    private static final List<SongMeta> METAS = new ArrayList<>();
    static { for (Entry e : ENTRIES) METAS.add(e.meta); }

    private SongLibrary() {}

    public static List<SongMeta> all() { return METAS; }
    public static int size()           { return METAS.size(); }

    public static SongMeta get(int index) {
        if (ENTRIES.isEmpty()) throw new IllegalStateException("No songs found under ./" + SONGS_DIR);
        return METAS.get(Math.floorMod(index, METAS.size()));
    }

    /** The parsed simfile (charts + timing) for a song — used by gameplay to build the chart. */
    public static Simfile simfileFor(int index) {
        if (ENTRIES.isEmpty()) throw new IllegalStateException("No songs found under ./" + SONGS_DIR);
        return ENTRIES.get(Math.floorMod(index, ENTRIES.size())).simfile;
    }

    private static List<Entry> scan() {
        List<Entry> list = new ArrayList<>();
        File dir = new File(SONGS_DIR);
        File[] folders = dir.listFiles(File::isDirectory);
        if (folders == null) {
            System.err.println("SongLibrary: no ./" + SONGS_DIR + " directory — no songs.");
            return list;
        }
        Arrays.sort(folders, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        for (File folder : folders) {
            File sm = firstSm(folder);
            if (sm == null) continue;
            Simfile s = SmParser.parse(sm);

            String folderPath = folder.getPath();
            String audio  = resolve(folderPath, s.music);
            String jacket = resolve(folderPath, s.banner);

            Map<String, Integer> ratings = new LinkedHashMap<>();
            for (Simfile.Chart c : s.charts) if (c.meter > 0) ratings.put(c.difficulty.toUpperCase(), c.meter);

            SongMeta meta = new SongMeta(
                    folder.getName(),
                    s.title.isEmpty() ? folder.getName() : s.title,
                    s.artist,
                    audio,
                    jacket,
                    sm.getPath(),
                    (int) Math.round(s.timing.firstBpm()),
                    (int) Math.round(s.offsetSec * 1000),
                    ratings);
            list.add(new Entry(meta, s));
            System.out.println("SongLibrary: " + meta + " chart-notes="
                    + (s.playableChart() == null ? 0 : s.playableChart().notes.size()));
        }
        if (list.isEmpty()) System.err.println("SongLibrary: ./" + SONGS_DIR + " has no valid .sm songs.");
        else System.out.println("SongLibrary: loaded " + list.size() + " songs from ./" + SONGS_DIR);
        return list;
    }

    private static File firstSm(File folder) {
        File[] sms = folder.listFiles((d, n) -> n.toLowerCase().endsWith(".sm"));
        return (sms == null || sms.length == 0) ? null : sms[0];
    }

    /** Resolve a #MUSIC/#BANNER path (relative to the song folder) to a repo-root-relative path. */
    private static String resolve(String folderPath, String ref) {
        if (ref == null || ref.isEmpty()) return "";
        return Paths.get(folderPath, ref).normalize().toString();
    }
}
