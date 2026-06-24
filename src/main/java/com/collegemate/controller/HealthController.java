package com.collegemate.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/api/isAlive")
    public String isAlive() {
        return "Alive";
    }
}
