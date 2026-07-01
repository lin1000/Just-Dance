package com.lin1000.justdance.gamepanel;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.Receiver;
import javax.sound.midi.Transmitter;
import javax.swing.*;

import com.github.strikerx3.jxinput.XInputDevice;
import com.github.strikerx3.jxinput.listener.XInputDeviceListener;
import com.lin1000.justdance.Project;
import com.lin1000.justdance.gamepanel.componentpanel.KeyboardControllerComponent;
import com.lin1000.justdance.gamepanel.componentpanel.MidiControllerComponent;
import com.lin1000.justdance.gamepanel.inputdevice.MainMenuMidiDeviceListener;
import com.lin1000.justdance.input.device.JXInputDeviceWatcher;
import com.lin1000.justdance.gamepanel.inputdevice.MainMenuKeyboardDeviceListener;
import com.lin1000.justdance.gamepanel.inputdevice.MainMenuXInputDeviceListener;
import com.lin1000.justdance.controller.SoundController;
import com.lin1000.justdance.gamepanel.action.MainMenuAction;
import com.lin1000.justdance.gamepanel.componentpanel.WebCamComponent;
import com.lin1000.justdance.gamepanel.componentpanel.XBoxControllerComponent;
import com.lin1000.justdance.input.Input;
import com.lin1000.justdance.input.device.MidiDeviceWatcher;
import com.lin1000.justdance.player.JXInputPlayer;
import com.lin1000.justdance.player.MidiPlayer;
import com.lin1000.justdance.song.Song;
import com.lin1000.justdance.ddd.*;

public class MainMenu extends JWindow
{
        //Upstream project vairable
        private Project project = null;

        //Windows variable
        private Window window = null;

        //Joystick Device passed into Main Menu
        private XInputDevice xInputDevice = null;

        //Joystick listener cache
        XInputDeviceListener xInputDeviceListener = null;

        //Midi Device passed into Dance
        private MidiDevice midiDevice = null;
        //Midi listener cache
        Receiver midiDeviceListener = null;

        //paint
        private Dimension dim;
        private Image buffer;
        private Graphics2D gc;
        //
        Image mark;
        Image menutitle;
        Image background;
        Image option[]=new Image[4];
        Image optionSelected[]=new Image[4];
        public static int musicOptionIndex =0; // 0,1,2,3

        //XBox Controller Component
        public XBoxControllerComponent xBoxControllerComponent = new XBoxControllerComponent(30,380);

        //Keyboard Controller Component
        public KeyboardControllerComponent keyboardControllerComponent = new KeyboardControllerComponent(30, 560);

        //Midi Controller Component
        public MidiControllerComponent midiControllerComponent = new MidiControllerComponent(200,470);

        //Webcam variable
        public WebCamComponent webCamComponent = null;

        //Sound Controller
        public SoundController soundController;
                       
        //Game Main Control Flow
        public int controlFlow=1; //1,2,3,4(exit)
        
        //Which Music
        //y_movement
        //BPM(Beats per Minutes)
        public Song whichSong;
        private int whichmusic;
        private int y_movement;
        private int BPM;

        //Player-chosen difficulty level (scroll speed). Cycled with LEFT/RIGHT in the song
        //selection screen and carried into gameplay (see Project.run -> Dance.speedModifier).
        public final SpeedModifier speedModifier = new SpeedModifier();
        public SpeedModifier.Difficulty getSelectedDifficulty() { return speedModifier.getDifficulty(); }

        //isFirstRound
        private boolean isFirstRound = true;

        // lock object for synchronization
        public final Object pauseLock = new Object();
        public boolean pause = false;

        //Formatter
        DecimalFormat optional1Decimalformatter = new DecimalFormat("0.#");
        DecimalFormat optional2Decimalformatter = new DecimalFormat("0.##");
        DecimalFormat optional3Decimalformatter = new DecimalFormat("0.###");

