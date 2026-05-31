import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule, DateAdapter, MAT_DATE_FORMATS, MAT_DATE_LOCALE } from '@angular/material/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';

import { CitaService } from '../../core/services/cita.service';
import { SedeService } from '../../core/services/sede.service';
import { EmpleadoService } from '../../core/services/empleado.service';
import { AuthService } from '../../core/services/auth.service';
import { CitaResponse, SedeResponse, EmpleadoResponse, EstadoCita } from '../../core/models/models';
import { ESTADO_LABELS, ESTADO_COLORS, ESTADO_ICONS, getTransicionesPermitidas, getSiguienteEstadoRecepcion, esEstadoFinal } from './cita-estado.util';
import { ModalConfirmacionComponent } from '../../shared/components/modal-confirmacion/modal-confirmacion.component';
import { CitaAgendarDialogComponent } from './cita-agendar-dialog.component';
import { CitaDetalleDialogComponent } from './cita-detalle-dialog.component';

@Component({
  selector: 'app-citas',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatDialogModule, MatTabsModule, MatButtonModule,
    MatDatepickerModule, MatNativeDateModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatPaginatorModule, MatProgressSpinnerModule, MatTooltipModule
  ],
  providers: [
    { provide: MAT_DATE_LOCALE, useValue: 'es-ES' }
  ],
  templateUrl: './citas.component.html',
  styleUrls: ['./citas.component.css']
})
export class CitasComponent implements OnInit {
  private citaService = inject(CitaService);
  private sedeService = inject(SedeService);
  private empleadoService = inject(EmpleadoService);
  private authService = inject(AuthService);
  private dialog = inject(MatDialog);
  private snack = inject(MatSnackBar);

  sedes = signal<SedeResponse[]>([]);
  veterinarios = signal<EmpleadoResponse[]>([]);
  citas = signal<CitaResponse[]>([]);
  loading = signal(false);
  tabActivo = signal(0);
  totalElements = signal(0);
  pageSize = signal(25);
  pageIndex = signal(0);

  sedeSeleccionada = signal<number | null>(null);
  fechaSeleccionada = signal(this.hoy());
  fechaDate = signal<Date>(new Date());
  vetSeleccionado = signal<number | null>(null);
  estadoFiltro = signal<EstadoCita | null>(null);
  busqueda = signal('');

  private searchTimeout: any;

  estadosActivos: EstadoCita[] = ['AGENDADA', 'CONFIRMADA', 'EN_SALA_ESPERA', 'EN_CONSULTORIO', 'COMPLETADA', 'CANCELADA', 'NO_ASISTIO'];
  kanbanCols = [
    { estado: 'CONFIRMADA' as EstadoCita, label: 'Confirmadas' },
    { estado: 'EN_SALA_ESPERA' as EstadoCita, label: 'En Espera' },
    { estado: 'EN_CONSULTORIO' as EstadoCita, label: 'En Consultorio' },
    { estado: 'COMPLETADA' as EstadoCita, label: 'Completadas' },
  ];

  getEstadoLabel = (e: EstadoCita) => ESTADO_LABELS[e];
  getEstadoColor = (e: EstadoCita) => ESTADO_COLORS[e];
  getEstadoIcon = (e: EstadoCita) => ESTADO_ICONS[e];
  getSiguienteEstado = (e: EstadoCita) => getSiguienteEstadoRecepcion(e);
  esEstadoFinal = (e: EstadoCita) => esEstadoFinal(e);

  getEstadoHexColor(e: EstadoCita): string {
    const map: Record<string, string> = {
      'AGENDADA': '#64748b', 'CONFIRMADA': '#3b82f6', 'EN_SALA_ESPERA': '#f59e0b',
      'EN_CONSULTORIO': '#f97316', 'COMPLETADA': '#10b981', 'CANCELADA': '#ef4444', 'NO_ASISTIO': '#ef4444'
    };
    return map[e] || '#00bdbd';
  }

  ngOnInit(): void {
    this.sedeService.listarActivas().subscribe(s => {
      this.sedes.set(s);
      const userSedes = this.authService.currentSedeIds();
      const filtered = userSedes.length > 0 ? s.filter(x => userSedes.includes(x.id)) : s;
      if (filtered.length > 0) {
        const saved = sessionStorage.getItem('vet_sede_activa');
        const savedId = saved ? parseInt(saved) : null;
        const id = savedId && filtered.some(x => x.id === savedId) ? savedId : filtered[0].id;
        this.onSedeChange(id);
      }
    });
  }

