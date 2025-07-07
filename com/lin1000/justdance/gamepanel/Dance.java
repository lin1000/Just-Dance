package com.lin1000.justdance.gamepanel;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.Optional;
import javax.swing.*;


import com.github.strikerx3.jxinput.XInputDevice;
import com.github.strikerx3.jxinput.listener.XInputDeviceListener;
import com.lin1000.justdance.XInputDevice.DanceKeyboardDeviceListener;
import com.lin1000.justdance.XInputDevice.DanceXInputDeviceListener;
import com.lin1000.justdance.beats.ArrowsProducer;
import com.lin1000.justdance.beats.Arrow;
import com.lin1000.justdance.beats.Beat;
import com.lin1000.justdance.controller.ConditionController;
import com.lin1000.justdance.controller.SoundController;
import com.lin1000.justdance.gamepanel.effect.EffectManager;
import com.lin1000.justdance.song.Song;

public class Dance extends JWindow
{
    //Windows variable
    private Window window;
    private boolean isSuperPaint = true;
    private Project project = null;

    //搖捍控制箭頭布林值，使用在Gui反應
    public boolean direct[] = new boolean[4];

    //Joystick Device passed into Main Menu
    private XInputDevice xInputDevice = null;

    //Joystick listener cache
    XInputDeviceListener xInputDeviceListener = null;

    //paint要用到的
    public Dimension dim;
    public Image buffer;
    public Graphics2D gc;
    //很多圖片
    Image image_left;
    Image image_leftfill;
    Image image_up;
    Image image_upfill;
    Image image_down;
    Image image_downfill;
    Image image_right;
    Image image_rightfill;
    Image arrow[] = new Image[4];
    Image image_leftman;


    //產生箭頭的class producer
    public ArrowsProducer producer;
    //情況控制中心
    public ConditionController conditionControl;
    //音樂控制中心
    public SoundController soundController;

    //Game Area Offset
    public final int g_off_x = 450; // 遊戲區域X偏移量
    public final int g_off_y = 0; // 遊戲區域Y偏移量
    //控制上排箭頭的位置
    public int arrow_y_position = 50;
    //public int width = 1280, height = 720;
    public int width = 2560, height = 1440;
    private final int dropSpeed = 300; // 每秒移動300像素
    private final int judgeLineY = 500; // 判定線Y座標

    //
    public Song song;

    //歌曲參數
    //music曲目值,移動速度值
    public int music;
    public int y_movement;
    public int BPM;

    //GAMEOVER專門會用到的繪圖參數
    public int gameover_left = 0;
    public int gameover_right = 500;

    //生命LIFE顯示區塊
    public int life_x = 20;
    public int life_y = 690;

    //Game Special Effects
    public final EffectManager effectManager = new EffectManager(g_off_x,g_off_y);

    //Audio Analysis Visualizer
    private static final int DISPLAY_SECONDS = 5;

//    //Audio Frequency Analysis Visualizer
//    public Thread audioVisualizerThread = null;
//    private static final int BINS = 2048;
//    private static final int HOP_SIZE = BINS/2;
//    private double[] lastMagnitudes = new double[BINS / 2];
//    private double[] currentMagnitudes = new double[BINS / 2];
//    private double AFFlux = 0;
//    public boolean isListening =  false;

    private DecimalFormat AFTimeFormat = new DecimalFormat("0.0");

