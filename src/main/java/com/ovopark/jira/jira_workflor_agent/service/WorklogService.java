package com.ovopark.jira.jira_workflor_agent.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


/**
 * @program: WorklogService.java
 * @description:
 * @author: sunmouren
 * @create: 2025-12-15
 **/
@Slf4j
@Service
@RequiredArgsConstructor
public class WorklogService {

    private final WorklogAgent worklogAgent;

    /**
     * 使用Agent进行智能对话
     */
    public String chatWithAgent(String message) {
        return worklogAgent.chat(message);
    }

}
