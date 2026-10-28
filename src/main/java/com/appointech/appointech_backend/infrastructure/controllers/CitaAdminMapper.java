package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.exception.FiltroCitasInvalidoException;
import com.appointech.appointech_backend.domain.models.CitaResumenAdmin;
import com.appointech.appointech_backend.domain.models.EstadoCita;
import com.appointech.appointech_backend.domain.models.FiltroCitasAdmin;
import com.appointech.appointech_backend.domain.models.PaginaResultado;
import com.appointech.appointech_backend.infrastructure.dto.CitaAdminItemResponse;
import com.appointech.appointech_backend.infrastructure.dto.CitasAdminPaginaResponse;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

final class CitaAdminMapper {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private CitaAdminMapper() {
    }

    static FiltroCitasAdmin aFiltro(Long tecnicoId, String estado, String fechaDesde, String fechaHasta,
                                    int pagina, int tamanio) {
        return new FiltroCitasAdmin(tecnicoId, aEstado(estado), aFecha("fechaDesde", fechaDesde),
                aFecha("fechaHasta", fechaHasta), pagina, tamanio);
    }

    static CitasAdminPaginaResponse aRespuesta(PaginaResultado<CitaResumenAdmin> pagina) {
        return new CitasAdminPaginaResponse(pagina.contenido().stream().map(CitaAdminMapper::aItem).toList(),
                pagina.pagina(), pagina.tamanio(), pagina.totalElementos(), pagina.totalPaginas());
    }

    private static CitaAdminItemResponse aItem(CitaResumenAdmin c) {
        return new CitaAdminItemResponse(c.id(), c.fechaHoraInicio().toLocalDate().toString(),
                c.fechaHoraInicio().format(HORA), c.fechaHoraFin().format(HORA), c.estado().name(),
                c.nombreCliente(), c.nombreTecnico(), c.servicio(), c.direccion());
    }

    private static EstadoCita aEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return null;
        }
        try {
            return EstadoCita.valueOf(estado.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new FiltroCitasInvalidoException("Estado inválido: " + estado);
        }
    }

    private static LocalDate aFecha(String parametro, String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(valor.trim());
        } catch (DateTimeParseException e) {
            throw new FiltroCitasInvalidoException(parametro + " debe tener el formato yyyy-MM-dd");
        }
    }
}
