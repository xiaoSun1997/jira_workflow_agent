package com.ovopark.jira.jira_workflor_agent.controller;

import com.ovopark.jira.jira_workflor_agent.service.WorklogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
/**
 * @program: WorklogController.java
 * @description:
 * @author: sunmouren
 * @create: 2025-12-15
 **/
@RestController
@RequestMapping("/api/worklog")
@RequiredArgsConstructor
public class WorklogController {

    private final WorklogService worklogService;

    /**
     * 与Agent对话
     */
    @GetMapping("/chat")
    public ResponseEntity<String> chat(@RequestParam(value = "message",required = false) String message) {
        String response = worklogService.chatWithAgent(message);
        return ResponseEntity.ok(response);
    }
}
