package com.appointech.appointech_backend.infrastructure.entities;

import com.appointech.appointech_backend.domain.models.EstadoPago;
import com.appointech.appointech_backend.domain.models.MetodoPago;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pago")
public class PagoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cita_id", nullable = false, unique = true)
    private Long citaId;

    @Column(name = "monto_cotizado", precision = 12, scale = 2)
    private BigDecimal montoCotizado;

    @Column(name = "monto_final", precision = 12, scale = 2)
    private BigDecimal montoFinal;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(columnDefinition = "metodo_pago")
    private MetodoPago metodo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "estado_pago")
    private EstadoPago estado;

    @Column(name = "referencia_externa", length = 255)
    private String referenciaExterna;

    @Column(name = "pagado_en")
    private LocalDateTime pagadoEn;

    @Column(name = "id_transaccion_externa", length = 100)
    private String idTransaccionExterna;

    @Column(name = "intentos_pago", nullable = false)
    private int intentosPago;
}
