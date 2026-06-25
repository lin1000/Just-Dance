package com.lin1000.justdance;

import java.util.Locale;

public class MvpStepByStep {

    // ====== 基本向量/矩陣工具（OpenGL 習慣：column-major）======
    static final class Vec4 {
        final double x, y, z, w;
        Vec4(double x, double y, double z, double w){ this.x=x; this.y=y; this.z=z; this.w=w; }
        @Override public String toString(){ return String.format(Locale.US, "(%.6f, %.6f, %.6f, %.6f)", x,y,z,w); }
    }

    static final class Mat4 {
        // column-major：m[c*4+r]
        final double[] m = new double[16];

        static Mat4 identity(){
            Mat4 a = new Mat4();
            a.m[0]=a.m[5]=a.m[10]=a.m[15]=1.0;
            return a;
        }

        static Mat4 translation(double tx, double ty, double tz){
            Mat4 t = identity();
            t.m[12]=tx; t.m[13]=ty; t.m[14]=tz;
            return t;
        }

        static Mat4 scale(double sx, double sy, double sz){
            Mat4 s = new Mat4();
            s.m[0]=sx; s.m[5]=sy; s.m[10]=sz; s.m[15]=1.0;
            return s;
        }

        static Mat4 rotationX(double rad){
            Mat4 r = identity();
            double c=Math.cos(rad), s=Math.sin(rad);
            r.m[5]=c; r.m[9]=-s;
            r.m[6]=s; r.m[10]=c;
            return r;
        }

        static Mat4 rotationY(double rad){
            Mat4 r = identity();
            double c=Math.cos(rad), s=Math.sin(rad);
            r.m[0]=c; r.m[8]=s;
            r.m[2]=-s; r.m[10]=c;
            return r;
        }

        static Mat4 rotationZ(double rad){
            Mat4 r = identity();
            double c=Math.cos(rad), s=Math.sin(rad);
            r.m[0]=c; r.m[4]=-s;
            r.m[1]=s; r.m[5]=c;
            return r;
        }

        static Mat4 perspective(double fovyRad, double aspect, double zNear, double zFar){
            double f = 1.0/Math.tan(fovyRad/2.0);
            Mat4 p = new Mat4();
            p.m[0] = f/aspect;
            p.m[5] = f;
            p.m[10] = (zFar+zNear)/(zNear - zFar);
            p.m[11] = -1.0;
            p.m[14] = (2.0*zFar*zNear)/(zNear - zFar);
            return p;
        }

        static Mat4 lookAt(
                double eyeX, double eyeY, double eyeZ,
                double cx, double cy, double cz,
                double upX, double upY, double upZ) {

            // f = normalize(center - eye)
            double fx=cx-eyeX, fy=cy-eyeY, fz=cz-eyeZ;
            double fl = Math.sqrt(fx*fx+fy*fy+fz*fz); fx/=fl; fy/=fl; fz/=fl;

            // up normalize
            double upl = Math.sqrt(upX*upX+upY*upY+upZ*upZ);
            double ux=upX/upl, uy=upY/upl, uz=upZ/upl;

            // s = f x up
            double sx = fy*uz - fz*uy;
            double sy = fz*ux - fx*uz;
            double sz = fx*uy - fy*ux;
            double sl = Math.sqrt(sx*sx+sy*sy+sz*sz);
            sx/=sl; sy/=sl; sz/=sl;

            // u' = s x f
            double tx = sy*fz - sz*fy;
            double ty = sz*fx - sx*fz;
            double tz = sx*fy - sy*fx;

            Mat4 m = identity();
            // 旋轉部分（把世界轉到相機座標）
            m.m[0]=sx; m.m[4]=sy; m.m[8]=sz;
            m.m[1]=tx; m.m[5]=ty; m.m[9]=tz;
            m.m[2]=-fx; m.m[6]=-fy; m.m[10]=-fz;

            // 平移部分：相當於把世界往「相機的反方向」移
            m.m[12] = -(sx*eyeX + sy*eyeY + sz*eyeZ);
            m.m[13] = -(tx*eyeX + ty*eyeY + tz*eyeZ);
            m.m[14] = -(-fx*eyeX - fy*eyeY - fz*eyeZ);

            return m;
        }

        Mat4 mul(Mat4 b){
            Mat4 out = new Mat4();
            for(int c=0;c<4;c++){
                for(int r=0;r<4;r++){
                    out.m[c*4+r] =
                            this.m[0*4+r]*b.m[c*4+0]
                                    + this.m[1*4+r]*b.m[c*4+1]
                                    + this.m[2*4+r]*b.m[c*4+2]
                                    + this.m[3*4+r]*b.m[c*4+3];
                }
            }
            return out;
        }

