package com.ecs160.hw2;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class OllamaClient {

    public String generateJsonResponse(String prompt) throws Exception {
        URL url = new URL("http://localhost:11434/api/generate");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");

        String body = "{\"model\":\"deepcoder:latest\",\"prompt\":\"" + prompt + "\",\"stream\":false}";
        
        OutputStream out = connection.getOutputStream();
        out.write(body.getBytes());
        out.close();

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder responseText = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            responseText.append(line);
        }
        reader.close();

        return responseText.toString();
    }
}


