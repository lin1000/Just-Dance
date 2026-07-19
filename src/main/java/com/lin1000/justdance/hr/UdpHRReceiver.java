package com.lin1000.justdance.hr;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Listens on UDP localhost:9999 for heart rate packets sent by ble_hr_bridge.py.
 * Packet format: "bpm:NN" (ASCII, e.g. "bpm:72").
 * getHeartRate() returns 0 when no packet has arrived in the last 5 seconds.
 */
public class UdpHRReceiver {

    public static final int PORT = 9999;
    private static final long STALE_MS = 5000;

    private final AtomicInteger bpm = new AtomicInteger(0);
    private final AtomicLong lastUpdateMs = new AtomicLong(0);
    private volatile boolean running = false;
    private volatile DatagramSocket socket;

    public void start() {
        running = true;
        Thread t = new Thread(this::listen, "udp-hr-receiver");
        t.setDaemon(true);
        t.start();
        System.out.println("[UdpHRReceiver] listening on UDP port " + PORT);
    }

    public void stop() {
        running = false;
        DatagramSocket s = socket;
        if (s != null) s.close();
    }

    /** Returns current BPM, or 0 if data is stale / bridge not running. */
    public int getHeartRate() {
        if (System.currentTimeMillis() - lastUpdateMs.get() > STALE_MS) return 0;
        return bpm.get();
    }

    private void listen() {
        try (DatagramSocket sock = new DatagramSocket(PORT)) {
            socket = sock;
            sock.setSoTimeout(2000);
            byte[] buf = new byte[64];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            while (running) {
                try {
                    sock.receive(packet);
                    String msg = new String(buf, 0, packet.getLength()).trim();
                    if (msg.startsWith("bpm:")) {
                        int val = Integer.parseInt(msg.substring(4));
                        if (val > 0 && val < 300) {
                            bpm.set(val);
                            lastUpdateMs.set(System.currentTimeMillis());
                        }
                    }
                } catch (java.net.SocketTimeoutException ignored) {
                    // expected — just loop to check running flag
                }
            }
        } catch (Exception e) {
            if (running) System.err.println("[UdpHRReceiver] error: " + e.getMessage());
        } finally {
            socket = null;
        }
    }
}
