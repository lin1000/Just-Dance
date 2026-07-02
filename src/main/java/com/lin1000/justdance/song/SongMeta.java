package com.lin1000.justdance.song;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Authored, immutable definition of a song — the single source of truth for its title,
 * artist, asset paths, tempo and per-difficulty foot ratings. Loaded by {@link SongLibrary}
 * from the .sm files under songs/.
 *
 * This is distinct from {@link Song}, which is the *runtime* result of analysing the audio
 * (beats, signal strengths, etc.). SongMeta is what the author declares; Song is what the
 * engine computes.
 */
public final class SongMeta {

    private final String id;
    private final String title;
    private final String artist;
    private final String audioPath;
    private final String jacketPath;
    private final String chartPath;
    private final int bpm;
    private final int offsetMs;
    private final Map<String, Integer> ratings; // difficulty label ("EASY"...) -> foot rating

    public SongMeta(String id, String title, String artist, String audioPath,
                    String jacketPath, String chartPath, int bpm, int offsetMs,
                    Map<String, Integer> ratings) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.audioPath = audioPath;
        this.jacketPath = jacketPath;
        this.chartPath = chartPath;
        this.bpm = bpm;
        this.offsetMs = offsetMs;
        this.ratings = (ratings == null) ? Collections.emptyMap()
                                         : Collections.unmodifiableMap(new HashMap<>(ratings));
    }

    public String getId()         { return id; }
    public String getTitle()      { return title; }
    public String getArtist()     { return artist; }
    public String getAudioPath()  { return audioPath; }
    public String getJacketPath() { return jacketPath; }
    public String getChartPath()  { return chartPath; }
    public int    getBpm()        { return bpm; }
    public int    getOffsetMs()   { return offsetMs; }

    /** Foot rating for a difficulty label (e.g. "NORMAL"); 0 if none authored. */
    public int rating(String difficultyLabel) {
        Integer r = ratings.get(difficultyLabel);
        return (r == null) ? 0 : r;
    }

    @Override public String toString() {
        return "SongMeta{" + id + " '" + title + "' bpm=" + bpm + " audio=" + audioPath + "}";
    }
}
