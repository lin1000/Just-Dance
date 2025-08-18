package com.lin1000.justdance.ddd;

// X 坐标表示左右方向的移动
// Y 表示屏幕上的上下移动
// Z 表示深度（因此 Z 轴垂直于您的屏幕）。正 Z 表示“朝向观察者”。
public class Vertex {
    public double x;
    public double y;
    public double z;
    public Vertex(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}