  onSedeChange(id: number): void {
    this.sedeSeleccionada.set(id);
    sessionStorage.setItem('vet_sede_activa', id.toString());
    this.cargarVeterinarios();
    this.cargarCitas();
  }

  onFechaChange(f: string): void {
    this.fechaSeleccionada.set(f);
    const parts = f.split('-');
    if (parts.length === 3) {
      this.fechaDate.set(new Date(parseInt(parts[0]), parseInt(parts[1]) - 1, parseInt(parts[2])));
    }
    this.cargarCitas();
  }

  onFechaDateChange(d: Date | null): void {
    if (!d) return;
    this.fechaDate.set(d);
    const f = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
    this.fechaSeleccionada.set(f);
    this.cargarCitas();
  }

  fechaFormateada(): string {
    const d = this.fechaDate();
    const hoy = new Date();
    if (d.toDateString() === hoy.toDateString()) return 'Hoy';

    hoy.setDate(hoy.getDate() + 1);
    if (d.toDateString() === hoy.toDateString()) return 'Mañana';

    return d.toLocaleDateString('es-ES', { weekday: 'long', day: 'numeric', month: 'long' });
  }

  onVetChange(id: number | null): void { this.vetSeleccionado.set(id); this.cargarCitas(); }
  onEstadoFiltro(e: EstadoCita | null): void { this.estadoFiltro.set(e); this.cargarCitas(); }
  onTabChange(i: number): void { this.tabActivo.set(i); this.pageIndex.set(0); this.cargarCitas(); }
  onPageChange(e: PageEvent): void { this.pageIndex.set(e.pageIndex); this.pageSize.set(e.pageSize); this.cargarCitas(); }

  onBusquedaChange(val: string): void {
    this.busqueda.set(val);
    clearTimeout(this.searchTimeout);
    this.searchTimeout = setTimeout(() => this.cargarCitas(), 400);
  }

  cargarVeterinarios(): void {
    const sedeId = this.sedeSeleccionada();
    if (!sedeId) return;
    this.empleadoService.listar(0, 100, '', true, sedeId).subscribe(res => {
      this.veterinarios.set(res.content.filter(e => e.nombresRoles?.includes('ROLE_VETERINARIO')));
    });
  }

  cargarCitas(): void {
    const sedeId = this.sedeSeleccionada();
    if (!sedeId) return;
    this.loading.set(true);

    const tab = this.tabActivo();
    let fecha: string | undefined;
    let estado: EstadoCita | undefined;

    if (tab === 0 || tab === 1) {
      fecha = this.fechaSeleccionada();
      estado = this.estadoFiltro() ?? undefined;
    } else {
      fecha = this.fechaSeleccionada() || undefined;
      estado = this.estadoFiltro() ?? undefined;
    }

    this.citaService.listar(
      sedeId, this.pageIndex(), this.pageSize(),
      this.busqueda(), fecha, estado,
      this.vetSeleccionado() ?? undefined
    ).subscribe({
      next: res => {
        this.citas.set(res.content);
        this.totalElements.set(res.totalElements);
        this.loading.set(false);
      },
      error: () => { this.loading.set(false); this.snack.open('Error al cargar citas', 'Cerrar', { duration: 3000 }); }
    });
  }

  getKanbanCitas(estado: EstadoCita): CitaResponse[] {
    return this.citas().filter(c => c.estado === estado);
  }

  abrirWizard(cita?: CitaResponse): void {
    const ref = this.dialog.open(CitaAgendarDialogComponent, {
      width: '800px', maxHeight: '95vh', disableClose: true,
      data: { cita: cita || null, sedeId: this.sedeSeleccionada() },
      panelClass: 'wizard-dialog-panel'
    });
    ref.afterClosed().subscribe(r => { if (r) this.cargarCitas(); });
  }

  abrirDetalle(cita: CitaResponse): void {
    const ref = this.dialog.open(CitaDetalleDialogComponent, {
      width: '650px', data: cita
    });
    ref.afterClosed().subscribe(r => {
      if (r === 'refresh') this.cargarCitas();
      else if (r === 'edit') this.abrirWizard(cita);
    });
  }

  avanzarEstado(cita: CitaResponse, event: Event): void {
    event.stopPropagation();
    const next = getSiguienteEstadoRecepcion(cita.estado);
    if (!next) return;
    this.citaService.cambiarEstado(cita.id, next).subscribe({
      next: () => { this.snack.open(`Estado cambiado a ${ESTADO_LABELS[next]}`, 'OK', { duration: 2000 }); this.cargarCitas(); },
      error: err => this.snack.open(err.error?.message || 'Error al cambiar estado', 'Cerrar', { duration: 3000 })
    });
  }

  private hoy(): string {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }
}