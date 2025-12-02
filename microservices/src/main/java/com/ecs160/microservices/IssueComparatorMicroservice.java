package com.ecs160.microservices;

import com.ecs160.OllamaClient;
import com.ecs160.annotations.Microservice;
import com.ecs160.annotations.Endpoint;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

@Microservice
public class IssueComparatorMicroservice {

    private OllamaClient client;
    private Gson gson;

    public IssueComparatorMicroservice() {
        this.client=new OllamaClient();
        this.gson=new Gson();
    }

    @Endpoint(url = "/check_equivalence")
    public String checkEquivalence(String input) {
        try {
            JsonObject data=gson.fromJson(input, JsonObject.class);
            if(data==null || !data.has("list1") || !data.has("list2")) {
                return "[]";
            }

            JsonArray firstList=data.getAsJsonArray("list1");
            JsonArray secondList=data.getAsJsonArray("list2");

            String prompt="Compare these two bug lists and return bugs from the first list "+
                          "that match bugs in the second list. Consider bugs matching if they "+
                          "describe the same issue even if worded differently.\n"+
                          "First list: "+firstList.toString()+"\n"+
                          "Second list: "+secondList.toString()+"\n"+
                          "Return a JSON array of matching bugs, or [] if no matches.";

            String response=client.generateJsonResponse(prompt);
            JsonArray matches=gson.fromJson(response, JsonArray.class);
            
            return matches!=null ? gson.toJson(matches) : "[]";
            
        } catch(Exception e) {
            return "[]";
        }
    }
}