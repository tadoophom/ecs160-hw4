package com.ecs160;

import com.ecs160.microservices.BugFinderMicroservice;
import com.ecs160.microservices.IssueSummarizerMicroservice;
import com.ecs160.microservices.IssueComparatorMicroservice;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

public class AppTest {

    private Gson gson;

    @BeforeEach
    void setUp() {
        gson = new Gson();
    }

    static class FakeOllamaClient extends OllamaClient {
        private String response;
        private boolean shouldThrow = false;

        void setResponse(String r) { this.response = r; }
        void setShouldThrow(boolean t) { this.shouldThrow = t; }

        @Override
        public String generateJsonResponse(String prompt) throws Exception {
            if (shouldThrow) throw new Exception("fake error");
            return response;
        }
    }

    private void injectFake(Object service, FakeOllamaClient fake) throws Exception {
        Field f = service.getClass().getDeclaredField("client");
        f.setAccessible(true);
        f.set(service, fake);
    }

    // BugFinder tests
    @Test
    void bugFinderNullReturnsEmpty() throws Exception {
        BugFinderMicroservice bf = new BugFinderMicroservice();
        assertEquals("[]", bf.findBugs(null));
    }

    @Test
    void bugFinderEmptyStringReturnsEmpty() throws Exception {
        BugFinderMicroservice bf = new BugFinderMicroservice();
        assertEquals("[]", bf.findBugs(""));
    }

    @Test
    void bugFinderWithCode() throws Exception {
        BugFinderMicroservice bf = new BugFinderMicroservice();
        FakeOllamaClient fake = new FakeOllamaClient();
        fake.setResponse("[{\"bug_type\":\"NullPointerDereference\",\"line\":5,\"description\":\"bad ptr\",\"filename\":\"code.c\"}]");
        injectFake(bf, fake);

        String result = bf.findBugs("int* ptr = NULL; *ptr = 10;");
        JsonArray bugs = gson.fromJson(result, JsonArray.class);
        
        assertEquals(1, bugs.size());
        assertEquals("NullPointerDereference", bugs.get(0).getAsJsonObject().get("bug_type").getAsString());
    }

    @Test
    void bugFinderHandlesSingleObj() throws Exception {
        BugFinderMicroservice bf = new BugFinderMicroservice();
        FakeOllamaClient fake = new FakeOllamaClient();
        fake.setResponse("{\"bug_type\":\"BufferOverflow\",\"line\":10,\"description\":\"oob\",\"filename\":\"code.c\"}");
        injectFake(bf, fake);

        String result = bf.findBugs("char buf[10]; buf[20] = 'x';");
        JsonArray arr = gson.fromJson(result, JsonArray.class);
        assertEquals(1, arr.size());
    }

    @Test
    void bugFinderExceptionReturnsEmpty() throws Exception {
        BugFinderMicroservice bf = new BugFinderMicroservice();
        FakeOllamaClient fake = new FakeOllamaClient();
        fake.setShouldThrow(true);
        injectFake(bf, fake);

        assertEquals("[]", bf.findBugs("some code"));
    }
    
    // IssueSummarizer tests
    @Test
    void summarizerBasicTest() throws Exception {
        IssueSummarizerMicroservice s = new IssueSummarizerMicroservice();
        FakeOllamaClient fake = new FakeOllamaClient();
        fake.setResponse("{\"bug_type\":\"MemoryLeak\",\"line\":42,\"description\":\"mem not freed\",\"filename\":\"utils.c\"}");
        injectFake(s, fake);

        String result = s.summarizeIssue("{\"description\":\"Memory leak in utils.c line 42\"}");
        JsonObject obj = gson.fromJson(result, JsonObject.class);
        
        assertEquals("MemoryLeak", obj.get("bug_type").getAsString());
        assertEquals(42, obj.get("line").getAsInt());
    }