        Vec4 mul(Vec4 v){
            double x = m[0]*v.x + m[4]*v.y + m[8]*v.z  + m[12]*v.w;
            double y = m[1]*v.x + m[5]*v.y + m[9]*v.z  + m[13]*v.w;
            double z = m[2]*v.x + m[6]*v.y + m[10]*v.z + m[14]*v.w;
            double w = m[3]*v.x + m[7]*v.y + m[11]*v.z + m[15]*v.w;
            return new Vec4(x,y,z,w);
        }
    }

    // ====== 格式化輸出 ======
    static void printStep(String title, Object value){
        System.out.println("[" + title + "] " + value);
    }
    static void printMat(String title, Mat4 M){
        System.out.println("[" + title + "] (column-major; each row shown visually)");
        for(int r=0;r<4;r++){
            System.out.printf(Locale.US, "  | %10.6f %10.6f %10.6f %10.6f |\n",
                    M.m[0*4+r], M.m[1*4+r], M.m[2*4+r], M.m[3*4+r]);
        }
    }

    // ====== 主程式：一步步把點帶過 MVP → NDC → 螢幕 ======
    public static void main(String[] args){
        Locale.setDefault(Locale.US);

        // 1) 參數（你可自行修改）
        Vec4 pModel = new Vec4(1, 1, 1, 1);       // 物件空間的點
        double degX = 30, degY = 45, degZ = 0;    // 物件旋轉
        double tx = 0, ty = 0, tz = 0;            // 物件平移
        double sx = 1, sy = 1, sz = 1;            // 物件縮放

        // 相機與投影
        double eyeX=0, eyeY=0, eyeZ=3;            // 相機位置
        double cx=0, cy=0, cz=0;                  // 看向中心
        double upX=0, upY=1, upZ=0;               // 上方向
        double fovDeg=60, aspect=16.0/9.0, zNear=0.1, zFar=100.0;

        // 螢幕大小（viewport）
        int width=1280, height=720;

        // 2) 建 Model 矩陣（縮放→旋轉→平移；注意乘法方向）
        Mat4 S = Mat4.scale(sx, sy, sz);
        Mat4 Rx = Mat4.rotationX(Math.toRadians(degX));
        Mat4 Ry = Mat4.rotationY(Math.toRadians(degY));
        Mat4 Rz = Mat4.rotationZ(Math.toRadians(degZ));
        Mat4 R = Rz.mul(Ry).mul(Rx);
        Mat4 T = Mat4.translation(tx, ty, tz);
        Mat4 M = T.mul(R).mul(S); // 將點右乘：M * p

        // 3) 建 View、Projection
        Mat4 V = Mat4.lookAt(eyeX, eyeY, eyeZ,  cx, cy, cz,  upX, upY, upZ);
        Mat4 P = Mat4.perspective(Math.toRadians(fovDeg), aspect, zNear, zFar);

        // 4) 逐步帶點過去
        printStep("Input Point (Model Space)", pModel);
        printMat("Model Matrix M", M);
        Vec4 pWorld = M.mul(pModel);
        printStep("After Model (World Space)", pWorld);

        printMat("View Matrix V", V);
        Vec4 pView = V.mul(pWorld);
        printStep("After View (View/Camera Space)", pView);

        printMat("Projection Matrix P", P);
        Vec4 pClip = P.mul(pView);
        printStep("After Projection (Clip Space)", pClip);

        // 5) 透視除法：NDC
        double ndcX = pClip.x / pClip.w;
        double ndcY = pClip.y / pClip.w;
        double ndcZ = pClip.z / pClip.w; // -1 ~ 1（通常 Z-buffer 會再轉到 0~1）
        printStep("NDC (x,y,z)", String.format(Locale.US, "(%.6f, %.6f, %.6f)", ndcX, ndcY, ndcZ));

        // 6) Viewport 對應到螢幕像素（OpenGL 標準：NDC -1..1 → 視口像素）
        double screenX = (ndcX * 0.5 + 0.5) * width;
        double screenY = (ndcY * 0.5 + 0.5) * height; // 注意：若以左上角為 (0,0) 可能要顛倒 Y
        printStep("Screen (px, py)", String.format(Locale.US, "(%.2f, %.2f) in %dx%d", screenX, screenY, width, height));
    }
}
