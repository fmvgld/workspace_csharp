package com.gescal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GesCalApplication {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(GesCalApplication.class);
		application.setHeadless(false);
		application.run(args);
	}

}
