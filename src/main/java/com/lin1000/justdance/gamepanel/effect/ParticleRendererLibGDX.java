package com.lin1000.justdance.gamepanel.effect;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.ScreenUtils;

import java.awt.image.BufferedImage;

public class ParticleRendererLibGDX {
    FrameBuffer fbo;
    SpriteBatch batch;
    ParticleEffect effect;
    int width = 800, height = 600;

    public ParticleRendererLibGDX() {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override
            public void create() {
                batch = new SpriteBatch();
                fbo = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, false);
                effect = new ParticleEffect();
                effect.load(Gdx.files.internal("effects/glow.p"), Gdx.files.internal("effects/"));
                effect.start();
            }
        }, config);
    }

    public BufferedImage render(float delta) {
        fbo.begin();
        Gdx.gl.glClearColor(0, 0, 0, 0);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.begin();
        effect.draw(batch, delta);
        batch.end();
        Pixmap pixmap = ScreenUtils.getFrameBufferPixmap(0, 0, width, height);
        fbo.end();

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgba8888 = pixmap.getPixel(x, height - y - 1);
                image.setRGB(x, y, rgba8888);
            }
        }
        pixmap.dispose();
        return image;
    }

    public void triggerEffect(float x, float y) {
        effect.setPosition(x, y);
        effect.start();
    }
}
