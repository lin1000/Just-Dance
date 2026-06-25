package com.lin1000.justdance.player;

import com.lin1000.justdance.gamepanel.inputdevice.MainMenuXInputDeviceListener;

public class JXInputPlayer {
    String name;
    int playerNum;
    int age;
    boolean isConnected;
    String ControllerID;
    MainMenuXInputDeviceListener mainMenuXInputDeviceListener = null;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPlayerNum() {
        return playerNum;
    }

    public void setPlayerNum(int playerNum) {
        this.playerNum = playerNum;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }

    public String getControllerID() {
        return ControllerID;
    }

    public void setControllerID(String controllerID) {
        ControllerID = controllerID;
    }

    public MainMenuXInputDeviceListener getMainMenuXInputDeviceListener() {
        return mainMenuXInputDeviceListener;
    }

    public void setMainMenuXInputDeviceListener(MainMenuXInputDeviceListener mainMenuXInputDeviceListener) {
        this.mainMenuXInputDeviceListener = mainMenuXInputDeviceListener;
    }
}
