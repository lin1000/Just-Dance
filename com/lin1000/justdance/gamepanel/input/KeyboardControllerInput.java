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
        SPACE,
        ESCAPE,
        PAGE_UP,
        PAGE_DOWN,
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
            case KeyEvent.VK_SPACE:
                setKeyboardControllerInputType(KeyboardControllerInputType.SPACE);
                break;
            case KeyEvent.VK_PAGE_UP:
                setKeyboardControllerInputType(KeyboardControllerInputType.PAGE_UP);
                break;
            case KeyEvent.VK_PAGE_DOWN:
                setKeyboardControllerInputType(KeyboardControllerInputType.PAGE_DOWN);
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
            case SPACE:
                super.setInputType(InputType.START);
                break;
            case ESCAPE:
                super.setInputType(InputType.BACK);
                break;
            case PAGE_UP:
                super.setInputType(InputType.LEFT_SHOULDER);
                break;
            case PAGE_DOWN:
                super.setInputType(InputType.RIGHT_SHOULDER);
                break;
            case UNKNOWN:
                super.setInputType(InputType.UNKNOWN);
        }
    }
}
