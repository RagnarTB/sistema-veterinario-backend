import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';

@Component({
  selector: 'app-delete-motivo-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, MatDialogModule, MatButtonModule, MatInputModule, MatFormFieldModule],
  template: `
    <h2 mat-dialog-title>Motivo de Eliminación</h2>
    <mat-dialog-content>
      <p class="mb-4 text-sm text-gray-600">Por favor, ingrese el motivo por el cual se eliminará este registro. Esta acción quedará grabada en el Kardex de Auditoría.</p>
      <mat-form-field appearance="outline" class="w-full">
        <mat-label>Motivo</mat-label>
        <textarea matInput [(ngModel)]="motivo" rows="3" placeholder="Ej. Registro duplicado, error de tipeo..."></textarea>
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button mat-dialog-close>Cancelar</button>
      <button mat-flat-button color="warn" [disabled]="!motivo.trim()" (click)="confirmar()">Eliminar</button>
    </mat-dialog-actions>
  `
})
export class DeleteMotivoDialogComponent {
  motivo = '';

  constructor(public dialogRef: MatDialogRef<DeleteMotivoDialogComponent>) {}

  confirmar() {
    if (this.motivo.trim()) {
      this.dialogRef.close(this.motivo.trim());
    }
  }
}
