package com.appointech.appointech_backend.infrastructure.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Solo los campos editables; cualquier otro campo del JSON (correo, rol, documento...) se ignora. */
public record ActualizarPerfilRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String nombre,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 150, message = "El apellido no puede superar 150 caracteres")
        String apellido,

        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = "\\+?[0-9][0-9 ()-]{6,19}", message = "El teléfono no tiene un formato válido")
        String telefono,

        @Pattern(regexp = "[^\\S\\r\\n]*+\\S.*", message = "La dirección no puede estar vacía")
        @Size(max = 255, message = "La dirección no puede superar 255 caracteres")
        String direccion
) {
}
