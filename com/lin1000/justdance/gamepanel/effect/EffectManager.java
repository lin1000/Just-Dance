package com.lin1000.justdance.gamepanel.effect;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EffectManager {
    private int g_off_x;
    private int g_off_y;
    private final List<FlashEffect> flashes = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final List<TextFlashEffect> falshesText = new ArrayList<>();
    private final List<LifeParticle> lifeParticles = new ArrayList<>();
    private final List<LifeParticleExtreme> lifeParticlesExtreme = new ArrayList<>();

    public EffectManager(int g_off_x, int g_off_y) {
        this.g_off_x = g_off_x;
        this.g_off_y = g_off_y;
    }

    public void addSpecialEffect(int x, int y) {
        flashes.add(new FlashEffect(x, y));
        for (int i = 0; i < 20; i++) { // number of particles
            Color c = Color.getHSBColor((float)Math.random(), 1, 1);
            particles.add(new Particle(x, y, c));
        }
    }

    public void addTextFlashEffect(String text, int x, int y) {
        falshesText.add(new TextFlashEffect(text, x, y));
    }

    public void addLifeParticleEffect(int x, int y){
        lifeParticles.add(new LifeParticle(x, y, Color.RED));
    }

    public void addLifeParticleExtremeEffect(int x, int y){
        lifeParticles.add(new LifeParticle(x, y, Color.RED));
    }

    public void drawAll(Graphics2D g) {

        falshesText.removeIf(f-> !f.isActive());
        Font orignalFont = g.getFont();
        g.setFont(new Font("verdana", Font.PLAIN, 80));
        for (TextFlashEffect f : falshesText) f.draw(g);
        g.setFont(orignalFont);

        flashes.removeIf(f -> !f.isActive());
        for (FlashEffect f : flashes) f.draw(g);

        Iterator<Particle> iter = particles.iterator();
        while (iter.hasNext()) {
            Particle p = iter.next();
            if (p.isAlive()) {
                p.update();
                p.draw(g);
            } else {
                iter.remove();
            }
        }

        Iterator<LifeParticle> lifeIter = lifeParticles.iterator();
        while (lifeIter.hasNext()) {
            LifeParticle lifep = lifeIter.next();
            if (lifep.isAlive()) {
                lifep.update();
                lifep.draw(g);
            } else {
                lifeIter.remove();
            }
        }

        Iterator<LifeParticleExtreme> lifeExtremeIter = lifeParticlesExtreme.iterator();
        while (lifeExtremeIter.hasNext()) {
            LifeParticleExtreme lifepEx = lifeExtremeIter.next();
            if (lifepEx.isAlive()) {
                lifepEx.update();
                lifepEx.draw(g);
            } else {
                lifeExtremeIter.remove();
            }
        }
    }
}