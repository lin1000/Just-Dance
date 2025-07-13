package com.lin1000.justdance.gamepanel.input;


public class Input {
    InputType inputType = null;
    boolean pressed = false;

    public enum InputType{
        A,
        B,
        X,
        Y,
        BACK,
        START,
        LEFT_SHOULDER,
        RIGHT_SHOULDER,
        UP,
        DOWN,
        LEFT,
        RIGHT,
        LEFT_THUMBSTICK,
        RIGHT_THUMBSTICK,
        GUIDE_BUTTON,
        UNKNOWN
    }

    public InputType getInputType() {
        return inputType;
    }

    public void setInputType(InputType inputType) {
        this.inputType = inputType;
    }

    public boolean isPressed() {
        return pressed;
    }

    public void setPressed(boolean pressed) {
        this.pressed = pressed;
    }
}
