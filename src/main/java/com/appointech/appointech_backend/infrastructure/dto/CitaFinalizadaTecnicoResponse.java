package com.appointech.appointech_backend.infrastructure.dto;

import com.appointech.appointech_backend.domain.models.EstadoCobro;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/** Cita finalizada de la agenda del técnico (mismos datos que CitaTecnicoResponse) más la situación de su cobro. */
public record CitaFinalizadaTecnicoResponse(
        Long id,
        String estado,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        String descripcionProblema,
        String direccionServicio,
        Double latitud,
        Double longitud,
        ClienteResumenResponse cliente,
        List<FotoCitaResponse> fotosElectrodomestico,
        @Schema(description = "SIN_COSTO_FINAL: el técnico aún no registró el costo; PENDIENTE: hay costo final por cobrar; "
                + "PAGADO: ya se cobró.")
        EstadoCobro estadoPago
) {
}
