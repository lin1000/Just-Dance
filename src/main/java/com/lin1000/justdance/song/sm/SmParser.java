package com.lin1000.justdance.song.sm;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal StepMania .sm parser (MSD tags). Handles the header tags this game needs plus
 * dance-single #NOTES. #STOPS and hold/roll tails are not modelled (hold/roll heads count as
 * taps); mines are ignored. Read as UTF-8 so CJK titles survive.
 */
public final class SmParser {

    private SmParser() {}

    public static Simfile parse(File file) {
        Simfile sm = new Simfile();
        String text;
        try {
            text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("SmParser: cannot read " + file + ": " + e);
            return sm;
        }
        text = text.replaceAll("//[^\\r\\n]*", ""); // strip // line comments

        // Each statement is #TAG:value; — the first ':' ends the tag, the next ';' ends the value.
        int i = 0;
        while ((i = text.indexOf('#', i)) >= 0) {
            int colon = text.indexOf(':', i);
            int semi = text.indexOf(';', i);
            if (colon < 0 || semi < 0 || colon > semi) { i++; continue; }
            String tag = text.substring(i + 1, colon).trim().toUpperCase();
            String value = text.substring(colon + 1, semi);
            handle(sm, tag, value);
            i = semi + 1;
        }
        return sm;
    }

    private static void handle(Simfile sm, String tag, String value) {
        switch (tag) {
            case "TITLE":      sm.title = value.trim(); break;
            case "SUBTITLE":   sm.subtitle = value.trim(); break;
            case "ARTIST":     sm.artist = value.trim(); break;
            case "MUSIC":      sm.music = value.trim(); break;
            case "BANNER":     sm.banner = value.trim(); break;
            case "BACKGROUND": sm.background = value.trim(); break;
            case "OFFSET":     sm.offsetSec = parseD(value, 0.0); break;
            case "BPMS":       parseBpms(sm, value); break;
            case "NOTES":      parseNotes(sm, value); break;
            default: /* ignore CDTITLE, SAMPLESTART, BGCHANGES, STOPS, etc. */
        }
    }

    private static double parseD(String s, double def) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return def; }
    }

    private static void parseBpms(Simfile sm, String value) {
        for (String pair : value.split(",")) {
            String[] kv = pair.split("=");
            if (kv.length == 2) {
                double bpm = parseD(kv[1], 0);
                if (bpm > 0) sm.timing.addBpm(parseD(kv[0], 0), bpm);
            }
        }
    }

    private static void parseNotes(Simfile sm, String value) {
        // NOTES value = stepsType : description : difficulty : meter : radar : notedata
        String[] p = value.split(":", 6);
        if (p.length < 6) return;
        Simfile.Chart c = new Simfile.Chart();
        c.stepsType = p[0].trim();
        c.difficulty = p[2].trim();
        try { c.meter = Integer.parseInt(p[3].trim()); } catch (Exception e) { c.meter = 0; }

        // Rows are scanned in chart order, so an open hold/roll head per lane is simply the
        // most recent unclosed one; the next `3` in that lane is its tail.
        Simfile.Note[] openHold = new Simfile.Note[4];

        String[] measures = p[5].split(",");
        for (int m = 0; m < measures.length; m++) {
            List<String> rows = new ArrayList<>();
            for (String line : measures[m].split("\\r?\\n")) {
                String t = line.trim();
                if (t.length() >= 4) rows.add(t); // a dance-single row is 4 columns
            }
            int R = rows.size();
            for (int r = 0; r < R; r++) {
                String row = rows.get(r);
                double beat = (m + (double) r / R) * 4.0; // 4 beats per measure
                for (int lane = 0; lane < 4 && lane < row.length(); lane++) {
                    char ch = row.charAt(lane);
                    if (ch == '1') {                       // tap
                        c.notes.add(new Simfile.Note(lane, beat));
                    } else if (ch == '2' || ch == '4') {   // hold head / roll head
                        Simfile.Note n = new Simfile.Note(lane, beat);
                        c.notes.add(n);
                        openHold[lane] = n;
                    } else if (ch == '3') {                // hold/roll tail
                        if (openHold[lane] != null) {
                            openHold[lane].endBeat = beat;
                            openHold[lane] = null;
                        }
                    }
                }
            }
        }
        sm.charts.add(c);
    }
}
