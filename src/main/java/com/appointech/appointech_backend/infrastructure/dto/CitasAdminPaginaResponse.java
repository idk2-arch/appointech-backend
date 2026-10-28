package com.appointech.appointech_backend.infrastructure.dto;

import java.util.List;

public record CitasAdminPaginaResponse(
        List<CitaAdminItemResponse> contenido,
        int pagina,
        int tamanio,
        long totalElementos,
        int totalPaginas
) {
}
