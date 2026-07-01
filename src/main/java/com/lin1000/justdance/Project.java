package com.lin1000.justdance;

import com.github.strikerx3.jxinput.XInputDevice;
import com.github.strikerx3.jxinput.exceptions.XInputNotLoadedException;
import com.lin1000.justdance.controller.SoundController;
import com.lin1000.justdance.gamepanel.Dance;
import com.lin1000.justdance.gamepanel.MainMenu;
import com.lin1000.justdance.input.device.JXInputDeviceWatcher;
import com.lin1000.justdance.input.device.MidiDeviceWatcher;
import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static java.awt.GraphicsDevice.WindowTranslucency.*;

public class Project extends JFrame implements Runnable
{
	private Thread projectThread;

	//JXInputDevice
	public XInputDevice xInputDevice = null;

	//MidiInputDevice
	MidiDevice midiDevice = null;

	//Device Watcher Polling based
	public JXInputDeviceWatcher jXInputDeviceWatcher = null;
	public MidiDeviceWatcher midiDeviceWatcher = null;


	//SoundController
	SoundController soundController = null;

	//private mainMenu main;
	private Dance dance;

	//流程變數
	public int controlFlow;
	//歌曲參數　
	//曲目變數,y_movement
	public int music;
	public int y_movement;
	public int BPM;

	//temp
	public boolean isFirstRound =true;

	//lock
	private final Object mainThreadPauseLock = new Object();

	//Video playing variables
	public BufferedImage currentVideoFrame;
	public FrameGrab frameGrab;
	public String videoPath = "img/intro.mp4";

	public Project()
	{
		super("Just Dance");
		//setting up keystroke

		//loading video resource
		/**
		try {
			loadVideo();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (JCodecException e) {
			throw new RuntimeException(e);
		}**/

		//temp
		projectThread=new Thread(this);
		projectThread.start();
	}
	
	public void run()
	{
		// 取得所有螢幕裝置
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] screens = ge.getScreenDevices();
		GraphicsDevice activeScreen = null;
		// 如果有第二個螢幕，就使用它
		if (screens.length > 1) {
			activeScreen = screens[1];
			System.out.println("activeScreen.isWindowTranslucencySupported(PERPIXEL_" +
					"TRANSPARENT)="+activeScreen.isWindowTranslucencySupported(PERPIXEL_TRANSPARENT));
			System.out.println("activeScreen.isWindowTranslucencySupported(TRANSLUCENT)="+activeScreen.isWindowTranslucencySupported(TRANSLUCENT));
			System.out.println("activeScreen.isWindowTranslucencySupported(PERPIXEL_TRANSLUCENT)="+activeScreen.isWindowTranslucencySupported(PERPIXEL_TRANSLUCENT));
			System.out.println("activeScreen.isDisplayChangeSupported()="+activeScreen.isDisplayChangeSupported());
			System.out.println("activeScreen.getType()="+activeScreen.getType());
//			// Get the display modes (Not Supported)
//			DisplayMode[] dm = activeScreen.getDisplayModes();
//			DisplayMode desiredMode = null;
//			for (DisplayMode mode : dm) {
//				System.out.println("Mode: " + mode.getWidth() + "x" + mode.getHeight() + ", Refresh Rate: " + mode.getRefreshRate());
//				if (mode.getWidth() == 3840 && mode.getHeight() == 2160 ) {
//					desiredMode = mode;
//					break;
//				}
//			}
//			if (desiredMode != null) {
//				// Set the display mode (if supported, otherwise handle gracefully)
//				try {
//					activeScreen.setDisplayMode(desiredMode);
//				} catch (Exception e) {
//					e.printStackTrace();
//					System.err.println("Failed to set display mode: " + e.getMessage());
//					System.exit(1);
//				}
//			this.setSize(desiredMode.getWidth(), desiredMode.getHeight()); // Set the size
//			this.setUndecorated(true); // Remove window decorations
//			}
			/**
			 * Set full screen mode
			 * activeScreen.setFullScreenWindow(this);
			 */
			/**/
			//activeScreen.setFullScreenWindow(this);
			this.setVisible(true);
			Rectangle bounds = activeScreen.getDefaultConfiguration().getBounds();
			int x = bounds.x + (bounds.width - this.getWidth()) / 2;
			int y = bounds.y + (bounds.height - this.getHeight()) / 2;
			bounds.setLocation(x,y);

		} else {
			// 否則顯示在預設螢幕中央
			activeScreen = screens[0];
			//this.setLocationRelativeTo(null);
			// Full-screen exclusive mode may not work on virtual/headless displays; fall back to maximised window
			if (activeScreen.isFullScreenSupported()) {
				activeScreen.setFullScreenWindow(this);
			} else {
				Rectangle bounds = activeScreen.getDefaultConfiguration().getBounds();
				this.setSize(bounds.width, bounds.height);
				this.setLocation(bounds.x, bounds.y);
			}
		}
		this.setVisible(true);

