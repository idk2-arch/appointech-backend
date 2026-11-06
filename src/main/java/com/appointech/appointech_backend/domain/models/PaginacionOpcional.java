package com.appointech.appointech_backend.domain.models;

import com.appointech.appointech_backend.domain.exception.FiltroCitasInvalidoException;

/**
 * Paginación opcional (página desde 0). Sin página ni tamaño no se pagina. Si llega solo uno, el otro toma su valor
 * por defecto. El tamaño va de 1 a {@value #TAMANIO_MAXIMO}.
 */
public record PaginacionOpcional(Integer pagina, Integer tamanio) {

    public static final int TAMANIO_POR_DEFECTO = 20;
    public static final int TAMANIO_MAXIMO = 100;

    /** Valida y completa los datos recibidos; un valor fuera de rango es un filtro inválido (400). */
    public static PaginacionOpcional de(Integer pagina, Integer tamanio) {
        if (pagina == null && tamanio == null) {
            return new PaginacionOpcional(null, null);
        }
        int paginaFinal = pagina == null ? 0 : pagina;
        int tamanioFinal = tamanio == null ? TAMANIO_POR_DEFECTO : tamanio;
        if (paginaFinal < 0) {
            throw new FiltroCitasInvalidoException("La página debe ser mayor o igual a 0");
        }
        if (tamanioFinal < 1 || tamanioFinal > TAMANIO_MAXIMO) {
            throw new FiltroCitasInvalidoException("El tamaño debe estar entre 1 y " + TAMANIO_MAXIMO);
        }
        return new PaginacionOpcional(paginaFinal, tamanioFinal);
    }

    public boolean paginado() {
        return pagina != null && tamanio != null;
    }
}
