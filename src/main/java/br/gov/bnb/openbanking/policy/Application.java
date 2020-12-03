package br.gov.bnb.openbanking.policy;

import org.apache.camel.opentracing.starter.CamelOpenTracing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@CamelOpenTracing
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
