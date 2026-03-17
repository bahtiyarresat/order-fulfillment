package com.example.fulfillment;

import io.camunda.client.annotation.Deployment;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Deployment(resources = "classpath:order-fulfillment.bpmn")
public class OrderFulfillmentApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderFulfillmentApplication.class, args);
    }

}
