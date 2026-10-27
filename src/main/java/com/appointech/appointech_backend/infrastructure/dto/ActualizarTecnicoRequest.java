package com.appointech.appointech_backend.infrastructure.dto;

import com.appointech.appointech_backend.domain.models.TipoDocumento;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

import java.util.List;

/** Todos los campos son opcionales: los que lleguen null no se modifican. La contraseña no se edita aquí. */
public record ActualizarTecnicoRequest(
        @Pattern(regexp = ".*\\S.*", message = "El nombre no puede estar vacío")
        String nombre,

        @Pattern(regexp = ".*\\S.*", message = "El apellido no puede estar vacío")
        String apellido,

        @Email(message = "El correo no tiene un formato válido")
        String correo,

        String telefono,

        TipoDocumento tipoDocumento,

        @Pattern(regexp = ".*\\S.*", message = "El número de documento no puede estar vacío")
        String numeroDocumento,

        @Pattern(regexp = ".*\\S.*", message = "La dirección base no puede estar vacía")
        String direccionBase,

        /** Obsoleto: el límite de distancia es único para todo el sistema; se ignora si llega. */
        Integer radioCoberturaKm,

        Double latitudBase,

        Double longitudBase,

        List<Long> especialidadIds
) {
}
