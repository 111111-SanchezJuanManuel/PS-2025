package org.example.escenalocal.services.impl;

import lombok.RequiredArgsConstructor;
import org.example.escenalocal.dashboard.*;
import org.example.escenalocal.entities.EventoEntity;
import org.example.escenalocal.entities.EventoTiposEntradaEntity;
import org.example.escenalocal.entities.VentaEntradaEntity;
import org.example.escenalocal.repositories.EventoRepository;
import org.example.escenalocal.repositories.VentaEntradaRepository;
import org.example.escenalocal.services.DashboardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

  private final VentaEntradaRepository ventaRepo;
  private final EventoRepository eventoRepo;

  @Override
  @Transactional(readOnly = true)
  public ProductorDashboardDto getDashboardProductor(Long productorId, LocalDate from, LocalDate to) {

    LocalDateTime fromDateTime = from.atStartOfDay();
    LocalDateTime toDateTime   = to.atTime(LocalTime.MAX);

    ProductorDashboardDto dto = new ProductorDashboardDto();

    ProductorKpiDto kpis = buildKpis(productorId, from, to, fromDateTime, toDateTime);
    dto.setKpis(kpis);

    dto.setVentasPorDia(
      ventaRepo.ventasPorDia(productorId, fromDateTime, toDateTime)
    );

    dto.setEntradasPorTipo(
      ventaRepo.entradasPorTipo(productorId, fromDateTime, toDateTime)
    );

    dto.setTopEventos(
      ventaRepo.rankingEventos(productorId, fromDateTime, toDateTime)
    );

    List<VentaEntradaEntity> ventasPeriodo =
      ventaRepo.ventasPorProductorEnPeriodo(productorId, fromDateTime, toDateTime);

    dto.setVentasRecientes(
      ventasPeriodo.stream()
        .limit(20)
        .map(this::mapVentaToVentaDashboardDto)
        .collect(Collectors.toList())
    );

    Map<Long, Long> entradasPorEvento = new HashMap<>();
    Map<Long, BigDecimal> recaudacionPorEvento = new HashMap<>();

    for (VentaEntradaEntity v : ventasPeriodo) {
      if (v.getEstadoPago() == null ||
        !"approved".equalsIgnoreCase(v.getEstadoPago())) {
        continue;
      }

      if (v.getTipoEntradaEvento() == null ||
        v.getTipoEntradaEvento().getEvento() == null) {
        continue;
      }

      Long eventoId = v.getTipoEntradaEvento().getEvento().getId();
      if (eventoId == null) continue;

      entradasPorEvento.merge(
        eventoId,
        (long) v.getCantidad(),
        Long::sum
      );

      BigDecimal monto = v.getMontoTotal(); 
      if (monto == null) {
        monto = BigDecimal.ZERO;
      }
      recaudacionPorEvento.merge(
        eventoId,
        monto,
        BigDecimal::add
      );
    }

    List<EventoEntity> eventos = eventoRepo.eventosDelProductorEnPeriodo(productorId, from, to);

    Map<Long, EventoEntity> eventosPorId = eventos.stream()
      .filter(e -> e.getId() != null)
      .collect(Collectors.toMap(EventoEntity::getId, e -> e));

    dto.setEventosResumen(
      eventos.stream()
        .map(e -> {
          Long entradas = entradasPorEvento.getOrDefault(e.getId(), 0L);
          BigDecimal recaudacion = recaudacionPorEvento.getOrDefault(e.getId(), BigDecimal.ZERO);
          return mapEventoToEventoDashboardDto(e, entradas, recaudacion);
        })
        .collect(Collectors.toList())
    );

    long capacidadTotalEventos = 0L;
    long entradasTotalesEventos = 0L;

    for (EventoEntity e : eventos) {
      Integer capacidad = null;
      if (e.getEstablecimiento() != null) {
        capacidad = e.getEstablecimiento().getCapacidad(); 
      }
      if (capacidad != null && capacidad > 0) {
        capacidadTotalEventos += capacidad;
        Long entradas = entradasPorEvento.getOrDefault(e.getId(), 0L);
        entradasTotalesEventos += entradas;
      }
    }

    double ocupacionPromedio = 0.0;
    if (capacidadTotalEventos > 0) {
      ocupacionPromedio = (double) entradasTotalesEventos / capacidadTotalEventos;
    }
    kpis.setOcupacionPromedio(ocupacionPromedio); 

    String mejorEventoNombre = null;
    BigDecimal mejorEventoRecaudacion = BigDecimal.ZERO;

    for (Map.Entry<Long, BigDecimal> entry : recaudacionPorEvento.entrySet()) {
      Long eventoId = entry.getKey();
      BigDecimal rec = entry.getValue() != null ? entry.getValue() : BigDecimal.ZERO;

      if (rec.compareTo(mejorEventoRecaudacion) > 0) {
        mejorEventoRecaudacion = rec;
        EventoEntity ev = eventosPorId.get(eventoId);
        if (ev != null) {
          mejorEventoNombre = ev.getEvento(); 
        }
      }
    }

    kpis.setMejorEventoNombre(mejorEventoNombre);
    kpis.setMejorEventoRecaudacion(mejorEventoRecaudacion);

    dto.setKpis(kpis);

    return dto;
  }

  
  private ProductorKpiDto buildKpis(Long productorId,
                                    LocalDate from,
                                    LocalDate to,
                                    LocalDateTime fromDateTime,
                                    LocalDateTime toDateTime) {

    ProductorKpiDto kpis = new ProductorKpiDto();

    BigDecimal totalRecaudado =
      Optional.ofNullable(
        ventaRepo.totalRecaudadoPorProductor(productorId, fromDateTime, toDateTime)
      ).orElse(BigDecimal.ZERO);

    Long entradasVendidas =
      Optional.ofNullable(
        ventaRepo.totalEntradasVendidasPorProductor(productorId, fromDateTime, toDateTime)
      ).orElse(0L);

    int eventosActivos =
      Optional.ofNullable(
        eventoRepo.countEventosDelProductorEnPeriodo(productorId, from, to)
      ).orElse(0);

    kpis.setTotalRecaudado(totalRecaudado);
    kpis.setEntradasVendidas(entradasVendidas);
    kpis.setEventosActivos(eventosActivos);

    kpis.setOcupacionPromedio(0.0);
    kpis.setMejorEventoNombre(null);
    kpis.setMejorEventoRecaudacion(BigDecimal.ZERO);

    return kpis;
  }

  
  private VentaDashboardDto mapVentaToVentaDashboardDto(VentaEntradaEntity v) {
    VentaDashboardDto dto = new VentaDashboardDto();

    dto.setFechaCompra(v.getFechaVenta());

    if (v.getUsuario() != null) {
      dto.setUsuarioNombre(v.getUsuario().getUsername());   
      dto.setUsuarioEmail(v.getUsuario().getEmail());     
    }

    EventoTiposEntradaEntity ete = v.getTipoEntradaEvento();
    if (ete != null) {
      if (ete.getEvento() != null) {
        dto.setEventoId(ete.getEvento().getId());
        dto.setEventoNombre(ete.getEvento().getEvento());  
      }
      if (ete.getTiposEntrada() != null) {
        dto.setTipoEntradaNombre(ete.getTiposEntrada().getEntrada());
      }
    }

    dto.setCantidad(v.getCantidad());
    dto.setTotal(v.getMontoTotal());
    dto.setEstadoPago(v.getEstadoPago());
    dto.setPaymentId(v.getPaymentId());

    return dto;
  }

  
  private EventoDashboardDto mapEventoToEventoDashboardDto(
    EventoEntity e,
    Long entradasVendidas,
    BigDecimal recaudacion
  ) {
    EventoDashboardDto dto = new EventoDashboardDto();

    dto.setEventoId(e.getId());
    dto.setNombre(e.getEvento());          
    dto.setFecha(e.getFecha());      

    if (e.getEstablecimiento() != null) {
      dto.setEstablecimientoNombre(e.getEstablecimiento().getEstablecimiento());
    }

    Integer capacidad = null;
    if (e.getEstablecimiento() != null) {
      capacidad = e.getEstablecimiento().getCapacidad(); 
    }

    dto.setCapacidadTotal(capacidad);

    dto.setEntradasVendidas(entradasVendidas != null ? entradasVendidas : 0L);
    dto.setRecaudacion(recaudacion != null ? recaudacion : BigDecimal.ZERO);

    double ocupacion = 0.0;
    if (capacidad != null && capacidad > 0 && entradasVendidas != null) {
      ocupacion = (double) entradasVendidas / capacidad; 
    }
    dto.setPorcentajeOcupacion(ocupacion);

    return dto;
  }

  public ArtistaDashboardDto dashboardArtista(Long artistaId, LocalDateTime from, LocalDateTime to) {

    Long totalEntradas = ventaRepo.totalEntradasPorArtista(artistaId, from, to);
    List<PuntoCantidadDiaDto> porDia = ventaRepo.entradasPorDiaArtista(artistaId, from, to);
    List<EventoAsistenciaDto> ranking = ventaRepo.rankingEventosPorArtista(artistaId, from, to);
    List<EntradasPorTipoDto> porTipo = ventaRepo.entradasPorTipoArtista(artistaId, from, to);

    long totalEventos = ranking.size();
    double promedio = totalEventos > 0 ? (double) totalEntradas / totalEventos : 0.0;

    String mejorNombre = null;
    Long mejorEntradas = 0L;
    if (!ranking.isEmpty()) {
      mejorNombre = ranking.get(0).getEventoNombre();
      mejorEntradas = ranking.get(0).getEntradasVendidas();
    }

    ArtistaKpisDto kpis = new ArtistaKpisDto(
      totalEntradas,
      totalEventos,
      promedio,
      mejorNombre,
      mejorEntradas
    );

    return new ArtistaDashboardDto(kpis, porDia, ranking, porTipo);
  }
}

