package com.lin1000.justdance.gamepanel.input;

import java.awt.event.KeyEvent;

public class KeyboardControllerInput extends Input{

    private KeyboardControllerInputType keyboardControllerInputType = null;

    public enum KeyboardControllerInputType{
        ENTER,
        UP,
        DOWN,
        LEFT,
        RIGHT,
        ESCAPE,
        UNKNOWN
    }

    public KeyboardControllerInput() {
    }

    public void setKeyEvent(KeyEvent keyEvent){
        switch(keyEvent.getKeyCode()){
            case KeyEvent.VK_ENTER:
                setKeyboardControllerInputType(KeyboardControllerInputType.ENTER);
                break;
            case KeyEvent.VK_UP:
                setKeyboardControllerInputType(KeyboardControllerInputType.UP);
                break;
            case KeyEvent.VK_DOWN:
                setKeyboardControllerInputType(KeyboardControllerInputType.DOWN);
                break;
            case KeyEvent.VK_LEFT:
                setKeyboardControllerInputType(KeyboardControllerInputType.LEFT);
                break;
            case KeyEvent.VK_RIGHT:
                setKeyboardControllerInputType(KeyboardControllerInputType.RIGHT);
                break;
            case KeyEvent.VK_ESCAPE:
                setKeyboardControllerInputType(KeyboardControllerInputType.ESCAPE);
                break;
                default:
                    setKeyboardControllerInputType(KeyboardControllerInputType.UNKNOWN);
                break;
        }
    }

    public void setKeyboardControllerInputType(KeyboardControllerInputType keyboardControllerInputType) {
        this.keyboardControllerInputType = keyboardControllerInputType;
        switch(keyboardControllerInputType){
            case ENTER:
                super.setInputType(InputType.A);
                break;
            case UP:
                super.setInputType(InputType.UP);
                break;
            case DOWN:
                super.setInputType(InputType.DOWN);
                break;
            case LEFT:
                super.setInputType(InputType.LEFT);
                break;
            case RIGHT:
                super.setInputType(InputType.RIGHT);
                break;
            case ESCAPE:
                super.setInputType(InputType.BACK);
                break;
            case UNKNOWN:
                super.setInputType(InputType.UNKNOWN);
        }
    }
}
