package com.appointech.appointech_backend.infrastructure.perf;

import org.springframework.aop.support.StaticMethodMatcherPointcut;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Selecciona los métodos públicos de los casos de uso, controladores y adaptadores (las capas donde se gasta el
 * tiempo de una petición). Usa solo Spring AOP, sin AspectJ, para no sumar dependencias.
 */
class PuntoCorteCapasAplicacion extends StaticMethodMatcherPointcut {

    private static final String BASE = "com.appointech.appointech_backend.";
    private static final List<String> PAQUETES = List.of(
            BASE + "infrastructure.controllers.");

    PuntoCorteCapasAplicacion() {
        setClassFilter(clase -> PAQUETES.stream().anyMatch(clase.getName()::startsWith));
    }

    @Override
    public boolean matches(Method metodo, Class<?> clase) {
        return metodo.getDeclaringClass() != Object.class;
    }
}
