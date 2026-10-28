package com.appointech.notifications.domain;

/** Event sent by the main backend when an appointment is confirmed. Only {@code customerEmail} is required. */
public record AppointmentConfirmedEvent(
        Long appointmentId,
        String customerEmail,
        String customerName,
        String service,
        String technician,
        String date,
        String time,
        String address
) {
}
