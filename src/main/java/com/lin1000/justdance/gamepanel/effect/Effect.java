package com.lin1000.justdance.gamepanel.effect;

import java.awt.*;

public class Effect {
    private final int x, y;
    private final long startTimeMillis;
    private final int duration = 400; // 毫秒
    private boolean active = true;

    public Effect(int x, int y) {
        this.x = x;
        this.y = y;
        this.startTimeMillis = System.currentTimeMillis();
    }

    public boolean isActive() {
        return active;
    }

    public void draw(Graphics2D g) {
        long now = System.currentTimeMillis();
        long elapsed = now - startTimeMillis;

        if (elapsed > duration) {
            active = false;
            return;
        }

        float progress = elapsed / (float) duration;
        int size = (int) (80 * progress); // 擴散
        float alpha = 1.0f - progress; // 漸淡

        // 開啟透明畫筆
        Composite original = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

        // 畫光圈
        g.setColor(Color.CYAN);
        g.fillOval(x - size / 2, y - size / 2, size, size);

        // 還原透明度
        g.setComposite(original);
    }
}
