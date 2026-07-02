import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule, MAT_DATE_LOCALE } from '@angular/material/core';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-edit-fecha-dialog',
  standalone: true,
  imports: [
    CommonModule, 
    FormsModule, 
    MatDialogModule, 
    MatButtonModule, 
    MatInputModule, 
    MatFormFieldModule, 
    MatDatepickerModule,
    MatNativeDateModule,
    MatIconModule
  ],
  providers: [
    { provide: MAT_DATE_LOCALE, useValue: 'es-ES' }
  ],
  template: `
    <h2 mat-dialog-title>Editar Próxima Dosis</h2>
    <mat-dialog-content>
      <p class="mb-4 text-sm text-gray-600">Seleccione la nueva fecha para la próxima dosis. Este cambio quedará registrado en el Kardex.</p>
      <mat-form-field appearance="outline" class="w-full">
        <mat-label>Fecha de Próxima Dosis</mat-label>
        <mat-icon matPrefix class="cursor-pointer mr-2" (click)="picker.open()">event</mat-icon>
        <input matInput [matDatepicker]="picker" [(ngModel)]="fecha" readonly (click)="picker.open()">
        <mat-datepicker-toggle matSuffix [for]="picker"></mat-datepicker-toggle>
        <mat-datepicker #picker></mat-datepicker>
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button mat-dialog-close>Cancelar</button>
      <button mat-flat-button color="primary" [disabled]="!fecha" (click)="confirmar()">Guardar Cambios</button>
    </mat-dialog-actions>
  `
})
export class EditFechaDialogComponent {
  fecha: Date | null = null;

  constructor(
    public dialogRef: MatDialogRef<EditFechaDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { fechaActual: string }
  ) {
    if (data.fechaActual) {
      this.fecha = new Date(data.fechaActual);
    }
  }

  confirmar() {
    if (this.fecha) {
      // Devolver string formato YYYY-MM-DD
      const year = this.fecha.getFullYear();
      const month = String(this.fecha.getMonth() + 1).padStart(2, '0');
      const day = String(this.fecha.getDate()).padStart(2, '0');
      this.dialogRef.close(`${year}-${month}-${day}`);
    }
  }
}
