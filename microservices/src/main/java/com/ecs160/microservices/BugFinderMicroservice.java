package com.ecs160.microservices;

import com.ecs160.OllamaClient;
import com.ecs160.annotations.Microservice;
import com.ecs160.annotations.Endpoint;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

@Microservice  
public class BugFinderMicroservice {

    private OllamaClient client;
    private Gson gson;

    public BugFinderMicroservice() {
        this.client=new OllamaClient();
        this.gson=new Gson();
    }

    @Endpoint(url = "/find_bugs")  
    public String findBugs(String code) {
        if(code==null || code.isEmpty()) {
            return "[]";
        }

        String prompt = "You are a bug detection system. Your ONLY job is to return JSON.\n\n" +
                       "Analyze this C code for bugs. Return a JSON array of bugs.\n\n" +
                       "Required format - each bug must have these exact fields:\n" +
                       "bug_type: string (e.g. \"NullPointerDereference\")\n" +
                       "line: number (approximate line number)\n" +
                       "description: string (what's wrong)\n" +
                       "filename: string (use \"code.c\")\n\n" +
                       "Example output:\n" +
                       "[{\"bug_type\":\"NullPointerDereference\",\"line\":2,\"description\":\"Dereferencing null pointer\",\"filename\":\"code.c\"}]\n\n" +
                       "If NO bugs found, return: []\n\n" +
                       "Do NOT write explanations. Return ONLY the JSON array.\n\n" +
                       "Code:\n" + code;

        try {
            String response = client.generateJsonResponse(prompt);
            
            response = response.replaceAll(",\\s*}", "}").replaceAll(",\\s*]", "]");
            
            try {
                JsonArray bugs = gson.fromJson(response, JsonArray.class);
                return gson.toJson(bugs);
            } catch(Exception e) {
                JsonObject singleBug = gson.fromJson(response, JsonObject.class);
                JsonArray bugArray = new JsonArray();
                bugArray.add(singleBug);
                return gson.toJson(bugArray);
            }
        } catch(Exception e) {
            return "[]";
        }
    }
}