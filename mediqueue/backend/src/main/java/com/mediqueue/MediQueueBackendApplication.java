package com.mediqueue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MediQueueBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(MediQueueBackendApplication.class, args);
        System.out.println("==================================================================");
        System.out.println("   MediQueue Hospital Analytics Backend Started on Port 8080      ");
        System.out.println("   REST APIs available at: http://localhost:8080/api/overview    ");
        System.out.println("==================================================================");
    }
}
