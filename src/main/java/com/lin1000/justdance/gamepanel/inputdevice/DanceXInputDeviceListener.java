package com.lin1000.justdance.gamepanel.inputdevice;

import com.github.strikerx3.jxinput.XInputAxes;
import com.github.strikerx3.jxinput.XInputComponents;
import com.github.strikerx3.jxinput.XInputDevice;
import com.github.strikerx3.jxinput.enums.XInputAxis;
import com.github.strikerx3.jxinput.enums.XInputButton;
import com.github.strikerx3.jxinput.listener.SimpleXInputDeviceListener;
import com.lin1000.justdance.gamepanel.Dance;
import com.lin1000.justdance.gamepanel.action.DanceAction;
import com.lin1000.justdance.input.Input;
import com.lin1000.justdance.input.XBoxControllerInput;

public class DanceXInputDeviceListener extends SimpleXInputDeviceListener {

    //Binding Main Window Target
    static Dance mainWindowTarget = null;

    //JXInputDevice
    // ��l��
    static float lastLX = 0f;
    static float lastLY = 0f;

    public DanceXInputDeviceListener(Dance mainWindowTarget) {
        this.mainWindowTarget = mainWindowTarget;
    }


    @Override
    public void connected() {
        // Resume the game
        System.out.println("listener-connected");
    }

    @Override
    public void disconnected() {
        // Pause the game and display a message
        System.out.println("listener-disconnected");
    }

    @Override
    public void buttonChanged(final XInputButton button, final boolean pressed) {
        // The given button was just pressed (if pressed == true) or released (pressed == false)
        System.out.printf("button : %s %s/n", button.name(), pressed ? " pressed" : "released");

        //Translate JXInputDevice Controller button into InputType
        XBoxControllerInput xBoxControllerInput = new XBoxControllerInput();
        xBoxControllerInput.setXInputButton(button);
        xBoxControllerInput.setPressed(pressed);
        DanceAction.getInstance().inputAction(xBoxControllerInput,mainWindowTarget);
    }

    public static void calculateAxis(XInputDevice device){
        //System.out.println("開始監控搖桿變化（左搖桿）...");
        XInputComponents components = device.getComponents();
        XInputAxes axes = components.getAxes();

        float currentLX = axes.get(XInputAxis.LEFT_THUMBSTICK_X);
        float currentLY = axes.get(XInputAxis.LEFT_THUMBSTICK_Y);

        if (Math.abs(currentLX - lastLX) > 0.01f) {
            onAxisChanged(XInputAxis.LEFT_THUMBSTICK_X, lastLX, currentLX);
            lastLX = currentLX;
        }

        if (Math.abs(currentLY - lastLY) > 0.01f) {
            onAxisChanged(XInputAxis.LEFT_THUMBSTICK_Y, lastLY, currentLY);
            lastLY = currentLY;
        }
    }

    private static void onAxisChanged(XInputAxis xInputAxis, float oldValue, float newValue) {
        System.out.printf("[Axis Changed] %s：%.2f → %.2f\n", xInputAxis, oldValue, newValue);
        XBoxControllerInput xBoxControllerInput = new XBoxControllerInput();

        /**
        switch(xInputAxis){
            case LEFT_THUMBSTICK_X -> {
                if(oldValue <=0.01f && newValue <=0.0f){ //move left
                    xBoxControllerInput.setXBoxControllerInputType(XBoxControllerInput.XBoxControllerInputType.LEFT_THUBMSTICK_MOVE_LEFT);
                    xBoxControllerInput.setPressed(true);
                    DanceAction.getInstance().inputAction(xBoxControllerInput,mainWindowTarget);
                }else if(oldValue >=-0.01f && newValue >=0.0f){ // move right
                    xBoxControllerInput.setXBoxControllerInputType(XBoxControllerInput.XBoxControllerInputType.LEFT_THUBMSTICK_MOVE_RIGHT);
                    xBoxControllerInput.setPressed(true);
                    DanceAction.getInstance().inputAction(xBoxControllerInput,mainWindowTarget);
                }else{ //keep standing
                    //donothing
                }
            }
            case LEFT_THUMBSTICK_Y-> {
                oldValue=1;
            }

        }**/

//        //Translate JXInputDevice Controller button into InputType
//        XBoxControllerInput xBoxControllerInput = new XBoxControllerInput();
//        xBoxControllerInput.setXInputButton(button);
//        xBoxControllerInput.setPressed(pressed);
//        DanceAction.getInstance().inputAction(xBoxControllerInput,mainWindowTarget);
    }
}
