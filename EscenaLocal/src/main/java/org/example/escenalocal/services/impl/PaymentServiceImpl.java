package org.example.escenalocal.services.impl;

import lombok.RequiredArgsConstructor;
import org.example.escenalocal.dtos.post.PostPaymentInfoDto;
import org.example.escenalocal.entities.EventoTiposEntradaEntity;
import org.example.escenalocal.entities.VentaEntradaEntity;
import org.example.escenalocal.repositories.EventoTiposEntradaRepository;
import org.example.escenalocal.repositories.NotificacionRepository;
import org.example.escenalocal.repositories.VentaEntradaRepository;
import org.example.escenalocal.auth.repository.UserRepository;
import org.example.escenalocal.services.MercadopagoService;
import org.example.escenalocal.services.NotificacionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl {

  private final MercadopagoService mercadopagoService;
  private final VentaEntradaRepository ventaRepo;
  private final UserRepository usuarioRepo;
  private final EventoTiposEntradaRepository eventoTipoRepo;
  private final NotificacionService notificacionService;

  @Transactional
  public void processPayment(Long paymentId) throws Exception {

    if (paymentId == null) {
      System.out.println("⚠ Webhook sin paymentId, no se puede procesar venta");
      return;
    }

    if (ventaRepo.existsByPaymentId(paymentId)) {
      System.out.println("⚠ Venta ya registrada para paymentId=" + paymentId + ", ignorando webhook");
      return;
    }

    PostPaymentInfoDto info = mercadopagoService.getPaymentInfo(paymentId);

    int intentos = 0;
    while ((info == null || info.getStatus() == null) && intentos < 3) {
      try {
        Thread.sleep(2000); 
      } catch (InterruptedException ignored) {}

      info = mercadopagoService.getPaymentInfo(paymentId);
      intentos++;
    }

    if (info == null || info.getStatus() == null) {
      System.out.println("⚠ Pago sin estado todavía: " + paymentId);
      return;
    }

    if (!"approved".equalsIgnoreCase(info.getStatus())) {
      System.out.println("⚠ Pago no aprobado: " + info.getStatus() + " (paymentId=" + paymentId + ")");
      return;
    }

    if (
      info.getUsuarioId() == null ||
        info.getEventoId() == null ||
        info.getTipoEntradaId() == null ||
        info.getCantidad() == null ||
        info.getPrecio() == null
    ) {
      System.out.println("⚠ Pago aprobado pero metadata incompleta. paymentId=" + paymentId);
      System.out.println("metadata=" + info);
      return;
    }

        

    VentaEntradaEntity venta = new VentaEntradaEntity();

    venta.setUsuario(
      usuarioRepo.findById(info.getUsuarioId())
        .orElseThrow(() -> new RuntimeException("Usuario no encontrado"))
    );

    EventoTiposEntradaEntity tipoEntrada =
      eventoTipoRepo.findById_EventoIdAndId_TiposEntradaId(
        info.getEventoId(),
        info.getTipoEntradaId()
      ).orElseThrow(() -> new RuntimeException("Tipo de entrada no encontrado"));

    venta.setTipoEntradaEvento(tipoEntrada);
    venta.setCantidad(info.getCantidad());
    venta.setPrecioUnitario(info.getPrecio());

    venta.setPaymentId(paymentId);                    
    venta.setEstadoPago(info.getStatus());            
    venta.setExternalReference(info.getExternalReference()); 
    venta.setFechaActualizacion(LocalDateTime.now());
    venta.setQrToken(UUID.randomUUID().toString());
    venta.setStatusDetail(info.getStatusDetail());    

    ventaRepo.save(venta);

    System.out.println("✅ Venta registrada correctamente (paymentId=" + paymentId + ")");

    Long compradorId = venta.getUsuario().getId();
    Long productorId = tipoEntrada.getEvento().getProductor().getUsuario().getId();
    String nombreEvento = tipoEntrada.getEvento().getEvento(); 
    Integer cantidad = venta.getCantidad();

    notificacionService.createCompraEntradaNotificacion(
      compradorId,
      cantidad,
      nombreEvento
    );

    if (!productorId.equals(compradorId)) {
      notificacionService.createVentaEntradaNotificacion(
        productorId,
        cantidad,
        nombreEvento
      );
    }
  }
}

