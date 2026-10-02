package org.serviceproject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ServiceProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceProjectApplication.class, args);
    }

}
