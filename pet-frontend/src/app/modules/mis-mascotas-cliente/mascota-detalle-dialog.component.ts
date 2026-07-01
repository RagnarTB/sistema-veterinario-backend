import { CommonModule, DatePipe } from '@angular/common';
import { Component, Inject, OnInit, inject, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';
import { ClienteService } from '../../core/services/cliente.service';
import { CitaResponse, DesparasitacionResponse, PacienteResponse, VacunaResponse } from '../../core/models/models';

interface MascotaDialogData {
  mascota: PacienteResponse;
}

@Component({
  selector: 'app-mascota-detalle-dialog',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    MatButtonModule,
    MatDialogModule,
    MatIconModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTableModule,
    MatTabsModule
  ],
  templateUrl: './mascota-detalle-dialog.component.html',
  styleUrl: './mascota-detalle-dialog.component.css'
})
export class MascotaDetalleDialogComponent implements OnInit {
  private clienteService = inject(ClienteService);
  private snack = inject(MatSnackBar);
  private dialogRef = inject(MatDialogRef<MascotaDetalleDialogComponent>);

  mascota: PacienteResponse;
  vacunas = signal<VacunaResponse[]>([]);
  desparasitaciones = signal<DesparasitacionResponse[]>([]);
  citas = signal<CitaResponse[]>([]);
  loadingPreventivo = signal(false);
  loadingCitas = signal(false);
  totalCitas = signal(0);
  pageIndex = signal(0);
  pageSize = signal(5);

  vacunaColumns = ['fechaAplicacion', 'nombreVacuna', 'fechaProximaDosis', 'responsable'];
  desparasitacionColumns = ['fechaAplicacion', 'tipo', 'productoUtilizado', 'fechaProximaDosis'];
  citaColumns = ['fecha', 'servicio', 'veterinario', 'estado'];

  constructor(@Inject(MAT_DIALOG_DATA) data: MascotaDialogData) {
    this.mascota = data.mascota;
  }

  ngOnInit(): void {
    this.cargarPreventivo();
    this.cargarCitas();
  }

  cerrar(): void {
    this.dialogRef.close();
  }

  cargarPreventivo(): void {
    this.loadingPreventivo.set(true);
    this.clienteService.listarVacunasDeMiMascota(this.mascota.id).subscribe({
      next: (res) => this.vacunas.set(res),
      error: () => this.snack.open('No se pudieron cargar las vacunas', 'Cerrar', { duration: 3000 })
    });
    this.clienteService.listarDesparasitacionesDeMiMascota(this.mascota.id).subscribe({
      next: (res) => {
        this.desparasitaciones.set(res);
        this.loadingPreventivo.set(false);
      },
      error: () => {
        this.loadingPreventivo.set(false);
        this.snack.open('No se pudieron cargar las desparasitaciones', 'Cerrar', { duration: 3000 });
      }
    });
  }

  cargarCitas(): void {
    this.loadingCitas.set(true);
    this.clienteService.listarCitasDeMiMascota(this.mascota.id, this.pageIndex(), this.pageSize()).subscribe({
      next: (page) => {
        this.citas.set(page.content);
        this.totalCitas.set(page.totalElements);
        this.loadingCitas.set(false);
      },
      error: () => {
        this.loadingCitas.set(false);
        this.snack.open('No se pudo cargar el historial de citas', 'Cerrar', { duration: 3000 });
      }
    });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.cargarCitas();
  }
}
