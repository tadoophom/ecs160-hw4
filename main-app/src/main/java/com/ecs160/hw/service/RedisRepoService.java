package com.ecs160.hw.service;

import com.ecs160.hw.model.Repo;
import com.ecs160.persistence.RedisDB;

public class RedisRepoService {
    private final RedisDB redisDB = new RedisDB();

    public Repo loadRepo(String repoId) {
        if (repoId == null || repoId.isEmpty()) {
            throw new IllegalArgumentException("repoId required");
        }
        Repo template = new Repo();
        template.repoId = repoId;
        return (Repo) redisDB.load(template);
    }
}
