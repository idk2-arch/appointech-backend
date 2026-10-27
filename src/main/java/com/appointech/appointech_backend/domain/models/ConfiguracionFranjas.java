package com.appointech.appointech_backend.domain.models;

import java.time.LocalTime;

public class ConfiguracionFranjas {

    private Long id;
    private Integer duracionBloqueMinutos;
    private Integer radioMaximoKm;
    private LocalTime horaInicioJornada;
    private LocalTime horaFinJornada;

    public ConfiguracionFranjas() {
    }

    public ConfiguracionFranjas(Long id, Integer duracionBloqueMinutos, Integer radioMaximoKm,
                                LocalTime horaInicioJornada, LocalTime horaFinJornada) {
        this.id = id;
        this.duracionBloqueMinutos = duracionBloqueMinutos;
        this.radioMaximoKm = radioMaximoKm;
        this.horaInicioJornada = horaInicioJornada;
        this.horaFinJornada = horaFinJornada;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getDuracionBloqueMinutos() {
        return duracionBloqueMinutos;
    }

    public void setDuracionBloqueMinutos(Integer duracionBloqueMinutos) {
        this.duracionBloqueMinutos = duracionBloqueMinutos;
    }

    public Integer getRadioMaximoKm() {
        return radioMaximoKm;
    }

    public void setRadioMaximoKm(Integer radioMaximoKm) {
        this.radioMaximoKm = radioMaximoKm;
    }

    public LocalTime getHoraInicioJornada() {
        return horaInicioJornada;
    }

    public void setHoraInicioJornada(LocalTime horaInicioJornada) {
        this.horaInicioJornada = horaInicioJornada;
    }

    public LocalTime getHoraFinJornada() {
        return horaFinJornada;
    }

    public void setHoraFinJornada(LocalTime horaFinJornada) {
        this.horaFinJornada = horaFinJornada;
    }

    private String telefonoWhatsappAdmin;

    public String getTelefonoWhatsappAdmin() {
        return telefonoWhatsappAdmin;
    }

    public void setTelefonoWhatsappAdmin(String telefonoWhatsappAdmin) {
        this.telefonoWhatsappAdmin = telefonoWhatsappAdmin;
    }

    /**
     * Cambia el teléfono de WhatsApp del administrador. Vacío o solo espacios lo borra; si tiene valor debe llevar
     * únicamente dígitos, espacios, guiones y un + inicial, con entre 10 y 15 dígitos.
     */
    public void cambiarTelefonoWhatsappAdmin(String telefono) {
        if (telefono == null || telefono.isBlank()) {
            this.telefonoWhatsappAdmin = null;
            return;
        }
        String limpio = telefono.trim();
        int digitos = limpio.replaceAll("\\D", "").length();
        if (!limpio.matches("\\+?[0-9][0-9 \\-]*") || digitos < 10 || digitos > 15) {
            throw new com.appointech.appointech_backend.domain.exception.TelefonoWhatsappInvalidoException(
                    "El teléfono de WhatsApp debe tener entre 10 y 15 dígitos y solo puede incluir dígitos, "
                            + "espacios, guiones y un signo + al inicio");
        }
        this.telefonoWhatsappAdmin = limpio;
    }

    public static final int RADIO_MAXIMO_PERMITIDO_KM = 100;

    /** Cambia el límite único de distancia; debe ser mayor que 0 y como máximo {@value #RADIO_MAXIMO_PERMITIDO_KM} km. */
    public void cambiarRadioMaximo(Integer kilometros) {
        if (kilometros == null || kilometros < 1 || kilometros > RADIO_MAXIMO_PERMITIDO_KM) {
            throw new com.appointech.appointech_backend.domain.exception.RadioCoberturaInvalidoException(
                    "El radio máximo de cobertura debe ser mayor que 0 y como máximo " + RADIO_MAXIMO_PERMITIDO_KM + " km");
        }
        this.radioMaximoKm = kilometros;
    }
}
