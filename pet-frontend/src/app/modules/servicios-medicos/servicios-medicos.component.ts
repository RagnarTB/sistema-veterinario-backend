import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { MatButtonModule } from '@angular/material/button';
import { ServicioMedicoService } from './servicio-medico.service';
import { ServicioMedicoResponse, TipoServicio } from '../../core/models/models';
import { ServicioMedicoDialogComponent } from './servicio-medico-dialog.component';
import { InsumosServicioDialogComponent } from './insumos-servicio-dialog.component';
import { ModalConfirmacionComponent } from '../../shared/components/modal-confirmacion/modal-confirmacion.component';

@Component({
  selector: 'app-servicios-medicos',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatTabsModule,
    MatButtonModule
  ],
  templateUrl: './servicios-medicos.component.html',
  styleUrls: ['./servicios-medicos.component.css']
})
export class ServiciosMedicosComponent implements OnInit {
  servicios = signal<ServicioMedicoResponse[]>([]);
  loading = signal(false);
  filtroNombre = signal('');
  filtroTipo = signal('');
  mostrarActivos = signal(true);

  tipos: TipoServicio[] = ['CONSULTA', 'VACUNACION', 'CIRUGIA', 'HOSPITALIZACION', 'ESTETICA', 'EXAMEN', 'OTRO'];

  serviciosFiltrados = computed(() => {
    let filtrados = this.servicios().filter(s => {
      const matchNombre = s.nombre.toLowerCase().includes(this.filtroNombre().toLowerCase());
      const matchTipo = !this.filtroTipo() || s.tipoServicio === this.filtroTipo();
      const matchEstado = s.activo === this.mostrarActivos();
      return matchNombre && matchTipo && matchEstado;
    });

    // Ordenamiento por tipo y luego nombre
    return filtrados.sort((a, b) => {
      const tipoA = a.tipoServicio || 'OTRO';
      const tipoB = b.tipoServicio || 'OTRO';
      if (tipoA !== tipoB) return tipoA.localeCompare(tipoB);
      return a.nombre.localeCompare(b.nombre);
    });
  });

  constructor(
    private service: ServicioMedicoService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.loading.set(true);
    // Cargamos todos los servicios (null) para filtrar localmente y que sea instantáneo
    this.service.listar().subscribe({
      next: (res) => {
        this.servicios.set(res);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  onTabChange(event: any): void {
    this.mostrarActivos.set(event.index === 0);
  }

  abrirDialogo(servicio?: ServicioMedicoResponse): void {
    const dialogRef = this.dialog.open(ServicioMedicoDialogComponent, {
      width: '600px',
      data: servicio || null
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        if (servicio) {
          this.service.actualizar(servicio.id, result).subscribe(() => {
            this.snackBar.open('Servicio actualizado con éxito', 'Cerrar', { duration: 3000 });
            this.cargar();
          });
        } else {
          this.service.crear(result).subscribe(() => {
            this.snackBar.open('Servicio creado con éxito', 'Cerrar', { duration: 3000 });
            this.cargar();
          });
        }
      }
    });
  }

  verInsumos(servicio: ServicioMedicoResponse): void {
    this.dialog.open(InsumosServicioDialogComponent, {
      width: '900px',
      data: servicio
    });
  }

  toggleEstado(servicio: ServicioMedicoResponse): void {
    const dialogRef = this.dialog.open(ModalConfirmacionComponent, {
      width: '400px',
      data: {
        title: servicio.activo ? 'Desactivar Servicio' : 'Reactivar Servicio',
        message: `¿Está seguro de ${servicio.activo ? 'desactivar' : 'reactivar'} el servicio "${servicio.nombre}"?`,
        confirmText: servicio.activo ? 'Desactivar' : 'Reactivar',
        isDestructive: servicio.activo
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.service.cambiarEstado(servicio.id, !servicio.activo).subscribe(() => {
          this.snackBar.open(`Servicio ${servicio.activo ? 'desactivado' : 'reactivado'}`, 'OK', { duration: 2000 });
          this.cargar();
        });
      }
    });
  }

  getTipoClass(tipo?: TipoServicio): string {
    if (!tipo) return 'chip-otro';
    return `chip-${tipo.toLowerCase()}`;
  }

  trackByServiceId(index: number, item: ServicioMedicoResponse): number {
    return item.id;
  }
}