		MainMenu mainMenu = null;
		Dance dance = null;
		while(true)
		{
			xInputDevice = initJXInputDevice();
			midiDevice = initMidiDevice();
			soundController = new SoundController();

			//com.lin1000.justdance.gamepanel.MainMenu
			System.out.println("****************(1)Step=MainMenu");
			mainMenu=null;
			if(mainMenu==null) {
				mainMenu = new MainMenu(this, isFirstRound, xInputDevice,midiDevice, soundController, activeScreen);
			}

			//Window mainwindow=new Window(main);
			//mainwindow.show();

			//GamePlay
			//取得流程變數，等待流程輪到3
			System.out.println("****************Step=(2)After Music Chosen");
			//取得曲目變數
			this.music = mainMenu.getwhichMusic();
			this.y_movement = mainMenu.getMovement();
			// BPM from the authored catalog (analysis getSongBPM() returns 0), so gameplay,
			// menu, and chart timing all agree on one value.
			this.BPM = com.lin1000.justdance.song.SongLibrary.get(this.music).getBpm();
            this.repaint();

			System.out.println("Step=(3)Dance Preparation");
			dance = new Dance(this, mainMenu.getWhichSong(), this.music,this.y_movement,this.BPM, xInputDevice, midiDevice,soundController,activeScreen);//�ǤJ�ȬO����!
			//carry the difficulty level chosen in the song-selection screen into gameplay
			dance.speedModifier.setDifficulty(mainMenu.getSelectedDifficulty());
			soundController.setMainTargetWindow(dance);
			mainMenu.setVisible(false);
			//Setting up and start counting the rhythm nanos
			//this.soundController.playBackgroundSound(music, false);
			soundController.initiateAudioDrivenMainTheadGameLoop(music); //non-blocking

			System.out.println("Step=(4)Dance Ready");
			//開始玩

			//mainThreadPause and wait until game thread notify.
			//Guard the wait() with the exit predicate so that (a) a spurious wakeup does
			//not let us proceed early, and (b) if the player's exit/replay notifyAll() races
			//ahead of this wait(), getExit() is already true and we never block forever.
			synchronized (getMainThreadPauseLock()){
                while (!dance.conditionControl.getExit()) {
                    try {
                        getMainThreadPauseLock().wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }


			/**
			 * Handle the restart or continues the game
			 */
			if(!(dance.conditionControl.getContinue())) gameStop();
			//replay
			if(dance!=null){
				//如果dance已經存在，則關閉它
				System.out.println("dance is not null, dispose it.");
				//producer.stop() sets the volatile isStop flag so no further arrows spawn.
				//The producer no longer owns a thread (spawning is driven from the game tick),
				//so the old deprecated/unsafe Thread.stop() call is gone.
				dance.producer.stop();
				dance.soundController.stop_all();
				dance.removeInputDeviceListener();//remove xInputDevice listener when xInputDevice is available.
				dance.setVisible(false);
				dance.dispose();
				dance = null;

				//dance.producer=null;
			}
			//soundController.getFpsTimer().cancel();
			soundController=null;
			//dance=null;
			isFirstRound = false;//已經第玩過一次
		}
	}
	public void gameStop()
	{
		projectThread=null;
		System.exit(0);
	}

	public XInputDevice initJXInputDevice()
	{
		try {
			if (!XInputDevice.isAvailable()) {
				System.out.println("XInput not available on this platform.");
				return null;
			}
			System.out.println("XInputDevice.getLibraryVersion()="+XInputDevice.getLibraryVersion());

			XInputDevice[] devices = XInputDevice.getAllDevices();
			for (XInputDevice device : devices) {
				System.out.println("device="+device + ", isConnected="+ device.isConnected());
				if (device.isConnected()) {
					System.out.println("device is Connected.");
					return device;
				}
			}
		} catch (UnsatisfiedLinkError | Exception e) {
			System.out.println("JXInput not available: " + e.getMessage());
		}
		return null;
	}

	public MidiDevice initMidiDevice()
	{
		MidiDevice.Info[] infos = null;
		MidiDevice device = null;
		try {
			infos = MidiSystem.getMidiDeviceInfo();
			for (MidiDevice.Info info : infos) {
				device = MidiSystem.getMidiDevice(info);
				System.out.println("===========MIDI DEVICE==============");
				System.out.println("info.getName()="+info.getName());
				System.out.println("info.getVendor()="+info.getVendor());
				System.out.println("info.getDescription()="+info.getDescription());
				System.out.println("info.getVersion()="+info.getVersion());
				System.out.println("device.getMaxTransmitters()="+device.getMaxTransmitters());
				System.out.println("device.getMicrosecondPosition()="+device.getMicrosecondPosition());
				System.out.println("device="+device);
				if (info.getName().equals("USB-MIDI") && device.getMaxTransmitters() !=0 && device.toString().contains("MidiInDevice")){
					device.open();
					System.out.println("Connected to Midi Device：" + info.getName());
					return device;
				}
			}
			System.err.println("Cannot find KAWAI CN201 Piano MIDI Device.");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private void loadVideo() throws IOException, JCodecException {
		FileChannelWrapper ch = NIOUtils.readableChannel(new File(videoPath));
		frameGrab = FrameGrab.createFrameGrab(ch);
	}

	public Object getMainThreadPauseLock() {
		return mainThreadPauseLock;
	}

	public static void gameStart()
	{
		Project myproject=new Project();
		myproject.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Image image = Toolkit.getDefaultToolkit().getImage("img/icon.png");
		System.out.println("image="+image);
		myproject.setIconImage(image);
		myproject.setName("Just com.lin1000.justdance.gamepanel.Dance");
		myproject.setVisible(true);
		myproject.setSize(300,300);

	}

}
