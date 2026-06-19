package com.lovers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LoversApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoversApplication.class, args);
    }
}
