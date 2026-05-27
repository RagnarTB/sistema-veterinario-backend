import { Component, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import { RolService } from '../../core/services/rol.service';
import { RolResponse } from '../../core/models/models';

@Component({
    selector: 'app-roles-dialog',
    standalone: true,
    imports: [
      CommonModule,
      ReactiveFormsModule,
      MatDialogModule,
      MatButtonModule,
      MatIconModule,
      MatProgressSpinnerModule,
    ],
    templateUrl: './roles-dialog.component.html',
    styleUrls: ['./roles-dialog.component.css']
})
export class RolesDialogComponent implements OnInit {
  roles = signal<RolResponse[]>([]);
  loading = signal(false);
  form: FormGroup;

  rolesProtegidos = ['ROLE_ADMIN', 'ROLE_CLIENTE', 'ROLE_VETERINARIO', 'ROLE_RECEPCIONISTA'];

  constructor(
    private dialogRef: MatDialogRef<RolesDialogComponent>,
    private rolService: RolService,
    private fb: FormBuilder,
    private snack: MatSnackBar
  ) {
    this.form = this.fb.group({
      nombre: ['', Validators.required]
    });
  }

  ngOnInit() {
    this.cargarRoles();
  }

  cargarRoles() {
    this.loading.set(true);
    this.rolService.listarTodos().subscribe({
      next: (data: any) => {
        this.roles.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.snack.open('Error al cargar roles', 'Cerrar', { duration: 3000 });
        this.loading.set(false);
      }
    });
  }

  isProtegido(nombre: string): boolean {
    return this.rolesProtegidos.includes(nombre);
  }

  agregarRol() {
    if (this.form.invalid) return;

    const nombreRol = this.form.value.nombre.toUpperCase().trim();
    this.loading.set(true);

    this.rolService.crear({ nombre: nombreRol }).subscribe({
      next: () => {
        this.snack.open('Rol agregado correctamente', 'Cerrar', { duration: 3000 });
        this.form.reset();
        this.cargarRoles();
      },
      error: (err: any) => {
        this.snack.open(err.error?.mensaje || 'Error al agregar rol', 'Cerrar', { duration: 3000 });
        this.loading.set(false);
      }
    });
  }

  eliminarRol(rol: RolResponse) {
    if (!confirm(`¿Está seguro de eliminar el rol ${rol.nombre}?`)) return;

    this.loading.set(true);
    this.rolService.eliminar(rol.id).subscribe({
      next: () => {
        this.snack.open('Rol eliminado correctamente', 'Cerrar', { duration: 3000 });
        this.cargarRoles();
      },
      error: (err: any) => {
        this.snack.open(err.error?.mensaje || 'Error al eliminar rol', 'Cerrar', { duration: 3000 });
        this.loading.set(false);
      }
    });
  }

  cambiarEstado(rol: RolResponse) {
    const accion = rol.activo ? 'desactivar' : 'activar';
    if (!confirm(`¿Está seguro de ${accion} el rol ${rol.nombre.replace('ROLE_', '')}?`)) return;

    this.loading.set(true);
    this.rolService.cambiarEstado(rol.id, !rol.activo).subscribe({
      next: () => {
        this.snack.open(`Rol ${!rol.activo ? 'activado' : 'desactivado'} correctamente`, 'Cerrar', { duration: 3000 });
        this.cargarRoles();
      },
      error: (err: any) => {
        this.snack.open(err.error?.mensaje || `Error al ${accion} rol`, 'Cerrar', { duration: 3000 });
        this.loading.set(false);
      }
    });
  }
}
