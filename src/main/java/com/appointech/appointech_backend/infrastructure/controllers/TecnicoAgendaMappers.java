package com.appointech.appointech_backend.infrastructure.controllers;

import com.appointech.appointech_backend.domain.models.BloqueIndisponibilidad;
import com.appointech.appointech_backend.domain.models.Cita;
import com.appointech.appointech_backend.domain.models.CitaConCliente;
import com.appointech.appointech_backend.domain.models.ClienteResumen;
import com.appointech.appointech_backend.domain.models.ParadaRuta;
import com.appointech.appointech_backend.domain.models.RutaDelDia;
import com.appointech.appointech_backend.infrastructure.dto.BloqueResponse;
import com.appointech.appointech_backend.infrastructure.dto.CitaTecnicoResponse;
import com.appointech.appointech_backend.infrastructure.dto.ClienteResumenResponse;
import com.appointech.appointech_backend.infrastructure.dto.RutaResponse;

/** Traduce los modelos de dominio de la agenda del técnico a los DTO de respuesta. */
final class TecnicoAgendaMappers {

    private TecnicoAgendaMappers() {
    }

    static BloqueResponse aRespuesta(BloqueIndisponibilidad bloque) {
        return new BloqueResponse(bloque.getId(), bloque.getInicio(), bloque.getFin(), bloque.getMotivo());
    }

    static CitaTecnicoResponse aRespuesta(CitaConCliente citaConCliente) {
        Cita cita = citaConCliente.cita();
        return new CitaTecnicoResponse(
                cita.getId(), cita.getEstado().name(), cita.getFechaHoraInicio(), cita.getFechaHoraFin(),
                cita.getDescripcionProblema(), cita.getDireccionServicio(), cita.getLatitud(), cita.getLongitud(),
                aRespuesta(citaConCliente.cliente()));
    }

    static RutaResponse aRespuesta(RutaDelDia ruta) {
        return new RutaResponse(
                ruta.fecha(), ruta.calculadaEn(),
                new RutaResponse.Punto(ruta.origen().latitud(), ruta.origen().longitud()),
                ruta.paradas().stream().map(TecnicoAgendaMappers::aRespuesta).toList(),
                redondear(ruta.totalKm()), redondear(ruta.totalMinutosDeViaje()));
    }

    private static RutaResponse.Parada aRespuesta(ParadaRuta parada) {
        return new RutaResponse.Parada(
                parada.orden(), parada.citaId(), parada.inicio(), parada.fin(), parada.direccionServicio(),
                parada.ubicacion().latitud(), parada.ubicacion().longitud(), aRespuesta(parada.cliente()),
                redondear(parada.distanciaKm()), redondear(parada.minutosDesdeAnterior()),
                parada.horaEstimadaLlegada(), parada.alcanzaATiempo(), parada.retrasoEstimadoMinutos());
    }

    private static ClienteResumenResponse aRespuesta(ClienteResumen cliente) {
        return cliente == null ? null
                : new ClienteResumenResponse(cliente.nombre(), cliente.apellido(), cliente.telefono());
    }

    private static double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
