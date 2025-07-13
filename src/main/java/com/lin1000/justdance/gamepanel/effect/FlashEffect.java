package com.lin1000.justdance.gamepanel.effect;

import java.awt.*;

public class FlashEffect {
    private final int x, y;
    private final long startTime;
    private final int duration = 400;
    private boolean active = true;
    private final Color color;

    public FlashEffect(int x, int y) {
        this.x = x;
        this.y = y;
        this.startTime = System.currentTimeMillis();
        this.color = Color.getHSBColor((float) Math.random(), 1.0f, 1.0f);
    }

    public boolean isActive() {
        return active;
    }

    public void draw(Graphics2D g) {
        long now = System.currentTimeMillis();
        long elapsed = now - startTime;

        if (elapsed > duration) {
            active = false;
            return;
        }

        float progress = elapsed / (float) duration;
        int size = (int) (80 * progress);
        float alpha = 1.0f - progress;

        // 開啟透明畫筆
        Composite original = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

        // 畫光圈
        g.setColor(color);
        g.fillOval(x - size / 2, y - size / 2, size, size);

        // 還原透明度
        g.setComposite(original);
    }
}
