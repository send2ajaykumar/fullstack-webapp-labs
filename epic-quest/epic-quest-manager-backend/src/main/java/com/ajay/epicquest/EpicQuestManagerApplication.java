package com.ajay.epicquest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@EnableCaching
public class EpicQuestManagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(EpicQuestManagerApplication.class, args);
	}

}
