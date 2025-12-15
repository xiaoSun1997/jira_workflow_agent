package com.ovopark.jira.jira_workflor_agent.model.dto;


import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * @program: WorklogEntry.java
 * @description:
 * @author: sunmouren
 * @create: 2025-12-15
 **/
@Getter
@Setter
@Accessors(chain=true)
public class WorklogEntry {
    private String issueKey;
    private double timeSpentHours;
    private String comment;
    private LocalDateTime started;
    /**
     * 相似度分数
     */
    private double similarityScore;
}
