package com.lin1000.justdance.gamepanel.componentpanel;

import com.github.sarxos.webcam.Webcam;
import com.lin1000.justdance.gamepanel.MainMenu;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.javacv.Frame;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;


public class WebCamComponent{

    private MainMenu mainTargetWindow = null;
    private JLabel cameraLabel;
    private Webcam webcam;
    private ExecutorService executor;
    private volatile boolean running = true;

    public WebCamComponent(MainMenu mainTargetWindow) {
        this.mainTargetWindow = mainTargetWindow;
//        setTitle("Swing Webcam 體感控制");
//        setSize(1024, 768);
//        setDefaultCloseOperation(EXIT_ON_CLOSE);

        //cameraLabel = new JLabel();
        //add(cameraLabel);

        java.util.List<Webcam> webcamList = Webcam.getWebcams();
        webcamList.stream().forEach(webcam->{System.out.println("webcam.getName()="+webcam.getName());});


        this.webcam = Webcam.getDefault();
        webcam.setViewSize(new Dimension(640, 480));
        webcam.open();

//        executor = Executors.newSingleThreadExecutor();
//        executor.submit(this.runCameraLoop(mainTargetWindow));
    }

    public void runCameraLoop(Graphics2D gc) {
        OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
        Java2DFrameConverter java2DConverter = new Java2DFrameConverter();

            BufferedImage image = webcam.getImage();
            if (image == null) return;

            // 轉換 BufferedImage 為 Mat
            Frame frame = java2DConverter.convert(image);
            Mat mat = converter.convert(frame);
            //mat = mat.reshape(1);mat.cols(1);
            //mat = mat.adjustROI(1,10, 40, 80);

            // 進行體感偵測 (例如偵測特定顏色物體作為控制器)
            //Point detectedPoint = detectColoredObject(mat);

            // 在畫面標記偵測到的位置
            //if (detectedPoint != null) {
            //    Imgproc.circle(mat, detectedPoint, 20, Scalar.GREEN, 4, Imgproc.LINE_8, 0);
            //    performGameAction(detectedPoint);
            //}

            // 顯示處理後的畫面
            BufferedImage img = java2DConverter.convert(converter.convert(mat));
            gc.drawImage(img, 10,30, 100, 100,null);
//            ImageIcon icon = new ImageIcon(java2DConverter.convert(converter.convert(mat)));
//            cameraLabel.setBackground(Color.black);
//            cameraLabel.setIcon(icon);
            StringBuilder sb = new StringBuilder();
            sb.append("frame.audioChannels="+frame.audioChannels+"\r\n");
            sb.append("frame.imageChannels="+frame.imageChannels+"\r\n");
            sb.append("frame.imageDepth="+frame.imageDepth+"\r\n");
            sb.append("frame.keyFrame="+frame.keyFrame+"\r\n");
            sb.append("frame.sampleRate="+frame.sampleRate+"\r\n");
            sb.append("frame.type="+frame.type+"\r\n");
//            cameraLabel.setText(sb.toString());
//            cameraLabel.setForeground(Color.black);
            //System.out.println(sb.toString());


//            try {
//                Thread.sleep(30);
//            } catch (InterruptedException e) {
//                running = false;
//                e.printStackTrace();
//            } catch(Exception e){
//                e.printStackTrace();
//            }
    }

//    // 偵測特定顏色物件作為體感控制 (範例以紅色為例)
//    private Point detectColoredObject(Mat mat) {
//        Mat hsvMat = new Mat();
//        Imgproc.cvtColor(mat, hsvMat, Imgproc.COLOR_BGR2HSV);
//
//        // 紅色區域 HSV 範圍 (可依需求調整)
//        Scalar lowerRed = new Scalar(0, 100, 100, 0);
//        Scalar upperRed = new Scalar(10, 255, 255, 0);
//        Mat mask = new Mat();
//        Core.inRange(hsvMat, lowerRed, upperRed, mask);
//
//        Moments moments = Imgproc.moments(mask, true);
//        if (moments.m00() > 5000) {
//            int x = (int) (moments.m10() / moments.m00());
//            int y = (int) (moments.m01() / moments.m00());
//            return new Point(x, y);
//        }
//        return null;
//    }

    // 實作遊戲互動 (視需求設計，例如點擊、移動等)
//    private void performGameAction(Point point) {
//        // 將點位資訊轉換成遊戲的互動指令
//        System.out.println("Detected action at: " + point.x() + ", " + point.y());
//    }

    public void close() {
        running = false;
        executor.shutdown();
        webcam.close();
    }

//    public static void main(String[] args) {
//
//        SwingUtilities.invokeLater(() -> {
//            new WebCamComponent().setVisible(true);
//        });
//    }


}


