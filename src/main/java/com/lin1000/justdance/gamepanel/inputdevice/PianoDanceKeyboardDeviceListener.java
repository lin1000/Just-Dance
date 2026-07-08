package com.lin1000.justdance.gamepanel.inputdevice;

import com.lin1000.justdance.gamepanel.GameplayScreen;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Minimal keyboard handling for piano mode: ESC ends the round. Unlike
 * {@link DanceKeyboardDeviceListener}, this does not go through {@code Input}/{@code InputType}
 * or a judgment singleton — piano mode's actual note input comes from MIDI (Phase 6), not the
 * keyboard, so this listener's only job is to make a round exitable during Phase 5's
 * orchestration-and-rendering-only skeleton.
 */
public class PianoDanceKeyboardDeviceListener extends KeyAdapter {

    private final GameplayScreen mainWindowTarget;

    public PianoDanceKeyboardDeviceListener(GameplayScreen mainWindowTarget) {
        this.mainWindowTarget = mainWindowTarget;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            mainWindowTarget.getConditionControl().setCondition(5); // 5 = exit game
        }
    }
}