    @Test
    void summarizerNoDescription() throws Exception {
        IssueSummarizerMicroservice s = new IssueSummarizerMicroservice();

        String result = s.summarizeIssue("{\"title\":\"some bug\"}");
        JsonObject obj = gson.fromJson(result, JsonObject.class);
        
        assertTrue(obj.get("description").getAsString().contains("Missing"));
    }

    @Test
    void summarizerNullInput() throws Exception {
        IssueSummarizerMicroservice s = new IssueSummarizerMicroservice();
        String result = s.summarizeIssue(null);
        JsonObject obj = gson.fromJson(result, JsonObject.class);
        assertEquals(-1, obj.get("line").getAsInt());
    }

    @Test 
    void summarizerLlmFails() throws Exception {
        IssueSummarizerMicroservice s = new IssueSummarizerMicroservice();
        FakeOllamaClient fake = new FakeOllamaClient();
        fake.setShouldThrow(true);
        injectFake(s, fake);
        
        String result = s.summarizeIssue("{\"description\":\"test\"}");
        assertTrue(result.contains("Failed"));
    }

    // IssueComparator tests 
    @Test
    void comparatorFindsMatch() throws Exception {
        IssueComparatorMicroservice c = new IssueComparatorMicroservice();
        FakeOllamaClient fake = new FakeOllamaClient();
        fake.setResponse("[{\"bug_type\":\"NullPtr\",\"line\":10,\"description\":\"null\",\"filename\":\"main.c\"}]");
        injectFake(c, fake);

        JsonObject input = new JsonObject();
        JsonArray l1 = new JsonArray();
        JsonObject b1 = new JsonObject();
        b1.addProperty("bug_type", "NullPtr");
        b1.addProperty("line", 10);
        b1.addProperty("description", "null check");
        b1.addProperty("filename", "main.c");
        l1.add(b1);
        
        JsonArray l2 = new JsonArray();
        JsonObject b2 = new JsonObject();
        b2.addProperty("bug_type", "NullPointer");
        b2.addProperty("line", 10); 
        b2.addProperty("description", "deref");
        b2.addProperty("filename", "main.c");
        l2.add(b2);

        input.add("list1", l1);
        input.add("list2", l2);

        String result = c.checkEquivalence(input.toString());
        JsonArray matches = gson.fromJson(result, JsonArray.class);
        assertEquals(1, matches.size());
    }

    @Test
    void comparatorNoMatch() throws Exception {
        IssueComparatorMicroservice c = new IssueComparatorMicroservice();
        FakeOllamaClient fake = new FakeOllamaClient();
        fake.setResponse("[]");
        injectFake(c, fake);

        JsonObject input = new JsonObject();
        input.add("list1", new JsonArray());
        input.add("list2", new JsonArray());
        
        assertEquals("[]", c.checkEquivalence(input.toString()));
    }

    @Test
    void comparatorMissingLists() throws Exception {
        IssueComparatorMicroservice c = new IssueComparatorMicroservice();

        // missing list1
        JsonObject input = new JsonObject();
        input.add("list2", new JsonArray());
        assertEquals("[]", c.checkEquivalence(input.toString()));

        // missing list2  
        JsonObject input2 = new JsonObject();
        input2.add("list1", new JsonArray());
        assertEquals("[]", c.checkEquivalence(input2.toString()));
    }

    @Test
    void comparatorBadInput() throws Exception {
        IssueComparatorMicroservice c = new IssueComparatorMicroservice();
        assertEquals("[]", c.checkEquivalence(null));
        assertEquals("[]", c.checkEquivalence("not json"));
    }

    @Test
    void comparatorLlmError() throws Exception {
        IssueComparatorMicroservice c = new IssueComparatorMicroservice();
        FakeOllamaClient fake = new FakeOllamaClient();
        fake.setShouldThrow(true);
        injectFake(c, fake);

        JsonObject input = new JsonObject();
        input.add("list1", new JsonArray());
        input.add("list2", new JsonArray());
        
        assertEquals("[]", c.checkEquivalence(input.toString()));
    }
}