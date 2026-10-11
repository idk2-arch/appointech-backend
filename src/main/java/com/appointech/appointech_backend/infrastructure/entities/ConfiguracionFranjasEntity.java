package com.appointech.appointech_backend.infrastructure.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "configuracion_franjas")
public class ConfiguracionFranjasEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "duracion_bloque_minutos", nullable = false)
    private Integer duracionBloqueMinutos;

    @Column(name = "radio_maximo_km", nullable = false)
    private Integer radioMaximoKm;

    @Column(name = "hora_inicio_jornada", nullable = false)
    private LocalTime horaInicioJornada;

    @Column(name = "hora_fin_jornada", nullable = false)
    private LocalTime horaFinJornada;
}
