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
@Table(name = "log_sistema")
public class LogSistemaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String nivel;

    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(nullable = false, length = 100)
    private String origen;

    @Column(length = 150)
    private String usuario;

    @Column(nullable = false, columnDefinition = "text")
    private String mensaje;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;
}
