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
        
        int firstBrace = response.indexOf('{');
        int firstBracket = response.indexOf('[');
        
        if (firstBrace != -1 && (firstBracket == -1 || firstBrace < firstBracket)) {
            int braces = 0;
            for (int i = firstBrace; i < response.length(); i++) {
                char c = response.charAt(i);
                if (c == '{') braces++;
                if (c == '}') braces--;
                if (braces == 0) {
                    return response.substring(firstBrace, i + 1).trim();
                }
            }
        } else if (firstBracket != -1) {
            int brackets = 0;
            for (int i = firstBracket; i < response.length(); i++) {
                char c = response.charAt(i);
                if (c == '[') brackets++;
                if (c == ']') brackets--;
                if (brackets == 0) {
                    return response.substring(firstBracket, i + 1).trim();
                }
            }
        }
        
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