    //constructor傳入值為曲目
    public Dance(Project project, Song song, int whichmusic, int y_movement, int BPM, XInputDevice xInputDevice, SoundController soundController, GraphicsDevice activeScreen) {
        super(project);
        this.project = project;
        //砍曲信息及Beats
        this.song = song;
        //歌曲參數
        this.music = whichmusic;
        this.y_movement = y_movement;
        this.BPM = BPM;
        System.out.println("==Dance Constructor==");
        System.out.println("song.getName()=" + song.getName());
        System.out.println("song.getBeats().size()=" + song.getBeats().size());
        System.out.println("whichmusic=" + whichmusic);
        System.out.println("y_movement=" + y_movement);
        System.out.println("BPM=" + BPM);

        //JWindow
        window = this;
        if (activeScreen != null) {
            activeScreen.setFullScreenWindow(this);
            Rectangle bounds = activeScreen.getDefaultConfiguration().getBounds();
            int x = bounds.x + (bounds.width - this.getWidth()) / 2;
            int y = bounds.y + (bounds.height - this.getHeight()) / 2;
            bounds.setLocation(x,y);
            this.setBounds(bounds);
            width =  this.getWidth();
            height = this.getHeight();
            life_x = 20;
            System.out.println("height="+height);
            System.out.println("bounds.getHeight()="+bounds.getHeight());
            System.out.println("life_y="+life_y);
            life_y = Math.min(life_y,height - 98);
            gameover_left = 0;
            gameover_right = width;
            System.out.println("bounds.width="+ bounds.width);
            System.out.println("bounds.height="+bounds.height);
            System.out.println("this.getWidth()="+ this.getWidth());
            System.out.println("this.getHeight()="+ this.getHeight());
            System.out.println("bounds.x="+ bounds.x);
            System.out.println("bounds.y="+ bounds.y);

        } else {
            // 沒有第二螢幕就顯示在主螢幕中央
            this.setLocationRelativeTo(null);
            setSize(1024,768);
            width = 1024;
            height = 768;
            life_x = 20;
            life_y = 670;
            activeScreen.setFullScreenWindow(this);
        }

        // 加上 KeyListener（需設定 focusable）
        this.setFocusable(true);
        this.addKeyListener(new DanceKeyboardDeviceListener(this));
        // 設定視窗屬性
        window.setVisible(true);
        window.requestFocus();
        getContentPane().setBackground(Color.white);

        //初始化情況控制中心 (must before window visible paint or it will not work)
        conditionControl = new ConditionController(this);
        conditionControl.setCondition(7);

        //double buffering
        dim = getSize();
        System.out.println("dim.width=" + dim.width);
        System.out.println("dim.height=" + dim.height);

        //loading image
        loadImage();

        //初始化按鈕狀態
        direct[0] = false;
        direct[1] = false;
        direct[2] = false;
        direct[3] = false;

        //初始化搖捍setup joystick
        this.xInputDevice = xInputDevice;
        if (xInputDevice != null) {
            // The SimpleXInputDeviceListener allows us to implement only the methods we actually need
            this.xInputDeviceListener = new DanceXInputDeviceListener(this);

            //add listener
            xInputDevice.addListener(xInputDeviceListener);
        } else {
            System.err.println("System have no input devices, please use keyboard to play");
           // throw new RuntimeException("JXInputDevice is null");
        }

        System.err.println(" this.BPM=" +this.BPM);
        producer = new ArrowsProducer(30, 130, 230, 330, this.BPM);//produce是一個thread會產生箭頭喔，BPM歌曲參數節拍數

        //Setting up and start counting the rhythm nanos
        this.soundController = soundController;

//        //usually not triggered unless loading music has some issues.
//        while (!this.soundController.getStatus()) {
//            System.out.println("loading music");
//        }


//        //Enable listening to audio input
//        audioVisualizerThread = new Thread(this::startListening);
//        audioVisualizerThread.start();
    }
        
        
    /*    public void  initDevice()
        {
                int cnf=JXInputManager.getNumberOfDevices();
                if(cnf!=0)
                {
                        JXInputDevice dev = JXInputManager.getJXInputDevice(0);

                                        //設定取消鍵反應
                                        new ButtonListener(dev.getButton(2))
                                        {                                                
                                                public void changed(JXInputButtonEvent ev) 
                                                {                                                                                                        
                                                     	
                                                        	if(ev.getButton().getState())//true
                                                        	{
                                                        		if(conditionControl.getGameOver())
                                                        		{
                                                         			conditionControl.setCondition(5);//5代表exit
                                                         		}
                                                               	}
                                                        	                                                                                                                                                                        
                                                        //repaint();
        
                                                }        
                                        }; 
                                        //設定esc反應
                                        new ButtonListener(dev.getButton(8))
                                        {                                                
                                                public void changed(JXInputButtonEvent ev) 
                                                {                                                                                                        
                                                     	
                                                        	if(ev.getButton().getState())//true
                                                        	{
                                                        		
                                                         			conditionControl.setCondition(5);//5代表exit
                                                              	}
                                                        	                                                                                                                                                                        
                                                        //repaint();
        
                                                }        
                                        }; 
                                        
                                        //設定確定鍵反應
                                        new ButtonListener(dev.getButton(1))
                                        {                                                
                                                public void changed(JXInputButtonEvent ev) 
                                                {                                                                                                        
                                                     	
                                                        	if(ev.getButton().getState())//true
                                                        	{
                                                        		if(conditionControl.getGameOver())
                                                        		{
                                                         			conditionControl.setCondition(6);//6代表playagain
                                                         		}
                                                               	}
                                                        	                                                                                                                                                                        
                                                        //repaint();
        
                                                }        
                                        }; 
                                        //alternative;
                                        //JXInputAxisEventListener listen =(JXInputAxisEventListener) new AxisListener(dev.getAxis(i));
                                        //JXInputEventManager.addListener(listen,dev.getAxis(i));
                                //}
                        //}
                                                
                }
        }*/

