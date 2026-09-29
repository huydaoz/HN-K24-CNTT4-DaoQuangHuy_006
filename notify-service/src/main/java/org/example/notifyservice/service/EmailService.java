package org.example.notifyservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String from;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${notification.mail.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    public void sendFoodOrderCreatedEmail(String recipient) {
        String cleanRecipient = recipient == null ? "" : recipient.trim();
        if (!StringUtils.hasText(cleanRecipient)) {
            throw new IllegalArgumentException("Recipient email must not be blank");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(cleanRecipient);
        message.setSubject("Đơn hàng đã được tạo thành công");
        message.setText("Đơn hàng của bạn đã được tạo thành công. Cảm ơn bạn đã đặt hàng!");
        mailSender.send(message);
    }
}
