package com.ecs160.hw.model;

import com.ecs160.persistence.annotations.Id;
import com.ecs160.persistence.annotations.PersistableField;
import com.ecs160.persistence.annotations.PersistableObject;

import java.util.List;

@PersistableObject
public class Repo {
    @Id
    @PersistableField
    public String repoId;

    @PersistableField
    public String language;

    @PersistableField
    public List<Issue> issues;
}
