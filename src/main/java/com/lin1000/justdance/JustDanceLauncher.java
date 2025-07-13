package com.lin1000.justdance;

import com.lin1000.justdance.gamepanel.Project;

import java.net.URL;
import java.net.URLClassLoader;

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
    }
}
