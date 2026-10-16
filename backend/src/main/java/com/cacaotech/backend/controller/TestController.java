package com.cacaotech.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/publico")
public class TestController {

    @GetMapping("/ping")
    public String ping() {
        return "¡La API de Cacao Tech está viva y respondiendo!";
    }
}