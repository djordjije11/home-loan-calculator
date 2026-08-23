package io.github.djordjije11;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
class HomeLoanCalculatorApplication {

    static void main(String[] args) {
        SpringApplication.run(HomeLoanCalculatorApplication.class, args);
    }
}
