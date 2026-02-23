package org.example.escenalocal.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ventas_entradas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VentaEntradaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "usuario_id")
  private UsuarioEntity usuario;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
    @JoinColumn(name = "id_evento", referencedColumnName = "id_evento"),
    @JoinColumn(name = "id_tipos_entrada", referencedColumnName = "id_tipos_entrada")
  })
  private EventoTiposEntradaEntity tipoEntradaEvento;

  private LocalDateTime fechaVenta = LocalDateTime.now();

  private int cantidad;

  private BigDecimal precioUnitario;

  @Column(name = "payment_id")
  private Long paymentId;

  @Column(name = "estado_pago")
  private String estadoPago;

  @Column(name = "external_reference")
  private String externalReference;

  @Column(name = "fecha_actualizacion")
  private LocalDateTime fechaActualizacion;

  @Column(name = "status_detail")
  private String statusDetail;

  public BigDecimal getMontoTotal() {
    if (precioUnitario == null) {
      return BigDecimal.ZERO;
    }
    return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
  }

  @Column(name = "qr_token", unique = true)
  private String qrToken;
}

