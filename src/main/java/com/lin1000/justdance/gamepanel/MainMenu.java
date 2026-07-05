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
        //Per-song album/jacket art shown in the song-wheel slots. Optional: if a file is
        //missing the wheel falls back to the gradient placeholder. Drop art at
        //img/jacket1.png .. img/jacket4.png (square recommended).
        Image jacket[]=new Image[4];
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
        public volatile Song whichSong;
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

                //Optional per-song jacket art from the catalog (falls back to the gradient
                //placeholder if the file is absent). One entry per song in SongLibrary.
                java.util.List<com.lin1000.justdance.song.SongMeta> songs =
                        com.lin1000.justdance.song.SongLibrary.all();
                jacket = new Image[songs.size()];
                for (int i = 0; i < songs.size(); i++) {
                    jacket[i] = kit.getImage(songs.get(i).getJacketPath());
                }
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
        // Per-song display title, typeset as text (no longer the plate images) so the wheel
        // StepMania/ITG-style song-select: neon backdrop, left song wheel, right detail +
        // difficulty panel, honest read-time readout, control-legend footer, tiny dev corner.
        // All per-song data (title/artist/bpm/jacket/chart) comes from SongLibrary, which
        // scans the .sm files under songs/ — instead of hardcoded arrays and a switch.
        public void menuscreen(int musicOptionIndex)
        {
            this.musicOptionIndex = musicOptionIndex;

            // per-song state pulled from the authored catalog (rendering handled by helpers)
            com.lin1000.justdance.song.SongMeta meta =
                    com.lin1000.justdance.song.SongLibrary.get(musicOptionIndex);
            this.whichmusic = musicOptionIndex;
            this.BPM = meta.getBpm();
            this.y_movement = 0; // legacy; scroll speed now comes from the difficulty level

            if (whichSong == null) { repaint(); return; }

            gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            gc.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            drawNeonBackground(gc);
            drawHeader(gc);
            drawSongWheel(gc, musicOptionIndex);
            drawDetailPanel(gc, musicOptionIndex);
            drawDevCorner(gc);
            drawFooter(gc);

            repaint();
        }

        private static Color dim(Color c, double f) {
            return new Color((int)(c.getRed()*f), (int)(c.getGreen()*f), (int)(c.getBlue()*f));
        }

        private void glassCard(Graphics2D gc, int x, int y, int w, int h) {
            gc.setColor(new Color(10, 16, 40, 150));
            gc.fillRoundRect(x, y, w, h, 16, 16);
            gc.setStroke(new BasicStroke(1f));
            gc.setColor(new Color(120, 170, 255, 45));
            gc.drawRoundRect(x, y, w, h, 16, 16);
        }

        private void drawNeonBackground(Graphics2D gc) {
            int w = getWidth(), h = getHeight();
            gc.setPaint(new GradientPaint(0, 0, new Color(0x0a0e27), 0, h, new Color(0x0a0f28)));
            gc.fillRect(0, 0, w, h);
            gc.setPaint(new RadialGradientPaint(new java.awt.geom.Point2D.Float(w*0.80f, -h*0.10f),
                Math.max(w, h)*0.75f, new float[]{0f, 1f},
                new Color[]{ new Color(0,180,255,70), new Color(0,180,255,0) }));
            gc.fillRect(0, 0, w, h);
            gc.setPaint(new RadialGradientPaint(new java.awt.geom.Point2D.Float(w*0.08f, h*1.05f),
                Math.max(w, h)*0.65f, new float[]{0f, 1f},
                new Color[]{ new Color(255,0,170,60), new Color(255,0,170,0) }));
            gc.fillRect(0, 0, w, h);
            gc.setColor(new Color(90, 150, 255, 15));
            for (int x = 0; x < w; x += 44) gc.drawLine(x, 64, x, h-52);
            for (int y = 64; y < h-52; y += 44) gc.drawLine(0, y, w, y);
        }

        private void drawHeader(Graphics2D gc) {
            int w = getWidth();
            gc.setColor(new Color(120, 180, 255, 45));
            gc.drawLine(0, 64, w, 64);
            gc.setFont(new Font("SansSerif", Font.BOLD, 26));
            gc.setColor(new Color(0x5fe6ff));
            gc.drawString("♪", 28, 42);
            FontMetrics fm = gc.getFontMetrics();
            int tx = 28 + fm.stringWidth("♪") + 12;
            gc.setPaint(new GradientPaint(tx, 0, new Color(0x57e0ff), tx+250, 0, new Color(0xff6ad5)));
            gc.drawString("SELECT MUSIC", tx, 42);
            String p = "PLAYER 1";
            gc.setFont(new Font("SansSerif", Font.BOLD, 13));
            FontMetrics pf = gc.getFontMetrics();
            int pw = pf.stringWidth(p) + 24;
            gc.setStroke(new BasicStroke(1f));
            gc.setColor(new Color(120, 180, 255, 100));
            gc.drawRoundRect(w-28-pw, 18, pw, 28, 20, 20);
            gc.setColor(new Color(0x8fb4e6));
            gc.drawString(p, w-28-pw+12, 37);
        }

        // Music-wheel scroll position in row units. Eases toward the selected index every
        // frame (menuscreen runs ~20fps), taking the shortest path around the wrap, so the
        // list rolls like a StepMania music wheel while the selection stays pinned center.
        private double wheelScroll = Double.NaN;
        // Last slot the wheel's center crossed — used to fire a tick sound per slot passed.
        private int wheelTickSlot = Integer.MIN_VALUE;

        private void drawSongWheel(Graphics2D gc, int sel) {
            int x = 26, w = 600;
            int songCount = com.lin1000.justdance.song.SongLibrary.size();
            gc.setColor(new Color(0x7f9fd0));
            gc.setFont(new Font("SansSerif", Font.BOLD, 12));
            gc.drawString(songCount + " SONGS", x+4, 100);

            // ease the wheel toward the selection via the shortest wrap-around path;
            // the further behind it is (fast seeking), the faster it rolls
            if (Double.isNaN(wheelScroll)) wheelScroll = sel;
            double delta = sel - wheelScroll;
            delta -= Math.round(delta / songCount) * (double) songCount;
            double ease = Math.min(0.60, 0.30 + 0.06 * Math.abs(delta));
            wheelScroll += delta * ease;
            if (Math.abs(delta) < 0.002) wheelScroll = sel;
            wheelScroll = ((wheelScroll % songCount) + songCount) % songCount;

            // tick once per slot the wheel's center crosses while rolling (silent if no audio)
            int tickSlot = (int) Math.round(wheelScroll);
            if (wheelTickSlot != Integer.MIN_VALUE && tickSlot != wheelTickSlot) {
                try { soundController.playEffectSound(1); } catch (Exception ignored) {}
            }
            wheelTickSlot = tickSlot;

            int y0 = 112, rowH = 80, gap = 8, spacing = rowH + gap;
            int viewH = 5 * spacing - gap;              // 5 visible slots
            int centerTop = y0 + 2 * spacing;           // center slot's row top

            java.awt.Shape viewClip = gc.getClip();
            gc.setClip(x - 8, y0 - 6, w + 16, viewH + 12);

            // draw integer song positions around the (fractional) scroll position; each
            // wraps into the catalog with floorMod, so the wheel is endless in both directions
            int jc = (int) Math.round(wheelScroll);
            for (int j = jc - 3; j <= jc + 3; j++) {
                int ry = centerTop + (int) Math.round((j - wheelScroll) * spacing);
                if (ry + rowH < y0 - 6 || ry > y0 + viewH + 6) continue;
                int songIdx = Math.floorMod(j, songCount);

                // proximity to center: 1 at the pinned slot, fading with distance
                double d = Math.abs(j - wheelScroll);
                boolean s = d < 0.5;                     // the (arriving) center row
                float fade = (float) Math.max(0.25, 1.0 - 0.28 * d);

                if (s) {
                    gc.setColor(new Color(60, 200, 255, 55));
                    gc.fillRoundRect(x-3, ry-3, w+6, rowH+6, 18, 18);
                    gc.setColor(new Color(0x0f1a3a));
                    gc.fillRoundRect(x, ry, w, rowH, 14, 14);
                    gc.setStroke(new BasicStroke(2f));
                    gc.setColor(new Color(90, 220, 255, 210));
                    gc.drawRoundRect(x, ry, w, rowH, 14, 14);
                } else {
                    gc.setColor(new Color(255, 255, 255, (int)(10*fade+2)));
                    gc.fillRoundRect(x, ry, w, rowH, 14, 14);
                    gc.setStroke(new BasicStroke(1f));
                    gc.setColor(new Color(120, 170, 255, (int)(28*fade)));
                    gc.drawRoundRect(x, ry, w, rowH, 14, 14);
                }
                int contentX = x + 18;
                if (s) {
                    gc.setColor(new Color(0x5fe6ff));
                    gc.setFont(new Font("SansSerif", Font.BOLD, 22));
                    gc.drawString("▶", x+8, ry+rowH/2+8);
                    contentX = x + 34;
                }
                // jacket: gradient placeholder (bright cyan→purple when centered, muted
                // otherwise) with the song's album art blitted on top if present
                int js = s ? 60 : 48;
                int jy = ry + (rowH-js)/2;
                if (s) gc.setPaint(new GradientPaint(contentX, jy, new Color(0x0bd3ff), contentX+js, jy+js, new Color(0x8a5bff)));
                else   gc.setPaint(new GradientPaint(contentX, jy, new Color(0x22305c), contentX+js, jy+js, new Color(0x38507f)));
                gc.fillRoundRect(contentX, jy, js, js, 10, 10);
                if (jacket[songIdx] != null) {
                    java.awt.Shape oldClip = gc.getClip();
                    gc.clip(new java.awt.geom.RoundRectangle2D.Float(contentX, jy, js, js, 10, 10));
                    gc.drawImage(jacket[songIdx], contentX, jy, js, js, this); // no-op if the file is missing → gradient shows
                    gc.setClip(oldClip);
                }
                gc.setStroke(new BasicStroke(1f));
                gc.setColor(new Color(255,255,255,s?70:(int)(22*fade)));
                gc.drawRoundRect(contentX, jy, js, js, 10, 10);

                // title + subtitle from the catalog, typeset (CJK-capable SansSerif),
                // fading with distance from the pinned center slot
                com.lin1000.justdance.song.SongMeta meta =
                        com.lin1000.justdance.song.SongLibrary.get(songIdx);
                int tx = contentX + js + 16;
                int cy = ry + rowH/2;
                gc.setColor(s ? Color.white : new Color(0.79f, 0.85f, 0.95f, fade));
                gc.setFont(new Font("SansSerif", Font.BOLD, s ? 23 : 17));
                gc.drawString(meta.getTitle(), tx, cy - 3);
                gc.setColor(new Color(0.50f, 0.62f, 0.82f, fade));
                gc.setFont(new Font("SansSerif", Font.PLAIN, 12));
                gc.drawString(meta.getArtist(), tx, cy + 18);
            }

            gc.setClip(viewClip);
        }

        private void drawDetailPanel(Graphics2D gc, int sel) {
            int x = 654, w = 600;

            com.lin1000.justdance.song.SongMeta meta =
                    com.lin1000.justdance.song.SongLibrary.get(sel);
            int ty = 100, th = 92;
            glassCard(gc, x, ty, w, th);
            gc.setColor(Color.white);
            gc.setFont(new Font("SansSerif", Font.BOLD, 30));
            gc.drawString(meta.getTitle(), x+22, ty+46);
            gc.setColor(new Color(0x8fb4e6));
            gc.setFont(new Font("SansSerif", Font.BOLD, 13));
            gc.drawString(meta.getArtist().toUpperCase(), x+22, ty+74);

            int dy0 = ty + th + 14, dh = 152;
            glassCard(gc, x, dy0, w, dh);
            drawDifficultyCard(gc, x, dy0, w);

            int sy = dy0 + dh + 14, sh = 108;
            glassCard(gc, x, sy, w, sh);
            drawStatsCard(gc, x, sy, w);
        }

        private void drawDifficultyCard(Graphics2D gc, int x, int y, int w) {
            SpeedModifier.Difficulty[] d = SpeedModifier.Difficulty.values();
            int sel = speedModifier.getDifficulty().ordinal();
            int pad = 18;

            gc.setColor(new Color(0x7f9fd0));
            gc.setFont(new Font("SansSerif", Font.BOLD, 12));
            gc.drawString("DIFFICULTY", x+pad, y+24);
            String hint = "◄ LEFT / RIGHT ►";
            gc.setColor(new Color(0x6f8fc0));
            gc.setFont(new Font("SansSerif", Font.PLAIN, 12));
            FontMetrics hf = gc.getFontMetrics();
            gc.drawString(hint, x+w-pad-hf.stringWidth(hint), y+24);

            int gap = 8;
            int innerW = w - pad*2;
            int pillW = (innerW - gap*3) / 4;
            int pillH = 46;
            int py = y + 42;
            for (int i = 0; i < 4; i++) {
                int px = x + pad + i*(pillW+gap);
                boolean s = (i == sel);
                Color base = d[i].color;
                int yy = py + (s ? -4 : 0);
                int hh = pillH + (s ? 8 : 0);
                if (s) {
                    gc.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 90));
                    gc.fillRoundRect(px-4, yy-4, pillW+8, hh+8, 14, 14);
                }
                gc.setPaint(new GradientPaint(px, yy, s ? base : dim(base, 0.42),
                        px, yy+hh, s ? dim(base, 0.55) : dim(base, 0.16)));
                gc.fillRoundRect(px, yy, pillW, hh, 12, 12);
                gc.setStroke(new BasicStroke(s ? 2.5f : 1f));
                gc.setColor(s ? Color.white : new Color(255, 255, 255, 60));
                gc.drawRoundRect(px, yy, pillW, hh, 12, 12);
                if (s) {
                    gc.setColor(base.brighter());
                    int mx = px + pillW/2;
                    gc.fillPolygon(new int[]{mx-6, mx+6, mx}, new int[]{yy-10, yy-10, yy-2}, 3);
                }
                gc.setColor(s ? Color.white : new Color(220, 220, 220, 180));
                gc.setFont(new Font("SansSerif", s ? Font.BOLD : Font.PLAIN, s ? 15 : 13));
                FontMetrics fm = gc.getFontMetrics();
                gc.drawString(d[i].label, px + (pillW-fm.stringWidth(d[i].label))/2, yy + hh/2 + fm.getAscent()/2 - 2);
            }

            int ry = py + pillH + 26;
            SpeedModifier.Difficulty cur = d[sel];
            // Foot rating = the song's playable-chart meter from the .sm (StepMania-style Lv.N).
            com.lin1000.justdance.song.sm.Simfile.Chart _chart =
                    com.lin1000.justdance.song.SongLibrary.simfileFor(musicOptionIndex).playableChart();
            int foot = (_chart != null) ? _chart.meter : 0;
            gc.setColor(cur.color.brighter());
            gc.setFont(new Font("SansSerif", Font.BOLD, 20));
            gc.drawString(cur.label, x+pad, ry+4);
            // per-song foot rating (StepMania-style) from the catalog, when authored
            if (foot > 0) {
                int lw = gc.getFontMetrics().stringWidth(cur.label);
                gc.setColor(new Color(0xffe07a));
                gc.setFont(new Font("SansSerif", Font.BOLD, 16));
                gc.drawString("Lv." + foot, x+pad+lw+14, ry+3);
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 4; i++) sb.append(i <= sel ? "★" : "☆");
            gc.setFont(new Font("SansSerif", Font.PLAIN, 16));
            gc.setColor(new Color(0xffd43b));
            gc.drawString(sb.toString(), x+pad, ry+26);

            double approach = 660.0 / cur.pxPerSec;
            String readK = "READ TIME";
            String readV = String.format("%.2fs · %d px/s", approach, (int)cur.pxPerSec);
            gc.setColor(new Color(0x7f9fd0));
            gc.setFont(new Font("SansSerif", Font.PLAIN, 11));
            FontMetrics rk = gc.getFontMetrics();
            gc.drawString(readK, x+w-pad-rk.stringWidth(readK), ry+4);
            gc.setColor(Color.white);
            gc.setFont(new Font("SansSerif", Font.BOLD, 15));
            FontMetrics rv = gc.getFontMetrics();
            gc.drawString(readV, x+w-pad-rv.stringWidth(readV), ry+26);
        }

        private void drawStatsCard(Graphics2D gc, int x, int y, int w) {
            int pad = 18;
            int[] len = whichSong.getSongLengthInMinutesAndSeconds();
            String[] k = { "BPM", "LENGTH" };
            String[] v = { String.valueOf(BPM), String.format("%d:%02d", len[0], len[1]) };
            int colW = (w - pad*2) / 2;
            for (int i = 0; i < 2; i++) {
                int cx = x + pad + i*colW;
                gc.setColor(new Color(0x7f9fd0));
                gc.setFont(new Font("SansSerif", Font.BOLD, 11));
                FontMetrics kf = gc.getFontMetrics();
                gc.drawString(k[i], cx + (colW-kf.stringWidth(k[i]))/2, y+30);
                gc.setColor(Color.white);
                gc.setFont(new Font("SansSerif", Font.BOLD, 26));
                FontMetrics vf = gc.getFontMetrics();
                gc.drawString(v[i], cx + (colW-vf.stringWidth(v[i]))/2, y+62);
            }
            String am;
            try { am = String.valueOf(soundController.getCurrentAudioAnalysisMode()); }
            catch (Exception e) { am = "FFT_BASS"; }
            String chip = "◂ " + am + " ▸";
            String lbl = "AUDIO ANALYSIS";
            gc.setFont(new Font("SansSerif", Font.PLAIN, 12));
            FontMetrics cf = gc.getFontMetrics();
            int chipW = cf.stringWidth(chip) + 24;
            int totalW = cf.stringWidth(lbl) + 12 + chipW;
            int sx = x + (w-totalW)/2;
            int sy = y + 92;
            gc.setColor(new Color(0x8fb4e6));
            gc.drawString(lbl, sx, sy);
            int chx = sx + cf.stringWidth(lbl) + 12;
            gc.setStroke(new BasicStroke(1f));
            gc.setColor(new Color(120, 180, 255, 100));
            gc.drawRoundRect(chx, sy-15, chipW, 22, 14, 14);
            gc.setColor(new Color(0xbcd4ff));
            gc.drawString(chip, chx+12, sy);
        }

        private void drawDevCorner(Graphics2D gc) {
            String[] lines = {
                "dev · " + whichSong.getName(),
                "feature: " + whichSong.getSongFeature(),
                "analysis: " + whichSong.getAudioAnalysisMethod(),
                "BPM detected: " + whichSong.getSongBPM() + "  (authored: " + BPM + ")",
                "signal max/min: " + whichSong.getMaxSignalStrengthByWindow() + " / " + whichSong.getMinSignalStrengthByWindow(),
                "frameRate: " + optional1Decimalformatter.format(whichSong.getFrameRate()/1000) + "kHz",
                "fft bin: " + String.format("%.2f", whichSong.getBinHzWidth()) + " Hz",
            };
            // Anchored just above the footer so it never collides with the song wheel,
            // however many songs the library holds.
            int x = 30, y = getHeight() - 52 - lines.length*14 - 10;
            gc.setColor(new Color(140, 160, 190, 120));
            gc.setFont(new Font("Monospaced", Font.PLAIN, 11));
            for (int i = 0; i < lines.length; i++) gc.drawString(lines[i], x, y + i*14);
        }

        private int footItem(Graphics2D gc, int x, int y, String key, String label) {
            gc.setColor(new Color(0x5fe6ff));
            gc.setFont(new Font("SansSerif", Font.BOLD, 13));
            FontMetrics kf = gc.getFontMetrics();
            gc.drawString(key, x, y);
            int kx = x + kf.stringWidth(key) + 6;
            gc.setColor(new Color(0x9fc0ee));
            gc.setFont(new Font("SansSerif", Font.PLAIN, 13));
            FontMetrics lf = gc.getFontMetrics();
            gc.drawString(label, kx, y);
            return kx + lf.stringWidth(label) + 22;
        }

        private void drawFooter(Graphics2D gc) {
            int w = getWidth(), h = getHeight();
            int fy = h - 52;
            gc.setColor(new Color(120, 180, 255, 40));
            gc.drawLine(0, fy, w, fy);
            int x = 28, y = h - 20;
            x = footItem(gc, x, y, "↑↓", "Song");
            x = footItem(gc, x, y, "←→", "Difficulty");
            x = footItem(gc, x, y, "↵ / A", "Start");
            x = footItem(gc, x, y, "Esc", "Back");
            String kk = "L / R", vv = "Audio Analysis";
            gc.setFont(new Font("SansSerif", Font.BOLD, 13));
            FontMetrics kf = gc.getFontMetrics();
            int rx = w - 28 - (kf.stringWidth(kk) + 6 + gc.getFontMetrics(new Font("SansSerif", Font.PLAIN, 13)).stringWidth(vv));
            gc.setColor(new Color(0x5fe6ff));
            gc.drawString(kk, rx, y);
            gc.setColor(new Color(0x6f8fc0));
            gc.setFont(new Font("SansSerif", Font.PLAIN, 13));
            gc.drawString(vv, rx + kf.stringWidth(kk) + 6, y);
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


