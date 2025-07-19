package com.lin1000.justdance.device;

import com.github.strikerx3.jxinput.XInputDevice;
import com.github.strikerx3.jxinput.exceptions.XInputNotLoadedException;
import com.lin1000.justdance.player.Player;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JXInputDeviceWatcher {
    private int knownDeviceCount = -1;
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private HashMap<String,Player> xInputDeviceHash = null;

    public interface JXDeviceListener {
        void onDeviceConnected();
        void onDeviceDisconnected();
    }

    private JXDeviceListener listener;

    public JXInputDeviceWatcher(){
        this.listener = new JXInputDeviceWatcher.JXDeviceListener() {
            @Override
            public void onDeviceConnected() {
                System.out.println("🎮 JXInput Device Connected");
            }

            @Override
            public void onDeviceDisconnected() {
                System.out.println("🎮 JXInput Device Disconnected");
            }
        };
    }

    public JXInputDeviceWatcher(JXDeviceListener listener) {
        this.listener = listener;
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
                device = null;
            }

            //then schedule the pooling at Fixed Rate
            scheduler.scheduleAtFixedRate(this::scanDevices, 0, 1000, TimeUnit.MICROSECONDS);

        } catch (XInputNotLoadedException e) {
            throw new RuntimeException(e);
        }

    }

    private void scanDevices() {
        System.out.println("Scanning Device...");
        if (!XInputDevice.isAvailable()) {
            System.out.println("XInput 不可用，請確認系統支援並已載入 DLL。");
            return;
        }
        System.out.println("Scanning Device...XInputDevice Driver is available..scanning.");

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
                Player p = xInputDeviceHash.get(device.toString());
                if(p==null){//New Device Join
                    p = extractDeviceIntoPlayer(device);
                    xInputDeviceHash.put(device.toString(),p);
                    listener.onDeviceConnected();
                }else if(p!=null && p.isConnected() && !device.isConnected()){//Device become offline
                    p.setConnected(device.isConnected());
                    listener.onDeviceDisconnected();
                } else if(p!=null && p.isConnected() &&  device.isConnected()){
                    //nothing changed
                }else if(p!=null && !p.isConnected() && !device.isConnected()) {
                    //nothing changed
                }else if(p!=null && !p.isConnected() && device.isConnected()){//Device come online again
                    p.setConnected(device.isConnected());
                    listener.onDeviceConnected();
                }

            }
            System.out.println("Scanning Device...XInputDevice Driver is available..scanning..."+xInputDeviceHash.size()+" devices.." );

        } catch (XInputNotLoadedException e) {
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
}
