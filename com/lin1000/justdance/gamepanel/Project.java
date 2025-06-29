package com.lin1000.justdance.gamepanel;

import com.github.strikerx3.jxinput.XInputDevice;
import com.github.strikerx3.jxinput.exceptions.XInputNotLoadedException;
import com.lin1000.justdance.controller.SoundController;

import javax.swing.*;
import java.awt.*;

import static java.awt.GraphicsDevice.WindowTranslucency.*;

public class Project extends JFrame implements Runnable
{
	private Thread projectThread;

	//JXInputDevice
	XInputDevice device = null;

	//SoundController
	SoundController soundController = null;

	//private mainMenu main;
	private Dance dance;

	//�y�{�ܼ�
	public int controlFlow;
	//�q���Ѽơ@
	//�����ܼ�,y_movement 
	public int music;
	public int y_movement;
	public int BPM;

	//temp
	public boolean isFirstRound =true;

	//lock
	private final Object mainThreadPauseLock = new Object();

	public Project()
	{
		super("Just Dance");
		//setting up keystroke

		//temp
		projectThread=new Thread(this);
		projectThread.start();
	}
	
	public void run()
	{
		// ���o�Ҧ��ù��˸m
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] screens = ge.getScreenDevices();
		GraphicsDevice activeScreen = null;
		// �p�G���ĤG�ӿù��A�N�ϥΥ�
		if (screens.length > 1) {
			activeScreen = screens[1];
			System.out.println("activeScreen.isWindowTranslucencySupported(PERPIXEL_TRANSPARENT)="+activeScreen.isWindowTranslucencySupported(PERPIXEL_TRANSPARENT));
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
			activeScreen.setFullScreenWindow(this);
			this.setVisible(true);
			Rectangle bounds = activeScreen.getDefaultConfiguration().getBounds();
			int x = bounds.x + (bounds.width - this.getWidth()) / 2;
			int y = bounds.y + (bounds.height - this.getHeight()) / 2;
			bounds.setLocation(x,y);

		} else {
			// �_�h��ܦb�w�]�ù�����
			activeScreen = screens[0];
			//this.setLocationRelativeTo(null);
			activeScreen.setFullScreenWindow(this);
		}
		this.setVisible(true);

		MainMenu mainMenu = null;
		Dance dance = null;
		while(true)
		{
			device = initJXInputDevice();
			soundController = new SoundController();

			//com.lin1000.justdance.gamepanel.MainMenu
			System.out.println("****************(1)Step=MainMenu");
			mainMenu=null;
			if(mainMenu==null) {
				mainMenu = new MainMenu(this, isFirstRound, device, soundController, activeScreen);
			}

			//Window mainwindow=new Window(main);
			//mainwindow.show();

			//GamePlay
			//���o�y�{�ܼơA���ݬy�{����3
			System.out.println("****************Step=(2)After Music Chosen");
			//���o�����ܼ�
			this.music = mainMenu.getwhichMusic();
			this.y_movement = mainMenu.getMovement();
			this.BPM = mainMenu.getWhichSong().getSongBPM();
            this.repaint();

			System.out.println("Step=(3)Dance Preparation");
			dance=new Dance(this, this.music,this.y_movement,this.BPM, device,soundController,activeScreen);//�ǤJ�ȬO����!
			soundController.setMainTargetWindow(dance);
			mainMenu.setVisible(false);
			//Setting up and start counting the rhythm nanos
			//this.soundController.playBackgroundSound(music, false);
			soundController.initiateAudioDrivenMainTheadGameLoop(music); //non-blocking

			System.out.println("Step=(4)Dance Ready");
			//�}�l��

			//mainThreadPause and wait until game thread notify
			synchronized (getMainThreadPauseLock()){
                try {
                    getMainThreadPauseLock().wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }


			/**
			 * Handle the restart or continues the game
			 */
			if(!(dance.conditionControl.getContinue())) gameStop();
			//replay
			if(dance!=null){
				//�p�Gdance�w�g�s�b�A�h������
				System.out.println("dance is not null, dispose it.");
				dance.producer.produceThread.stop();
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
			isFirstRound = false;//�w�g�Ī��L�@��
		}
	}
	public void gameStop()
	{
		projectThread=null;
		System.exit(0);
	}

	public XInputDevice initJXInputDevice()
	{
		if (!XInputDevice.isAvailable()) {
			System.out.println("XInput ���i�ΡA�нT�{�t�Τ䴩�äw���J DLL�C");
			return null;
		}

		// ���o���a 1 ������]0~3 ���� 4 �ӥi�౱��^
		XInputDevice[] devices = null;
		XInputDevice device = null;
		try {
			devices = XInputDevice.getAllDevices();
			for(int i=0; i < devices.length ;i++){
				device = devices[i];
				System.out.println("devicce="+device + ", isConnected="+ device.isConnected());
				if (device.isConnected()) {
					System.out.println("device is Connected.");
					break;
				}
				device=null;
			}
		} catch (XInputNotLoadedException e) {
			throw new RuntimeException(e);
		}

		System.out.println("using device="+device);

		return device;
	}

	public Object getMainThreadPauseLock() {
		return mainThreadPauseLock;
	}

	public static void main(String args[])
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
