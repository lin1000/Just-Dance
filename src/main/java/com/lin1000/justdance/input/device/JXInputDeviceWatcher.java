package com.lin1000.justdance.input.device;

import com.github.strikerx3.jxinput.XInputDevice;
import com.github.strikerx3.jxinput.exceptions.XInputNotLoadedException;
import com.lin1000.justdance.gamepanel.inputdevice.MainMenuXInputDeviceListener;
import com.lin1000.justdance.gamepanel.MainMenu;
import com.lin1000.justdance.player.Player;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JXInputDeviceWatcher {

    private MainMenu mainTargetWindow = null;
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private HashMap<String,Player> xInputDeviceHash = null;

    public interface JXDeviceListener {
        void onDeviceDiscovered(XInputDevice device);
        void onDeviceConnected(XInputDevice device);
        void onDeviceDisconnected(XInputDevice device);
    }

    private JXDeviceListener listener;

    public JXInputDeviceWatcher(){
        this.listener = new JXInputDeviceWatcher.JXDeviceListener() {

            @Override
            public void onDeviceDiscovered(XInputDevice device) {
                System.out.println("🎮 JXInput Device ["+ device+ "] is Connected.");
            }

            @Override
            public void onDeviceConnected(XInputDevice device) {
                System.out.println("🎮 JXInput Device ["+ device+ "] is Connected");
            }

            @Override
            public void onDeviceDisconnected(XInputDevice device) {
                System.out.println("🎮 JXInput Device [\"+ device+ \"] is Disconnected");
            }
        };
    }

    public JXInputDeviceWatcher(JXDeviceListener listener) {
        this.listener = listener;
    }

    public MainMenu getMainTargetWindow() {
        return mainTargetWindow;
    }

    public void setMainTargetWindow(MainMenu mainTargetWindow) {
        this.mainTargetWindow = mainTargetWindow;
    }

    private Player extractDeviceIntoPlayer(XInputDevice device){
        Player p = new Player();
        p.setName("Player");
        p.setPlayerNum(device.getPlayerNum());
        p.setAge((int)(Math.random()*18));
        p.setConnected(device.isConnected());
        p.setControllerID(device.toString());
        return p;
    }

    public void start() {
        if (!XInputDevice.isAvailable()) {
            System.err.println("XInput Driver is not available ino JXInputDeviceWatcher.");
            return;
        }

        //Scan and register JXInputDevice first
        XInputDevice[] devices = null;
        XInputDevice device = null;
        //JXInputDevice
        try {
            devices = XInputDevice.getAllDevices();
            int count = devices != null ? devices.length : 0;
            xInputDeviceHash = new HashMap<>(count);

            int connectedCount = 0;
            for (int i = 0; i < devices.length; i++) {
                device = devices[i];
                if (device.isConnected()) {
                    connectedCount++;
                }
                Player p = extractDeviceIntoPlayer(device);
                xInputDeviceHash.put(device.toString(),p);
            }

            //then schedule the pooling at Fixed Rate
            scheduler.scheduleAtFixedRate(this::scanDevices, 0, 2, TimeUnit.SECONDS);

        } catch (XInputNotLoadedException e) {
            throw new RuntimeException(e);
        }

    }

    private void scanDevices() {
        //System.out.println("Scanning Device...");
        if (!XInputDevice.isAvailable()) {
            System.out.println("XInput 不可用，請確認系統支援並已載入 DLL。");
            return;
        }
        //System.out.println("Scanning Device...XInputDevice Driver is available..scanning.");

        // 取得玩家 1 的控制器（0~3 對應 4 個可能控制器）
        XInputDevice[] devices = null;
        XInputDevice device = null;
        //JXInputDevice
        try {
            devices = XInputDevice.getAllDevices();
            int count = devices!=null?devices.length:0;
            int connectedCount = 0;
            for(int i=0; i < devices.length ;i++){
                device = devices[i];
                device.poll(); //Critical Step to update the controller state include isConnected.
                Player p = xInputDeviceHash.get(device.toString());
                if(p==null){//New Device Join
                    p = extractDeviceIntoPlayer(device);
                    xInputDeviceHash.put(device.toString(),p);
                    listener.onDeviceDiscovered(device);
                }else if(p!=null && p.isConnected() && !device.isConnected()){//Device become offline
                    p.setConnected(device.isConnected());
                    MainMenuXInputDeviceListener mainMenuXInputDeviceListener = p.getMainMenuXInputDeviceListener();
                    device.removeListener(mainMenuXInputDeviceListener);
                    p.setMainMenuXInputDeviceListener(null);
                    listener.onDeviceDisconnected(device);
                } else if(p!=null && p.isConnected() &&  device.isConnected()){
                    //nothing changed
                }else if(p!=null && !p.isConnected() && !device.isConnected()) {
                    //nothing changed
                }else if(p!=null && !p.isConnected() && device.isConnected()){//Device come online again
                    System.out.println("JXInput Device ["+device+"] come online");
                    p.setConnected(device.isConnected());
                    MainMenuXInputDeviceListener mainMenuXInputDeviceListener = new MainMenuXInputDeviceListener(mainTargetWindow);
                    p.setMainMenuXInputDeviceListener(mainMenuXInputDeviceListener);
                    // The SimpleXInputDeviceListener allows us to implement only the methods we actually need
                    //add listener to enable game interactive by JXInputDevice
                    device.addListener(mainMenuXInputDeviceListener);
                    listener.onDeviceConnected(device);
                }

            }
            System.out.println("Scanning Device...XInputDevice Driver is available..scanning..."+xInputDeviceHash.size()+" devices.." );
            xInputDeviceHash.values().stream().forEach(player->{ System.out.println(player.getControllerID() + "=" + player.isConnected());});

        } catch (XInputNotLoadedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public HashMap<String, Player> getxInputDeviceHash() {
        return xInputDeviceHash;
    }

    public Player getPlayer(int playerNum){
        Stream<Map.Entry<String, Player>> entryStream = xInputDeviceHash.entrySet().stream();
        //Stream<Player> valueStream = xInputDeviceHash.values().stream();
        Stream<Map.Entry<String, Player>> playerStream = entryStream.filter(entry -> entry.getValue().getPlayerNum() == playerNum);
        List<Map.Entry<String, Player>> playerCollection = playerStream.collect(Collectors.toList());
        return playerCollection.get(0).getValue().isConnected()?playerCollection.get(0).getValue():null;
    }

    public void stop() {
        scheduler.shutdownNow();
    }


    public XInputDevice initJXInputDevice()
    {
        if (!XInputDevice.isAvailable()) {
            System.out.println("XInput 不可用，請確認系統支援並已載入 DLL。");
            return null;
        } else {
            System.out.println("XInputDevice.getLibraryVersion()="+XInputDevice.getLibraryVersion());
        }

        // 取得玩家 1 的控制器（0~3 對應 4 個可能控制器）
        XInputDevice[] devices = null;
        XInputDevice device = null;
        try {
            devices = XInputDevice.getAllDevices();
            for(int i=0; i < devices.length ;i++){
                device = devices[i];
                System.out.println("device="+device + ", isConnected="+ device.isConnected());
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
}
