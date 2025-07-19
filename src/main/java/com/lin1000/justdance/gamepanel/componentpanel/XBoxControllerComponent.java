package com.lin1000.justdance.gamepanel.componentpanel;

import java.awt.*;
import java.awt.image.ImageObserver;

public class XBoxControllerComponent {

    private static Image iconXBoxController = null;
    private int p_off_x,p_off_y = 0;
    Color circleBackgroundColor = new Color(240, 240, 240);
    Color circleBorderColor = new Color(180, 220, 255);
    int circleWidth = 120;
    int circleHeight = 120;
    int circleBorder = 10;
    int width = 180;
    int height = 180;


    static{
        //loadImage
            Toolkit kit=Toolkit.getDefaultToolkit();
            iconXBoxController=kit.getImage("img/icon-xboxcontroller.png");
    }

    public XBoxControllerComponent( int p_off_x, int p_off_y) {
        this.p_off_x=p_off_x;
        this.p_off_y=p_off_y;
    }

    public void draw(Graphics g) {

        Graphics2D gc = (Graphics2D) g.create();

        int originalWidth = iconXBoxController.getWidth(null);
        int originalHeight = iconXBoxController.getHeight(null);

        double widthRatio = (double) width / originalWidth;
        double heightRatio = (double) height / originalHeight;

        double scale = Math.min(widthRatio, heightRatio);

        int newWidth = (int) (originalWidth * scale);
        int newHeight = (int) (originalHeight * scale);

        // Calculate position to center the image
        int x = (width - newWidth) / 2;
        int y = (height - newHeight) / 2;

        gc.setColor(circleBorderColor);
        gc.fillOval(p_off_x+30,p_off_y-5,circleWidth,circleHeight);
        gc.setColor(circleBackgroundColor);
        gc.fillOval(p_off_x+30,p_off_y-5,circleWidth-circleBorder/2,circleHeight-circleBorder/2);
        gc.drawImage(iconXBoxController, p_off_x,p_off_y,newWidth,newHeight,  null);
        gc.drawString("Payer 1 Connected!", p_off_x, p_off_y+140);
        gc.dispose();
    }

}
