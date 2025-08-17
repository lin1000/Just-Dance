package com.lin1000.justdance.gamepanel.componentpanel;

import com.lin1000.justdance.input.Input;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public class DDDCanvasComponent {

    //DDDCanvasComponent Outside In Variable
    int x;
    int y;
    int width;
    int height;

    //DDDCanvasComponent Inside Style Variable

    // --- Window / framebuffer ---
    private final int renderW = 1920;   // internal render width
    private final int renderH = 1080;   // internal render height (16:9)
    private final BufferedImage frame = new BufferedImage(renderW, renderH, BufferedImage.TYPE_INT_RGB);
    private final int[] pixels = ((DataBufferInt) frame.getRaster().getDataBuffer()).getData();

    // --- Map (grid) ---
    // 0=empty, 1..n=wall type
    private final int mapW = 24, mapH = 24;
    private final int[][] map = new int[][]{
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,2,2,2,2,2,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,2,0,0,0,2,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,2,0,3,0,2,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,2,0,0,0,2,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,2,2,2,2,2,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1}
    };

    // --- Player / camera ---
    private double posX = 12.0, posY = 12.0; // start in the middle area
    private double dirX = 1.0, dirY = 0.0;   // facing east
    private double planeX = 0.0, planeY = 0.66; // camera plane (FOV ~ 66°)

    // --- Input state ---
    private boolean fwd, back, left, right, turnL, turnR, sprint;

    // --- Timing ---
    private volatile boolean running = true;
    private double fps;

    // --- zBuffer for future sprites ---
    private final double[] zBuffer = new double[renderW];

    public DDDCanvasComponent(int x, int y, int width, int height){
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        // initialize the frame buffer
    }
    // --- Render ---
    public void renderFrame() {
        // clear sky/floor
        int sky = 0x2a3357;   // dark blue-ish
        int floor = 0x2b2b2b; // dark grey
        for (int y = 0; y < renderH; y++) {
            int color = (y < renderH / 2) ? sky : floor;
            int rowStart = y * renderW;
            for (int x = 0; x < renderW; x++) pixels[rowStart + x] = color;
        }

        // raycast per column
        for (int x = 0; x < renderW; x++) {
            double cameraX = 2.0 * x / renderW - 1.0; // [-1, 1]
            double rayDirX = dirX + planeX * cameraX;
            double rayDirY = dirY + planeY * cameraX;

            int mapX = (int) posX;
            int mapY = (int) posY;

            double deltaDistX = (rayDirX == 0) ? 1e30 : Math.abs(1.0 / rayDirX);
            double deltaDistY = (rayDirY == 0) ? 1e30 : Math.abs(1.0 / rayDirY);

            int stepX, stepY;
            double sideDistX, sideDistY;

            if (rayDirX < 0) { stepX = -1; sideDistX = (posX - mapX) * deltaDistX; }
            else { stepX = 1; sideDistX = (mapX + 1.0 - posX) * deltaDistX; }
            if (rayDirY < 0) { stepY = -1; sideDistY = (posY - mapY) * deltaDistY; }
            else { stepY = 1; sideDistY = (mapY + 1.0 - posY) * deltaDistY; }

            int hit = 0; // which cell >0
            int side = 0; // 0 x-side, 1 y-side
            while (hit == 0) {
                if (sideDistX < sideDistY) {
                    sideDistX += deltaDistX;
                    mapX += stepX;
                    side = 0;
                } else {
                    sideDistY += deltaDistY;
                    mapY += stepY;
                    side = 1;
                }
                if (mapY < 0 || mapX < 0 || mapY >= mapH || mapX >= mapW) { hit = 1; break; }
                hit = map[mapY][mapX];
            }

            double perpWallDist;
            if (side == 0) perpWallDist = ((mapX - posX) + (1 - stepX) / 2.0) / rayDirX;
            else perpWallDist = ((mapY - posY) + (1 - stepY) / 2.0) / rayDirY;
            if (perpWallDist <= 1e-6) perpWallDist = 1e-6;
            zBuffer[x] = perpWallDist;

            int lineHeight = (int) (renderH / perpWallDist);
            int drawStart = -lineHeight / 2 + renderH / 2; if (drawStart < 0) drawStart = 0;
            int drawEnd   =  lineHeight / 2 + renderH / 2; if (drawEnd >= renderH) drawEnd = renderH - 1;

            // base color by wall id
            int wallId = (mapY < 0 || mapX < 0 || mapY >= mapH || mapX >= mapW) ? 1 : map[mapY][mapX];
            int baseColor = switch (wallId) {
                case 2 -> 0x9e3d2f; // brick red
                case 3 -> 0x3b8d99; // teal
                default -> 0xbfbfbf; // concrete
            };

            // simple shading: darker on Y-sides + distance falloff
            double shade = (side == 1) ? 0.75 : 1.0;
            shade *= 1.0 / (1.0 + 0.1 * perpWallDist * perpWallDist); // distance attenuation
            int shaded = shadeColor(baseColor, shade);

            for (int y = drawStart; y <= drawEnd; y++) {
                pixels[y * renderW + x] = shaded;
            }
        }
    }

    public void draw(Graphics g) {
        renderFrame();
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.drawImage(frame, x, y, width, height, null);
        g2.setColor(Color.WHITE);
        g2.drawString(String.format("FPS: %.1f  Pos:(%.2f,%.2f)", fps, posX, posY), 10, 20);
        g2.drawString("W/S=前後  A/D=平移  ←/→=旋轉  Shift=加速  Esc=退出", 10, 36);

    }

    private static int shadeColor(int rgb, double s) {
        int r = (int) Math.max(0, Math.min(255, ((rgb >> 16) & 0xff) * s));
        int g = (int) Math.max(0, Math.min(255, ((rgb >> 8) & 0xff) * s));
        int b = (int) Math.max(0, Math.min(255, (rgb & 0xff) * s));
        return (r << 16) | (g << 8) | b;
    }

    // --- Update player and physics ---
    public void update(double dt) {
        double moveSpeed = (sprint ? 5.0 : 3.0) * dt; // units per second
        double rotSpeed  = 2.2 * dt;                  // radians per second

        // rotation
        if (turnL) rotate(-rotSpeed);
        if (turnR) rotate(+rotSpeed);

        // movement: forward/back
        if (fwd) move(dirX, dirY, moveSpeed);
        if (back) move(-dirX, -dirY, moveSpeed);

        // strafe: left/right (perpendicular to dir)
        if (left) move(+dirY, -dirX, moveSpeed);
        if (right) move(-dirY, +dirX, moveSpeed);
    }

    private void rotate(double angle) {
        double oldDirX = dirX;
        dirX = dirX * Math.cos(angle) - dirY * Math.sin(angle);
        dirY = oldDirX * Math.sin(angle) + dirY * Math.cos(angle);
        double oldPlaneX = planeX;
        planeX = planeX * Math.cos(angle) - planeY * Math.sin(angle);
        planeY = oldPlaneX * Math.sin(angle) + planeY * Math.cos(angle);
    }

    private void move(double mx, double my, double amt) {
        double nx = posX + mx * amt;
        double ny = posY + my * amt;
        // slide-friendly collision
        if (isWalkable(nx, posY)) posX = nx;
        if (isWalkable(posX, ny)) posY = ny;
    }

    private boolean isWalkable(double x, double y) {
        int ix = (int) x;
        int iy = (int) y;
        if (ix < 0 || iy < 0 || ix >= mapW || iy >= mapH) return false;
        return map[iy][ix] == 0; // 0 = empty
    }

    // --- Input ---
    public void keyPressed(Input input) {
        switch (input.getInputType()) {
            case W -> fwd = true;
            case S -> back = true;
            case A -> left = true;
            case D -> right = true;
            //case Input.InputType.A-> turnL = true;
            //case KeyEvent.VK_RIGHT -> turnR = true;
            //case KeyEvent.VK_SHIFT -> sprint = true;
            //case KeyEvent.VK_ESCAPE -> running = false;
        }
    }


    public void keyReleased(Input input) {
        switch (input.getInputType()) {
            case W -> fwd = false;
            case S -> back = false;
            case A -> left = false;
            case D -> right = false;
//            case KeyEvent.VK_D -> right = false;
//            case KeyEvent.VK_LEFT -> turnL = false;
//            case KeyEvent.VK_RIGHT -> turnR = false;
//            case KeyEvent.VK_SHIFT -> sprint = false;
        }
    }
    public void keyTyped(KeyEvent e) {}
}
