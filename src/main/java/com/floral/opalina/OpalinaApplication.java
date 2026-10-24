package com.floral.opalina;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class OpalinaApplication {
	public static void main(String[] args) {
		SpringApplication.run(OpalinaApplication.class, args);
	}

}
