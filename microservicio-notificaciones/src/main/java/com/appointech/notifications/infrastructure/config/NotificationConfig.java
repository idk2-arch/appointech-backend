package com.appointech.notifications.infrastructure.config;

import com.appointech.notifications.application.EmailNotificationService;
import com.appointech.notifications.domain.ports.EmailSender;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the framework-free application layer into the Spring context. */
@Configuration
public class NotificationConfig {

    @Bean
    public EmailNotificationService emailNotificationService(EmailSender emailSender) {
        return new EmailNotificationService(emailSender);
    }
}
