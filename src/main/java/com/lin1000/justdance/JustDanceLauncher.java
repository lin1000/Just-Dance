package com.lin1000.justdance;

import java.net.URL;
import java.net.URLClassLoader;
import com.lin1000.justdance.Project;

public class JustDanceLauncher {

    public static void main(String args[]){

        System.out.println("Entering JustDanceLauncher");

        //Print Current Directory
        String currentDirectory = System.getProperty("user.dir");
        System.out.println("Current working directory: " + currentDirectory);

        System.out.println("Current URLClassLoader Classpath:====START");
        // Get the ClassLoader of the current thread
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        // Check if the ClassLoader is an instance of URLClassLoader
        if (classLoader instanceof URLClassLoader) {
            URL[] classpath1 = ((URLClassLoader) classLoader).getURLs();
            System.out.println("Classpath:");
            for (URL url : classpath1) {
                System.out.println(url.getFile());
            }
        } else {
            System.out.println("The current ClassLoader is not an instance of URLClassLoader.");
        }
        System.out.println("Current URLClassLoader Classpath:====END");

        // Create an instance of Project


        Project.gameStart();

//        try {
//            while(true){
//                    Thread.sleep(2000);
//            }
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }

//        try {
//            HidGamepad hidGamepad = new HidGamepad();
//        }catch(Exception e){
//            e.printStackTrace();
//        }

//        HidServices services = HidManager.getHidServices();
//        services.start();
//        System.out.println("services.getAttachedHidDevices().size()="+ services.getAttachedHidDevices().size());
//        for (HidDevice d : services.getAttachedHidDevices()) {
//            System.out.printf(
//                    "Product=%s  VID=%04x PID=%04x  usagePage=0x%02x usage=0x%02x  path=%s%n",
//                    d.getProduct(), d.getVendorId(), d.getProductId(),
//                    d.getUsagePage(), d.getUsage(), d.getPath()
//            );
//        }
//        services.shutdown();

    }
}
