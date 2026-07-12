package com.lin1000.justdance.gamepanel.effect;

import java.awt.Color;
import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * A second, independent water-dance showcase — a true per-droplet particle simulation rather
 * than {@link WaterDanceEffect}'s parametric ribbon arcs. Each droplet is its own physically
 * integrated particle (gravity, launch-velocity spray cone, individual lifetime), rendered with
 * real additive blending (via a hand-rolled {@link Composite}) so overlapping bright droplets
 * genuinely brighten and bloom toward white — the closest a pure-Java2D, no-GPU-shader pipeline
 * can get to an HDR-style particle engine. Deliberately kept in its own file/class/entry point
 * so it never touches {@link WaterDanceEffect}: a separate homage, same "no reproduced show
 * content" rule.
 */
public class WaterDanceParticleEffect {

    private static final double GRAVITY = 780.0;      // px/s^2
    private static final int MAX_PARTICLES = 2600;

    private final int width, height, groundY, jetCount;
    private final double bpm;
    private final Random rng = new Random();
    private int lastBeatIndex = Integer.MIN_VALUE;
    private double lastTickSec = -1;

    private final List<Particle> particles = new ArrayList<>(MAX_PARTICLES);

    // Offscreen accumulation layer spanning the jet band; particles are splatted here with
    // additive blending, then blurred for bloom and drawn back sharp on top.
    private final int bandTop;
    private final int bandHeight;
    private final BufferedImage jetLayer;

    private static final class Particle {
        double x, y, vx, vy;
        double prevX, prevY; // last frame's position, for a motion-blur trail segment
        double age;      // seconds since spawn
        double lifeSec;  // total lifetime
        double size;
        boolean splash;

        boolean alive(double nowAge) { return nowAge < lifeSec; }
    }

    public WaterDanceParticleEffect(int width, int height, int jetCount, double bpm) {
        this.width = width;
        this.height = height;
        this.groundY = height - 70;
        this.jetCount = Math.max(3, jetCount);
        this.bpm = bpm > 0 ? bpm : 120;

        this.bandTop = Math.max(0, groundY - 360);
        this.bandHeight = Math.min(height, groundY + 50) - bandTop;
        this.jetLayer = new BufferedImage(Math.max(1, width), Math.max(1, bandHeight), BufferedImage.TYPE_INT_ARGB);
    }

    private int xForIndex(int i) {
        int margin = width / 10;
        if (jetCount == 1) return width / 2;
        return margin + i * (width - 2 * margin) / (jetCount - 1);
    }

    private double leanFor(int index) {
        double center = (jetCount - 1) / 2.0;
        return index < center ? 1.0 : (index > center ? -1.0 : 0.0);
    }

    /** Advances physics for every live particle and fires new bursts on beat boundaries. */
    public void tick(double nowSec) {
        double dt = lastTickSec < 0 ? 0 : nowSec - lastTickSec;
        lastTickSec = nowSec;
        if (dt <= 0 || dt > 0.25) dt = 0; // clock jump guard, mirrors the rest of this codebase

        double beatSec = 60.0 / bpm;
        int beatIndex = (int) Math.floor(nowSec / beatSec);
        if (beatIndex != lastBeatIndex && nowSec >= 0) {
            lastBeatIndex = beatIndex;
            fireBurst(beatIndex);
        }

        List<Particle> splashSpawns = null;
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.age += dt;
            p.prevX = p.x;
            p.prevY = p.y;
            p.vy += GRAVITY * dt;
            p.x += p.vx * dt;
            p.y += p.vy * dt;

            if (!p.splash && p.y >= groundY) {
                p.y = groundY;
                if (splashSpawns == null) splashSpawns = new ArrayList<>();
                splashSpawns.addAll(spawnSplash(p));
                it.remove();
                continue;
            }
            if (!p.alive(p.age)) it.remove();
        }
        if (splashSpawns != null) particles.addAll(splashSpawns);

