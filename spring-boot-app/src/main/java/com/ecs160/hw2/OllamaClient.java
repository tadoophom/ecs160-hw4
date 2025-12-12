package com.ecs160.hw2;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OllamaClient {

    public String generateJsonResponse(String prompt) throws Exception {
        URL url = new URL("http://localhost:11434/api/generate");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");

        String body = "{\"model\":\"deepcoder:latest\",\"prompt\":\"" + escapeJson(prompt) + "\",\"stream\":false}";

        try (OutputStream out = connection.getOutputStream()) {
            out.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int status = connection.getResponseCode();
        if (status != 200) {
            throw new RuntimeException("Ollama /api/generate failed: HTTP " + status);
        }

        String responseText = readAll(connection.getInputStream());

        String response = extractJsonField(responseText, "response");
        
        response = response.replaceAll("(?s)<think>.*?</think>", "");
        
        if (response.contains("```json")) {
            int startIndex = response.indexOf("```json") + 7;
            int endIndex = response.indexOf("```", startIndex);
            if (endIndex > startIndex) {
                response = response.substring(startIndex, endIndex).trim();
            }
        }
        
        int firstBrace = response.indexOf('{');
        int firstBracket = response.indexOf('[');

        String extracted = response;
        if (firstBrace != -1 && (firstBracket == -1 || firstBrace < firstBracket)) {
            int braces = 0;
            for (int i = firstBrace; i < response.length(); i++) {
                char c = response.charAt(i);
                if (c == '{') braces++;
                if (c == '}') braces--;
                if (braces == 0) {
                    extracted = response.substring(firstBrace, i + 1).trim();
                    break;
                }
            }
        } else if (firstBracket != -1) {
            int brackets = 0;
            for (int i = firstBracket; i < response.length(); i++) {
                char c = response.charAt(i);
                if (c == '[') brackets++;
                if (c == ']') brackets--;
                if (brackets == 0) {
                    extracted = response.substring(firstBracket, i + 1).trim();
                    break;
                }
            }
        }

        System.out.println("Ollama output:\n" + extracted);
        return extracted;
    }

    private String readAll(java.io.InputStream in) throws Exception {
        if (in == null) {
            return "";
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString().trim();
        }
    }

    private String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(text.length() + 16);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
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


