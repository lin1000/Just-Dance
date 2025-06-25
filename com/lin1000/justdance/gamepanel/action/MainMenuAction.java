package com.lin1000.justdance.gamepanel.action;

import com.lin1000.justdance.gamepanel.MainMenu;
import com.lin1000.justdance.gamepanel.input.Input;

public class MainMenuAction {

    private static MainMenuAction mainMenuAction = null;
    private static Object mainMenuActionSingletonLock = new Object();

    private MainMenuAction() {
    }

    public static MainMenuAction getInstance(){
        synchronized (mainMenuActionSingletonLock) {
            if (mainMenuAction == null) {
                mainMenuAction = new MainMenuAction();
            }
        }
        return mainMenuAction;
    }

    public void inputAction(Input input, MainMenu mainWindowTarget) {
        switch (mainWindowTarget.getcontrolFlow()) {
            case 1: //1 means in game landing screen
                if (input.getInputType() == Input.InputType.DOWN && input.isPressed()) {
                    //do nothing
                } else if (input.getInputType() == Input.InputType.UP && input.isPressed()) {
                    //do nothing
                } else if (input.getInputType() == Input.InputType.A && input.isPressed()) {
                    //do nothing
                } else if (input.getInputType() == Input.InputType.START && input.isPressed()) {
                    mainWindowTarget.controlFlow = 2;//2 means chose music
                } else if (input.getInputType() == Input.InputType.BACK && input.isPressed()) {
                    System.out.println("BACK IS pressed");
                    mainWindowTarget.controlFlow = 4;//4 means exit
                } else if (input.getInputType() == Input.InputType.LEFT_SHOULDER && input.isPressed()) {
                    //do nothing
                } else if (input.getInputType() == Input.InputType.RIGHT_SHOULDER && input.isPressed()) {
                    //do nothing
                }

                break;
            case 2://2 means in chose music (main menu screen)
                if (input.getInputType() == Input.InputType.DOWN && input.isPressed()) {
                    mainWindowTarget.musicOptionIndex++;
                    if (mainWindowTarget.musicOptionIndex == 4) mainWindowTarget.musicOptionIndex = 0;
                    mainWindowTarget.soundController.playBackgroundSound(mainWindowTarget.musicOptionIndex, true);
                    mainWindowTarget.repaint();
                } else if (input.getInputType() == Input.InputType.UP && input.isPressed()) {
                    mainWindowTarget.musicOptionIndex--;
                    if (mainWindowTarget.musicOptionIndex == -1) mainWindowTarget.musicOptionIndex = 3;
                    mainWindowTarget.soundController.playBackgroundSound(mainWindowTarget.musicOptionIndex, true);
                    mainWindowTarget.repaint();
                } else if (input.getInputType() == Input.InputType.A && input.isPressed()) {
                    switch (mainWindowTarget.controlFlow) {
                        case 1:
                            mainWindowTarget.controlFlow++;
                            //mainWindowTarget.soundController.playBackgroundSound(mainWindowTarget.musicOptionIndex);
                            break;
                        case 2:
                            mainWindowTarget.soundController.playMainMenuSound(1);
                            mainWindowTarget.controlFlow++;
                            mainWindowTarget.menuscreen();
                            break;
                        case 3:
                            break;
                        default:
                    }
                }
                break;

        }
    }


}