        // Hard cap: trim oldest first so a runaway burst rate can't grow this unbounded.
        while (particles.size() > MAX_PARTICLES) particles.remove(0);
    }

    private List<Particle> spawnSplash(Particle landed) {
        List<Particle> out = new ArrayList<>(4);
        int n = 3 + rng.nextInt(3);
        for (int i = 0; i < n; i++) {
            Particle s = new Particle();
            s.x = landed.x;
            s.y = groundY - 1;
            s.prevX = s.x;
            s.prevY = s.y;
            double ang = Math.PI * (0.15 + 0.7 * rng.nextDouble());
            double speed = 40 + rng.nextDouble() * 70;
            s.vx = Math.cos(ang) * speed * (rng.nextBoolean() ? 1 : -1);
            s.vy = -Math.sin(ang) * speed * 0.6;
            s.age = 0;
            s.lifeSec = 0.18 + rng.nextDouble() * 0.14;
            s.size = 1.6 + rng.nextDouble() * 1.4;
            s.splash = true;
            out.add(s);
        }
        return out;
    }

    private void fireBurst(int beatIndex) {
        boolean measureBurst = beatIndex % 4 == 0;
        if (measureBurst) {
            for (int i = 0; i < jetCount; i++) emitFan(xForIndex(i), leanFor(i), 5, 11, 560, 760, 0.95, 1.3);
        } else {
            int center = Math.floorMod(beatIndex, jetCount);
            int spread = Math.max(1, jetCount / 6);
            for (int d = -spread; d <= spread; d++) {
                int idx = Math.floorMod(center + d, jetCount);
                double falloff = 1.0 - Math.abs(d) / (double) (spread + 1);
                emitFan(xForIndex(idx), leanFor(idx), 3, (int) (5 + falloff * 5),
                        340 + falloff * 220, 480 + falloff * 200, 0.55, 0.9);
            }
        }
    }

    /**
     * A nozzle firing several tight "sub-jet" streams at a fixed fan of angles (like a real
     * multi-orifice fountain nozzle head) — each sub-jet gets many particles along nearly the
     * same trajectory (tight jitter, not wide random scatter), so it traces a crisp, dense arc.
     * The whole fan together reads as a fountain jet, not a diffuse dust cloud.
     */
    private void emitFan(int baseX, double lean, int subJets, int particlesPerSubJet,
                          double minSpeed, double maxSpeed, double minLife, double maxLife) {
        double fanSpreadDeg = 24;
        double centerAngleDeg = 15 * lean;
        for (int j = 0; j < subJets; j++) {
            double t = subJets == 1 ? 0.5 : j / (double) (subJets - 1);
            double subAngleDeg = centerAngleDeg + (t - 0.5) * fanSpreadDeg;
            double subAngle = Math.toRadians(subAngleDeg);
            double subSpeed = minSpeed + rng.nextDouble() * (maxSpeed - minSpeed);
            for (int i = 0; i < particlesPerSubJet; i++) {
                Particle p = new Particle();
                p.x = baseX;
                p.y = groundY;
                p.prevX = p.x;
                p.prevY = p.y;
                double angleJitter = Math.toRadians((rng.nextDouble() - 0.5) * 3.0);
                double speedJitter = subSpeed * (0.92 + rng.nextDouble() * 0.16);
                double ang = subAngle + angleJitter;
                p.vx = Math.sin(ang) * speedJitter;
                p.vy = -Math.cos(ang) * speedJitter;
                p.age = 0;
                p.lifeSec = minLife + rng.nextDouble() * (maxLife - minLife);
                p.size = 2.0 + rng.nextDouble() * 1.8;
                p.splash = false;
                particles.add(p);
            }
        }
    }

    public void draw(Graphics2D gc, double nowSec) {
        gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawBackground(gc, nowSec);
        renderParticleLayer();
        drawBloom(gc);
        drawReflection(gc);
        gc.drawImage(jetLayer, 0, bandTop, null);
        drawVignette(gc);
    }

    // ---- background --------------------------------------------------------------------

    private void drawBackground(Graphics2D gc, double nowSec) {
        gc.setColor(Color.black);
        gc.fillRect(0, 0, width, height);

        float breathe = (float) (0.5 + 0.5 * Math.sin(nowSec * 0.55));
        int alpha = (int) (50 + 35 * breathe);
        RadialGradientPaint cool = new RadialGradientPaint(
                new Point2D.Float(width / 2f, groundY - 110f), width * 0.6f,
                new float[]{0f, 0.6f, 1f},
                new Color[]{new Color(70, 90, 220, alpha), new Color(60, 50, 150, alpha / 2), new Color(60, 50, 150, 0)});
        gc.setPaint(cool);
        gc.fillRect(0, 0, width, height);

        RadialGradientPaint warm = new RadialGradientPaint(
                new Point2D.Float(width / 2f, groundY), width * 0.5f,
                new float[]{0f, 1f},
                new Color[]{new Color(255, 150, 70, 24), new Color(255, 150, 70, 0)});
        gc.setPaint(warm);
        gc.fillRect(0, 0, width, height);

        int n = 22;
        for (int i = 0; i < n; i++) {
            double seed = i * 41.7;
            double speed = 5 + 4 * ((seed * 11) % 1.0);
            double x = ((seed * 97 + nowSec * speed) % (width + 60)) - 30;
            double y = height * 0.12 + ((seed * 53) % 1.0) * height * 0.5;
            int a = (int) (8 + 10 * (0.5 + 0.5 * Math.sin(nowSec * 0.4 + seed)));
            double r = 1.4 + 2.0 * ((seed * 13) % 1.0);
            gc.setColor(new Color(200, 210, 255, a));
            gc.fill(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
        }
    }

    private void drawVignette(Graphics2D gc) {
        RadialGradientPaint vignette = new RadialGradientPaint(
                new Point2D.Float(width / 2f, height * 0.5f), Math.max(width, height) * 0.75f,
                new float[]{0.55f, 1f},
                new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 115)});
        gc.setPaint(vignette);
        gc.fillRect(0, 0, width, height);
    }

    // ---- particle layer: nozzles + additive droplets ---------------------------------------

    private void renderParticleLayer() {
        Graphics2D lg = jetLayer.createGraphics();
        lg.setComposite(java.awt.AlphaComposite.Clear);
        lg.fillRect(0, 0, jetLayer.getWidth(), jetLayer.getHeight());
        lg.setComposite(java.awt.AlphaComposite.SrcOver);
        lg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        lg.translate(0, -bandTop);

        drawNozzleGlow(lg);

        // Real additive blending: overlapping bright droplets brighten toward white instead of
        // just alpha-compositing over each other — this is what gives a particle-dense burst its
        // "glowing, blown-out core" look, the visual signature of HDR-style particle engines.
        lg.setComposite(AdditiveComposite.INSTANCE);
        for (Particle p : particles) drawParticle(lg, p);
        lg.setComposite(java.awt.AlphaComposite.SrcOver);

        lg.dispose();
    }

    private void drawNozzleGlow(Graphics2D g) {
        for (int i = 0; i < jetCount; i++) {
            int x = xForIndex(i);
            RadialGradientPaint base = new RadialGradientPaint(
                    new Point2D.Float(x, groundY), 18f,
                    new float[]{0f, 0.35f, 1f},
                    new Color[]{new Color(255, 250, 235, 220), new Color(255, 205, 120, 130), new Color(255, 185, 100, 0)});
            g.setPaint(base);
            g.fillOval(x - 18, groundY - 18, 36, 36);
        }
    }

    private void drawParticle(Graphics2D g, Particle p) {
        double frac = p.age / p.lifeSec;
        if (frac >= 1.0) return;
        // Blackbody-ish cooling: young droplets are hot white-blue, aging toward gold, fading
        // toward dim ember red just before death — driven purely by particle age, no per-jet
        // color bookkeeping needed.
        Color c;
        float coreAlpha;
        if (p.splash) {
            c = lerp(new Color(255, 235, 200), new Color(255, 150, 70), (float) frac);
            coreAlpha = (float) (0.8 * (1 - frac));
        } else if (frac < 0.25) {
            c = lerp(new Color(255, 255, 250), new Color(255, 214, 140), (float) (frac / 0.25));
            coreAlpha = 0.95f;
        } else {
            c = lerp(new Color(255, 214, 140), new Color(255, 120, 60), (float) ((frac - 0.25) / 0.75));
            coreAlpha = (float) Math.max(0.05, 0.95 * (1 - (frac - 0.25) / 0.75));
        }
        double size = p.size * (p.splash ? 1.0 : (1.0 - 0.25 * frac));
        int a = clampAlpha(coreAlpha);

        // Motion-blur trail: a short fading streak from last frame's position to this one,
        // scaled to the particle's own speed — this is what makes a spray of individual dots
        // read as flowing water instead of scattered static specks.
        double dx = p.x - p.prevX, dy = p.y - p.prevY;
        if (!p.splash && (dx * dx + dy * dy) > 1.0) {
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), clampAlpha(coreAlpha * 0.55f)));
            g.setStroke(new java.awt.BasicStroke((float) Math.max(1.0, size * 0.7),
                    java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
            g.draw(new java.awt.geom.Line2D.Double(p.prevX, p.prevY, p.x, p.y));
        }

        g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), a));
        g.fill(new Ellipse2D.Double(p.x - size / 2, p.y - size / 2, size, size));
    }

    // ---- bloom + reflection (same offscreen-buffer trick as WaterDanceEffect) --------------

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
        g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.95f));
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
        double squash = 0.4;
        Graphics2D g2 = (Graphics2D) gc.create();
        java.awt.geom.AffineTransform xf = new java.awt.geom.AffineTransform();
        xf.translate(0, groundY);
        xf.scale(1, -squash);
        xf.translate(0, -groundY);
        g2.transform(xf);
        g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.30f));
        g2.drawImage(jetLayer, 0, bandTop, null);
        g2.dispose();

        java.awt.GradientPaint fade = new java.awt.GradientPaint(
                0, groundY, new Color(0, 0, 0, 0),
                0, groundY + 70, new Color(0, 0, 0, 255));
        Graphics2D g3 = (Graphics2D) gc.create();
        g3.setPaint(fade);
        g3.fillRect(0, groundY, width, 70);
        g3.dispose();
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

    /**
     * True additive compositing (dst = min(255, dst + src*srcAlpha)) so overlapping bright
     * droplets brighten and blow out toward white, instead of Porter-Duff SRC_OVER's normal
     * alpha blend. Java2D ships no built-in additive composite, so this hand-rolls one via the
     * {@link Composite}/{@link CompositeContext} SPI — the standard, dependency-free way to add
     * a custom blend mode to Graphics2D.
     */
    private static final class AdditiveComposite implements Composite {
        static final AdditiveComposite INSTANCE = new AdditiveComposite();

        @Override
        public CompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
            return new CompositeContext() {
                @Override
                public void dispose() {}

                @Override
                public void compose(Raster src, Raster dstIn, WritableRaster dstOut) {
                    int w = Math.min(src.getWidth(), dstIn.getWidth());
                    int h = Math.min(src.getHeight(), dstIn.getHeight());
                    int[] s = new int[4];
                    int[] d = new int[4];
                    int[] o = new int[4];
                    for (int y = 0; y < h; y++) {
                        for (int x = 0; x < w; x++) {
                            src.getPixel(x, y, s);
                            dstIn.getPixel(x, y, d);
                            float sa = s[3] / 255f;
                            o[0] = Math.min(255, d[0] + (int) (s[0] * sa));
                            o[1] = Math.min(255, d[1] + (int) (s[1] * sa));
                            o[2] = Math.min(255, d[2] + (int) (s[2] * sa));
                            o[3] = Math.min(255, d[3] + s[3]);
                            dstOut.setPixel(x, y, o);
                        }
                    }
                }
            };
        }
    }
}
