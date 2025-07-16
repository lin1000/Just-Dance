package com.lin1000.justdance.XInputDevice;


import com.lin1000.justdance.gamepanel.Dance;
import com.lin1000.justdance.gamepanel.action.DanceAction;
import com.lin1000.justdance.gamepanel.input.KeyboardControllerInput;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class DanceKeyboardDeviceListener extends KeyAdapter {

    //Binding Main Window Target
    Dance mainWindowTarget = null;

    //JXInputDevice
    // Initial Value
    static float lastLX = 0f;
    static float lastLY = 0f;

    public DanceKeyboardDeviceListener(Dance mainWindowTarget) {
        this.mainWindowTarget = mainWindowTarget;
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Handle keyTyped event if needed
        super.keyTyped(e);
        System.out.println("Dance Key Typed: " + e.getKeyCode());
    }

    @Override
    public void keyPressed(KeyEvent e) {
        //Translate Keyboard Event into InputType
        System.out.println("Dance Key keyPressed: " + e.getKeyCode());
        KeyboardControllerInput keybnoardControllerInput = new KeyboardControllerInput();
        keybnoardControllerInput.setKeyEvent(e);
        keybnoardControllerInput.setPressed(true);
        DanceAction.getInstance().inputAction(keybnoardControllerInput,this.mainWindowTarget);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        System.out.println("Dance Key keyReleased: " + e.getKeyCode());
        //Translate Keyboard Event into InputType
        KeyboardControllerInput keyboardControllerInput = new KeyboardControllerInput();
        keyboardControllerInput.setKeyEvent(e);
        keyboardControllerInput.setPressed(false);
        DanceAction.getInstance().inputAction(keyboardControllerInput,this.mainWindowTarget);
    }

}
