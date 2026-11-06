package com.appointech.appointech_backend.domain.models;

/**
 * Modelo de lectura: una cita junto con los datos del técnico que la atiende y de su especialidad.
 * Los datos del técnico y la especialidad son null cuando la cita aún no tiene técnico (solicitud pendiente).
 * {@code asignadaPorNombre} es el nombre del administrador que la asignó manualmente, o null si no se conoce o no aplica.
 */
public record DetalleCita(
        Cita cita,
        String tecnicoNombre,
        String tecnicoApellido,
        String tecnicoTelefono,
        String especialidadNombre,
        String tecnicoFotoClave,
        String asignadaPorNombre
) {

    /** Detalle sin datos del administrador que asignó. */
    public DetalleCita(Cita cita, String tecnicoNombre, String tecnicoApellido, String tecnicoTelefono,
                       String especialidadNombre, String tecnicoFotoClave) {
        this(cita, tecnicoNombre, tecnicoApellido, tecnicoTelefono, especialidadNombre, tecnicoFotoClave, null);
    }

    /** Detalle sin foto del técnico ni datos del administrador que asignó. */
    public DetalleCita(Cita cita, String tecnicoNombre, String tecnicoApellido, String tecnicoTelefono,
                       String especialidadNombre) {
        this(cita, tecnicoNombre, tecnicoApellido, tecnicoTelefono, especialidadNombre, null, null);
    }
}
