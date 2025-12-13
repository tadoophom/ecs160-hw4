package com.ecs160.hw2;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ModerationServiceController {

    private OllamaClient client = new OllamaClient();

    @GetMapping("/find_bugs")
    public String findBugs(@RequestParam String code) {
        if (code == null || code.isEmpty()) {
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
                "[{\"bug_type\":\"NullPointerDereference\",\"line\":2,\"description\":\"Dereferencing null pointer\",\"filename\":\"code.c\"}]\n\n"
                +
                "If NO bugs found, return: []\n\n" +
                "Do NOT write explanations. Return ONLY the JSON array.\n\n" +
                "Code:\n" + code;

        try {
            String response = client.generateJsonResponse(prompt);
            response = response.replaceAll(",\\s*}", "}").replaceAll(",\\s*]", "]");
            return response;
        } catch (Exception e) {
            return "[]";
        }
    }

    @GetMapping("/summarize_issue")
    public String summarizeIssue(@RequestParam String issue) {
        if (issue == null || issue.isEmpty()) {
            return "{}";
        }

        String prompt = "You are a bug analysis system. Return ONLY valid JSON.\n\n" +
                "Analyze this GitHub issue and extract bug information.\n\n" +
                "Required format:\n" +
                "bug_type: string (type of bug, e.g. \"NullPointerException\")\n" +
                "line: number (line number mentioned, or -1 if not specified)\n" +
                "description: string (brief summary of the issue)\n" +
                "filename: string (file mentioned, or \"unknown\" if not specified)\n\n" +
                "Example: {\"bug_type\":\"NullPointerException\",\"line\":42,\"description\":\"Null pointer error in auth module\",\"filename\":\"auth.c\"}\n\n"
                +
                "Return ONLY the JSON object. Do NOT add explanations.\n\n" +
                "GitHub Issue:\n" + issue;

        try {
            String response = client.generateJsonResponse(prompt);
            return response;
        } catch (Exception e) {
            return "{\"bug_type\":\"\",\"line\":-1,\"description\":\"Failed to process\",\"filename\":\"\"}";
        }
    }

    @GetMapping("/compare_issues")
    public String compareIssues(@RequestParam String issue1, @RequestParam String issue2) {
        if (issue1 == null || issue1.isEmpty() || issue2 == null || issue2.isEmpty()) {
            return "{}";
        }

        String prompt = "Compare these two GitHub issues and determine if they describe the same bug.\n\n" +
                "Return a JSON object with:\n" +
                "is_same_issue: boolean (true if they describe the same bug)\n" +
                "confidence: number (0.0 to 1.0)\n" +
                "explanation: string (brief reasoning)\n\n" +
                "Example: {\"is_same_issue\":true,\"confidence\":0.95,\"explanation\":\"Both describe null pointer in auth module\"}\n\n"
                +
                "Return ONLY the JSON object. Do NOT add explanations.\n\n" +
                "Issue 1:\n" + issue1 + "\n\n" +
                "Issue 2:\n" + issue2;

        try {
            String response = client.generateJsonResponse(prompt);
            return response;
        } catch (Exception e) {
            return "{\"is_same_issue\":false,\"confidence\":0.0,\"explanation\":\"Error processing\"}";
        }
    }

    @GetMapping("/check_equivalence")
    public String checkEquivalence(@RequestParam String body) {
        String prompt = "You must return ONLY valid JSON. No explanations, no thinking process, no extra text.\n\n" +
                "Compare these two bug lists and return a JSON object with:\n" +
                "- equivalent: boolean (true if lists describe same bugs)\n" +
                "- confidence: number between 0.0 and 1.0\n" +
                "- explanation: string (one short sentence)\n\n" +
                "Input data:\n" + body + "\n\n" +
                "Return ONLY the JSON object, nothing else:";

        try {
            String result = client.generateJsonResponse(prompt);
            return result;
        } catch (Exception e) {
            return "{\"equivalent\":false,\"confidence\":0.0,\"explanation\":\"Error processing\"}";
        }
    }
}
