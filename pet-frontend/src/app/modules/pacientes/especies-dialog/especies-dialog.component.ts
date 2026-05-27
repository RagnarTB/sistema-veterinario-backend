import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogRef, MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';

import { EspecieService } from '../../../core/services/especie.service';
import { EspecieResponse } from '../../../core/models/models';
import { ModalConfirmacionComponent } from '../../../shared/components/modal-confirmacion/modal-confirmacion.component';

@Component({
  selector: 'app-especies-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatTableModule,
    MatIconModule,
    MatInputModule,
    MatFormFieldModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTooltipModule
  ],
  templateUrl: './especies-dialog.component.html',
  styleUrls: ['./especies-dialog.component.css']
})
export class EspeciesDialogComponent implements OnInit {
  private especieService = inject(EspecieService);
  private snack = inject(MatSnackBar);
  private dialog = inject(MatDialog);

  dataSource = signal<EspecieResponse[]>([]);
  displayedColumns: string[] = ['nombre', 'acciones'];
  loading = signal(false);

  nuevoNombre = new FormControl('', [Validators.required, Validators.minLength(2)]);

  ngOnInit() {
    this.cargar();
  }

  cargar() {
    this.loading.set(true);
    this.especieService.listar().subscribe({
      next: (res) => {
        this.dataSource.set(res);
        this.loading.set(false);
      },
      error: () => {
        this.snack.open('Error al cargar especies', 'Cerrar', { duration: 3000 });
        this.loading.set(false);
      }
    });
  }

  agregar() {
    if (this.nuevoNombre.invalid) return;
    this.loading.set(true);
    const nombreEspecie = this.nuevoNombre.value?.trim().toUpperCase();
    this.especieService.crear({ nombre: nombreEspecie }).subscribe({
      next: () => {
        this.snack.open('Especie guardada', 'Exito', { duration: 3000 });
        this.nuevoNombre.reset();
        this.cargar();
      },
      error: (err) => {
        this.snack.open(err.error?.mensaje || 'Error al guardar', 'Cerrar', { duration: 4000 });
        this.loading.set(false);
      }
    });
  }

  cambiarEstado(especie: EspecieResponse) {
    const dialogRef = this.dialog.open(ModalConfirmacionComponent, {
      width: '400px',
      data: {
        title: especie.activo ? 'Desactivar Especie' : 'Activar Especie',
        message: `¿Seguro que deseas ${especie.activo ? 'desactivar' : 'activar'} la especie ${especie.nombre}?`,
        confirmText: especie.activo ? 'Desactivar' : 'Activar',
        isDestructive: especie.activo
      }
    });

    dialogRef.afterClosed().subscribe(res => {
      if (res) {
        this.loading.set(true);
        this.especieService.cambiarEstado(especie.id).subscribe({
          next: () => {
            this.snack.open('Estado modificado', 'Ok', { duration: 3000 });
            this.cargar();
          },
          error: (err) => {
            this.loading.set(false);
            this.snack.open(err.error?.mensaje || 'No se puede desactivar (probablemente esté en uso)', 'Cerrar', { duration: 5000 });
          }
        });
      }
    });
  }

  soloLetras(event: KeyboardEvent): void {
    const teclas_permitidas = ['Backspace', 'Delete', 'ArrowLeft', 'ArrowRight', 'Tab', 'Home', 'End'];
    const patron = /^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]$/;
    if (!teclas_permitidas.includes(event.key) && !patron.test(event.key)) {
      event.preventDefault();
    }
  }

  eliminarFisicamente(especie: EspecieResponse) {
    const dialogRef = this.dialog.open(ModalConfirmacionComponent, {
      width: '400px',
      data: {
        title: 'Borrado Físico Definitivo',
        message: `¿ELIMINAR PERMANENTEMENTE la especie ${especie.nombre}? No debe tener pacientes afiliados.`,
        confirmText: 'Borrar Definitivamente',
        isDestructive: true
      }
    });

    dialogRef.afterClosed().subscribe(res => {
      if (res) {
        this.loading.set(true);
        this.especieService.eliminarFisicamente(especie.id).subscribe({
          next: () => {
            this.snack.open('Especie eliminada', 'Ok', { duration: 3000 });
            this.cargar();
          },
          error: (err) => {
            this.loading.set(false);
            // This is where our backend throws the error because it is tied!
            this.snack.open(err.error?.mensaje || 'No se puede borrar porque está en uso por pacientes.', 'Cerrar', { duration: 5000 });
          }
        });
      }
    });
  }
}
