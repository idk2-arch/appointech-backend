package com.appointech.appointech_backend.infrastructure.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cita")
public class CitaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(name = "tecnico_id")
    private Long tecnicoId;

    @Column(name = "especialidad_id", nullable = false)
    private Long especialidadId;

    @Column(name = "descripcion_problema", nullable = false, columnDefinition = "text")
    private String descripcionProblema;

    @Column(name = "direccion_servicio", nullable = false, length = 255)
    private String direccionServicio;

    private Double latitud;

    private Double longitud;

    @Column(name = "fecha_hora_inicio")
    private LocalDateTime fechaHoraInicio;

    @Column(name = "fecha_hora_fin")
    private LocalDateTime fechaHoraFin;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "estado_cita")
    private EstadoCitaEntity estado;

    @Column(columnDefinition = "text")
    private String diagnostico;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn;

    @Column(name = "actualizada_en")
    private LocalDateTime actualizadaEn;
}
