package com.lin1000.justdance.XInputDevice;

import com.github.strikerx3.jxinput.XInputAxes;
import com.github.strikerx3.jxinput.XInputComponents;
import com.github.strikerx3.jxinput.XInputDevice;
import com.github.strikerx3.jxinput.enums.XInputAxis;
import com.github.strikerx3.jxinput.enums.XInputButton;
import com.github.strikerx3.jxinput.listener.SimpleXInputDeviceListener;
import com.lin1000.justdance.gamepanel.MainMenu;
import com.lin1000.justdance.gamepanel.action.MainMenuAction;
import com.lin1000.justdance.gamepanel.input.Input;
import com.lin1000.justdance.gamepanel.input.XBoxControllerInput;

public class MainMenuXInputDeviceListener extends SimpleXInputDeviceListener {

    //Binding Main Window Target
    MainMenu mainWindowTarget = null;

    //JXInputDevice
    // ��l��
    static float lastLX = 0f;
    static float lastLY = 0f;

    public MainMenuXInputDeviceListener(MainMenu mainWindowTarget) {
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
        System.out.printf("���s %s �Q%s%n", button.name(), pressed ? "���U" : "��}");

        //Translate JXInputDevice Controller button into InputType
        XBoxControllerInput xBoxControllerInput = new XBoxControllerInput();
        xBoxControllerInput.setXInputButton(button);
        xBoxControllerInput.setPressed(pressed);
        MainMenuAction.getInstance().inputAction(xBoxControllerInput,mainWindowTarget);
    }

    public static void calculateAxis(XInputDevice device){
        //System.out.println("�}�l�ʱ��n���ܤơ]���n��^...");
        XInputComponents components = device.getComponents();
        XInputAxes axes = components.getAxes();

        float currentLX = axes.get(XInputAxis.LEFT_THUMBSTICK_X);
        float currentLY = axes.get(XInputAxis.LEFT_THUMBSTICK_Y);

        if (Math.abs(currentLX - lastLX) > 0.01f) {
            onAxisChanged("Left X", lastLX, currentLX);
            lastLX = currentLX;
        }

        if (Math.abs(currentLY - lastLY) > 0.01f) {
            onAxisChanged("Left Y", lastLY, currentLY);
            lastLY = currentLY;
        }
    }

    private static void onAxisChanged(String axisName, float oldValue, float newValue) {
        System.out.printf("[�b�ܤ�] %s�G%.2f �� %.2f\n", axisName, oldValue, newValue);
    }
}
