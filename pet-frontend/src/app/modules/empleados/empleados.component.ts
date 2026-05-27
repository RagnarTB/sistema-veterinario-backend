import { Component, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormControl, FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { debounceTime, distinctUntilChanged } from 'rxjs';

import { EmpleadoService } from '../../core/services/empleado.service';
import { SedeService } from '../../core/services/sede.service';
import { EmpleadoResponse, SedeResponse } from '../../core/models/models';
import { EmpleadoDialogComponent } from './empleado-dialog.component';
import { RolesDialogComponent } from './roles-dialog.component';
import { ModalConfirmacionComponent } from '../../shared/components/modal-confirmacion/modal-confirmacion.component';

@Component({
  selector: 'app-empleados',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    MatTabsModule
  ],
  templateUrl: './empleados.component.html',
  styleUrls: ['./empleados.component.css']
})
export class EmpleadosComponent implements OnInit {
  displayedColumns: string[] = ['nombre', 'dni', 'roles', 'estado', 'acciones'];
  dataSource = signal<EmpleadoResponse[]>([]);
  totalElements = signal(0);
  pageSize = signal(10);
  pageIndex = signal(0);
  loading = signal(false);
  estadoActual = signal<boolean | null>(true);

  sedes = signal<SedeResponse[]>([]);
  sedeSeleccionada = signal<number | null>(null);

  searchControl = new FormControl('');

  constructor(
    private empleadoService: EmpleadoService,
    private sedeService: SedeService,
    private dialog: MatDialog,
    private snack: MatSnackBar
  ) {
    this.searchControl.valueChanges.pipe(
      debounceTime(400),
      distinctUntilChanged()
    ).subscribe(() => {
      this.pageIndex.set(0);
      this.cargarEmpleados();
    });
  }

  ngOnInit() {
    this.cargarSedes();
    this.cargarEmpleados();
  }

  cargarSedes() {
    this.sedeService.listar(0, 100).subscribe({
      next: (res: any) => this.sedes.set(res.content || []),
      error: () => console.error('Error al cargar sedes')
    });
  }

  onSedeChange() {
    this.pageIndex.set(0);
    this.cargarEmpleados();
  }

  cargarEmpleados() {
    this.loading.set(true);
    const searchTerm = this.searchControl.value || '';
    this.empleadoService.listar(this.pageIndex(), this.pageSize(), searchTerm, this.estadoActual(), this.sedeSeleccionada() || undefined).subscribe({
      next: (page: any) => {
        this.dataSource.set(page.content);
        this.totalElements.set(page.totalElements);
        this.loading.set(false);
      },
      error: () => {
        this.snack.open('Error al cargar empleados', 'Cerrar', { duration: 3000 });
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
    this.cargarEmpleados();
  }

  onPageChange(event: PageEvent) {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.cargarEmpleados();
  }

  abrirModal(empleado?: EmpleadoResponse) {
    const dialogRef = this.dialog.open(EmpleadoDialogComponent, {
      width: '600px',
      data: { empleado },
      disableClose: true
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) this.cargarEmpleados();
    });
  }

  gestionarRoles() {
    this.dialog.open(RolesDialogComponent, {
      width: '500px',
      disableClose: false
    });
  }

  cambiarEstado(empleado: EmpleadoResponse) {
    const nuevoEstado = !empleado.activo;
    const accion = nuevoEstado ? 'concederá acceso' : 'revocará el acceso';

    const dialogRef = this.dialog.open(ModalConfirmacionComponent, {
      width: '400px',
      data: {
        title: nuevoEstado ? 'Activar Empleado' : 'Desactivar Empleado',
        message: `¿Estás seguro de ${nuevoEstado ? 'activar' : 'desactivar'} a ${empleado.nombre}? Se le ${accion} al sistema.`,
        confirmText: nuevoEstado ? 'Sí, Activar' : 'Sí, Desactivar',
        isDestructive: !nuevoEstado
      }
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed) {
        this.loading.set(true);
        this.empleadoService.cambiarEstado(empleado.id, nuevoEstado).subscribe({
          next: () => {
            this.snack.open(`Empleado ${nuevoEstado ? 'activado' : 'desactivado'}`, 'Cerrar', { duration: 3000 });
            this.cargarEmpleados();
          },
          error: () => {
            this.snack.open(`Error al cambiar el estado del empleado`, 'Cerrar', { duration: 3000 });
            this.loading.set(false);
          }
        });
      }
    });
  }
}
