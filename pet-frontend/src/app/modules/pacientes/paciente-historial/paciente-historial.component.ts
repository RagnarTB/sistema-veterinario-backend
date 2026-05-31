import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MatTabsModule } from '@angular/material/tabs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { PacienteService } from '../../../core/services/paciente.service';
import { VacunaService } from '../../../core/services/vacuna.service';
import { DesparasitacionService } from '../../../core/services/desparasitacion.service';
import { PacienteResponse, VacunaResponse, DesparasitacionResponse } from '../../../core/models/models';
import { RegistrarVacunaDialogComponent } from './registrar-vacuna-dialog/registrar-vacuna-dialog.component';
import { RegistrarDesparasitacionDialogComponent } from './registrar-desparasitacion-dialog/registrar-desparasitacion-dialog.component';

@Component({
  selector: 'app-paciente-historial',
  standalone: true,
  imports: [
    CommonModule, 
    MatTabsModule, 
    MatButtonModule, 
    MatIconModule, 
    MatTableModule,
    MatDialogModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './paciente-historial.component.html',
  styleUrls: ['./paciente-historial.component.css']
})
export class PacienteHistorialComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private pacienteService = inject(PacienteService);
  private vacunaService = inject(VacunaService);
  private desparasitacionService = inject(DesparasitacionService);
  private snack = inject(MatSnackBar);
  private dialog = inject(MatDialog);

  pacienteId!: number;
  paciente = signal<PacienteResponse | null>(null);
  loading = signal<boolean>(true);

  // Tablas
  vacunas = signal<VacunaResponse[]>([]);
  desparasitaciones = signal<DesparasitacionResponse[]>([]);

  vacunasColumns = ['fechaAplicacion', 'nombreVacuna', 'fechaProximaDosis', 'responsable', 'observaciones'];
  desparasitacionColumns = ['fechaAplicacion', 'tipo', 'productoUtilizado', 'pesoAlMomento', 'fechaProximaDosis', 'responsable'];

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.pacienteId = +id;
      this.cargarTodo();
    } else {
      this.volver();
    }
  }

  volver(): void {
    this.router.navigate(['/app/pacientes']);
  }

  cargarTodo(): void {
    this.loading.set(true);
    this.pacienteService.buscarPorId(this.pacienteId).subscribe({
      next: (res) => {
        this.paciente.set(res);
        this.cargarVacunas();
        this.cargarDesparasitaciones();
        this.loading.set(false);
      },
      error: () => {
        this.snack.open('Error al cargar datos del paciente', 'Cerrar', { duration: 3000 });
        this.loading.set(false);
        this.volver();
      }
    });
  }

  cargarVacunas(): void {
    this.vacunaService.listarPorPaciente(this.pacienteId).subscribe({
      next: (res) => this.vacunas.set(res),
      error: () => this.snack.open('Error al cargar vacunas', 'Cerrar', { duration: 3000 })
    });
  }

  cargarDesparasitaciones(): void {
    this.desparasitacionService.listarPorPaciente(this.pacienteId).subscribe({
      next: (res) => this.desparasitaciones.set(res),
      error: () => this.snack.open('Error al cargar desparasitaciones', 'Cerrar', { duration: 3000 })
    });
  }

  abrirRegistrarVacuna(): void {
    const dialogRef = this.dialog.open(RegistrarVacunaDialogComponent, {
      width: '500px',
      data: { pacienteId: this.pacienteId }
    });

    dialogRef.afterClosed().subscribe(res => {
      if (res) this.cargarVacunas();
    });
  }

  abrirRegistrarDesparasitacion(): void {
    const dialogRef = this.dialog.open(RegistrarDesparasitacionDialogComponent, {
      width: '500px',
      data: { pacienteId: this.pacienteId }
    });

    dialogRef.afterClosed().subscribe(res => {
      if (res) this.cargarDesparasitaciones();
    });
  }
}
