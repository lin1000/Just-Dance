package com.lin1000.justdance.gamepanel.effect;

import java.awt.*;

public class LifeParticle {
    public float x, y;
    public float vx, vy;
    public float life = 1.0f; // 1 = full, 0 = dead
    public final Color color;

    public LifeParticle(int x, int y, Color color) {
        this.x = x;
        this.y = y;
        this.vx = (float) (Math.random() * 4 - 2);
        this.vy = (float) (Math.random() * -4 - 1);
        this.color = color;
    }

    public void update() {
        x += vx;
        y += vy;
        vy += 0.2f; // gravity
        life -= 0.03f;
    }

    public boolean isAlive() {
        return life > 0;
    }

    public void draw(Graphics2D g) {
        if (!isAlive()) return;
        Composite original = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, life));
        g.setColor(color);
        g.fillOval((int)x, (int)y, 6, 6);
        g.setComposite(original);
    }
}