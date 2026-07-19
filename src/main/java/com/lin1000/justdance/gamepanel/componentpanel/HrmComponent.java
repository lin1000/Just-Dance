package com.lin1000.justdance.gamepanel.componentpanel;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;

/**
 * Floating "HR Monitor Connected" badge matching the style of
 * XBoxControllerComponent / MidiControllerComponent / KeyboardControllerComponent.
 *
 * The heart+ECG icon is pre-rendered once into a BufferedImage so it is
 * identical in quality to the PNG-backed sibling components.
 */
public class HrmComponent {

    // Pre-rendered at class-load time — same lifecycle as the static PNG loads
    // in the other component classes.
    private static final BufferedImage ICON = buildIcon();

    // ── icon construction ────────────────────────────────────────────────────

    private static BufferedImage buildIcon() {
        int sz = 170;
        BufferedImage img = new BufferedImage(sz, sz, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,   RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,      RenderingHints.VALUE_RENDER_QUALITY);

        // Heart geometry: two lobes + pointed base, classic Valentine shape
        int cx = sz / 2;        // 85
        int cy = sz / 2 - 2;   // 83  (slightly above centre — room for label)
        int s  = sz / 4;       // 42  (lobe radius)

        GeneralPath heart = makeHeart(cx, cy, s);

        // Soft drop-shadow
        g.setColor(new Color(140, 0, 20, 55));
        g.translate(0, 5);
        g.fill(heart);
        g.translate(0, -5);

        // Gradient fill: salmon → crimson
        g.setPaint(new GradientPaint(cx, cy - s,       new Color(255, 110, 110),
                                     cx, cy + s + 5,   new Color(185, 10,  40)));
        g.fill(heart);

        // ECG pulse line clipped inside the heart
        Shape savedClip = g.getClip();
        g.clip(heart);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(makeEcg(cx, cy, s));
        g.setClip(savedClip);

        // Subtle bright rim
        g.setStroke(new BasicStroke(1.8f));
        g.setColor(new Color(255, 255, 255, 90));
        g.draw(heart);

        g.dispose();
        return img;
    }

    /**
     * Classic Valentine heart: two symmetric arcs at the top, one point at the
     * bottom. Path starts from the top-centre dip and goes clockwise.
     */
    private static GeneralPath makeHeart(int cx, int cy, int s) {
        GeneralPath p = new GeneralPath();
        p.moveTo(cx, cy);                                         // top-centre dip
        // Left lobe upper arc
        p.curveTo(cx,     cy - s * 0.55f,
                  cx - s, cy - s * 0.55f,
                  cx - s, cy);
        // Left side → bottom tip
        p.curveTo(cx - s, cy + s * 0.75f,
                  cx,     cy + s * 0.95f,
                  cx,     cy + s);
        // Right side ← bottom tip (mirror)
        p.curveTo(cx,     cy + s * 0.95f,
                  cx + s, cy + s * 0.75f,
                  cx + s, cy);
        // Right lobe upper arc
        p.curveTo(cx + s, cy - s * 0.55f,
                  cx,     cy - s * 0.55f,
                  cx,     cy);
        p.closePath();
        return p;
    }

    /** Classic PQRST ECG waveform running horizontally through the heart. */
    private static GeneralPath makeEcg(int cx, int cy, int s) {
        float yc = cy + s * 0.2f;                      // baseline, below centre
        GeneralPath p = new GeneralPath();
        p.moveTo(cx - s,           yc);                 // flat left
        p.lineTo(cx - s * 0.45f,  yc);
        p.lineTo(cx - s * 0.28f,  yc - s * 0.18f);    // P wave up
        p.lineTo(cx - s * 0.12f,  yc);                 // P wave down
        p.lineTo(cx - s * 0.02f,  yc + s * 0.12f);    // Q dip
        p.lineTo(cx + s * 0.12f,  yc - s * 0.82f);    // R spike (tall)
        p.lineTo(cx + s * 0.26f,  yc + s * 0.32f);    // S dip
        p.lineTo(cx + s * 0.45f,  yc);                 // return baseline
        p.lineTo(cx + s,           yc);                 // flat right
        return p;
    }

    // ── instance (same fields and drift pattern as the other components) ─────

    private final int p_off_x;
    private final int p_off_y;

    private final Color circleBackgroundColor = new Color(255, 228, 228);
    private final Color circleBorderColor     = new Color(255, 140, 140);
    private final int   circleWidth  = 120;
    private final int   circleHeight = 120;
    private final int   circleBorder = 10;
    private final int   width  = 170;
    private final int   height = 170;

    private double angle = 0.8;

    public HrmComponent(int p_off_x, int p_off_y) {
        this.p_off_x = p_off_x;
        this.p_off_y = p_off_y;
    }

    public void draw(Graphics g, int bpm) {
        Graphics2D gc = (Graphics2D) g.create();
        gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        gc.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Slow floating drift — identical to the other components
        angle += 0.03;
        if (angle > Math.PI * 2) angle = 0;
        double driftRadius = 5;
        int driftX = (int)(driftRadius * Math.cos(angle));
        int driftY = (int)(driftRadius * Math.sin(angle * 1.2));

        // Pulse scale: icon grows slightly once per heartbeat
        double beatPeriod = (bpm > 0) ? 60.0 / bpm : 1.0;
        double tSec       = System.nanoTime() / 1_000_000_000.0;
        double beatPhase  = (tSec % beatPeriod) / beatPeriod;
        double pulse      = Math.max(0, Math.sin(beatPhase * Math.PI * 2));
        double reactScale = 1.0 + 0.07 * pulse;

        int originalWidth  = ICON.getWidth();
        int originalHeight = ICON.getHeight();
        double scale       = Math.min((double) width  / originalWidth,
                                      (double) height / originalHeight) * reactScale;
        int newWidth  = (int)(originalWidth  * scale);
        int newHeight = (int)(originalHeight * scale);

        // Badge circle (same colours / sizes as sibling components)
        gc.setColor(circleBorderColor);
        gc.fillOval(p_off_x + 30, p_off_y - 5, circleWidth, circleHeight);
        gc.setColor(circleBackgroundColor);
        gc.fillOval(p_off_x + 30, p_off_y - 5,
                    circleWidth  - circleBorder / 2,
                    circleHeight - circleBorder / 2);

        // Icon image
        gc.drawImage(ICON, p_off_x + driftX - 3, p_off_y + driftY, newWidth, newHeight, null);

        // Label
        gc.setFont(new Font("verdana", Font.PLAIN, 14));
        gc.setColor(Color.DARK_GRAY);
        if (bpm > 0) gc.drawString(bpm + " BPM", p_off_x + 52, p_off_y + 115);
        gc.drawString("HR Monitor Connected!", p_off_x, p_off_y + 140);

        gc.dispose();
    }
}
