package org.example.notifyservice.consumer;

import org.example.notifyservice.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class BookingCreatedConsumer {

    private final EmailService emailService;

    public BookingCreatedConsumer(EmailService emailService) {
        this.emailService = emailService;
    }

    @KafkaListener(topics = "booking-created", groupId = "notify-service")
    public void consume(String email) {
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("Notify Service: Email không hợp lệ!");
        }
        emailService.sendBookingCreatedEmail(email);
    }
}
