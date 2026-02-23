import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { NgxChartsModule } from '@swimlane/ngx-charts';
import { DashboardService } from '../../services/dashboard.service';

@Component({
  selector: 'app-graficos-productor',
  standalone: true,
  imports: [CommonModule, NgxChartsModule, RouterLink],
  templateUrl: './graficos-productor.component.html',
  styleUrls: ['./graficos-productor.component.css']
})
export class GraficosProductorComponent implements OnInit {

  productorId!: number;
  from!: string;
  to!: string;

  loading = false;
  error?: string;

  dashboard: any;

  ventasPorDiaChart: { name: string; value: number }[] = [];
  entradasPorTipoChart: { name: string; value: number }[] = [];
  recaudacionEventosChart: { name: string; value: number }[] = [];

  view: [number, number] = [700, 300];

  constructor(
    private route: ActivatedRoute,
    private dashboardService: DashboardService
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.productorId = Number(params['productorId']);
      this.from = params['from'];
      this.to = params['to'];

      if (!this.productorId || !this.from || !this.to) {
        this.error = 'Faltan parámetros para cargar los gráficos';
        console.error('Params gráficos:', params);
        return;
      }

      this.cargarDatos();
    });
  }

  cargarDatos(): void {
    this.loading = true;
    this.error = undefined;

    this.dashboardService
      .getDashboardProductor(this.from, this.to, this.productorId)
      .subscribe({
        next: data => {
          this.dashboard = data;
          this.buildCharts();
          this.loading = false;
        },
        error: err => {
          console.error('Error cargando datos para gráficos', err);
          this.error = 'No se pudieron cargar los gráficos';
          this.loading = false;
        }
      });
  }

  

  private buildCharts(): void {
    this.buildVentasPorDiaChart();
    this.buildEntradasPorTipoChart();
    this.buildRecaudacionEventosChart();
  }

  private buildVentasPorDiaChart(): void {
    const source = this.dashboard?.ventasPorDia || [];

    this.ventasPorDiaChart = source.map((p: any) => ({
      name: p.fecha,
      value: Number(p.totalDia ?? 0)
    }));
  }

  private buildEntradasPorTipoChart(): void {
    const source = this.dashboard?.entradasPorTipo || [];

    this.entradasPorTipoChart = source.map((t: any) => ({
      name: t.tipoEntradaNombre,
      value: Number(t.cantidadVendida ?? 0)
    }));
  }

  private buildRecaudacionEventosChart(): void {
    const source = this.dashboard?.topEventos || [];

    this.recaudacionEventosChart = source.map((e: any) => ({
      name: e.eventoNombre,
      value: Number(e.recaudacion ?? 0)
    }));
  }

  

  formatFechaEjeX = (value: string): string => {
  if (!value) return '';

  const parts = value.split('-');
  if (parts.length === 3) {
    const [yyyy, mm, dd] = parts;
    return `${dd}/${mm}/${yyyy}`;
  }

  return value;
  };

  formatMoneda = (value: number): string => {
    return new Intl.NumberFormat('es-AR', {
      style: 'currency',
      currency: 'ARS',
      maximumFractionDigits: 2
    }).format(value ?? 0);
  };

  tooltipMoneda = (data: any): string => {
    const value = data?.value ?? data?.data?.value ?? 0;
    return this.formatMoneda(value);
  };
}

