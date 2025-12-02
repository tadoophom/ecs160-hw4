package com.ecs160;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class OllamaClient {

    private Gson gson;

    public OllamaClient() {
        gson=new Gson();
    }

    public String generateJsonResponse(String prompt) throws Exception {
        URL url=new URL("http://localhost:11434/api/generate");
        HttpURLConnection connection=(HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");

        String fullPrompt="Return ONLY valid JSON. "+prompt;
        String escaped=fullPrompt.replace("\"", "\\\"").replace("\n", "\\n");
        String body="{\"model\":\"deepcoder:latest\",\"prompt\":\""+escaped+"\",\"stream\":false}";

        OutputStream out=connection.getOutputStream();
        out.write(body.getBytes());
        out.close();

        BufferedReader reader=new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String responseLine;
        StringBuilder responseText=new StringBuilder();
        while((responseLine=reader.readLine())!=null) {
            responseText.append(responseLine);
        }
        reader.close();

        JsonObject responseJson=gson.fromJson(responseText.toString(), JsonObject.class);
        String text=responseJson.get("response").getAsString();

        System.out.println("ollama raw text: "+text.substring(0, Math.min(500, text.length())));

        text=text.replaceAll("(?s)<think>.*?</think>", "");
        
        if(text.contains("```json")) {
            int startIndex=text.indexOf("```json")+7;
            int endIndex=text.indexOf("```", startIndex);
            if(endIndex>startIndex) {
                text=text.substring(startIndex, endIndex).trim();
            }
        }

        System.out.println("ollama after cleaning: "+text.substring(0, Math.min(200, text.length())));

        int firstBrace=text.indexOf('{');
        int firstBracket=text.indexOf('[');
        
        if(firstBrace!=-1 && (firstBracket==-1 || firstBrace<firstBracket)) {
            int braces=0;
            for(int i=firstBrace; i<text.length(); i++) {
                char c=text.charAt(i);
                if(c=='{') braces++;
                if(c=='}') braces--;
                if(braces==0) {
                    return text.substring(firstBrace, i+1).trim();
                }
            }
        }
        else if(firstBracket!=-1) {
            int brackets=0;
            for(int i=firstBracket; i<text.length(); i++) {
                char c=text.charAt(i);
                if(c=='[') brackets++;
                if(c==']') brackets--;
                if(brackets==0) {
                    return text.substring(firstBracket, i+1).trim();
                }
            }
        }

        return text.trim();
    }
}
