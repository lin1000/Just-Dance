package com.lin1000.justdance.input.device;

import org.hid4java.*;
import org.hid4java.event.HidServicesEvent;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public final class HidGamepad implements HidServicesListener, AutoCloseable {
    private final HidServices hid;
    private volatile HidDevice dev;
    private volatile boolean running;
    private Thread loop;

    // 簡單的狀態：按鍵 bit 與四個軸值（之後你可改成更完整的映射）
    private final AtomicInteger buttons = new AtomicInteger(0);
    private final int[] axes = new int[4];

    public HidGamepad() {
        this.hid = HidManager.getHidServices();
        hid.addHidServicesListener(this);
        hid.start();
        pickInitial();
    }

    private void pickInitial() {
        for (HidDevice d : hid.getAttachedHidDevices()) {
            if (isGameController(d)) {
                //if(d.getVendorId()==0x056a) {
                if(d.getVendorId() == 0x045e  && d.getUsagePage() == 0x01 && d.getUsage() == 0x05) {
                    open(d);
                    break;
                }else{
                    System.out.println("VendorID is not 0x056a skipped ("+ String.format("0x%04x", d.getVendorId())+")");
                }
            }
        }
    }

    private static boolean isGameController(HidDevice d) {
        int page = d.getUsagePage() & 0xFFFF;
        int use  = d.getUsage() & 0xFFFF;
        // 符合兩種常見表示方式就收：Generic Desktop(0x01) Joystick(0x04)/GamePad(0x05) 或 Game Controls(0x05)
        return (page == 0x01 && (use == 0x04 || use == 0x05 || use == 0x06)) || (page == 0x05);
    }

    private synchronized void open(HidDevice d) {
        try { if (dev != null && dev.isOpen()) dev.close(); } catch (Exception ignore) {}
        dev = d;
        if (!dev.isOpen()) {
            dev.open();
            startReader(dev);
            System.out.printf("HID Device Opened: %s (VID=%04x PID=%04x UsagePage=0x%02x Usage=0x%02x)%n",
                    dev.getProduct(), dev.getVendorId(), dev.getProductId(), dev.getUsagePage(), dev.getUsage());
        }
    }

    private void startReader(HidDevice dev) {
        running = true;
        loop = new Thread(() -> {
            byte[] buf = new byte[64]; // 大多常見裝置 64 bytes；真實長度以 descriptor 為準
            byte[] last = new byte[0];
            while (running && dev != null && dev.isOpen()) {
                int n = dev.read(buf, 100); // 1 秒 timeout；>0 代表有資料
                if (n > 0) {
                    byte[] rep = Arrays.copyOf(buf, n);
                    if (!Arrays.equals(rep, last)) {
                        last = rep;
                        decodeReport(rep);      // 解析（先做「學習模式」）
                        debugDump(rep);         // 前期：可見就好
                    }
                }
                System.out.println("n("+n + ")running+dev"+ String.format("0x%04x",dev.getVendorId()));
            }
        }, "hid-reader");
        loop.setDaemon(true);
        loop.start();
    }

    private void decodeReport(byte[] r) {
        // 【學習模式(入門版)】：先抓到「哪個 bit 變了」
        // 多數裝置第一個 byte 可能是 reportId（沒有就 0），按鍵通常緊接其後
        int off = 1;                       // 先假設 r[0] 是 reportId
        if (r.length >= off + 2) {
            int b = (r[off] & 0xFF) | ((r[off+1] & 0xFF) << 8); // 先抓 16 個按鍵bit
            buttons.set(b);
        }
        // 軸值（示意）：很多手把每軸 16bit，小端序；實務上請依你裝置 mapping 調整
        if (r.length >= off + 6) {
            axes[0] = (r[off+2] & 0xFF) | ((r[off+3] & 0xFF) << 8);
            axes[1] = (r[off+4] & 0xFF) | ((r[off+5] & 0xFF) << 8);
            // 視需要再擴充 axes[2], axes[3]...
        }
    }

    private static void debugDump(byte[] r) {
        StringBuilder sb = new StringBuilder("<");
        for (byte b : r) sb.append(String.format(" %02x", b));
        sb.append(" ]");
        System.out.println(sb.toString());
    }

    // 提供遊戲端查詢
    public boolean isPressed(int bit) { return (buttons.get() & (1 << bit)) != 0; }
    public int axis(int i) { return axes[i]; }

    // 監聽熱插拔
    @Override public void hidDeviceAttached(HidServicesEvent e) {
        HidDevice d = e.getHidDevice();
        if (isGameController(d)) open(d);
    }
    @Override public void hidDeviceDetached(HidServicesEvent e) {
        if (dev != null && e.getHidDevice().getId().equals(dev.getId())) {
            System.out.println("HID detached");
            running = false;
            try { if (dev.isOpen()) dev.close(); } catch (Exception ignore) {}
            dev = null;
        }
    }
    @Override public void hidFailure(HidServicesEvent e) {
        System.err.println("HID failure: " + e);
    }

    @Override
    public void hidDataReceived(HidServicesEvent hidServicesEvent) {
        System.out.println("hidDataReceived");
    }

    @Override public void close() {
        running = false;
        try { if (dev != null && dev.isOpen()) dev.close(); } catch (Exception ignore) {}
        hid.shutdown();
    }
}