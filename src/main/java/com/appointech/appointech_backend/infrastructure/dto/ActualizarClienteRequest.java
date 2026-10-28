package com.appointech.appointech_backend.infrastructure.dto;

import com.appointech.appointech_backend.domain.models.TipoDocumento;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

/** Todos los campos son opcionales: los que lleguen null no se modifican. La contraseña no se edita aquí. */
public record ActualizarClienteRequest(
        @Pattern(regexp = "[^\\S\\r\\n]*+\\S.*", message = "El nombre no puede estar vacío")
        String nombre,

        @Pattern(regexp = "[^\\S\\r\\n]*+\\S.*", message = "El apellido no puede estar vacío")
        String apellido,

        @Email(message = "El correo no tiene un formato válido")
        String correo,

        String telefono,

        TipoDocumento tipoDocumento,

        @Pattern(regexp = "[^\\S\\r\\n]*+\\S.*", message = "El número de documento no puede estar vacío")
        String numeroDocumento,

        @Pattern(regexp = "[^\\S\\r\\n]*+\\S.*", message = "La dirección no puede estar vacía")
        String direccion,

        Double latitud,

        Double longitud
) {
}
