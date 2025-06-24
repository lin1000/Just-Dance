package com.lin1000.justdance.beats;

//真正的箭頭
public class arrow extends Object {
    public int x;
    public int y;

    arrow(int x_position) {
        x = x_position;
        y = 730;

    }

    public int move(int y_movement) {
        y = y - y_movement; //movement
        return y;
    }
}
