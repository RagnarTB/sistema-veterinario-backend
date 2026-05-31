import { Component, signal, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { HospitalizacionService, HospitalizacionResponse } from '../../core/services/hospitalizacion.service';
import { JaulaService, JaulaResponse } from '../../core/services/jaula.service';
import { AuthService } from '../../core/services/auth.service';
import { SedeService } from '../../core/services/sede.service';
import { SedeResponse } from '../../core/models/models';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { HospitalizacionIngresoDialogComponent } from './hospitalizacion-ingreso-dialog.component';
import { MonitoreoHospitalizacionDialogComponent } from './monitoreo-hospitalizacion-dialog.component';
import { ModalConfirmacionComponent } from '../../shared/components/modal-confirmacion/modal-confirmacion.component';
import { FormsModule } from '@angular/forms';
@Component({
  selector: 'app-hospitalizacion',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDialogModule,
    MatSidenavModule,
    FormsModule
  ],
  templateUrl: './hospitalizacion.component.html',
  styleUrls: ['./hospitalizacion.component.css']
})
export class HospitalizacionComponent implements OnInit {
  // Estado general
  todasLasJaulas: JaulaResponse[] = [];
  hospitalizacionesActivas: HospitalizacionResponse[] = [];
  
  // Grid combinada
  jaulasGrid = signal<any[]>([]);
  
  // Kardex Lateral
  pacienteSeleccionado: HospitalizacionResponse | null = null;
  historialPaciente: any[] = [];
  sidenavAbierto = false;
  
  loading = signal(true);
  sedeActualId: number = 1;
  sedes = signal<SedeResponse[]>([]);

  private hospitalizacionService = inject(HospitalizacionService);
  private jaulaService = inject(JaulaService);
  private authService = inject(AuthService);
  private sedeService = inject(SedeService);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);

  ngOnInit() {
    this.sedeActualId = Number(localStorage.getItem('vet_sede_id')) || 1;
    this.cargarSedes();
    this.cargarTodo();
  }

  cargarSedes() {
    this.sedeService.listarActivas().subscribe({
      next: (res) => this.sedes.set(res),
      error: () => {}
    });
  }

  onSedeChange() {
    this.sedeActualId = Number(this.sedeActualId);
    this.cargarTodo();
  }

  cargarTodo() {
    if (!this.sedeActualId) return;
    this.loading.set(true);

    // Cargar jaulas
    this.jaulaService.listarJaulasPorSede(this.sedeActualId).subscribe({
      next: (jaulas) => {
        this.todasLasJaulas = jaulas;
        
        // Cargar pacientes
        this.hospitalizacionService.listarActivas(this.sedeActualId || undefined).subscribe({
          next: (activas) => {
            this.hospitalizacionesActivas = activas;
            this.combinarGrid();
            this.loading.set(false);
          },
          error: () => this.manejarError('hospitalizaciones')
        });
      },
      error: () => this.manejarError('jaulas')
    });
  }

  combinarGrid() {
    const grid = this.todasLasJaulas.map(j => {
      // Buscar si la jaula tiene paciente asignado
      const hospedaje = this.hospitalizacionesActivas.find(h => h.jaulaNumero === j.numero);
      
      return {
        jaula: j,
        paciente: hospedaje || null,
        estaOcupada: !!hospedaje
      };
    });

    // Ordenar: Ocupadas primero, o por numero
    grid.sort((a, b) => a.jaula.numero.localeCompare(b.jaula.numero));
    this.jaulasGrid.set(grid);
  }

  manejarError(tipo: string) {
    this.snackBar.open(`Error al cargar ${tipo}`, 'Cerrar', { duration: 3000 });
    this.loading.set(false);
  }

  // ---- ACCIONES DE JAULA (VACÍA) ----
  cambiarEstadoJaula(jaulaId: number, nuevoEstado: string): void {
    const dialogRef = this.dialog.open(ModalConfirmacionComponent, {
      width: '400px',
      data: {
        title: 'Confirmar Acción',
        message: `¿Mover jaula a estado ${nuevoEstado}?`,
        confirmText: 'Mover'
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.jaulaService.cambiarEstado(jaulaId, nuevoEstado).subscribe({
          next: () => this.cargarTodo(),
          error: () => this.snackBar.open('Error al cambiar estado', 'Cerrar', { duration: 3000 })
        });
      }
    });
  }

  abrirIngresoModal(jaulaOpcional?: any) {
    const dialogRef = this.dialog.open(HospitalizacionIngresoDialogComponent, {
      width: '600px',
      data: { jaulaPreseleccionada: jaulaOpcional }
    });

    dialogRef.afterClosed().subscribe(res => {
      if (res) this.cargarTodo();
    });
  }

  // ---- ACCIONES DE PACIENTE (OCUPADA) ----
  abrirMonitoreoModal(h: HospitalizacionResponse) {
    const dialogRef = this.dialog.open(MonitoreoHospitalizacionDialogComponent, {
      width: '600px',
      data: h
    });

    dialogRef.afterClosed().subscribe(res => {
      if (res) this.cargarTodo();
    });
  }

  darDeAlta(id: number) {
    const dialogRef = this.dialog.open(ModalConfirmacionComponent, {
      width: '400px',
      data: {
        title: 'Dar de Alta',
        message: '¿Está seguro de dar de alta a este paciente?',
        confirmText: 'Dar de Alta'
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.hospitalizacionService.darDeAlta(id).subscribe({
          next: () => {
            this.snackBar.open('Paciente dado de alta.', 'Cerrar', { duration: 3000 });
            this.cargarTodo();
          },
          error: () => this.snackBar.open('Error al dar de alta', 'Cerrar', { duration: 3000 })
        });
      }
    });
  }

  abrirHistorial(h: HospitalizacionResponse) {
    this.pacienteSeleccionado = h;
    this.sidenavAbierto = true;
    this.hospitalizacionService.obtenerHistorial(h.id).subscribe({
      next: (data) => this.historialPaciente = data,
      error: () => this.snackBar.open('Error al cargar historial', 'Cerrar', { duration: 3000 })
    });
  }

  cerrarHistorial() {
    this.sidenavAbierto = false;
    this.pacienteSeleccionado = null;
    this.historialPaciente = [];
  }

  // ---- ESTILOS ----
  getBorderColor(estado: string, ocupada?: boolean): string {
    if (ocupada) return '#3b82f6';
    switch(estado) {
      case 'DISPONIBLE': return '#86efac';
      case 'EN_DESINFECCION': return '#fdba74';
      case 'MANTENIMIENTO': return '#cbd5e1';
      case 'BLOQUEO_CUARENTENA': return '#f87171';
      default: return '#e2e8f0';
    }
  }

  getGravedadStyle(nivel: string): Record<string, string> {
    switch(nivel) {
      case 'ESTABLE': return { 'background-color': '#dcfce7', 'color': '#166534' };
      case 'OBSERVACION': return { 'background-color': '#ffedd5', 'color': '#9a3412' };
      case 'CRITICO': return { 'background-color': '#fee2e2', 'color': '#991b1b' };
      default: return { 'background-color': '#f1f5f9', 'color': '#475569' };
    }
  }
}