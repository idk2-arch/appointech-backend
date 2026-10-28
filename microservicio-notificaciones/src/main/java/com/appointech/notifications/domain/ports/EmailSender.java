package com.appointech.notifications.domain.ports;

/** Output port: sends a plain-text email. Callers do not know the protocol (SMTP). */
public interface EmailSender {

    /** Throws an unchecked exception if the email could not be sent. */
    void send(String recipient, String subject, String body);
}
