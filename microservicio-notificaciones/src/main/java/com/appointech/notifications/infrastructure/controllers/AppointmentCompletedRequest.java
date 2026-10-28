package com.appointech.notifications.infrastructure.controllers;

import com.appointech.notifications.domain.AppointmentCompletedEvent;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** HTTP body of {@code POST /api/notificaciones/cita-finalizada}; the JSON names are the contract with the backend. */
public record AppointmentCompletedRequest(
        @JsonProperty("citaId") Long appointmentId,
        @JsonProperty("correoCliente")
        @NotBlank(message = "El correo del cliente es obligatorio")
        @Email(message = "El correo del cliente no tiene un formato válido")
        String customerEmail,
        @JsonProperty("nombreCliente") String customerName,
        @JsonProperty("servicio") String service,
        @JsonProperty("tecnico") String technician,
        @JsonProperty("diagnostico") String diagnosis
) {

    AppointmentCompletedEvent toEvent() {
        return new AppointmentCompletedEvent(appointmentId, customerEmail, customerName, service, technician, diagnosis);
    }
}
