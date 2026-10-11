package com.appointech.appointech_backend.infrastructure.entities;

import jakarta.persistence.*;
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
@Table(name = "bloque_indisponibilidad")
public class BloqueIndisponibilidadEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tecnico_id", nullable = false)
    private Long tecnicoId;

    @Column(nullable = false)
    private LocalDateTime inicio;

    @Column(nullable = false)
    private LocalDateTime fin;

    @Column(length = 255)
    private String motivo;
}
