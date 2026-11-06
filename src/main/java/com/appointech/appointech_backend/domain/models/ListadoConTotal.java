package com.appointech.appointech_backend.domain.models;

import java.util.List;

/** Elementos devueltos por una consulta junto con el total de registros que cumplen el filtro (aunque se pida solo una página). */
public record ListadoConTotal<T>(List<T> elementos, long total) {
}
