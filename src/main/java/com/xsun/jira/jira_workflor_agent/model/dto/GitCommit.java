package com.xsun.jira.jira_workflor_agent.model.dto;


import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * @program: GitCommit.java
 * @description:
 * @author: sunmouren
 * @create: 2025-12-14
 **/
@Getter
@Setter
@Accessors(chain=true)
public class GitCommit {
    private String commitId;
    private String author;
    private String message;
    private LocalDateTime commitTime;
    private String repository;
    private int filesChanged;
    private int additions;
    private int deletions;
}
