package com.ecs160.persistence;

import com.ecs160.persistence.annotations.Id;
import com.ecs160.persistence.annotations.PersistableField;
import com.ecs160.persistence.annotations.PersistableObject;
import com.ecs160.persistence.annotations.LazyLoad;
import javassist.util.proxy.Proxy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.Jedis;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RedisDBTest {

    private static final String SIMPLE_ENTITY_KEY = "redisdb-test-entity-1";

    private Jedis jedis;
    private RedisDB redisDB;

    @BeforeEach
    void setUp() {
        jedis = new Jedis("localhost", 6379);
        redisDB = new RedisDB();
        jedis.del(SIMPLE_ENTITY_KEY);
    }

    @AfterEach
    void tearDown() {
        if (jedis != null) {
            jedis.del(SIMPLE_ENTITY_KEY);
            jedis.close();
        }
    }

    @Test
    void persistAndLoadSimpleEntity() {
        SimpleEntity entity = new SimpleEntity();
        entity.id = SIMPLE_ENTITY_KEY;
        entity.name = "Sample";
        entity.count = 42;

        assertTrue(redisDB.persist(entity), "persist should succeed");

        SimpleEntity template = new SimpleEntity();
        template.id = SIMPLE_ENTITY_KEY;

        SimpleEntity loaded = (SimpleEntity) redisDB.load(template);
        assertNotNull(loaded, "loaded entity should not be null");
        assertEquals("Sample", loaded.name);
        assertEquals(42, loaded.count);
    }

    @Test
    void persistRepoStoresIssueIdsAsCsv() {
        RepoRecord repo = new RepoRecord();
        repo.repoId = "repo:redisdb-test-2";
        IssueRecord issue1 = new IssueRecord();
        issue1.issueId = "issue:test-1";
        issue1.description = "First issue";
        IssueRecord issue2 = new IssueRecord();
        issue2.issueId = "issue:test-2";
        issue2.description = "Second issue";
        repo.issues = new java.util.ArrayList<>();
        repo.issues.add(issue1);
        repo.issues.add(issue2);

        assertTrue(redisDB.persist(repo), "persisting repo with issues should succeed");

        String storedCsv = jedis.hget(repo.repoId, "issues");
        assertNotNull(storedCsv, "issues CSV should be stored");
        assertEquals("issue:test-1,issue:test-2", storedCsv);

        jedis.del(repo.repoId, issue1.issueId, issue2.issueId);
    }

    @Test
    void loadRepoPopulatesIssueList() {
        RepoRecord repo = new RepoRecord();
        repo.repoId = "repo:redisdb-test-3";
        IssueRecord issue1 = new IssueRecord();
        issue1.issueId = "issue:test-3";
        issue1.description = "Third issue";
        IssueRecord issue2 = new IssueRecord();
        issue2.issueId = "issue:test-4";
        issue2.description = "Fourth issue";
        repo.issues = new java.util.ArrayList<>();
        repo.issues.add(issue1);
        repo.issues.add(issue2);

        assertTrue(redisDB.persist(repo), "persisting repo should succeed");

        RepoRecord template = new RepoRecord();
        template.repoId = repo.repoId;
        RepoRecord loaded = (RepoRecord) redisDB.load(template);
        assertNotNull(loaded);
        assertNotNull(loaded.issues);
        assertEquals(2, loaded.issues.size());
        assertEquals("issue:test-3", loaded.issues.get(0).issueId);
        assertEquals("Third issue", loaded.issues.get(0).description);
        assertEquals("issue:test-4", loaded.issues.get(1).issueId);
        assertEquals("Fourth issue", loaded.issues.get(1).description);

        jedis.del(repo.repoId, issue1.issueId, issue2.issueId);
    }

    @Test
    void invalidAnnotationsThrowException() {
        InvalidEntity invalid = new InvalidEntity();
        invalid.name = "bad";
        assertThrows(IllegalArgumentException.class, () -> redisDB.persist(invalid));

        SimpleEntity template = new SimpleEntity();
        assertThrows(IllegalArgumentException.class, () -> redisDB.load(template));
    }

    @Test
    void testLazyLoading() {
        LazyEntity entity = new LazyEntity();
        entity.id = "lazy:1";
        entity.content = "Lazy Content";

        assertTrue(redisDB.persist(entity));

        LazyEntity template = new LazyEntity();
        template.id = "lazy:1";

        LazyEntity loaded = (LazyEntity) redisDB.load(template);

        assertTrue(loaded instanceof Proxy, "Loaded object should be a proxy");

        assertNull(loaded.content, "Field should be null before access");

        assertEquals("Lazy Content", loaded.getContent());

        assertEquals("Lazy Content", loaded.content, "Field should be populated after access");

        jedis.del(entity.id);
    }

    @Test
    void testLazyLoadingList() {
        LazyListEntity entity = new LazyListEntity();
        entity.id = "lazyList:1";

        IssueRecord issue = new IssueRecord();
        issue.issueId = "issue:lazy-1";
        issue.description = "Lazy Issue";

        entity.issues = new java.util.ArrayList<>();
        entity.issues.add(issue);

        assertTrue(redisDB.persist(entity));

        LazyListEntity template = new LazyListEntity();
        template.id = "lazyList:1";

        LazyListEntity loaded = (LazyListEntity) redisDB.load(template);

        assertTrue(loaded instanceof Proxy);
        assertNull(loaded.issues);

        List<IssueRecord> loadedIssues = loaded.getIssues();
        assertNotNull(loadedIssues);
        assertEquals(1, loadedIssues.size());
        assertEquals("Lazy Issue", loadedIssues.get(0).description);

        jedis.del(entity.id, issue.issueId);
    }

    @PersistableObject
    static class SimpleEntity {
        @Id
        @PersistableField
        String id;

        @PersistableField
        String name;

        @PersistableField
        int count;
    }

    @PersistableObject
    static class IssueRecord {
        @Id
        @PersistableField
        String issueId;

        @PersistableField
        String description;
    }

    @PersistableObject
    static class RepoRecord {
        @Id
        @PersistableField
        String repoId;

        @PersistableField
        List<IssueRecord> issues;
    }

    @PersistableObject
    public static class LazyEntity {
        @Id
        @PersistableField
        public String id;

        @PersistableField
        public String content;

        @LazyLoad(field = "content")
        public String getContent() {
            return content;
        }
    }

    @PersistableObject
    public static class LazyListEntity {
        @Id
        @PersistableField
        public String id;

        @PersistableField
        public List<IssueRecord> issues;

        @LazyLoad(field = "issues")
        public List<IssueRecord> getIssues() {
            return issues;
        }
    }

    static class InvalidEntity {
        String name;
    }
}
