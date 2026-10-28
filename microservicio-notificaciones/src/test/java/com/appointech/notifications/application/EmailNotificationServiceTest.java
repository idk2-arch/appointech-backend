package com.appointech.notifications.application;

import com.appointech.notifications.domain.AppointmentCompletedEvent;
import com.appointech.notifications.domain.AppointmentConfirmedEvent;
import com.appointech.notifications.domain.ports.EmailSender;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EmailNotificationServiceTest {

    /** Test double of the port: records what was sent or fails on demand. */
    private static class FakeEmailSender implements EmailSender {
        final List<String[]> sent = new ArrayList<>();
        RuntimeException failure;

        @Override
        public void send(String recipient, String subject, String body) {
            if (failure != null) {
                throw failure;
            }
            sent.add(new String[]{recipient, subject, body});
        }
    }

    private final FakeEmailSender sender = new FakeEmailSender();
    private final EmailNotificationService service = new EmailNotificationService(sender);

    private AppointmentConfirmedEvent confirmed() {
        return new AppointmentConfirmedEvent(42L, "ana@mail.com", "Ana Lopez", "Plomería", "Luis Perez",
                "2026-11-03", "10:00", "Calle 1 # 2-3");
    }

    @Test
    void confirmedAppointmentIsSentToTheCustomerWithSubjectAndDataInSpanish() {
        service.notifyAppointmentConfirmed(confirmed());

        assertEquals(1, sender.sent.size());
        String[] email = sender.sent.get(0);
        assertEquals("ana@mail.com", email[0]);
        assertEquals("Tu cita en AppoinTech fue confirmada", email[1]);
        assertTrue(email[2].startsWith("Hola Ana Lopez,"));
        assertTrue(email[2].contains("Servicio: Plomería"));
        assertTrue(email[2].contains("Fecha: martes 3 de noviembre de 2026"), email[2]);
        assertTrue(email[2].contains("Hora: 10:00"));
        assertTrue(email[2].contains("Técnico: Luis Perez"));
        assertTrue(email[2].contains("Dirección: Calle 1 # 2-3"));
    }

    @Test
    void confirmedAppointmentWithoutOptionalDataUsesDefaultTexts() {
        service.notifyAppointmentConfirmed(new AppointmentConfirmedEvent(1L, "ana@mail.com", null, " ", null, null,
                null, null));

        String body = sender.sent.get(0)[2];
        assertTrue(body.startsWith("Hola cliente,"));
        assertTrue(body.contains("Servicio: No disponible"));
        assertTrue(body.contains("Fecha: No disponible"));
        assertTrue(body.contains("Técnico: No disponible"));
    }

    @Test
    void aNonIsoDateIsShownAsReceived() {
        service.notifyAppointmentConfirmed(new AppointmentConfirmedEvent(1L, "ana@mail.com", "Ana", "S", "T",
                "3 de noviembre", "10:00", "D"));

        assertTrue(sender.sent.get(0)[2].contains("Fecha: 3 de noviembre"));
    }

    @Test
    void completedAppointmentIncludesTheTechnicianDiagnosis() {
        service.notifyAppointmentCompleted(new AppointmentCompletedEvent(42L, "ana@mail.com", "Ana Lopez", "Plomería",
                "Luis Perez", "Sifón roto; se reemplazó."));

        String[] email = sender.sent.get(0);
        assertEquals("ana@mail.com", email[0]);
        assertEquals("Tu servicio fue finalizado", email[1]);
        assertTrue(email[2].startsWith("Hola Ana Lopez,"));
        assertTrue(email[2].contains("Servicio: Plomería"));
        assertTrue(email[2].contains("Técnico: Luis Perez"));
        assertTrue(email[2].contains("Diagnóstico del técnico:\nSifón roto; se reemplazó."));
    }

    @Test
    void completedAppointmentWithoutDiagnosisSaysSo() {
        service.notifyAppointmentCompleted(new AppointmentCompletedEvent(42L, "ana@mail.com", "Ana", "S", "T", null));

        assertTrue(sender.sent.get(0)[2].contains("El técnico no registró un diagnóstico."));
    }

    @Test
    void aSendFailureIsNotPropagated() {
        sender.failure = new IllegalStateException("smtp down");

        assertDoesNotThrow(() -> service.notifyAppointmentConfirmed(confirmed()));
        assertDoesNotThrow(() -> service.notifyAppointmentCompleted(
                new AppointmentCompletedEvent(1L, "ana@mail.com", "Ana", "S", "T", "d")));
        assertTrue(sender.sent.isEmpty());
    }
}
