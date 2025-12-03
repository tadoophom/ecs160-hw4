package com.ecs160.hw2;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        String response = extractJsonField(responseText.toString(), "response");
        return response;
    }

    private String extractJsonField(String json, String fieldName) {
        Pattern pattern = Pattern.compile("\"" + fieldName + "\":\"((?:[^\"\\\\]|\\.)*)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return unescapeJson(matcher.group(1));
        }
        return json;
    }

    private String unescapeJson(String text) {
        return text.replace("\\\"", "\"").replace("\\n", "\n").replace("\\r", "\r");
    }
}


