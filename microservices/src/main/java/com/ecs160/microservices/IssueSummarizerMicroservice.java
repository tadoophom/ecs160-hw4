package com.ecs160.microservices;

import com.ecs160.OllamaClient;
import com.ecs160.annotations.Microservice;
import com.ecs160.annotations.Endpoint;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

@Microservice
public class IssueSummarizerMicroservice {

    private OllamaClient client;
    private Gson gson;

    public IssueSummarizerMicroservice() {
        this.client=new OllamaClient();
        this.gson=new Gson();
    }

    @Endpoint(url = "/summarize_issue")
    public String summarizeIssue(String issueJson) {
        JsonObject issue=gson.fromJson(issueJson, JsonObject.class);
        if(issue==null || !issue.has("description")) {
            return errorResponse("Missing or invalid description field");
        }

        String description=issue.get("description").getAsString();
        
        String prompt = "You are a bug analysis system. Return ONLY valid JSON.\n\n" +
                   "Analyze this GitHub issue and extract bug information.\n\n" +
                   "Required format:\n" +
                   "bug_type: string (type of bug, e.g. \"NullPointerException\")\n" +
                   "line: number (line number mentioned, or -1 if not specified)\n" +
                   "description: string (brief summary of the issue)\n" +
                   "filename: string (file mentioned, or \"unknown\" if not specified)\n\n" +
                   "Example: {\"bug_type\":\"NullPointerException\",\"line\":42,\"description\":\"Null pointer error in auth module\",\"filename\":\"auth.c\"}\n\n" +
                   "Return ONLY the JSON object. Do NOT add explanations.\n\n" +
                   "GitHub Issue:\n" + description;


        try {
            String llmOutput=client.generateJsonResponse(prompt);
            gson.fromJson(llmOutput, JsonObject.class);
            return llmOutput;
        } catch(Exception e) {
            return errorResponse("Failed to process: "+e.getMessage());
        }
    }

    private String errorResponse(String msg) {
        return String.format(
            "{\"bug_type\":\"\",\"line\":-1,\"description\":\"%s\",\"filename\":\"\"}", 
            msg
        );
    }
}