package com.appointech.notifications;

import com.appointech.notifications.application.EmailNotificationService;
import com.appointech.notifications.domain.ports.EmailSender;
import com.appointech.notifications.infrastructure.adapters.SmtpEmailSender;
import com.appointech.notifications.infrastructure.controllers.NotificationController;
import com.appointech.notifications.infrastructure.security.JwtFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.web.SecurityFilterChain;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Checks that the component scan from {@link NotificationsApplication} finds and wires every layer. */
@SpringBootTest(properties = "jwt.secret=secreto-de-prueba-del-microservicio-con-mas-de-32-caracteres")
class NotificationsApplicationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void theContextFindsEveryBeanFromTheRootPackage() {
        assertNotNull(context.getBean(NotificationController.class));
        assertNotNull(context.getBean(EmailNotificationService.class));
        assertNotNull(context.getBean(JwtFilter.class));
        assertNotNull(context.getBean(SecurityFilterChain.class));
    }

    @Test
    void thePortIsImplementedByTheSmtpAdapter() {
        assertInstanceOf(SmtpEmailSender.class, context.getBean(EmailSender.class));
    }
}
