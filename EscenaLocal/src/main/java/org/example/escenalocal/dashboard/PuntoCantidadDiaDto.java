package org.example.escenalocal.dashboard;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class PuntoCantidadDiaDto {

  private LocalDate fecha;
  private Long cantidad;

  public PuntoCantidadDiaDto(Object fecha, Number cantidad) {
    this.fecha = toLocalDate(fecha);
    this.cantidad = (cantidad != null) ? cantidad.longValue() : 0L;
  }

  private LocalDate toLocalDate(Object value) {
    if (value == null) return null;

    if (value instanceof LocalDate ld) return ld;
    if (value instanceof LocalDateTime ldt) return ldt.toLocalDate();
    if (value instanceof java.sql.Date sd) return sd.toLocalDate();

    if (value instanceof java.util.Date d) {
      return new java.sql.Date(d.getTime()).toLocalDate();
    }

    throw new IllegalArgumentException("Tipo de fecha no soportado: " + value.getClass());
  }
}

