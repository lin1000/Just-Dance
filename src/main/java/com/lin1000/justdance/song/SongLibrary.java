package com.lin1000.justdance.song;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The song catalog — a single, stable source of truth for every song's metadata.
 *
 * Loaded once from songs/songs.json (read as UTF-8 so CJK titles are safe on any platform).
 * If the file is missing or malformed, a built-in default catalog is used so the game always
 * has songs. Every component (menu, sound, chart producer, gameplay) reads song bindings from
 * here instead of hardcoding them or relying on filesystem ordering.
 */
public final class SongLibrary {

    private static final String PATH = "songs/songs.json";
    private static final List<SongMeta> SONGS = load();

    private SongLibrary() {}

    public static List<SongMeta> all() { return SONGS; }
    public static int size()           { return SONGS.size(); }

    /** Song at an index, wrapping so callers never go out of bounds. */
    public static SongMeta get(int index) {
        return SONGS.get(Math.floorMod(index, SONGS.size()));
    }

    private static List<SongMeta> load() {
        File f = new File(PATH);
        if (f.exists()) {
            try (InputStreamReader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
                JsonValue root = new JsonReader().parse(r);
                List<SongMeta> list = new ArrayList<>();
                for (JsonValue s = root.child; s != null; s = s.next) {
                    Map<String, Integer> ratings = new HashMap<>();
                    JsonValue rv = s.get("ratings");
                    if (rv != null) {
                        for (JsonValue e = rv.child; e != null; e = e.next) ratings.put(e.name, e.asInt());
                    }
                    list.add(new SongMeta(
                            s.getString("id", ""),
                            s.getString("title", ""),
                            s.getString("artist", ""),
                            s.getString("audio", ""),
                            s.getString("jacket", ""),
                            s.getString("chart", "foot/foot.txt"),
                            s.getInt("bpm", 120),
                            s.getInt("offsetMs", 0),
                            ratings));
                }
                if (!list.isEmpty()) {
                    System.out.println("SongLibrary: loaded " + list.size() + " songs from " + PATH);
                    return list;
                }
                System.err.println("SongLibrary: " + PATH + " had no songs — using built-in defaults");
            } catch (Exception e) {
                System.err.println("SongLibrary: failed to read " + PATH + " — using built-in defaults: " + e);
            }
        } else {
            System.out.println("SongLibrary: " + PATH + " not found — using built-in defaults");
        }
        return defaults();
    }

    /** Built-in catalog, matching songs.json, used when the data file is absent/invalid. */
    private static List<SongMeta> defaults() {
        List<SongMeta> l = new ArrayList<>();
        l.add(new SongMeta("tianmimi", "甜蜜蜜（舞曲版）", "Teresa Teng · Dance Remix",
                "sound/musicbox/music4.wav", "img/jacket1.png", "foot/foot.txt", 400, 0,
                ratingMap(2, 4, 6, 9)));
        l.add(new SongMeta("barbiegirl", "BARBIE GIRL — AQUA", "Aqua",
                "sound/musicbox/music3.wav", "img/jacket2.png", "foot/foot.txt", 120, 0,
                ratingMap(1, 3, 5, 7)));
        l.add(new SongMeta("happyboys", "Barbie Happy Boys", "Party Mix",
                "sound/musicbox/music1.wav", "img/jacket3.png", "foot/foot.txt", 180, 0,
                ratingMap(2, 4, 6, 8)));
        l.add(new SongMeta("devilghost", "DEVIL + GHOST", "Hardcore",
                "sound/musicbox/music2.wav", "img/jacket4.png", "foot/foot.txt", 300, 0,
                ratingMap(3, 5, 8, 10)));
        return l;
    }

    private static Map<String, Integer> ratingMap(int easy, int normal, int hard, int expert) {
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put("EASY", easy); m.put("NORMAL", normal); m.put("HARD", hard); m.put("EXPERT", expert);
        return m;
    }
}
