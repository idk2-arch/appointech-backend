package com.appointech.appointech_backend.infrastructure.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tecnico")
public class TecnicoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private UsuarioEntity usuario;

    @Column(name = "direccion_base", length = 255)
    private String direccionBase;

    @Column(name = "latitud_base")
    private Double latitudBase;

    @Column(name = "longitud_base")
    private Double longitudBase;

    @Column(name = "radio_cobertura_km", nullable = false)
    private Integer radioCoberturaKm = 10;

    @ManyToMany
    @JoinTable(
            name = "tecnico_especialidad",
            joinColumns = @JoinColumn(name = "tecnico_id"),
            inverseJoinColumns = @JoinColumn(name = "especialidad_id")
    )
    private List<EspecialidadEntity> especialidades = new ArrayList<>();
}