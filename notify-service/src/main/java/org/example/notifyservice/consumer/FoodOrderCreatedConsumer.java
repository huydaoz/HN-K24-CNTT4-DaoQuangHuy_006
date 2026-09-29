package org.example.notifyservice.consumer;

import org.example.notifyservice.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class FoodOrderCreatedConsumer {

    private static final Logger log = LoggerFactory.getLogger(FoodOrderCreatedConsumer.class);
    private final EmailService emailService;

    public FoodOrderCreatedConsumer(EmailService emailService) {
        this.emailService = emailService;
    }

    @KafkaListener(topics = "${notification.kafka.food-order-created-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String email) {
        if (!StringUtils.hasText(email)) {
            log.warn("Received empty email in food-order-created topic");
            return;
        }
        emailService.sendFoodOrderCreatedEmail(email.trim());
    }
}
