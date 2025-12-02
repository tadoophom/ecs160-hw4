package com.ecs160.hw2;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ModerationServiceController {

    @GetMapping("/find_bugs")
    public String findBugs(@RequestParam String code) {
        return "[]";
    }

    @GetMapping("/summarize_issue")
    public String summarizeIssue(@RequestParam String issue) {
        return "{}";
    }
}
