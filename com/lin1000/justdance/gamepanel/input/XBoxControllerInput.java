package com.lin1000.justdance.gamepanel.input;

import com.github.strikerx3.jxinput.enums.XInputButton;

public class XBoxControllerInput extends Input{

    private XBoxControllerInputType xBoxControllerInputType = null;

    public enum XBoxControllerInputType{
        A,
        B,
        X,
        Y,
        BACK,
        START,
        LEFT_SHOULDER,
        RIGHT_SHOULDER,
        DPAD_UP,
        DPAD_DOWN,
        DPAD_LEFT,
        DPAD_RIGHT,
        LEFT_THUBMSTICK,
        RIGHT_THUMBSTICK,
        GUIDE_BUTTON,
        UNKNOWN,
    }

    public XBoxControllerInput() {
    }

    public XBoxControllerInputType getxBoxControllerInputType() {
        return xBoxControllerInputType;
    }

    public void setXInputButton(XInputButton button){
        switch(button){
            case A:
                setXBoxControllerInputType(XBoxControllerInputType.A);
                break;
            case B:
                setXBoxControllerInputType(XBoxControllerInputType.B);
                break;
            case X:
                setXBoxControllerInputType(XBoxControllerInputType.X);
                break;
            case Y:
                setXBoxControllerInputType(XBoxControllerInputType.Y);
                break;
            case BACK:
                setXBoxControllerInputType(XBoxControllerInputType.BACK);
                break;
            case START:
                setXBoxControllerInputType(XBoxControllerInputType.START);
                break;
            case LEFT_SHOULDER:
                setXBoxControllerInputType(XBoxControllerInputType.LEFT_SHOULDER);
                break;
            case RIGHT_SHOULDER:
                setXBoxControllerInputType(XBoxControllerInputType.RIGHT_SHOULDER);
                break;
            case LEFT_THUMBSTICK:
                setXBoxControllerInputType(XBoxControllerInputType.LEFT_THUBMSTICK);
                break;
            case RIGHT_THUMBSTICK:
                setXBoxControllerInputType(XBoxControllerInputType.RIGHT_THUMBSTICK);
                break;
            case DPAD_UP:
                setXBoxControllerInputType(XBoxControllerInputType.DPAD_UP);
                break;
            case DPAD_DOWN:
                setXBoxControllerInputType(XBoxControllerInputType.DPAD_DOWN);
                break;
            case DPAD_LEFT:
                setXBoxControllerInputType(XBoxControllerInputType.DPAD_LEFT);
                break;
            case DPAD_RIGHT:
                setXBoxControllerInputType(XBoxControllerInputType.DPAD_RIGHT);
                break;
            case GUIDE_BUTTON:
                setXBoxControllerInputType(XBoxControllerInputType.GUIDE_BUTTON);
                break;
            case UNKNOWN:
                setXBoxControllerInputType(XBoxControllerInputType.UNKNOWN);
                break;
        }
    }

    public void setXBoxControllerInputType(XBoxControllerInputType xBoxControllerInputType) {
        this.xBoxControllerInputType = xBoxControllerInputType;
        switch(xBoxControllerInputType){
            case A:
                super.setInputType(InputType.A);
                break;
            case B:
                super.setInputType(InputType.B);
                break;
            case X:
                super.setInputType(InputType.X);
                break;
            case Y:
                super.setInputType(InputType.Y);
                break;
            case BACK:
                super.setInputType(InputType.BACK);
                break;
            case START:
                super.setInputType(InputType.START);
                break;
            case LEFT_SHOULDER:
                super.setInputType(InputType.LEFT_SHOULDER);
                break;
            case RIGHT_SHOULDER:
                super.setInputType(InputType.RIGHT_SHOULDER);
                break;
            case DPAD_UP:
                super.setInputType(InputType.UP);
                break;
            case DPAD_DOWN:
                super.setInputType(InputType.DOWN);
                break;
            case DPAD_LEFT:
                super.setInputType(InputType.LEFT);
                break;
            case DPAD_RIGHT:
                super.setInputType(InputType.RIGHT);
                break;
            case GUIDE_BUTTON:
                super.setInputType(InputType.GUIDE_BUTTON);
        }
    }
}
