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
    int width = 170;
    int height = 170;
    double maxReactScale  = 0.03;
    double minReactScale = 0.03;
    double deltaReactScale = 0.01;
    private double angle = 0; // 動畫用角度值（radian）

    static{
        //loadImage
            Toolkit kit=Toolkit.getDefaultToolkit();
            iconXBoxController=kit.getImage("img/icon-xboxcontroller.png");
    }

    public XBoxControllerComponent( int p_off_x, int p_off_y) {
        this.p_off_x=p_off_x;
        this.p_off_y=p_off_y;
    }

    public void setRactScale(){
        maxReactScale=0.08;
    }

    public void draw(Graphics g) {

        Graphics2D gc = (Graphics2D) g.create();

        int originalWidth = iconXBoxController.getWidth(null);
        int originalHeight = iconXBoxController.getHeight(null);

        double widthRatio = (double) width / originalWidth;
        double heightRatio = (double) height / originalHeight;
        double scale = Math.min(widthRatio, heightRatio);

        //drifting effect
        angle += 0.03; // 調整速度
        if (angle > Math.PI * 2) angle = 0;
        double reactScale = 1.0 + maxReactScale * Math.sin(angle); // 呼吸大小變化
        double driftRadius = 5; // 飄移半徑
        int driftX = (int) (driftRadius * Math.cos(angle));
        int driftY = (int) (driftRadius * Math.sin(angle* 1.2)); // y 軸變速有趣一點

        int newWidth = (int) (originalWidth * scale * reactScale);
        int newHeight = (int) (originalHeight * scale * reactScale);

        maxReactScale = - deltaReactScale;
        if (maxReactScale <= minReactScale) { // 最小縮放
            maxReactScale = minReactScale;
        }

        // Calculate position to center the image
        int x = p_off_x + driftX-3;
        int y = p_off_y + driftY;

        gc.setColor(circleBorderColor);
        gc.fillOval(p_off_x+30,p_off_y-5,circleWidth,circleHeight);
        gc.setColor(circleBackgroundColor);
        gc.fillOval(p_off_x+30,p_off_y-5,circleWidth-circleBorder/2,circleHeight-circleBorder/2);
        gc.drawImage(iconXBoxController, x,y,newWidth,newHeight,  null);
        gc.drawString("Joystick Payer Cwswsqwsdaswwssdddwawsaonnected!", p_off_x, p_off_y+140);

        gc.dispose();
    }

}
