package com.lin1000.justdance.gamepanel.effect;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.LinearGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A row of choreographed fountain jets, synced to a song's BPM — the standalone "water dance"
 * showcase (attract-mode style, no scoring). Purely a lighting/motion homage to musical-fountain
 * shows in general (parabolic jet arcs, warm/cool uplighting, wave choreography, glow, and
 * reflection): it does not reproduce any studio's specific characters, projected imagery, or
 * show content.
 *
 * Each jet is a tapered, gradient-filled ribbon (not a chain of dots) with sparkle highlights
 * and a landing splash. A soft bloom halo and a faded floor reflection are built from a small
 * offscreen "jet layer" buffer, composited under/around the sharp jets — cheap, dependency-free
 * approximations of glow and wet-floor reflection using only {@link ConvolveOp} and alpha
 * compositing, no shaders or external imaging libraries.
 *
 * Owns its own jet state and is driven by wall-clock-since-demo-start (not the audio clip's
 * playback position — {@code SoundController.playBackgroundSound(music, true)} doesn't populate
 * an audio clock, and this effect is decorative, not judged, so wall-clock is an acceptable and
 * much simpler substitute here).
 */
public class WaterDanceEffect {

    private static final double FLIGHT_SEC = 0.85; // time for a jet's arc from launch to landing
    private static final double FADE_SEC = 0.22;   // splash + lingering fade after landing

    private final int width;
    private final int height;
    private final int groundY;
    private final int jetCount;
    private final double bpm;
    private final Random rng = new Random();

    private final List<Jet> activeJets = new ArrayList<>();
    private int lastBeatIndex = Integer.MIN_VALUE;

    // Offscreen working layer spanning just the jet row (not the full canvas) — everything
    // bright (nozzles, ribbons, sparkles, splashes) is drawn here once per frame, then reused
    // three ways: drawn sharp on top, blurred for a bloom halo, and flipped for the reflection.
    private final int bandTop;
    private final int bandHeight;
    private final BufferedImage jetLayer;

    private static final class Jet {
        final double launchSec;
        final int baseX;
        final double arcHeight;
        final double arcDrift;   // horizontal drift from base to landing point
        final boolean bigBurst;
        final double seed;       // deterministic per-jet variation for sparkle placement

        Jet(double launchSec, int baseX, double arcHeight, double arcDrift, boolean bigBurst, double seed) {
            this.launchSec = launchSec;
            this.baseX = baseX;
            this.arcHeight = arcHeight;
            this.arcDrift = arcDrift;
            this.bigBurst = bigBurst;
            this.seed = seed;
        }
    }

