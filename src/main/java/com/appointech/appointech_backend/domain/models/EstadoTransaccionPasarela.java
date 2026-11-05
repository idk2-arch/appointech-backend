package com.appointech.appointech_backend.domain.models;

/** Estado de una transacción según la pasarela, ya normalizado (cada proveedor usa sus propios nombres). */
public enum EstadoTransaccionPasarela {
    PENDIENTE, APROBADA, RECHAZADA, ERROR, ANULADA
}
