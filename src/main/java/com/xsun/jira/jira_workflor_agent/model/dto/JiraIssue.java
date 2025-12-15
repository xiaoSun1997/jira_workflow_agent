package com.xsun.jira.jira_workflor_agent.model.dto;


import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * @program: JiraIssue.java
 * @description:
 * @author: sunmouren
 * @create: 2025-12-14
 **/
@Getter
@Setter
@Accessors(chain=true)
public class JiraIssue {
    private String key;
    private String summary;
    private String description;
    private String status;
    private LocalDateTime created;
    private LocalDateTime updated;
}
