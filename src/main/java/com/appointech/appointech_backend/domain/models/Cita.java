package com.appointech.appointech_backend.domain.models;

import com.appointech.appointech_backend.domain.exception.DatosCitaInvalidosException;
import com.appointech.appointech_backend.domain.exception.EstadoCitaInvalidoException;

import java.time.LocalDateTime;

public class Cita {

    private Long id;
    private Long clienteId;
    private Long tecnicoId;
    private Long especialidadId;
    private String descripcionProblema;
    private String direccionServicio;
    private Double latitud;
    private Double longitud;
    private LocalDateTime fechaHoraInicio;
    private LocalDateTime fechaHoraFin;
    private EstadoCita estado;
    private String diagnostico;
    private LocalDateTime creadaEn;
    private LocalDateTime actualizadaEn;
    private boolean agendamientoManual;
    private String telefonoContacto;
    private String recomendaciones;
    private LocalDateTime fechaDiagnostico;

    public Cita() {
    }

    // ---- Reglas del ciclo de vida (sin framework): el caso de uso decide quién actúa, la cita decide qué es válido ----

    /** Avanza al siguiente estado de la ejecución del servicio (CONFIRMADA→EN_CAMINO→EN_PROCESO→FINALIZADA). */
    public void avanzarA(EstadoCita nuevo) {
        if (estado == null || !estado.puedeAvanzarA(nuevo)) {
            throw new EstadoCitaInvalidoException(mensajeTransicionInvalida(nuevo));
        }
        this.estado = nuevo;
    }

    private String mensajeTransicionInvalida(EstadoCita nuevo) {
        if (estado == null) {
            return "La cita no tiene un estado válido";
        }
        if (estado.esFinal()) {
            return "La cita está " + estado + " y no admite cambios de estado";
        }
        String permitido = estado.siguiente().map(EstadoCita::name).orElse("ninguno");
        return "No se puede pasar la cita de " + estado + " a " + nuevo + "; el único cambio permitido es a " + permitido;
    }

    /** Cancelación por el cliente: solo mientras la cita está PENDIENTE o CONFIRMADA. */
    public void cancelarPorCliente() {
        validarModificablePorCliente("cancelar");
        this.estado = EstadoCita.CANCELADA;
    }

    /** Cancelación por el técnico asignado: vale en cualquier estado que no sea final. */
    public void cancelarPorTecnico() {
        if (estado == null || estado.esFinal()) {
            throw new EstadoCitaInvalidoException("La cita está " + estado + " y no se puede cancelar");
        }
        this.estado = EstadoCita.CANCELADA;
    }

    /** Verifica que el cliente aún pueda cancelar o reprogramar; lanza 409 si no. */
    public void validarModificablePorCliente(String accion) {
        if (estado == null || !estado.admiteCancelacionOReprogramacionDelCliente()) {
            throw new EstadoCitaInvalidoException(
                    "Solo se puede " + accion + " una cita PENDIENTE o CONFIRMADA; la cita está " + estado);
        }
    }

    public void reprogramar(LocalDateTime nuevoInicio, LocalDateTime nuevoFin) {
        validarModificablePorCliente("reprogramar");
        this.fechaHoraInicio = nuevoInicio;
        this.fechaHoraFin = nuevoFin;
    }

    /** Crea o actualiza el diagnóstico; solo mientras el servicio está EN_PROCESO. */
    public void registrarDiagnostico(String diagnostico, String recomendaciones, LocalDateTime ahora) {
        if (estado != EstadoCita.EN_PROCESO) {
            throw new EstadoCitaInvalidoException(
                    "Solo se puede registrar el diagnóstico cuando la cita está EN_PROCESO; la cita está " + estado);
        }
        if (diagnostico == null || diagnostico.isBlank()) {
            throw new DatosCitaInvalidosException("El diagnóstico es obligatorio");
        }
        this.diagnostico = diagnostico.strip();
        this.recomendaciones = recomendaciones == null || recomendaciones.isBlank() ? null : recomendaciones.strip();
        this.fechaDiagnostico = ahora;
    }

    public String getRecomendaciones() {
        return recomendaciones;
    }

    public void setRecomendaciones(String recomendaciones) {
        this.recomendaciones = recomendaciones;
    }

    public LocalDateTime getFechaDiagnostico() {
        return fechaDiagnostico;
    }

    public void setFechaDiagnostico(LocalDateTime fechaDiagnostico) {
        this.fechaDiagnostico = fechaDiagnostico;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public Long getTecnicoId() {
        return tecnicoId;
    }

    public void setTecnicoId(Long tecnicoId) {
        this.tecnicoId = tecnicoId;
    }

    public Long getEspecialidadId() {
        return especialidadId;
    }

    public void setEspecialidadId(Long especialidadId) {
        this.especialidadId = especialidadId;
    }

    public String getDescripcionProblema() {
        return descripcionProblema;
    }

    public void setDescripcionProblema(String descripcionProblema) {
        this.descripcionProblema = descripcionProblema;
    }

    public String getDireccionServicio() {
        return direccionServicio;
    }

    public void setDireccionServicio(String direccionServicio) {
        this.direccionServicio = direccionServicio;
    }

    public Double getLatitud() {
        return latitud;
    }

    public void setLatitud(Double latitud) {
        this.latitud = latitud;
    }

    public Double getLongitud() {
        return longitud;
    }

    public void setLongitud(Double longitud) {
        this.longitud = longitud;
    }

    public LocalDateTime getFechaHoraInicio() {
        return fechaHoraInicio;
    }

    public void setFechaHoraInicio(LocalDateTime fechaHoraInicio) {
        this.fechaHoraInicio = fechaHoraInicio;
    }

    public LocalDateTime getFechaHoraFin() {
        return fechaHoraFin;
    }

    public void setFechaHoraFin(LocalDateTime fechaHoraFin) {
        this.fechaHoraFin = fechaHoraFin;
    }

    public EstadoCita getEstado() {
        return estado;
    }

    public void setEstado(EstadoCita estado) {
        this.estado = estado;
    }

    public String getDiagnostico() {
        return diagnostico;
    }

    public void setDiagnostico(String diagnostico) {
        this.diagnostico = diagnostico;
    }

    public LocalDateTime getCreadaEn() {
        return creadaEn;
    }

    public void setCreadaEn(LocalDateTime creadaEn) {
        this.creadaEn = creadaEn;
    }

    public LocalDateTime getActualizadaEn() {
        return actualizadaEn;
    }

    public void setActualizadaEn(LocalDateTime actualizadaEn) {
        this.actualizadaEn = actualizadaEn;
    }

    public boolean isAgendamientoManual() {
        return agendamientoManual;
    }

    public void setAgendamientoManual(boolean agendamientoManual) {
        this.agendamientoManual = agendamientoManual;
    }

    public String getTelefonoContacto() {
        return telefonoContacto;
    }

    public void setTelefonoContacto(String telefonoContacto) {
        this.telefonoContacto = telefonoContacto;
    }
}
