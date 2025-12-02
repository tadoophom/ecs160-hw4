package com.ecs160.hw.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MicroserviceCaller {
    private final String baseUrl;
    
    public MicroserviceCaller(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    public String post(String path, String body) throws Exception {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        
        URL url = new URL(baseUrl + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true); 
            
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = body.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            if (conn.getResponseCode() == 200) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line).append('\n');
                    }
                    return response.toString().trim();
                }
            } else {
                throw new RuntimeException("Failed : " + conn.getResponseCode());
            }
        } finally {
            conn.disconnect();
        }
    }
}