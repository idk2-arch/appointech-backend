package com.appointech.notifications.infrastructure.controllers;

import com.appointech.notifications.domain.AppointmentConfirmedEvent;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** HTTP body of {@code POST /api/notificaciones/cita-confirmada}; the JSON names are the contract with the backend. */
public record AppointmentConfirmedRequest(
        @JsonProperty("citaId") Long appointmentId,
        @JsonProperty("correoCliente")
        @NotBlank(message = "El correo del cliente es obligatorio")
        @Email(message = "El correo del cliente no tiene un formato válido")
        String customerEmail,
        @JsonProperty("nombreCliente") String customerName,
        @JsonProperty("servicio") String service,
        @JsonProperty("tecnico") String technician,
        @JsonProperty("fecha") String date,
        @JsonProperty("hora") String time,
        @JsonProperty("direccion") String address
) {

    AppointmentConfirmedEvent toEvent() {
        return new AppointmentConfirmedEvent(appointmentId, customerEmail, customerName, service, technician,
                date, time, address);
    }
}
