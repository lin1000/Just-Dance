package com.lin1000.justdance.XInputDevice;

import com.lin1000.justdance.gamepanel.MainMenu;
import com.lin1000.justdance.gamepanel.action.DanceAction;
import com.lin1000.justdance.gamepanel.action.MainMenuAction;
import com.lin1000.justdance.gamepanel.input.KeyboardControllerInput;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class MainMenuKeyboardDeviceListener extends KeyAdapter {

    //Binding Main Window Target
    MainMenu mainWindowTarget = null;

    //JXInputDevice
    // ªì©l­È
    static float lastLX = 0f;
    static float lastLY = 0f;

    public MainMenuKeyboardDeviceListener(MainMenu mainWindowTarget) {
        this.mainWindowTarget = mainWindowTarget;
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Handle keyTyped event if needed
        //super.keyTyped(e);
        System.out.println("MainMenu Key Typed: " + e.getKeyCode());
    }

    @Override
    public void keyPressed(KeyEvent e) {
        System.out.println("MainMenu Key keyPressed: " + e.getKeyCode());
        //Translate Keyboard Event into InputType
        KeyboardControllerInput keybnoardControllerInput = new KeyboardControllerInput();
        keybnoardControllerInput.setKeyEvent(e);
        keybnoardControllerInput.setPressed(true);
        MainMenuAction.getInstance().inputAction(keybnoardControllerInput,this.mainWindowTarget);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        System.out.println("MainMenu Key keyReleased: " + e.getKeyCode());
        //Translate Keyboard Event into InputType
        KeyboardControllerInput keyboardControllerInput = new KeyboardControllerInput();
        keyboardControllerInput.setKeyEvent(e);
        keyboardControllerInput.setPressed(false);
        MainMenuAction.getInstance().inputAction(keyboardControllerInput,this.mainWindowTarget);
    }

}
