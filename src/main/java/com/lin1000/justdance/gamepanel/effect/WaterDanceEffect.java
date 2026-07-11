package com.lin1000.justdance.gamepanel.effect;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A row of choreographed fountain jets, synced to a song's BPM — the standalone "water dance"
 * showcase (attract-mode style, no scoring). Purely a lighting/motion homage to musical-fountain
 * shows in general (parabolic jet arcs, warm/cool uplighting, wave choreography): it does not
 * reproduce any studio's specific characters, projected imagery, or show content.
 *
 * Owns its own jet state and is driven by wall-clock-since-demo-start (not the audio clip's
 * playback position — {@code SoundController.playBackgroundSound(music, true)} doesn't populate
 * an audio clock, and this effect is decorative, not judged, so wall-clock is an acceptable and
 * much simpler substitute here).
 */
public class WaterDanceEffect {

    private static final double FLIGHT_SEC = 0.85; // time for a jet's arc from launch to landing
    private static final double FADE_SEC = 0.18;   // extra lingering fade after landing

    private final int width;
    private final int height;
    private final int groundY;
    private final int jetCount;
    private final double bpm;
    private final Random rng = new Random();

    private final List<Jet> activeJets = new ArrayList<>();
    private int lastBeatIndex = Integer.MIN_VALUE;

    private static final class Jet {
        final double launchSec;
        final int baseX;
        final double arcHeight;
        final double arcDrift;   // horizontal drift from base to landing point
        final boolean bigBurst;

        Jet(double launchSec, int baseX, double arcHeight, double arcDrift, boolean bigBurst) {
            this.launchSec = launchSec;
            this.baseX = baseX;
            this.arcHeight = arcHeight;
            this.arcDrift = arcDrift;
            this.bigBurst = bigBurst;
        }
    }

    public WaterDanceEffect(int width, int height, int jetCount, double bpm) {
        this.width = width;
        this.height = height;
        this.groundY = height - 70;
        this.jetCount = Math.max(3, jetCount);
        this.bpm = bpm > 0 ? bpm : 120;
    }

    private int xForIndex(int i) {
        int margin = width / 10;
        if (jetCount == 1) return width / 2;
        return margin + i * (width - 2 * margin) / (jetCount - 1);
    }

    /** Advances the show: fires new jets on beat boundaries, expires landed-and-faded ones. */
    public void tick(double nowSec) {
        double beatSec = 60.0 / bpm;
        int beatIndex = (int) Math.floor(nowSec / beatSec);
        if (beatIndex != lastBeatIndex && nowSec >= 0) {
            lastBeatIndex = beatIndex;
            fireWave(nowSec, beatIndex);
        }
        activeJets.removeIf(j -> nowSec - j.launchSec > FLIGHT_SEC + FADE_SEC);
    }

    // Each nozzle's arc leans toward the row's center, so left-half jets sweep right and
    // right-half jets sweep left — a converging fan, echoing the reference photo's crossing
    // arcs, rather than every jet drifting in an independent random direction.
    private double leanFor(int index) {
        double center = (jetCount - 1) / 2.0;
        return index < center ? 1.0 : (index > center ? -1.0 : 0.0);
    }

    private void fireWave(double nowSec, int beatIndex) {
        boolean measureBurst = beatIndex % 4 == 0;
        if (measureBurst) {
            // Downbeat: every nozzle fires together — the big synchronized "hit."
            for (int i = 0; i < jetCount; i++) {
                double h = 170 + rng.nextDouble() * 90;
                double drift = leanFor(i) * (110 + rng.nextDouble() * 70);
                activeJets.add(new Jet(nowSec, xForIndex(i), h, drift, true));
            }
        } else {
            // Off-beat: a travelling wave — a small window of nozzles centered on a position
            // that sweeps across the row beat by beat.
            int center = Math.floorMod(beatIndex, jetCount);
            int spread = Math.max(1, jetCount / 6);
            for (int d = -spread; d <= spread; d++) {
                int idx = Math.floorMod(center + d, jetCount);
                double falloff = 1.0 - Math.abs(d) / (double) (spread + 1);
                double h = 90 + falloff * 90 + rng.nextDouble() * 30;
                double drift = leanFor(idx) * (70 + rng.nextDouble() * 60);
                activeJets.add(new Jet(nowSec, xForIndex(idx), h, drift, false));
            }
        }
    }

    public void draw(Graphics2D gc, double nowSec) {
        drawAmbientWash(gc, nowSec);
        for (Jet j : activeJets) drawJet(gc, j, nowSec);
        drawNozzleLine(gc);
    }

    private void drawAmbientWash(Graphics2D gc, double nowSec) {
        // Slow-breathing blue-purple mist glow behind the jet row, echoing a musical-fountain
        // show's uplit haze — ambience only, no projected imagery.
        float breathe = (float) (0.5 + 0.5 * Math.sin(nowSec * 0.6));
        int alpha = (int) (50 + 40 * breathe);
        RadialGradientPaint glow = new RadialGradientPaint(
                new Point2D.Float(width / 2f, groundY - 120f), width * 0.55f,
                new float[]{0f, 1f},
                new Color[]{new Color(120, 90, 220, alpha), new Color(120, 90, 220, 0)});
        gc.setPaint(glow);
        gc.fillRect(0, 0, width, height);
    }

    private void drawNozzleLine(Graphics2D gc) {
        for (int i = 0; i < jetCount; i++) {
            int x = xForIndex(i);
            RadialGradientPaint base = new RadialGradientPaint(
                    new Point2D.Float(x, groundY), 14f,
                    new float[]{0f, 1f},
                    new Color[]{new Color(255, 220, 150, 160), new Color(255, 220, 150, 0)});
            gc.setPaint(base);
            gc.fillOval(x - 14, groundY - 14, 28, 28);
        }
    }

    private void drawJet(Graphics2D gc, Jet j, double nowSec) {
        double t = (nowSec - j.launchSec) / FLIGHT_SEC;
        double drawT = Math.min(1.0, Math.max(0.0, t));
        float postLandFade = 1f;
        if (t > 1.0) {
            postLandFade = (float) Math.max(0, 1.0 - (t - 1.0) * FLIGHT_SEC / FADE_SEC);
        }

        // Consistently warm gold, matching the reference photo's jets — the blue/purple only
        // ever appears in the ambient background wash (drawAmbientWash), never on the water.
        Color near = new Color(255, 207, 92);
        Color far = new Color(255, 243, 196);

        int samples = 16;
        for (int s = 0; s <= samples; s++) {
            double st = drawT * s / samples;
            if (st > drawT) break;
            double x = j.baseX + j.arcDrift * st;
            double y = groundY - j.arcHeight * 4 * st * (1 - st);
            double along = s / (double) samples; // 0 at nozzle, 1 near current tip
            float size = (float) (j.bigBurst ? 8 - 4 * along : 6 - 3 * along);
            float alpha = (float) (0.85 * (1 - 0.5 * along)) * postLandFade;
            if (alpha <= 0.02f || size <= 0.5f) continue;
            Color c = lerp(near, far, (float) along);
            gc.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), clampAlpha(alpha)));
            gc.fill(new Ellipse2D.Double(x - size / 2, y - size / 2, size, size));
        }
    }

    private static Color lerp(Color a, Color b, float f) {
        f = Math.max(0, Math.min(1, f));
        return new Color(
                (int) (a.getRed() + (b.getRed() - a.getRed()) * f),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * f),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * f));
    }

    private static int clampAlpha(float a) {
        return Math.max(0, Math.min(255, (int) (a * 255)));
    }
}