    public int getWaveX() {
        return g_off_x;
    }

    public int getWaveY() {
        return height - getWaveH();
    }

    public int getWaveW() {
        return width - g_off_x;
    }

    public int getWaveH() {
        return 150;
    }


    public void update(Graphics g)
        {
                System.out.println("update before paint");
                paint(g);
        }

        public void paint(Graphics g) 
        {
            long nowNanos = 0;
            double elapsedSeconds = 0;
            if(soundController!= null) {
                nowNanos = System.nanoTime() ;
                elapsedSeconds = (nowNanos - soundController.getStartTimeNanos()) / 1_000_000_000.0;
//                System.out.print("nowMicros=" + System.currentTimeMillis());
//                System.out.print(",nowNanos=" + nowNanos);
//                System.out.print(",nowNanos=" + nowNanos);
//                System.out.println(", elapsedSeconds=" + elapsedSeconds);

                // screen control logic 20250510
                try {
                    int removecondition = 0;
                    //while (true) {
                    if (xInputDevice!=null && xInputDevice.poll()) {
                        // 輪詢控制器狀態，觸發事件
                        DanceXInputDeviceListener.calculateAxis(xInputDevice);
                    }

                    //定時把字幕消除
                    if ((removecondition %= 200) == 0) {
                        //conditionControl.setCondition(7);
                    }
                    removecondition++;

                    //tohandle:
                    producer.move(conditionControl, this.y_movement);//歌曲參數y_movement
                    //tohandle:repaint();



                    //}

//                if (device.poll()) {
//                    // 輪詢控制器狀態，觸發事件
//                    com.lin1000.justdance.XInputDevice.DanceXInputDeviceListener.calculateAxis(device);
//                }

//                while (!(conditionControl.getExit())) {
//                    if (device.poll()) {
//                        // 輪詢控制器狀態，觸發事件
//                        com.lin1000.justdance.XInputDevice.DanceXInputDeviceListener.calculateAxis(device);
//                    }
//                    Thread.sleep(20);
//                    repaint();
//
//                } //離開此歌
                    //結束所有音樂
//                this.soundController.stop_all();
                } catch (Exception e) {
                    //throw new RuntimeException(e);
                    e.printStackTrace();
                }

                //double buffering of entire screen
                //dim = getSize();
                buffer = createImage(width, height);
                gc = (Graphics2D) buffer.getGraphics();
                gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                gc.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

    //            catch (java.lang.InterruptedException e) {
    //            }

                try {
                    //如果叫了super.paint畫面會閃爍,因為super.paint()會去執行清除畫面的動作
                    if (isSuperPaint) {
                        super.paint(g);
                        isSuperPaint = false;
                    }

                    if (!(conditionControl.getGameOver())) {

                    //-- clear background -->
                    gc.setColor(Color.white);
                    gc.fillRect(0, 0, width, height);
                    //-- clear background -->

                    //-- 箭頭反應區 -->
                    if (!this.direct[0]) gc.drawImage(image_left, g_off_x+30, g_off_y+arrow_y_position, this);
                    else gc.drawImage(image_leftfill, g_off_x+30, g_off_y+arrow_y_position, this);
                    if (!this.direct[1]) gc.drawImage(image_down, g_off_x+130, g_off_y+arrow_y_position, this);
                    else gc.drawImage(image_downfill, g_off_x+130, g_off_y+arrow_y_position, this);
                    if (!this.direct[2]) gc.drawImage(image_up, g_off_x+230, g_off_y+arrow_y_position, this);
                    else gc.drawImage(image_upfill, g_off_x+230, g_off_y+arrow_y_position, this);
                    if (!this.direct[3]) gc.drawImage(image_right, g_off_x+330, g_off_y+arrow_y_position, this);
                    else gc.drawImage(image_rightfill, g_off_x+330, g_off_y+arrow_y_position, this);
                    //-- 箭頭反應區 -->


                        gc.setColor(Color.black);
                        //把畫布上所有的箭頭畫出來，共四個vec[0,1,2,3]
                        for (int vec_index = 0; vec_index < 4; vec_index++) {
                            for (int element_index = 0; element_index < producer.vec[vec_index].size(); element_index++) {
                                Arrow myarrow = (Arrow) producer.vec[vec_index].get(element_index);
                                gc.drawImage(arrow[vec_index], g_off_x+myarrow.x, g_off_y+myarrow.y, null);
                            }
                        }

                        //反應出judgementor--conditionControl情況控制中心的情況
                        //gc.setFont(new Font("verdana", Font.PLAIN, 80));
                        if (conditionControl.getCondition() == 0)
                            effectManager.addTextFlashEffect("PERFECT",g_off_x+((width-g_off_x)/6), g_off_y+(height/2));
                        if (conditionControl.getCondition() == 1)
                            effectManager.addTextFlashEffect(conditionControl.getperfectCounter() + "GOOD",g_off_x+((width-g_off_x)/6), g_off_y+(height/2));
                        //gc.drawString(conditionControl.getperfectCounter() + "COMBO", 120, conditionControl.getY());
                        if (conditionControl.getCondition() == 2)
                            effectManager.addTextFlashEffect("GOOD",g_off_x+((width-g_off_x)/6), g_off_y+(height/2));
                            //gc.drawString("  GOOD", 120, conditionControl.getY());
                        if (conditionControl.getCondition() == 3) {
                            effectManager.addTextFlashEffect("Miss", g_off_x + ((width - g_off_x) / 6), g_off_y + (height / 2));
                            conditionControl.setCondition(7);
                        }
                        //gc.drawString("  Miss", 120, conditionControl.getY());

                        //分數顯示
                        gc.setFont(new Font("verdana", Font.PLAIN, 18));
                        gc.drawString("SCORE:", g_off_x+20, g_off_y+20);
                        gc.drawString(conditionControl.getScore() + "", g_off_x+100, g_off_y+20);

                        //FPS Info Block
                        int fps_x = g_off_x+300;
                        int fps_y = g_off_y+20;
                        int fps_w = 150;
                        int fps_h = 20;
                        gc.setColor(Color.white);
                        gc.fillRect(fps_x,fps_y,fps_w,fps_h);
                        gc.setColor(Color.blue);
                        gc.drawString(" Time:", fps_x, fps_y);
                        gc.drawString(String.format("%.2f s", elapsedSeconds) , fps_x+80, fps_y);
                        gc.setColor(Color.BLACK);
                        gc.drawRect(1,1,width-2, height-2);
                        //畫左邊的人
                        gc.drawImage(image_leftman, 0, 0, 360, 669, null);
                        //Special Effects
                        effectManager.drawAll((Graphics2D) gc);

                    } else {
                        gc.setColor(Color.black);
                        gc.fillRect(0, 0, gameover_left, height);
                        gc.fillRect(gameover_right, 0, width, height);
                        gc.setFont(new Font("標階體", Font.PLAIN, 26));
                        gc.drawString("GAME OVER", g_off_x+155, g_off_y+(height / 2) - 20);
                        gc.drawString("(A) DANCE!!!  (X) EXIT", g_off_x+110, g_off_y+(height / 2) + 15);
//                        gameover_left = 1;
//                        gameover_left *= 2.8;
//                        gameover_right /= 1.1;
//                        if (gameover_left >= 480 || gameover_right <= 50) {
//                            gameover_left = 500;
//                            gameover_right = 0;
//                            gc.setColor(Color.white);
//                            gc.setFont(new Font("標階體", Font.PLAIN, 26));
//                            gc.drawString("GAME OVER", g_off_x+155, g_off_y+(height / 2) - 20);
//                            gc.drawString("(A) DANCE!!!  (X) EXIT", g_off_x+110, g_off_y+(height / 2) + 15);
//                        }
                    }

                    /**
                     * audio analysis visualizer - INFO
                     */
                    // 顯示時間與樣本數資訊
                    gc.setColor(Color.black);
                    gc.drawString("Time: " + AFTimeFormat.format(soundController.currentSec) + "s / " + AFTimeFormat.format(soundController.durationSec) + "s", 10, 20);
                    gc.drawString("Sample: " + soundController.currentPlaybackSample + " / " + song.getSamples().length, 10, 40);
                    gc.drawString("Time: " + AFTimeFormat.format(soundController.currentSecSourceDataLine) + "s / " + AFTimeFormat.format(soundController.durationSec) + "s", 10, 60);
                    gc.drawString("Sample: " + soundController.currentLongFramePositionSourceDataLine + " / " + song.getSamples().length, 10, 80);
                    gc.drawString("available: " + soundController.currentAvailableSourceDataLine  , 10, 100);
                    gc.drawString("currentBufferSize: " + soundController.currentBufferSize  , 10, 120);

                    /**
                     * Audio analysis visualizer - v2 (paint in wave block defined by waveX,Y,W,H)
                     * int waveX = x of wave form
                     * int waveY = y of wave form
                     * int waveW = wave form area width
                     * int waveH = wave form area height
                     */
                    int waveX = getWaveX();
                    int waveY = getWaveY();
                    int waveW = getWaveW();
                    int waveH = getWaveH();
//                    System.out.println("waveX=" + waveX);
//                    System.out.println("waveY=" + waveY);
//                    System.out.println("waveW=" + waveW);
//                    System.out.println("waveH=" + waveH);

                    // 畫波形（避免跳動：預先平均 + 定點計算）
                    // Normal Wave Color
                    gc.setColor(Color.green);
                    int middle = waveH / 2;
                    int totalVisibleSamples = (int)(DISPLAY_SECONDS * song.getSampleRate());
                    int startSample = Math.max(0, soundController.currentPlaybackSample - totalVisibleSamples/2);
                    int samplesPerPixel = Math.max(1, totalVisibleSamples / waveW); //will calculate how many samples should be considered in 1 X pixel
                    int widthPerBar = 20;
                    System.out.println("middle=" + middle);
                    System.out.println("totalVisibleSamples=" + totalVisibleSamples);
                    System.out.println("totalVisibleSamples/2=" + totalVisibleSamples/2);
                    System.out.println("soundController.currentPlaybackSample=" + soundController.currentPlaybackSample);
                    System.out.println("startSample=" + startSample);
                    System.out.println("samplesPerPixel=" + samplesPerPixel);

                    int beatSuppressionBar = 0;
                    for (int x = 0; x < waveW; x+=widthPerBar) { //FOR EACH POINT(BAR) IN SCREEN
                        int index = startSample + x * samplesPerPixel;
                        if (index >= song.getSamples().length) break;
                        double avg = 0;
                        for (int k = 0; k < samplesPerPixel*widthPerBar && index + k < song.getSamples().length; k++) {//FOR EACH GROUPING OF Samples [k0,k1...kn]
                            avg += song.getSamples()[index + k]; //SUM UP ALL SAMPLES(NORMALIZED) IN THIS GROUP
                        }
                        avg = (double)(avg/(double)samplesPerPixel*widthPerBar);
                        int y = (int) (avg * middle);
                        System.out.println("y="+y);
                        gc.fillRect(waveX+x, waveY+middle, widthPerBar, y);
                        gc.setColor(Color.green);

                        if(x <= waveW/2 && beatSuppressionBar==0){
                            Optional<Beat> signalBeat = song.getBeats().stream().filter(beat-> beat.getFrameIndex()==index || Math.abs(beat.getFrameIndex() -index) <= samplesPerPixel*widthPerBar).findFirst();
                            gc.setColor(Color.red);
                            if(signalBeat.isPresent()){
                                //gc.drawString(String.valueOf(signalBeat.get().getSingalStrength()) + ", frameIndex:" + signalBeat.get().getFrameIndex(), waveX+x, waveY);
                                gc.drawOval(waveX+x, waveY, 10, 10);
                                beatSuppressionBar = 10;
                            }
                        } else if(x > waveW/2 && beatSuppressionBar==0){
                            Optional<Beat> signalBeat = song.getBeats().stream().filter(beat-> beat.getFrameIndex()==index || Math.abs(beat.getFrameIndex() -index) <= samplesPerPixel*widthPerBar).findFirst();
                            gc.setColor(Color.GRAY);
                            if(signalBeat.isPresent()){
                                //gc.drawString(String.valueOf(signalBeat.get().getSingalStrength()) + ", frameIndex:" + signalBeat.get().getFrameIndex(), waveX+x, waveY);
                                gc.drawOval(waveX+x, waveY, 10, 10);
                            }
                            beatSuppressionBar = 10;
                        }
                        beatSuppressionBar--;
                        if(beatSuppressionBar<0) beatSuppressionBar=0;
                    }

//                    for (int x = 0; x < waveW; x++) { //FOR EACH POINT(BAR) IN SCREEN
//                        int index = startSample + x * samplesPerPixel;
//                        if (index >= song.getSamples().length) break;
//                        double avg = 0;
//                        for (int k = 0; k < samplesPerPixel && index + k < song.getSamples().length; k++) {//FOR EACH GROUPING OF Samples [k0,k1...kn]
//                            avg += song.getSamples()[index + k]; //SUM UP ALL SAMPLES(NORMALIZED) IN THIS GROUP
//                        }
//                        avg = (double)(avg/(double)samplesPerPixel);
//                        int y = (int) (avg * middle);
//                        gc.drawLine(waveX+x, waveY+middle, waveX+x, waveY+middle - y);
//                        gc.setColor(Color.green);
//
//                        if(x <= waveW/2 && beatSuppressionPixel==0){
//                            Optional<Beat> signalBeat = song.getBeats().stream().filter(beat-> beat.getFrameIndex()==index || Math.abs(beat.getFrameIndex() -index) <= samplesPerPixel).findFirst();
//                            gc.setColor(Color.red);
//                            if(signalBeat.isPresent()){
//                                //gc.drawString(String.valueOf(signalBeat.get().getSingalStrength()) + ", frameIndex:" + signalBeat.get().getFrameIndex(), waveX+x, waveY);
//                                gc.drawOval(waveX+x, waveY, 10, 10);
//                                beatSuppressionPixel = 50;
//                            }
//                        } else if(x > waveW/2 && beatSuppressionPixel==0){
//                            Optional<Beat> signalBeat = song.getBeats().stream().filter(beat-> beat.getFrameIndex()==index || Math.abs(beat.getFrameIndex() -index) <= samplesPerPixel).findFirst();
//                            gc.setColor(Color.GRAY);
//                            if(signalBeat.isPresent()){
//                                //gc.drawString(String.valueOf(signalBeat.get().getSingalStrength()) + ", frameIndex:" + signalBeat.get().getFrameIndex(), waveX+x, waveY);
//                                gc.drawOval(waveX+x, waveY, 10, 10);
//                            }
//                            beatSuppressionPixel = 50;
//                        }
//                        beatSuppressionPixel--;
//                        if(beatSuppressionPixel<0) beatSuppressionPixel=0;
//                    }

                    // Progress Bar Time Mark and its color 時間軸刻度
                    gc.setColor(Color.gray);
                    for (int i = 0; i <= DISPLAY_SECONDS; i++) {
                        int x = (int) (i * waveW / (double) DISPLAY_SECONDS); //calculate x position of each second mark
                        double timeMark = soundController.currentSecSourceDataLine + i - DISPLAY_SECONDS / 2.0;

                        // check if it is (in the beginning before the current second reach 1/2 of DISPLAY_SECONDS
                        if (soundController.currentSecSourceDataLine <= (double) DISPLAY_SECONDS/2){
                            timeMark =  i;
                            gc.drawLine(waveX + x, waveY+waveH - 15, waveX + x, waveY+waveH);
                            gc.drawString(AFTimeFormat.format(timeMark) + "s", waveX +x + 2, waveY+waveH -2 );
                        }else{
                            timeMark = soundController.currentSecSourceDataLine + i - DISPLAY_SECONDS / 2.0;
                        }

                        //Draw the TimeMark (line and string) in under wave block.
                        if (timeMark >= 0 && timeMark <= soundController.durationSec) {
                            gc.drawLine(waveX + x, waveY+waveH - 15, waveX + x, waveY+waveH);
                            gc.drawString(AFTimeFormat.format(timeMark) + "s", waveX +x + 2, waveY+waveH -2 );
                        }
                    }

                    // Progress Bar Color
                    gc.setColor(Color.red);
                    // Play Progress Bar (in the beginning before the current second reach 1/2 of DISPLAY_SECONDS
                    if(soundController.currentSecSourceDataLine <= (double) DISPLAY_SECONDS /2){
                        int currentX = (int) (soundController.currentSecSourceDataLine * waveW / (double) DISPLAY_SECONDS);
                        gc.drawLine(waveX+currentX, waveY, waveX+currentX, waveY+waveH);
                        gc.drawString("" + AFTimeFormat.format(soundController.currentSecSourceDataLine) + "s / "    , currentX, waveY+waveH-30);

                    }else{// Play Progress Bar (in the middle after the current second reached 1/2 of the DISPLAY_SECONDS
                        //DRAW THE PROGRESS LINE in the middle of wave block
                        int currentX = (int) (waveX + (waveW / 2.0));
                        gc.drawLine(currentX, waveY, currentX, waveY+waveH);
                        //gc.drawString("" + AFTimeFormat.format(soundController.currentSecSourceDataLine) + "s / "    , currentX, waveY+waveH-30);
                    }

                    /**
                     * audio frequency visualizer - BEGIN
                     */
//                    int AFWidth = width;
//                    int AFHeight = 150;
//                    int AFx = g_off_x;
//                    int AFy = g_off_y + height - AFHeight;
//                    gc.setColor(Color.green);
//                    for (int i = 0; i < soundController.BINS / 2; i++) {
//                        int barHeight = (int) (soundController.currentMagnitudes[i] * AFHeight);
//                        gc.fillRect(i * AFWidth / (soundController.BINS / 2), AFy+barHeight, AFWidth / (soundController.BINS / 2), AFy+AFHeight - barHeight);
//                    }
//
//                    gc.setColor(Color.red);
//                    int fluxBar = (int) (soundController.AFFluxBeatStrength * 20);
//                    gc.fillRect(AFWidth - 50, AFHeight - fluxBar, 40, fluxBar);
//                    gc.drawString("Flux: " + String.format("%.3f", soundController.AFFluxBeatStrength), AFWidth - 140, 20);

                    /**
                     * audio frequency visualizer - END
                     */

                    //生命顯示
//                        gc.drawString("LIFE", g_off_x+life_x, g_off_y+life_y);
//                        gc.setColor(Color.red);
//                        gc.drawRect(g_off_x+life_x, g_off_y+life_y + 10, 300, 25);
//                        gc.fillRect(g_off_x+life_x, g_off_y+life_y+10, conditionControl.getLife() * 3, 25);
                    gc.drawString("LIFE", life_x, g_off_y+life_y);
                    gc.setColor(Color.red);
                    gc.drawRect(life_x, g_off_y+life_y + 10, 300, 25);
                    gc.fillRect(life_x, g_off_y+life_y+10, conditionControl.getLife() * 3, 25);


                    g.drawImage(buffer, 0, 0, width, height, this);

                } catch (java.lang.NullPointerException f) {
                    f.printStackTrace();
                }
            }else {
                System.err.println("SoundController is null, cannot paint.");
            }
        }
        
