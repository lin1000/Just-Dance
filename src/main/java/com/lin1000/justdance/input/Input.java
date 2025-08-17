package com.lin1000.justdance.input;


public class Input {
    InputType inputType = null;
    boolean pressed = false;

    public enum InputType{
        A,
        S,
        D,
        W,
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
        LEFT_THUMBSTICK_MOVE_LEFT,
        LEFT_THUMBSTICK_MOVE_RIGHT,
        LEFT_THUMBSTICK_MOVE_FORWARD,
        LEFT_THUMBSTICK_MOVE_BACKWARD,
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
