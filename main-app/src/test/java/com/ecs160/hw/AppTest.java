package com.ecs160.hw;

import static org.junit.Assert.*;
import com.ecs160.hw.model.Issue;
import com.ecs160.hw.util.JsonHandler;
import org.junit.Test;

public class AppTest {

    @Test
    public void testIssueCreation() {
        Issue issue = new Issue("NullPointer", 42, "bad pointer", "main.c");
        assertEquals("NullPointer", issue.getBugType());
        assertEquals(42, issue.getLine());
        assertEquals("bad pointer", issue.getDescription());
        assertEquals("main.c", issue.getFilename());
    }

    @Test
    public void testIssueWithId() {
        Issue issue = new Issue("iss-123", "MemoryLeak", 10, "leak found", "util.c");
        assertEquals("iss-123", issue.getIssueId());
    }

    @Test
    public void testSetters() {
        Issue issue = new Issue();
        issue.setBugType("BufferOverflow");
        issue.setLine(99);
        assertEquals("BufferOverflow", issue.getBugType());
        assertEquals(99, issue.getLine());
    }

    @Test
    public void testEquals() {
        Issue a = new Issue("iss-1", "Bug", 10, "desc", "file.c");
        Issue b = new Issue("iss-1", "Bug", 10, "desc", "file.c");
        assertEquals(a, b);
        
        Issue c = new Issue("iss-2", "Bug", 10, "desc", "file.c");
        assertNotEquals(a, c);
    }

    @Test
    public void testJsonHandler() {
        Issue issue = new Issue("NullPointer", 0, "null ref", "main.c");
        String json = JsonHandler.issueToJson(issue);
        assertTrue(json.contains("\"bug_type\": \"NullPointer\""));
        assertTrue(json.contains("\"description\": \"null ref\""));
    }

    @Test
    public void testJsonNulls() {
        Issue issue = new Issue();
        String json = JsonHandler.issueToJson(issue);
        assertTrue(json.contains("\"bug_type\": \"unknown\""));
    }

    @Test
    public void testJsonEscaping() {
        Issue issue = new Issue("Bug", 0, "said \"hello\"\nbye", "test.c");
        String json = JsonHandler.issueToJson(issue);
        assertTrue(json.contains("\\\""));
        assertTrue(json.contains("\\n"));
    }
}