        //loadImage what paint need
        public void loadImage()
        {
                Toolkit kit=Toolkit.getDefaultToolkit();
                image_left=kit.getImage("img/left.gif");
                image_leftfill=kit.getImage("img/leftfill.gif");
                image_up=kit.getImage("img/up.gif");
                image_upfill=kit.getImage("img/upfill.gif");
                image_down=kit.getImage("img/down.gif");
                image_downfill=kit.getImage("img/downfill.gif");
                image_right=kit.getImage("img/right.gif");
                image_rightfill=kit.getImage("img/rightfill.gif");
                
                image_leftman=kit.getImage("img/leftman.jpg");
                
                arrow[0]=kit.getImage("img/jumpleft.gif");
                arrow[1]=kit.getImage("img/jumpdown.gif");
                arrow[2]=kit.getImage("img/jumpup.gif");
                arrow[3]=kit.getImage("img/jumpright.gif");
                
        }
//
//    private void startListening() {
//        AudioFormat format = null;
//        TargetDataLine line = null;
//        try {
//            format = new AudioFormat(44100, 16, 1, true, false);
//            line = AudioSystem.getTargetDataLine(format);
//            line.open(format, BINS * 2);
//            line.start();
//            isListening = true;
//
//            byte[] buffer = new byte[BINS * 2];
//            double[] real = new double[BINS];
//            double[] imag = new double[BINS];
//
//            while (isListening) {
//                int bytesRead = line.read(buffer, 0, buffer.length);
//                if (bytesRead < BINS * 2) continue;
//
//                for (int i = 0, j = 0; i < BINS; i++, j += 2) {
//                    int low = buffer[j] & 0xFF;
//                    int high = buffer[j + 1];
//                    int sample = (high << 8) | low;
//                    real[i] = sample / 32768.0;
//                    imag[i] = 0;
//                }
//
//                FFT.fft(real, imag);
//                AFFlux = 0;
//                for (int i = 0; i < BINS / 2; i++) {
//                    currentMagnitudes[i] = Math.sqrt(real[i] * real[i] + imag[i] * imag[i]);
//                    double diff = currentMagnitudes[i] - lastMagnitudes[i];
//                    if (diff > 0) AFFlux += diff;
//                    lastMagnitudes[i] = currentMagnitudes[i];
//                }
//                repaint();
//            }
//
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        } finally {
//            isListening = false;
//            if (audioVisualizerThread != null) {
//                try {
//                    line.drain();
//                    line.close();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//                audioVisualizerThread = null;
//            }
//        }
//    }

        public Project getProject() {
            return project;
        }

        public void removeInputDeviceListener() {
            //remove xInputDevice listener when xInputDevice is available.
            if (xInputDevice!=null) {
                xInputDevice.removeListener(xInputDeviceListener);
            }
            this.removeAll();
        }

}





