package com.ecs160.hw.util;
import com.ecs160.hw.model.Issue;

public class JsonHandler {
    
    public static String issueToJson(Issue issue) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"bug_type\": \"").append(escape(issue.getBugType() != null ? issue.getBugType() : "unknown")).append("\",");
        sb.append("\"line\": 0,");
        sb.append("\"description\": \"").append(escape(issue.getDescription() != null ? issue.getDescription() : "")).append("\",");
        sb.append("\"filename\": \"").append(escape(issue.getFilename() != null ? issue.getFilename() : "")).append("\"");
        sb.append("}");
        return sb.toString();
    }
    
    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}