        //3D Experimental
        public static ArrayList<Triangle> tris;
        public int[] dddx = new int[1];
        public int[] dddy = new int[1];
        static{
            //3D Experimental
            tris = new ArrayList<Triangle>();
            tris.add(new Triangle(new Vertex(100, 100, 100),
                    new Vertex(-100, -100, 100),
                    new Vertex(-100, 100, -100),
                    Color.WHITE));
            tris.add(new Triangle(new Vertex(100, 100, 100),
                    new Vertex(-100, -100, 100),
                    new Vertex(100, -100, -100),
                    Color.RED));
            tris.add(new Triangle(new Vertex(-100, 100, -100),
                    new Vertex(100, -100, -100),
                    new Vertex(100, 100, 100),
                    Color.GREEN));
            tris.add(new Triangle(new Vertex(-100, 100, -100),
                    new Vertex(100, -100, -100),
                    new Vertex(-100, -100, 100),
                    Color.BLUE));
        }

        public MainMenu(Project project, boolean isFirstRound, XInputDevice xInputDevice, MidiDevice midiDevice, SoundController soundController, GraphicsDevice activeScreen)
        {
            super(project);
            this.project = project;
            window = this;
            if (activeScreen != null) {
                Rectangle bounds = activeScreen.getDefaultConfiguration().getBounds();
                int x = bounds.x + (bounds.width - this.getWidth()) / 2;
                int y = bounds.y + (bounds.height - this.getHeight()) / 2;
                this.setLocation(x, y);
                this.setBounds(bounds);
                //activeScreen.setFullScreenWindow(this);
            } else {
                // 沒有第二螢幕就顯示在主螢幕中央
                this.setLocationRelativeTo(null);
                setSize(1024,768);
                activeScreen.setFullScreenWindow(this);
            }

            // Setup KeyListener（set focusable beforehand is required）
            this.setFocusable(true);
            this.addKeyListener(new MainMenuKeyboardDeviceListener(this));
            this.addMouseMotionListener(new MouseMotionListener() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    double xi = 180.0 / MainMenu.this.getWidth();
                    double yi = 180.0 / MainMenu.this.getHeight();
                    dddx[0] = (int) (e.getX() * xi);
                    dddy[0] = -(int) (e.getY() * yi);
                    System.out.println("xi="+xi+",yi="+yi);
                }
                @Override
                public void mouseMoved(MouseEvent e) {
                    double xi = 180.0 / MainMenu.this.getWidth();
                    double yi = 180.0 / MainMenu.this.getHeight();
                    dddx[0] = (int) (e.getX() * xi);
                    dddy[0] = -(int) (e.getY() * yi);
                    System.out.println("xi="+xi+",yi="+yi);
                    System.out.println("e.getX()="+e.getX()+",e.getY()="+e.getY());
                    System.out.println("dddx[0]="+dddx[0]+",dddy[0]="+dddy[0]);
                }
            });
            // 設定視窗屬性
            window.setVisible(true);
            window.requestFocusInWindow();

            //temp
        	this.isFirstRound = isFirstRound;
            getContentPane().setBackground(Color.white);

            //double buffering
            dim = getSize();
            buffer = createImage(dim.width, dim.height);
            gc = (Graphics2D) buffer.getGraphics();

            gc.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            // You can also enable antialiasing for text:

            gc.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            //loading image resource
            loadImage();

            //Sound Controller
            this.soundController = soundController;
            //setup joystick and register joystick event listener
            this.xInputDevice = xInputDevice;
            if (xInputDevice != null) {
                // The SimpleXInputDeviceListener allows us to implement only the methods we actually need
                this.xInputDeviceListener = new MainMenuXInputDeviceListener(this);
                //add listener
                xInputDevice.addListener(xInputDeviceListener);

            } else {
                System.err.println("System have no input devices, please use keyboard to play");
                //throw new RuntimeException("JXInputDevice is null");
            }

            //Initialize and setup Midi Device
            this.midiDevice = midiDevice;
            try{
                if(midiDevice != null) {
                    // The SimpleMidiDeviceListener allows us to implement only the methods we actually need
                    this.midiDeviceListener = new MainMenuMidiDeviceListener(this);
                    Transmitter transmitter = midiDevice.getTransmitter();
                    transmitter.setReceiver(this.midiDeviceListener);
                } else {
                    System.err.println("System has NO midi devices, please use computer keyboard to play");
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("System has midi devices, but cannot correctly setup. Please use computer keyboard to play");
            }


            try {
                soundController.playMainMenuSound(0);
                /**
                //after loading video
                while (true) {
                    Picture picture = null;
                    picture = project.frameGrab!=null?project.frameGrab.getNativeFrame():null;
                    if (picture == null) break;
                    BufferedImage currentFrame = AWTUtil.toBufferedImage(picture);
                    gc.drawImage(currentFrame, 0, 0, getWidth(), getHeight(), null);
                    repaint();
                    //Thread.sleep(20); // 約 30 FPS
                }**/
            } catch (Exception e) {
                e.printStackTrace();
                //skip the intro vide and continue
             }

            boolean headlessDemo = System.getenv("HEADLESS_DEMO") != null;
            if (isFirstRound)//show game landing screen for the first time
            {
                //soundControl.play_beginSound(2);
                paintInitial(0);//

                //JXInputDeviceWatcher Setup and Start
                try {
                    //Device Watcher Polling based
                    project.jXInputDeviceWatcher = new JXInputDeviceWatcher();
                    Thread.sleep(0);
                } catch (java.lang.InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    project.jXInputDeviceWatcher.setMainTargetWindow(this);
                    project.jXInputDeviceWatcher.start();
                } catch (LinkageError e) {
                    System.out.println("JXInputDeviceWatcher not available: " + e.getMessage());
                    project.jXInputDeviceWatcher = null;
                }

                //MidiDeviceWatcher Setup and Start
                try {
                    //Device Watcher Polling based
                    project.midiDeviceWatcher = new MidiDeviceWatcher();
                    Thread.sleep(0);
                } catch (java.lang.InterruptedException e) {
                    e.printStackTrace();
                }
                project.midiDeviceWatcher.setMainTargetWindow(this);
                project.midiDeviceWatcher.start();

                int paintIndex = 0;
                int frameCount = 0;
                boolean webCamUnavailable = false;
                while (controlFlow == 1) {
                    try {
                        if (xInputDevice != null && xInputDevice.poll()) {
                            //輪詢控制器狀態，觸發事件
                            MainMenuXInputDeviceListener.calculateAxis(xInputDevice);
                        }
                        paintInitial(paintIndex++);
                        Thread.sleep(16);

                        //showing camera component
                        if(webCamComponent==null && !webCamUnavailable) {
                            try {
                                webCamComponent= new WebCamComponent(this);
                            } catch (Exception e) {
                                System.err.println("WebCam not available: " + e.getMessage());
                                webCamUnavailable = true;
                            }
                        }

                        // auto-advance past splash in headless/demo mode after ~2 seconds (120 frames @ 16ms)
                        if (headlessDemo && ++frameCount > 120) {
                            controlFlow = 2;
                        }

                    } catch (java.lang.InterruptedException e) {
                        e.printStackTrace();
                    }


                    if (controlFlow == 4) // leave game directly
                        System.exit(0);

                    paintIndex %= 20;
                }
                mark = null;
            }

            if (controlFlow == 1) controlFlow = 2;

            boolean isDefaultMusic = true;
            int menuFrameCount = 0;
            while (controlFlow == 2) { // show main menu screen
                try {
                    if (xInputDevice !=null && xInputDevice.poll()) {
                        // 輪詢控制器狀態，觸發事件
                        MainMenuXInputDeviceListener.calculateAxis(xInputDevice);
                    }
                    Thread.sleep(50);
                    if (isDefaultMusic) {
                        isDefaultMusic = false;
                        Input defaultInput = new Input();
                        defaultInput.setInputType(Input.InputType.GUIDE_BUTTON);
                        defaultInput.setPressed(true);
                        MainMenuAction.getInstance().inputAction(defaultInput, this);
                    }
                    try {
                        menuscreen(musicOptionIndex);
                    } catch (Exception e) {
                        System.err.println("menuscreen error (non-fatal): " + e.getMessage());
                    }
                } catch (java.lang.InterruptedException e) {
                    e.printStackTrace();
                }

                // auto-select first song in headless/demo mode after ~3 seconds (60 frames @ 50ms)
                if (headlessDemo && ++menuFrameCount > 60) {
                    Input selectInput = new Input();
                    selectInput.setInputType(Input.InputType.A);
                    selectInput.setPressed(true);
                    MainMenuAction.getInstance().inputAction(selectInput, this);
                }
            }

            /**
            if(this.xInputDeviceListener!=null) {
                this.xInputDevice.removeListener(this.xInputDeviceListener);
            }**/
        }


    public void update(Graphics g) {
        paint(g);
    }

    public void paint(Graphics g) {
        try {
            Graphics2D gc = (Graphics2D) g;
            gc.drawImage(buffer, 0, 0, dim.width, dim.height, this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
        
        //loadImage what paint need
        public void loadImage()
        {                
                Toolkit kit=Toolkit.getDefaultToolkit();
                mark=kit.getImage("img/mark.png");
                background=kit.getImage("img/background.jpg");
                menutitle=kit.getImage("img/menutitle.jpg");

                option[0]=kit.getImage("img/option1.jpg");
                option[1]=kit.getImage("img/option2.jpg");
                option[2]=kit.getImage("img/option3.jpg");
                option[3]=kit.getImage("img/option4.jpg");
                optionSelected[0]=kit.getImage("img/option1selected.jpg");
                optionSelected[1]=kit.getImage("img/option2selected.jpg");
                optionSelected[2]=kit.getImage("img/option3selected.jpg");
                optionSelected[3]=kit.getImage("img/option4selected.jpg");
        }
        
        //paint initial screen
        public void paintInitial(int paintIndex)
        {
                //-- clear background -->
                gc.setColor( Color.black );
                gc.fillRect( 0, 0, dim.width, dim.height );
                //-- clear background -->
                gc.setFont(new Font("verdana",Font.PLAIN,10));
                gc.drawImage(mark,0,0,getWidth(), getHeight(),this);
                gc.setColor(Color.white);
                gc.drawString("resolution : "+getWidth()+ "x" + getHeight(), 10, 15);

                gc.setColor(Color.white);
                gc.setFont(new Font("verdana",Font.PLAIN,20));
                if(paintIndex > 5)  {gc.drawString("Press Start Button",550,600);}

                if(project.jXInputDeviceWatcher != null){
                    JXInputPlayer p1 = project.jXInputDeviceWatcher.getPlayer(0);
                    if(p1!=null) {
                        xBoxControllerComponent.draw(gc);
                    }
                }

                if(keyboardControllerComponent != null){
                    keyboardControllerComponent.draw(gc);
                }

                if(project.midiDeviceWatcher != null) {
                    MidiPlayer midiPlayer = project.midiDeviceWatcher.getMidiPlayer(0);
                    if (midiPlayer != null || true) {
                        midiControllerComponent.draw(gc);
                    }
                }

                if(webCamComponent!= null) webCamComponent.runCameraLoop(gc);


                //3D Experimental
                // 生成的形状以原点 (0, 0, 0) 为中心，稍后我们将围绕该点进行旋转。
                double heading = Math.toRadians(dddx[0]);
                Matrix3 headingTransform = new Matrix3(new double[]{
                        Math.cos(heading), 0, -Math.sin(heading),
                        0, 1, 0,
                        Math.sin(heading), 0, Math.cos(heading)
                });
                double pitch = Math.toRadians(dddy[0]);
                Matrix3 pitchTransform = new Matrix3(new double[]{
                        1, 0, 0,
                        0, Math.cos(pitch), Math.sin(pitch),
                        0, -Math.sin(pitch), Math.cos(pitch)
                });
                //提前进行矩阵合并
                Matrix3 transform = headingTransform.multiply(pitchTransform);

                AffineTransform originalTransform = gc.getTransform();
                gc.translate(getWidth() / 2, getHeight() / 2);
                gc.setColor(Color.WHITE);
                for (Triangle t : tris) {
                    gc.setColor(t.color);
                    Vertex v1 = transform.transform(t.v1);
                    Vertex v2 = transform.transform(t.v2);
                    Vertex v3 = transform.transform(t.v3);
                    Path2D path = new Path2D.Double();
                    path.moveTo(v1.x, v1.y);
                    path.lineTo(v2.x, v2.y);
                    path.lineTo(v3.x, v3.y);
                    path.closePath();
                    gc.draw(path);
                }
                gc.setTransform(originalTransform);
                repaint();
        }
        
        //?e?X?D???
        public void menuscreen(int musicOptionIndex)
        {
            int menuInfoX = 350;
            int menuInfoY = 80;
            int menuInfoXSelect = 313;

            this.musicOptionIndex = musicOptionIndex;
            //gc.drawImage(background, 0, 0, 1024, 768, this);
            gc.drawImage(background, 0, 0, getWidth(), getHeight(), this);
            gc.drawImage(menutitle, menuInfoX, menuInfoY, 555, 60, this);

            switch (musicOptionIndex) {
                case 0:
                    this.whichmusic = 0;//whichmusic represent which muisc has been chosen
                    this.y_movement = 15;//y_movement represent the speed of the Aarrow
                    this.BPM = 400; //BPM represent the beats per minutes of the music
                    gc.drawImage(option[1], menuInfoX, 260, 555, 60, this);
                    gc.drawImage(option[2], menuInfoX, 320, 555, 60, this);
                    gc.drawImage(option[3], menuInfoX, 380, 555, 60, this);
                    gc.drawImage(optionSelected[0], menuInfoXSelect, 195, 640, 69, this);
                    break;
                case 1:
                    this.whichmusic = 1;//whichmusic represent which muisc has been chosen
                    this.y_movement = 7;//y_movement represent the speed of the Aarrow
                    this.BPM = 120;//BPM represent the beats per minutes of the music
                    gc.drawImage(option[0], menuInfoX, 200, 555, 60, this);
                    gc.drawImage(option[2], menuInfoX, 320, 555, 60, this);
                    gc.drawImage(option[3], menuInfoX, 380, 555, 60, this);
                    gc.drawImage(optionSelected[1], menuInfoXSelect, 255, 640, 69, this);
                    break;
                case 2:
                    this.whichmusic = 2;//whichmusic represent which muisc has been chosen
                    this.y_movement = 5;//y_movement represent the speed of the Aarrow
                    this.BPM = 180;//BPM represent the beats per minutes of the music
                    gc.drawImage(option[0], menuInfoX, 200, 555, 60, this);
                    gc.drawImage(option[1], menuInfoX, 260, 555, 60, this);
                    gc.drawImage(option[3], menuInfoX, 380, 555, 60, this);
                    gc.drawImage(optionSelected[2], menuInfoXSelect, 315, 640, 69, this);
                    break;
                case 3:
                    this.whichmusic = 3;//whichmusic represent which muisc has been chosen
                    this.y_movement = 12;//y_movement represent the speed of the Aarrow
                    this.BPM = 300;//BPM represent the beats per minutes of the music
                    gc.drawImage(option[0], menuInfoX, 200, 555, 60, this);
                    gc.drawImage(option[1], menuInfoX, 260, 555, 60, this);
                    gc.drawImage(option[2], menuInfoX, 320, 555, 60, this);
                    gc.drawImage(optionSelected[3], menuInfoXSelect, 375, 640, 69, this);
                    break;
            }

            //draw song information
            int songInfoX = 400;
            int songInfoY = 500;
            int songIntoLineHeight = 23;
            if (whichSong == null) { repaint(); return; }
            gc.setColor(Color.black);
            gc.setFont(new Font("verdana", Font.ITALIC, 25));
            gc.drawString("Song Name: " + whichSong.getName(), songInfoX, songInfoY);
            gc.drawString("Song Duration: " + String.format("%02d",whichSong.getSongLengthInMinutesAndSeconds()[0]) + ":" + String.format("%02d", whichSong.getSongLengthInMinutesAndSeconds()[1]), songInfoX, songInfoY + songIntoLineHeight);
            gc.drawString("Song Feature: " + whichSong.getSongFeature(), songInfoX, songInfoY + songIntoLineHeight*2);
            gc.drawString("Total Beats: " + whichSong.getSongNumOfBeats(), songInfoX, songInfoY + songIntoLineHeight*3);
            gc.drawString("BPM: " + whichSong.getSongBPM(), songInfoX, songInfoY + songIntoLineHeight*4);
            gc.drawString("Frame Rate: " + optional1Decimalformatter.format(whichSong.getFrameRate()/1000) + "kHz", songInfoX, songInfoY + songIntoLineHeight*5);
            gc.drawString("Audio Analysis Method: " + whichSong.getAudioAnalysisMethod(), songInfoX, songInfoY + songIntoLineHeight*6);
            gc.drawString("Max Signal Strength: " + whichSong.getMaxSignalStrengthByWindow(), songInfoX, songInfoY + songIntoLineHeight*7);
            gc.drawString("Min Signal Strength: " + whichSong.getMinSignalStrengthByWindow(), songInfoX, songInfoY + songIntoLineHeight*8);
            gc.drawString("FFT Bin (Bandwidth): " + String.format("%.2f",whichSong.getBinHzWidth()), songInfoX, songInfoY + songIntoLineHeight*9);

            drawDifficultySelector(gc);

            repaint();
        }

        /**
         * Fancy difficulty-level selector: a centered row of colored chips (EASY .. EXPERT),
         * the chosen one enlarged with a glow, plus a "LEFT / RIGHT" hint. Higher level =
         * faster scroll = harder. Cycled by MainMenuAction on LEFT/RIGHT.
         */
        private void drawDifficultySelector(java.awt.Graphics2D gc) {
            SpeedModifier.Difficulty[] diffs = SpeedModifier.Difficulty.values();
            int selIdx = speedModifier.getDifficulty().ordinal();

            int chipW = 150, chipH = 54, gap = 14;
            int totalW = diffs.length * chipW + (diffs.length - 1) * gap;
            int dx = (getWidth() - totalW) / 2;   // centered horizontally
            int dy = 96;

            java.awt.Stroke oldStroke = gc.getStroke();

            // heading
            gc.setColor(Color.white);
            gc.setFont(new Font("verdana", Font.BOLD, 24));
            gc.drawString("SELECT DIFFICULTY", dx, dy - 16);

            for (int i = 0; i < diffs.length; i++) {
                int cx = dx + i * (chipW + gap);
                boolean sel = (i == selIdx);
                Color base = diffs[i].color;

                if (sel) {
                    // glow halo behind the selected chip
                    gc.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 130));
                    gc.fillRoundRect(cx - 8, dy - 8, chipW + 16, chipH + 16, 22, 22);
                }
                gc.setColor(sel ? base : base.darker().darker());
                gc.fillRoundRect(cx, dy, chipW, chipH, 16, 16);
                gc.setStroke(new java.awt.BasicStroke(sel ? 3f : 1f));
                gc.setColor(sel ? Color.white : new Color(180, 180, 180));
                gc.drawRoundRect(cx, dy, chipW, chipH, 16, 16);

                String lbl = diffs[i].label;
                gc.setFont(new Font("verdana", sel ? Font.BOLD : Font.PLAIN, sel ? 22 : 17));
                java.awt.FontMetrics fm = gc.getFontMetrics();
                int lx = cx + (chipW - fm.stringWidth(lbl)) / 2;
                int ly = dy + (chipH + fm.getAscent()) / 2 - 4;
                gc.setColor(sel ? Color.white : new Color(225, 225, 225));
                gc.drawString(lbl, lx, ly);
            }

            // navigation hint
            gc.setStroke(oldStroke);
            gc.setColor(Color.white);
            gc.setFont(new Font("verdana", Font.ITALIC, 17));
            String hint = "◄  LEFT / RIGHT to change  ►";
            java.awt.FontMetrics hfm = gc.getFontMetrics();
            gc.drawString(hint, (getWidth() - hfm.stringWidth(hint)) / 2, dy + chipH + 28);
        }
        
        //return controlflow
        public int getcontrolFlow()
        {
            return this.controlFlow;
        }
        
        //return music no.
        public int getwhichMusic()
        {
        	return this.whichmusic;
        }
        
        //return y_movement
        public int getMovement()
        {
        	return this.y_movement;
        }

        //return BPM
        public int getBPM()
        {
        	return this.BPM;
        }

        public Song getWhichSong() {
            return whichSong;
        }

        public void setWhichSong(Song whichSong) {
            this.whichSong = whichSong;
        }
}


