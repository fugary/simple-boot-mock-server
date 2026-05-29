package com.fugary.simple.mock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SimpleBootMockServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimpleBootMockServerApplication.class, args);
    }

}
