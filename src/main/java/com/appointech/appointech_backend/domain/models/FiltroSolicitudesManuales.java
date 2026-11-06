package com.appointech.appointech_backend.domain.models;

/**
 * Filtros opcionales (null = sin filtrar) del listado de solicitudes manuales. Si {@code pagina} y {@code tamanio}
 * vienen nulos no hay paginación y se devuelven todas. La página empieza en 0.
 */
public record FiltroSolicitudesManuales(
        MotivoSolicitudEspecial motivo,
        Long especialidadId,
        EstadoCita estado,
        Integer pagina,
        Integer tamanio
) {
    /** Todas las solicitudes pendientes, sin paginar. */
    public static FiltroSolicitudesManuales sinFiltros() {
        return new FiltroSolicitudesManuales(null, null, null, null, null);
    }

    public boolean paginado() {
        return pagina != null && tamanio != null;
    }
}
