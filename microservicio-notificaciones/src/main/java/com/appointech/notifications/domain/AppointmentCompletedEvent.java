package com.appointech.notifications.domain;

/** Event sent by the main backend when the technician completes the appointment; {@code diagnosis} may be empty. */
public record AppointmentCompletedEvent(
        Long appointmentId,
        String customerEmail,
        String customerName,
        String service,
        String technician,
        String diagnosis
) {
}
