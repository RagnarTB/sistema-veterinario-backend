import { Component, Inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import { RolService } from '../../core/services/rol.service';
import { PermisoService, PermisoDTO } from '../../core/services/permiso.service';
import { RolResponse } from '../../core/models/models';

@Component({
  selector: 'app-permisos-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatCheckboxModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './permisos-dialog.component.html',
  styleUrls: ['./roles-dialog.component.css'] // reutilizamos los estilos del modal de roles
})
export class PermisosDialogComponent implements OnInit {
  loading = signal(false);
  permisosDisponibles = signal<PermisoDTO[]>([]);
  permisosSeleccionados = new Set<string>();

  constructor(
    public dialogRef: MatDialogRef<PermisosDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { rol: RolResponse },
    private rolService: RolService,
    private permisoService: PermisoService,
    private snack: MatSnackBar
  ) {
    if (this.data.rol.permisos) {
      this.data.rol.permisos.forEach(p => this.permisosSeleccionados.add(p.nombre));
    }
  }

  ngOnInit(): void {
    this.cargarPermisos();
  }

  cargarPermisos() {
    this.loading.set(true);
    this.permisoService.listarTodos().subscribe({
      next: (data) => {
        this.permisosDisponibles.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.snack.open('Error al cargar permisos', 'Cerrar', { duration: 3000 });
        this.loading.set(false);
      }
    });
  }

  togglePermiso(nombre: string, checked: boolean) {
    if (checked) {
      this.permisosSeleccionados.add(nombre);
    } else {
      this.permisosSeleccionados.delete(nombre);
    }
  }

  guardar() {
    this.loading.set(true);
    const permisosArray = Array.from(this.permisosSeleccionados);
    this.rolService.actualizarPermisos(this.data.rol.id, permisosArray).subscribe({
      next: (rolActualizado) => {
        this.snack.open('Permisos actualizados correctamente', 'Cerrar', { duration: 3000 });
        this.dialogRef.close(rolActualizado);
      },
      error: (err: any) => {
        this.snack.open(err.error?.mensaje || 'Error al actualizar permisos', 'Cerrar', { duration: 3000 });
        this.loading.set(false);
      }
    });
  }
}
