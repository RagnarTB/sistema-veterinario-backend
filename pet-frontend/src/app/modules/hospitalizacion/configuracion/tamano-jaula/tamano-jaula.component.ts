import { Component, signal, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { TamanoJaulaService, TamanoJaula } from '../../../../core/services/tamano-jaula.service';
import { ModalConfirmacionComponent } from '../../../../shared/components/modal-confirmacion/modal-confirmacion.component';

@Component({
  selector: 'app-tamano-jaula-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatButtonModule, MatDialogModule, MatProgressSpinnerModule],
  template: `
    <div style="padding: 24px; font-family: system-ui, -apple-system, sans-serif;">
      <h2 style="margin: 0 0 16px 0; font-size: 20px; font-weight: 600; color: #1e293b;">
        {{ data ? 'Editar Tamaño' : 'Nuevo Tamaño' }}
      </h2>
      <form [formGroup]="form" (ngSubmit)="guardar()">
        <div style="margin-bottom: 16px;">
          <label style="display: block; font-size: 13px; font-weight: 500; color: #475569; margin-bottom: 6px;">Nombre</label>
          <input formControlName="nombre" type="text"
            style="width: 100%; padding: 10px 12px; background: white; border: 1px solid #cbd5e1; border-radius: 8px; outline: none; font-size: 14px; box-sizing: border-box;"
            placeholder="Ej: Pequeña">
        </div>
        <div style="margin-bottom: 24px; display: flex; align-items: center; gap: 8px;">
          <input formControlName="activo" type="checkbox" id="activo" style="width: 16px; height: 16px; cursor: pointer;">
          <label for="activo" style="font-size: 14px; color: #334155; cursor: pointer; user-select: none;">Activo</label>
        </div>
        
        <div style="display: flex; justify-content: flex-end; gap: 12px;">
          <button type="button" mat-button (click)="dialogRef.close()" style="border-radius: 8px;">Cancelar</button>
          <button type="submit" mat-flat-button color="primary" style="border-radius: 8px;" [disabled]="form.invalid || saving()">
            <mat-spinner *ngIf="saving()" diameter="20" style="display: inline-block; margin-right: 8px;"></mat-spinner>
            Guardar
          </button>
        </div>
      </form>
    </div>
  `
})
export class TamanoJaulaDialogComponent {
  data = inject(MAT_DIALOG_DATA) as TamanoJaula | null;
  dialogRef = inject(MatDialogRef);
  fb = inject(FormBuilder);
  servicio = inject(TamanoJaulaService);
  snackBar = inject(MatSnackBar);

  saving = signal(false);

  form: FormGroup = this.fb.group({
    nombre: [this.data?.nombre || '', Validators.required],
    activo: [this.data?.activo !== false]
  });

  guardar() {
    if (this.form.invalid) return;
    this.saving.set(true);

    const payload = this.form.value;

    const request = this.data?.id 
      ? this.servicio.actualizar(this.data.id, payload)
      : this.servicio.crear(payload);

    request.subscribe({
      next: (res) => {
        this.snackBar.open('Tamaño guardado', 'Cerrar', { duration: 3000 });
        this.dialogRef.close(true);
      },
      error: () => {
        this.snackBar.open('Error al guardar', 'Cerrar', { duration: 3000 });
        this.saving.set(false);
      }
    });
  }
}

@Component({
  selector: 'app-tamano-jaula',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    MatSnackBarModule
  ],
  templateUrl: './tamano-jaula.component.html',
  styleUrls: ['./tamano-jaula.component.css']
})
export class TamanoJaulaComponent implements OnInit {
  displayedColumns: string[] = ['nombre', 'estado', 'acciones'];
  dataSource = signal<TamanoJaula[]>([]);
  loading = signal(true);

  private servicio = inject(TamanoJaulaService);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);

  ngOnInit() {
    this.cargarDatos();
  }

  cargarDatos() {
    this.loading.set(true);
    this.servicio.listar().subscribe({
      next: (data) => {
        this.dataSource.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      }
    });
  }

  eliminar(id: number) {
    const dialogRef = this.dialog.open(ModalConfirmacionComponent, {
      width: '400px',
      data: {
        title: 'Eliminar Tamaño',
        message: '¿Está seguro de eliminar este tamaño? Solo se podrá si no hay jaulas asignadas.',
        confirmText: 'Eliminar',
        isDestructive: true
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.servicio.eliminar(id).subscribe({
          next: () => {
            this.snackBar.open('Tamaño eliminado', 'Cerrar', { duration: 3000 });
            this.cargarDatos();
          },
          error: (err) => {
            this.snackBar.open(err?.error?.mensaje || 'No se puede eliminar el tamaño porque está en uso.', 'Cerrar', { duration: 5000 });
          }
        });
      }
    });
  }

  abrirModal(tamano?: TamanoJaula) {
    const dialogRef = this.dialog.open(TamanoJaulaDialogComponent, {
      width: '400px',
      data: tamano
    });

    dialogRef.afterClosed().subscribe(res => {
      if (res) this.cargarDatos();
    });
  }
}

