package com.restaurantqr.platform.controller;

import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TestComponent {

    @PostConstruct
    public void init() {
        log.info("TestComponent in controller package initialized");
    }
}