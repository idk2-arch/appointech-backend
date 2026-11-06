package com.appointech.appointech_backend.domain.models;

import com.appointech.appointech_backend.domain.exception.ReglasFranjasInvalidasException;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Objects;

public class ConfiguracionFranjas {

    public static final int DURACION_MINIMA_PERMITIDA_MINUTOS = 15;
    public static final int DURACION_MAXIMA_PERMITIDA_MINUTOS = 480;

    private Long id;
    private Integer duracionBloqueMinutos;
    private Integer radioMaximoKm;
    private LocalTime horaInicioJornada;
    private LocalTime horaFinJornada;

    public ConfiguracionFranjas() {
    }

    /** Única definición del respaldo cuando aún no hay configuración guardada: 120 min, 15 km, jornada 08:00-18:00. */
    public static ConfiguracionFranjas porDefecto() {
        return new ConfiguracionFranjas(null, 120, 15, LocalTime.of(8, 0), LocalTime.of(18, 0));
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

    /**
     * Cambia la duración del bloque y la jornada de una sola vez, validando el conjunto: la duración entre
     * {@value #DURACION_MINIMA_PERMITIDA_MINUTOS} y {@value #DURACION_MAXIMA_PERMITIDA_MINUTOS} minutos, el inicio
     * anterior al fin y al menos un bloque completo dentro de la jornada. Si algo es inválido no cambia nada.
     */
    public void cambiarReglasDeFranjas(Integer duracionMinutos, LocalTime inicioJornada, LocalTime finJornada) {
        if (duracionMinutos == null || duracionMinutos < DURACION_MINIMA_PERMITIDA_MINUTOS
                || duracionMinutos > DURACION_MAXIMA_PERMITIDA_MINUTOS) {
            throw new ReglasFranjasInvalidasException("La duración del bloque debe estar entre "
                    + DURACION_MINIMA_PERMITIDA_MINUTOS + " y " + DURACION_MAXIMA_PERMITIDA_MINUTOS + " minutos");
        }
        if (inicioJornada == null || finJornada == null) {
            throw new ReglasFranjasInvalidasException("La hora de inicio y la de fin de la jornada son obligatorias");
        }
        if (!inicioJornada.isBefore(finJornada)) {
            throw new ReglasFranjasInvalidasException("La hora de inicio de la jornada debe ser anterior a la de fin");
        }
        if (Duration.between(inicioJornada, finJornada).toMinutes() < duracionMinutos) {
            throw new ReglasFranjasInvalidasException("La jornada debe poder contener al menos un bloque completo");
        }
        this.duracionBloqueMinutos = duracionMinutos;
        this.horaInicioJornada = inicioJornada;
        this.horaFinJornada = finJornada;
    }

    /** True si los valores dados cambian la duración o la jornada con las que se ofrecieron las franjas. */
    public boolean cambiaranFranjasOfrecidas(Integer duracionMinutos, LocalTime inicioJornada, LocalTime finJornada) {
        return !Objects.equals(duracionBloqueMinutos, duracionMinutos)
                || !Objects.equals(horaInicioJornada, inicioJornada)
                || !Objects.equals(horaFinJornada, finJornada);
    }
}
