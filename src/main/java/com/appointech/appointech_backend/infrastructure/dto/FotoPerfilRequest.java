package com.appointech.appointech_backend.infrastructure.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Imagen en base64 (con o sin prefijo data:image/...;base64,); 1 MB decodificado ocupa ~1,4 millones de caracteres. */
public record FotoPerfilRequest(
        @NotBlank @Size(max = FotoPerfilRequest.LONGITUD_MAXIMA) String imagenBase64
) {
    static final int LONGITUD_MAXIMA = 1_500_000;
}
