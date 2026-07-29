package com.thirdball.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public Map<String, Object> status() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("application", "Third Ball API");
        response.put("status", "running");
        response.put("players", "/api/players");
        response.put("tournaments", "/api/tournaments");
        response.put("practiceSessions", "/api/practice-sessions");
        return response;
    }
}
