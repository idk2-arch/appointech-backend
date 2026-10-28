package com.appointech.appointech_backend.domain.models;

/** Imagen ya decodificada cuyo formato real se comprobó con sus bytes de cabecera. */
public final class ImagenValidada {

    private final byte[] contenido;
    private final TipoImagen tipo;

    public ImagenValidada(byte[] contenido, TipoImagen tipo) {
        this.contenido = contenido.clone();
        this.tipo = tipo;
    }

    public byte[] contenido() {
        return contenido.clone();
    }

    public TipoImagen tipo() {
        return tipo;
    }
}
