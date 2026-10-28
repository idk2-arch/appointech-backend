package com.appointech.appointech_backend.domain.models;

import java.util.List;

/** Una página de resultados (página desde 0) con los totales de la consulta completa. */
public record PaginaResultado<T>(
        List<T> contenido,
        int pagina,
        int tamanio,
        long totalElementos,
        int totalPaginas
) {
}
