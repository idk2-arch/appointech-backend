package com.appointech.notifications.application;

import com.appointech.notifications.domain.AppointmentCompletedEvent;
import com.appointech.notifications.domain.AppointmentConfirmedEvent;
import com.appointech.notifications.domain.ports.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * Builds the Spanish email texts and sends them through the {@link EmailSender} port.
 * A send failure is logged as WARN (without email content or credentials) and never propagated.
 * It is registered as a bean in {@code infrastructure.config.NotificationConfig} so this layer stays framework-free.
 */
public class EmailNotificationService {

    static final String CONFIRMED_SUBJECT = "Tu cita en AppoinTech fue confirmada";
    static final String COMPLETED_SUBJECT = "Tu servicio fue finalizado";

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);
    private static final DateTimeFormatter LONG_DATE =
            DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", Locale.of("es", "CO"));
    private static final String NOT_AVAILABLE = "No disponible";

    private final EmailSender emailSender;

    public EmailNotificationService(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    public void notifyAppointmentConfirmed(AppointmentConfirmedEvent event) {
        send(event.appointmentId(), event.customerEmail(), CONFIRMED_SUBJECT, confirmedBody(event));
    }

    public void notifyAppointmentCompleted(AppointmentCompletedEvent event) {
        send(event.appointmentId(), event.customerEmail(), COMPLETED_SUBJECT, completedBody(event));
    }

    String confirmedBody(AppointmentConfirmedEvent e) {
        return "Hola " + valueOr(e.customerName(), "cliente") + ",\n\n"
                + "Tu cita en AppoinTech fue confirmada. Estos son los datos:\n\n"
                + "Servicio: " + valueOr(e.service(), NOT_AVAILABLE) + "\n"
                + "Fecha: " + formatDate(e.date()) + "\n"
                + "Hora: " + valueOr(e.time(), NOT_AVAILABLE) + "\n"
                + "Técnico: " + valueOr(e.technician(), NOT_AVAILABLE) + "\n"
                + "Dirección: " + valueOr(e.address(), NOT_AVAILABLE) + "\n\n"
                + "Gracias por confiar en AppoinTech.";
    }

    String completedBody(AppointmentCompletedEvent e) {
        return "Hola " + valueOr(e.customerName(), "cliente") + ",\n\n"
                + "Tu servicio fue finalizado.\n\n"
                + "Servicio: " + valueOr(e.service(), NOT_AVAILABLE) + "\n"
                + "Técnico: " + valueOr(e.technician(), NOT_AVAILABLE) + "\n\n"
                + "Diagnóstico del técnico:\n"
                + valueOr(e.diagnosis(), "El técnico no registró un diagnóstico.") + "\n\n"
                + "Gracias por confiar en AppoinTech.";
    }

    private void send(Long appointmentId, String recipient, String subject, String body) {
        try {
            emailSender.send(recipient, subject, body);
        } catch (RuntimeException ex) {
            log.warn("No se pudo enviar el correo de la cita {}: {}", appointmentId, ex.getClass().getSimpleName());
        }
    }

    private static String valueOr(String text, String defaultValue) {
        return text == null || text.isBlank() ? defaultValue : text.strip();
    }

    /** Turns 2026-11-03 into "martes 3 de noviembre de 2026"; if it is not an ISO date, shows the received text. */
    private static String formatDate(String text) {
        String clean = valueOr(text, NOT_AVAILABLE);
        try {
            return LocalDate.parse(clean).format(LONG_DATE);
        } catch (DateTimeParseException ex) {
            return clean;
        }
    }
}