    public WaterDanceEffect(int width, int height, int jetCount, double bpm) {
        this.width = width;
        this.height = height;
        this.groundY = height - 70;
        this.jetCount = Math.max(3, jetCount);
        this.bpm = bpm > 0 ? bpm : 120;

        this.bandTop = Math.max(0, groundY - 320);
        this.bandHeight = Math.min(height, groundY + 40) - bandTop;
        this.jetLayer = new BufferedImage(Math.max(1, width), Math.max(1, bandHeight), BufferedImage.TYPE_INT_ARGB);
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
                activeJets.add(new Jet(nowSec, xForIndex(i), h, drift, true, rng.nextDouble() * 1000));
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
                activeJets.add(new Jet(nowSec, xForIndex(idx), h, drift, false, rng.nextDouble() * 1000));
            }
        }
    }

    public void draw(Graphics2D gc, double nowSec) {
        gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawAmbientWash(gc, nowSec);
        drawMist(gc, nowSec);

        renderJetLayer(nowSec);
        drawBloom(gc);
        drawReflection(gc);
        gc.drawImage(jetLayer, 0, bandTop, null);

        drawVignette(gc);
    }

    // ---- background ----------------------------------------------------------------------

    private void drawAmbientWash(Graphics2D gc, double nowSec) {
        gc.setColor(Color.black);
        gc.fillRect(0, 0, width, height);

        // Slow-breathing blue-purple mist glow behind the jet row, echoing a musical-fountain
        // show's uplit haze — ambience only, no projected imagery.
        float breathe = (float) (0.5 + 0.5 * Math.sin(nowSec * 0.6));
        int alpha = (int) (55 + 40 * breathe);
        RadialGradientPaint glow = new RadialGradientPaint(
                new Point2D.Float(width / 2f, groundY - 100f), width * 0.62f,
                new float[]{0f, 0.6f, 1f},
                new Color[]{new Color(130, 100, 230, alpha), new Color(90, 60, 170, alpha / 2), new Color(90, 60, 170, 0)});
        gc.setPaint(glow);
        gc.fillRect(0, 0, width, height);

        // A dimmer, warmer bounce-light low near the ground — real fountain mist catches the
        // gold jet light too, not just the cool stage wash above it.
        RadialGradientPaint warmBounce = new RadialGradientPaint(
                new Point2D.Float(width / 2f, groundY), width * 0.5f,
                new float[]{0f, 1f},
                new Color[]{new Color(255, 170, 90, 26), new Color(255, 170, 90, 0)});
        gc.setPaint(warmBounce);
        gc.fillRect(0, 0, width, height); // full-canvas fill so the radial falloff itself
                                           // provides the soft edge — a bounded rect here
                                           // would show as a visible seam at its border.
    }

    /** Slow-drifting faint motes for atmosphere — deterministic from index+time, no stored state. */
    private void drawMist(Graphics2D gc, double nowSec) {
        int n = 22;
        for (int i = 0; i < n; i++) {
            double seed = i * 37.13;
            double speed = 6 + 4 * ((seed * 11) % 1.0);
            double x = ((seed * 97 + nowSec * speed) % (width + 60)) - 30;
            double y = height * 0.12 + ((seed * 53) % 1.0) * height * 0.5;
            double phase = nowSec * 0.4 + seed;
            int a = (int) (8 + 10 * (0.5 + 0.5 * Math.sin(phase)));
            double r = 1.4 + 2.2 * ((seed * 13) % 1.0);
            gc.setColor(new Color(190, 200, 255, a));
            gc.fill(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
        }
    }

    private void drawVignette(Graphics2D gc) {
        RadialGradientPaint vignette = new RadialGradientPaint(
                new Point2D.Float(width / 2f, height * 0.5f), Math.max(width, height) * 0.75f,
                new float[]{0.55f, 1f},
                new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 110)});
        gc.setPaint(vignette);
        gc.fillRect(0, 0, width, height);
    }

    // ---- jet layer: nozzles + ribbons + sparkles + splashes, drawn once, reused 3 ways -----

    private void renderJetLayer(double nowSec) {
        Graphics2D lg = jetLayer.createGraphics();
        lg.setComposite(AlphaComposite.Clear);
        lg.fillRect(0, 0, jetLayer.getWidth(), jetLayer.getHeight());
        lg.setComposite(AlphaComposite.SrcOver);
        lg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        lg.translate(0, -bandTop);

        drawNozzleGlow(lg);
        for (Jet j : activeJets) drawJetRibbon(lg, j, nowSec);

        lg.dispose();
    }

    private void drawNozzleGlow(Graphics2D g) {
        for (int i = 0; i < jetCount; i++) {
            int x = xForIndex(i);
            RadialGradientPaint base = new RadialGradientPaint(
                    new Point2D.Float(x, groundY), 20f,
                    new float[]{0f, 0.35f, 1f},
                    new Color[]{new Color(255, 250, 235, 235), new Color(255, 210, 130, 150), new Color(255, 190, 110, 0)});
            g.setPaint(base);
            g.fillOval(x - 20, groundY - 20, 40, 40);
        }
    }

    private void drawJetRibbon(Graphics2D g, Jet j, double nowSec) {
        double t = (nowSec - j.launchSec) / FLIGHT_SEC;
        double drawT = Math.min(1.0, Math.max(0.0, t));
        if (drawT <= 0.01) return;

        float postLandFade = 1f;
        if (t > 1.0) {
            postLandFade = (float) Math.max(0, 1.0 - (t - 1.0) * FLIGHT_SEC / FADE_SEC);
            if (postLandFade <= 0.01f) return;
        }

        int samples = 26;
        int usable = Math.max(2, (int) Math.round(samples * drawT));
        double[] xs = new double[usable + 1];
        double[] ys = new double[usable + 1];
        for (int s = 0; s <= usable; s++) {
            double st = drawT * s / usable;
            xs[s] = j.baseX + j.arcDrift * st;
            ys[s] = groundY - j.arcHeight * 4 * st * (1 - st);
        }

        double baseWidth = j.bigBurst ? 9.5 : 6.5;
        double tipWidth = j.bigBurst ? 3.0 : 1.8;

        // Tapered ribbon polygon: for each sample, offset left/right along the path's normal
        // by the width at that point, then close the loop (left side forward, right side back).
        Path2D.Double poly = new Path2D.Double();
        double[] rxArr = new double[usable + 1], ryArr = new double[usable + 1];
        double[] lxArr = new double[usable + 1], lyArr = new double[usable + 1];
        for (int s = 0; s <= usable; s++) {
            double along = s / (double) usable;
            double w = baseWidth + (tipWidth - baseWidth) * along;
            double dx, dy;
            if (s == 0) { dx = xs[1] - xs[0]; dy = ys[1] - ys[0]; }
            else if (s == usable) { dx = xs[s] - xs[s - 1]; dy = ys[s] - ys[s - 1]; }
            else { dx = xs[s + 1] - xs[s - 1]; dy = ys[s + 1] - ys[s - 1]; }
            double len = Math.hypot(dx, dy);
            double nx = len < 1e-6 ? 1 : -dy / len;
            double ny = len < 1e-6 ? 0 : dx / len;
            lxArr[s] = xs[s] + nx * w / 2; lyArr[s] = ys[s] + ny * w / 2;
            rxArr[s] = xs[s] - nx * w / 2; ryArr[s] = ys[s] - ny * w / 2;
        }
        poly.moveTo(lxArr[0], lyArr[0]);
        for (int s = 1; s <= usable; s++) poly.lineTo(lxArr[s], lyArr[s]);
        for (int s = usable; s >= 0; s--) poly.lineTo(rxArr[s], ryArr[s]);
        poly.closePath();

        Color hot = new Color(255, 250, 235);
        Color gold = new Color(255, 197, 90);
        int baseAlpha = clampAlpha(0.92f * postLandFade);
        int tipAlpha = clampAlpha(0.30f * postLandFade);
        LinearGradientPaint fill = new LinearGradientPaint(
                new Point2D.Double(j.baseX, groundY), new Point2D.Double(xs[usable], ys[usable]),
                new float[]{0f, 0.35f, 1f},
                new Color[]{
                        new Color(hot.getRed(), hot.getGreen(), hot.getBlue(), baseAlpha),
                        new Color(gold.getRed(), gold.getGreen(), gold.getBlue(), clampAlpha(0.75f * postLandFade)),
                        new Color(gold.getRed(), gold.getGreen(), gold.getBlue(), tipAlpha)
                });
        g.setPaint(fill);
        g.fill(poly);

        drawSparkles(g, j, xs, ys, usable, postLandFade);

        if (t > 1.0) {
            drawSplash(g, j, (t - 1.0) * FLIGHT_SEC / FADE_SEC, postLandFade);
        }
    }

    private void drawSparkles(Graphics2D g, Jet j, double[] xs, double[] ys, int usable, float postLandFade) {
        int spots = 6;
        for (int k = 1; k <= spots; k++) {
            double along = k / (double) (spots + 1);
            int idx = (int) Math.round(along * usable);
            if (idx > usable) continue;
            double twinkle = 0.5 + 0.5 * Math.sin(j.seed + along * 9.0);
            float alpha = (float) (0.55 * twinkle) * postLandFade;
            if (alpha <= 0.02f) continue;
            double size = j.bigBurst ? 3.6 : 2.6;
            g.setColor(new Color(255, 255, 245, clampAlpha(alpha)));
            g.fill(new Ellipse2D.Double(xs[idx] - size / 2, ys[idx] - size / 2, size, size));
        }
    }

    private void drawSplash(Graphics2D g, Jet j, double progress, float fade) {
        progress = Math.max(0, Math.min(1, progress));
        double landX = j.baseX + j.arcDrift;
        double radius = 6 + 22 * progress;
        float alpha = (float) (0.55 * (1 - progress)) * fade;
        if (alpha > 0.02f) {
            g.setStroke(new java.awt.BasicStroke(1.6f));
            g.setColor(new Color(255, 235, 200, clampAlpha(alpha)));
            g.draw(new Ellipse2D.Double(landX - radius, groundY - radius * 0.35, radius * 2, radius * 0.7));
        }
        int droplets = 5;
        for (int d = 0; d < droplets; d++) {
            double ang = Math.PI * (0.15 + 0.7 * d / (double) (droplets - 1));
            double dist = 4 + 16 * progress;
            double dx = Math.cos(ang) * dist;
            double dy = -Math.sin(ang) * dist * 0.6;
            float da = (float) (0.5 * (1 - progress)) * fade;
            if (da <= 0.02f) continue;
            g.setColor(new Color(255, 230, 180, clampAlpha(da)));
            g.fill(new Ellipse2D.Double(landX + dx - 1.3, groundY + dy - 1.3, 2.6, 2.6));
        }
    }

    // ---- bloom + reflection, built from the shared jetLayer --------------------------------

    private void drawBloom(Graphics2D gc) {
        int sw = Math.max(1, jetLayer.getWidth() / 3);
        int sh = Math.max(1, jetLayer.getHeight() / 3);
        BufferedImage small = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D sg = small.createGraphics();
        sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        sg.drawImage(jetLayer, 0, 0, sw, sh, null);
        sg.dispose();

        BufferedImage blurred = blur(blur(small));

        Graphics2D g2 = (Graphics2D) gc.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f));
        g2.drawImage(blurred, 0, bandTop, jetLayer.getWidth(), jetLayer.getHeight(), null);
        g2.dispose();
    }

    private static BufferedImage blur(BufferedImage src) {
        float[] k = {
                1 / 16f, 2 / 16f, 1 / 16f,
                2 / 16f, 4 / 16f, 2 / 16f,
                1 / 16f, 2 / 16f, 1 / 16f
        };
        ConvolveOp op = new ConvolveOp(new Kernel(3, 3, k), ConvolveOp.EDGE_NO_OP, null);
        return op.filter(src, null);
    }

    private void drawReflection(Graphics2D gc) {
        // Squashed (not 1:1) mirror about the ground line — a real wet-floor reflection reads
        // as compressed, not a full-height duplicate of the jet arcs.
        double squash = 0.42;
        Graphics2D g2 = (Graphics2D) gc.create();
        java.awt.geom.AffineTransform xf = new java.awt.geom.AffineTransform();
        xf.translate(0, groundY);
        xf.scale(1, -squash);
        xf.translate(0, -groundY);
        g2.transform(xf);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.32f));
        g2.drawImage(jetLayer, 0, bandTop, null);
        g2.dispose();

        // Fade the reflection into the black floor rather than leaving a hard mirror edge.
        GradientPaint fade = new GradientPaint(
                0, groundY, new Color(0, 0, 0, 0),
                0, groundY + 70, new Color(0, 0, 0, 255));
        Graphics2D g3 = (Graphics2D) gc.create();
        g3.setPaint(fade);
        g3.fillRect(0, groundY, width, 70);
        g3.dispose();
    }

    private static int clampAlpha(float a) {
        return Math.max(0, Math.min(255, (int) (a * 255)));
    }
}
