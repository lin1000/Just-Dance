package com.lin1000.justdance.gamepanel;

import com.lin1000.justdance.controller.ConditionController;
import com.lin1000.justdance.gamepanel.effect.EffectManager;

/**
 * The shared contract between the arrow-mode gameplay screen ({@link Dance}) and any other
 * game-mode screen (e.g. a future piano/MIDI mode) that {@link com.lin1000.justdance.Project}'s
 * per-round loop, {@link com.lin1000.justdance.controller.SoundController}'s audio-driven
 * clock, {@link com.lin1000.justdance.controller.FPSTimerTask}, and
 * {@link ConditionController}'s scoring/life logic need in order to drive a round without
 * knowing which concrete mode is running.
 *
 * Every method here already exists on {@code Dance} today; this interface only names the
 * subset of it that those four collaborators actually reach into (confirmed by reading each
 * call site directly), so a second gameplay class can satisfy the same seam without either
 * class needing to share a common base class. {@code setVisible(boolean)}/{@code dispose()}
 * are declared here because {@code Project}'s teardown calls them on this interface type, but
 * every implementation is expected to extend {@code JWindow}, which already provides matching
 * signatures — implementing this interface requires zero extra code for those two methods.
 */
public interface GameplayScreen {

    /** Advance one simulation step (input polling, note movement/judgment). Audio-clock-driven. */
    void tick();

    /** Repaint the screen. Provided by {@code JWindow}/{@code Component} on any implementation. */
    void repaint();

    void setDeltaTime(double deltaTime);

    void setDeltaFrame(long deltaFrame);

    ConditionController getConditionControl();

    /** Stop this mode's background note-production (e.g. {@code ArrowsProducer.stop()}). */
    void stopGameplay();

    EffectManager getEffectManager();

    int getLifeX();

    int getLifeY();

    /** The game-area vertical pixel offset ({@code g_off_y} on {@code Dance}). */
    int getOffsetY();

    /** Detach whichever hardware input listeners this mode attached during construction. */
    void removeInputDeviceListener();

    void setVisible(boolean visible);

    void dispose();
}
