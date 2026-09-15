package com.harmoniedev.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class HarmonieDevApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(HarmonieDevApiApplication.class, args);
	}

}
