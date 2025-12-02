package com.ecs160;

import com.ecs160.Launcher;

public class App {

    public static void main(String[] args) {
        try {
            Launcher launcher = new Launcher();
            launcher.scanForMicroservices("com.ecs160.microservices");

            System.out.println("Starting server on port 8080");
            launcher.launch(8080);

        } catch (Exception e) {
            System.out.println("Error starting server: " + e.getMessage());
        }
    }
}