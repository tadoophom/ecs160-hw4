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
                       "[{\"bug_type\":\"NullPointerDereference\",\"line\":2,\"description\":\"Dereferencing null pointer\",\"filename\":\"code.c\"}]\n\n" +
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
        return "{}";
    }

    @GetMapping("/compare_issues")
    public String compareIssues(@RequestParam String issue1, @RequestParam String issue2) {
        return "{}";
    }
}
