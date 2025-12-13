package com.ecs160.hw.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class MicroserviceCaller {
    private final String baseUrl;
    
    public MicroserviceCaller(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    public String get(String path, Map<String, String> queryParams) throws Exception {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            if (query.length() > 0) {
                query.append('&');
            }
            query.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            query.append('=');
            query.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }

        String urlString = baseUrl + path + (query.length() == 0 ? "" : "?" + query);
        URL url = URI.create(urlString).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        try {
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() == 200) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line).append('\n');
                    }
                    return response.toString().trim();
                }
            }

            throw new RuntimeException("Failed : " + conn.getResponseCode());
        } finally {
            conn.disconnect();
        }
    }
}