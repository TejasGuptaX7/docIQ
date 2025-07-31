package com.vectormind.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.vectormind.api")
public class ApiApplication {

    public static void main(String[] args) {
        // Don't try to load environment variables here - let Spring do it
        SpringApplication.run(ApiApplication.class, args);
    }
}