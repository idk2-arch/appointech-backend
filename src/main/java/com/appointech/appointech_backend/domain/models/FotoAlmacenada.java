package com.appointech.appointech_backend.domain.models;

/** Bytes de una foto guardada junto con su tipo de contenido. */
public final class FotoAlmacenada {

    private final byte[] contenido;
    private final String tipoContenido;

    public FotoAlmacenada(byte[] contenido, String tipoContenido) {
        this.contenido = contenido.clone();
        this.tipoContenido = tipoContenido;
    }

    public byte[] contenido() {
        return contenido.clone();
    }

    public String tipoContenido() {
        return tipoContenido;
    }
}
