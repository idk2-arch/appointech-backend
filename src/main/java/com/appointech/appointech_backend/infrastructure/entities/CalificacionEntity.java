package com.appointech.appointech_backend.infrastructure.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "calificacion")
public class CalificacionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cita_id", nullable = false, unique = true)
    private Long citaId;

    @Column(nullable = false)
    private Short puntuacion;

    @Column(columnDefinition = "text")
    private String comentario;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn;
}
