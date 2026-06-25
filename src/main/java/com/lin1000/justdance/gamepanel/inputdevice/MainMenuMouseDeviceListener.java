package com.lin1000.justdance.gamepanel.inputdevice;

import com.lin1000.justdance.gamepanel.MainMenu;
import com.lin1000.justdance.gamepanel.action.MainMenuAction;
import com.lin1000.justdance.input.KeyboardControllerInput;

import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

public class MainMenuMouseDeviceListener implements MouseMotionListener {

    //Binding Main Window Target
    MainMenu mainWindowTarget = null;

    public MainMenuMouseDeviceListener(MainMenu mainWindowTarget) {
        this.mainWindowTarget = mainWindowTarget;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        double yi = 180.0 / mainWindowTarget.getHeight();
        double xi = 180.0 / mainWindowTarget.getWidth();
        mainWindowTarget.dddx[0] = (int) (e.getX() * xi);
        mainWindowTarget.dddy[0] = -(int) (e.getY() * yi);
        mainWindowTarget.repaint();
    }
    @Override
    public void mouseMoved(MouseEvent e) {
        double yi = 180.0 / mainWindowTarget.getHeight();
        double xi = 180.0 / mainWindowTarget.getWidth();
        mainWindowTarget.dddx[0] = (int) (e.getX() * xi);
        mainWindowTarget.dddy[0] = -(int) (e.getY() * yi);
        mainWindowTarget.repaint();
    }
}
