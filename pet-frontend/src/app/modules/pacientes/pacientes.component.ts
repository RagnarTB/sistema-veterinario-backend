import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { RouterModule } from '@angular/router';

import { MatTableModule } from '@angular/material/table';
import { MatPaginator, MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTabsModule } from '@angular/material/tabs';

import { PacienteService } from '../../core/services/paciente.service';
import { AuthService } from '../../core/services/auth.service';
import { PacienteResponse } from '../../core/models/models';
import { ModalConfirmacionComponent } from '../../shared/components/modal-confirmacion/modal-confirmacion.component';
import { PacienteDialogComponent } from './paciente-dialog/paciente-dialog.component';
import { EspeciesDialogComponent } from './especies-dialog/especies-dialog.component';

@Component({
  selector: 'app-pacientes',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ReactiveFormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatFormFieldModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatDialogModule,
    MatSnackBarModule,
    MatTabsModule,
    DatePipe
  ],
  providers: [DatePipe],
  templateUrl: './pacientes.component.html',
  styleUrl: './pacientes.component.css'
})
export class PacientesComponent implements OnInit {
  private pacienteService = inject(PacienteService);
  private authService = inject(AuthService);
  private dialog = inject(MatDialog);
  private snack = inject(MatSnackBar);

  displayedColumns: string[] = ['nombre', 'dueno', 'nacimiento', 'estado', 'acciones'];
  dataSource = signal<PacienteResponse[]>([]);
  totalElements = signal(0);
  pageSize = signal(10);
  pageIndex = signal(0);
  loading = signal(false);
  estadoActual = signal<boolean | null>(true);

  searchControl = new FormControl('');
  isAdmin = this.authService.isAdmin();
  esCliente = this.authService.activeRole() === 'ROLE_CLIENTE';
  canManagePacientes = this.authService.hasPermission('GESTIONAR_PACIENTES');

  ngOnInit(): void {
    this.cargarPacientes();

    this.searchControl.valueChanges
      .pipe(debounceTime(400), distinctUntilChanged())
      .subscribe(() => {
        this.pageIndex.set(0);
        this.cargarPacientes();
      });
  }

  cargarPacientes() {
    this.loading.set(true);
    const search = this.searchControl.value || '';

    this.pacienteService.listar(this.pageIndex(), this.pageSize(), search, this.estadoActual()).subscribe({
      next: (pageData) => {
        this.dataSource.set(pageData.content);
        this.totalElements.set(pageData.totalElements);
        this.loading.set(false);
      },
      error: () => {
        this.snack.open('Error al cargar la lista de pacientes', 'Cerrar', { duration: 3000 });
        this.loading.set(false);
      }
    });
  }

  onTabChange(index: number) {
    if (index === 0) {
      this.estadoActual.set(true);
    } else if (index === 1) {
      this.estadoActual.set(false);
    } else {
      this.estadoActual.set(null);
    }
    this.pageIndex.set(0);
    this.cargarPacientes();
  }

  onPageChange(event: PageEvent) {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.cargarPacientes();
  }

  abrirModalEspecies() {
    this.dialog.open(EspeciesDialogComponent, {
      width: '600px',
      disableClose: true
    });
  }

  abrirModal(paciente?: PacienteResponse) {
    const dialogRef = this.dialog.open(PacienteDialogComponent, {
      width: '600px',
      data: paciente || null,
      disableClose: true
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loading.set(true);
        if (paciente) {
          this.pacienteService.actualizar(paciente.id, result).subscribe({
            next: () => {
              this.snack.open('Paciente actualizado correctamente', 'Cerrar', { duration: 3000 });
              this.cargarPacientes();
            },
            error: (err) => {
              this.loading.set(false);
              this.snack.open(err.error?.mensaje || 'Error al actualizar', 'Cerrar', { duration: 4000 });
            }
          });
        } else {
          this.pacienteService.crear(result).subscribe({
            next: () => {
              this.snack.open('Paciente registrado correctamente', 'Cerrar', { duration: 3000 });
              this.cargarPacientes();
            },
            error: (err) => {
              this.loading.set(false);
              this.snack.open(err.error?.mensaje || 'Error al crear', 'Cerrar', { duration: 4000 });
            }
          });
        }
      }
    });
  }

  cambiarEstado(paciente: PacienteResponse) {
    const nuevoEstado = !paciente.activo;
    const accion = nuevoEstado ? 'reactivará' : 'dará de baja';

    const dialogRef = this.dialog.open(ModalConfirmacionComponent, {
      width: '400px',
      data: {
        title: nuevoEstado ? 'Dar de Alta' : 'Dar de Baja',
        message: `¿Estás seguro de que quieres dar de ${nuevoEstado ? 'alta' : 'baja'} a ${paciente.nombre}? Se le ${accion} en el sistema clínico.`,
        confirmText: nuevoEstado ? 'Sí, Dar de Alta' : 'Sí, Dar de Baja',
        isDestructive: !nuevoEstado
      }
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed) {
        this.loading.set(true);
        this.pacienteService.cambiarEstado(paciente.id, nuevoEstado).subscribe({
          next: () => {
            this.snack.open(`Paciente ${nuevoEstado ? 'activado' : 'dado de baja'}`, 'Cerrar', { duration: 3000 });
            this.cargarPacientes();
          },
          error: () => {
            this.loading.set(false);
            this.snack.open('Error al cambiar el estado', 'Cerrar', { duration: 3000 });
          }
        });
      }
    });
  }
}
