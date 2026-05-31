import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartType } from 'chart.js';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { ReporteService } from '../../core/services/reporte.service';
import { VacunaService } from '../../core/services/vacuna.service';
import { DesparasitacionService } from '../../core/services/desparasitacion.service';
import { DashboardResumen, TopProducto, CitasVeterinario, VacunaResponse, DesparasitacionResponse } from '../../core/models/models';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatTableModule,
    MatIconModule,
    BaseChartDirective,
    MatProgressSpinnerModule
  ],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css'],
})
export class DashboardComponent implements OnInit {
  private reporteService = inject(ReporteService);
  private vacunaService = inject(VacunaService);
  private desparasitacionService = inject(DesparasitacionService);

  loading = signal(true);
  resumen = signal<DashboardResumen | null>(null);
  topProductos = signal<TopProducto[]>([]);
  veterinarios = signal<CitasVeterinario[]>([]);
  
  proximasVacunas = signal<VacunaResponse[]>([]);
  proximasDesparasitaciones = signal<DesparasitacionResponse[]>([]);
  alertasStock = signal<any[]>([]);

  authService = inject(AuthService);

  // Configuración de la tabla
  displayedColumns: string[] = ['nombre', 'citas'];
  vacunasColumns: string[] = ['paciente', 'vacuna', 'fecha'];
  desparasitacionColumns: string[] = ['paciente', 'producto', 'fecha'];

  // Configuración del Chart
  chartType: ChartType = 'bar';
  chartData: ChartConfiguration['data'] = {
    labels: [],
    datasets: []
  };

  chartOptions: ChartConfiguration['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      tooltip: {
        backgroundColor: 'rgba(15, 23, 42, 0.9)',
        titleColor: '#fff',
        bodyColor: '#cbd5e1',
        padding: 12,
        cornerRadius: 8,
        displayColors: false
      }
    },
    scales: {
      y: {
        beginAtZero: true,
        grid: {
          color: 'rgba(255, 255, 255, 0.05)',
        },
        ticks: { color: '#94a3b8' }
      },
      x: {
        grid: { display: false },
        ticks: { color: '#94a3b8' }
      }
    }
  };

  ngOnInit() {
    this.cargarDatos();
  }

  cargarDatos() {
    // Para simplificar, hacemos 3 llamadas concurrentes en un flujo simple sin RxJS forkJoin
    // debido al signal state
    let pending = 3;
    const checkDone = () => {
      pending--;
      if (pending === 0) this.loading.set(false);
    };

    this.reporteService.getDashboard().subscribe({
      next: (data) => {
        this.resumen.set(data);
        checkDone();
      },
      error: () => checkDone()
    });

    this.reporteService.getTopProductos().subscribe({
      next: (data) => {
        this.topProductos.set(data);
        this.actualizarGrafico(data);
        checkDone();
      },
      error: () => checkDone()
    });

    this.reporteService.getRendimientoVeterinarios().subscribe({
      next: (data) => {
        this.veterinarios.set(data);
        checkDone();
      },
      error: () => checkDone()
    });

    this.vacunaService.listarProximasDosis().subscribe({
      next: (data) => {
        this.proximasVacunas.set(data);
      }
    });

    this.desparasitacionService.listarProximasDosis().subscribe({
      next: (data) => {
        this.proximasDesparasitaciones.set(data);
      }
    });

    const role = this.authService.activeRole();
    if (role === 'ROLE_ADMIN' || role === 'ROLE_RECEPCIONISTA') {
      this.reporteService.getAlertasStock().subscribe({
        next: (data) => {
          this.alertasStock.set(data);
        }
      });
    }
  }

  actualizarGrafico(datos: TopProducto[]) {
    // Si no hay datos, evitamos error
    if (!datos || datos.length === 0) {
      this.chartData = { labels: [], datasets: [] };
      return;
    }

    const labels = datos.map(d => d.nombreProducto || 'Desconocido');
    const dataValues = datos.map(d => d.cantidadVendida || 0);

    // Paleta premium
    const bgColors = [
      'rgba(0, 189, 189, 0.8)',
      'rgba(139, 92, 246, 0.8)',
      'rgba(59, 130, 246, 0.8)',
      'rgba(16, 185, 129, 0.8)',
      'rgba(245, 158, 11, 0.8)'
    ];

    this.chartData = {
      labels: labels,
      datasets: [
        {
          data: dataValues,
          backgroundColor: bgColors,
          borderRadius: 8,
          borderSkipped: false,
          barPercentage: 0.6,
        }
      ]
    };
  }
}
