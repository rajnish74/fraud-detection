package com.rift;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MoneyMulingEngineApplication {

	public static void main(String[] args) {

        SpringApplication.run(MoneyMulingEngineApplication.class, args);
        System.out.println("🚀 Money Muling Detector started on http://localhost:8080");
        System.out.println("📝 Upload CSV at http://localhost:8080");
        System.out.println("🔍 Test API at http://localhost:8080/api/health");
	}

}
