import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-cambiar-password-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="card p-4">
      <h3 class="font-bold mb-4">Cambiar Contraseña</h3>
      <div class="form-group mb-3">
        <label class="form-label text-xs">Contraseña Actual</label>
        <input type="password" class="form-control" [(ngModel)]="passwordActual" placeholder="Actual...">
      </div>
      <div class="form-group mb-3">
        <label class="form-label text-xs">Nueva Contraseña</label>
        <input type="password" class="form-control" [(ngModel)]="passwordNueva" placeholder="Min 6 caracteres...">
      </div>
      <div class="flex justify-end gap-sm mt-4">
        <button class="btn btn-secondary" (click)="dialogRef.close()">Cancelar</button>
        <button class="btn btn-primary" (click)="guardar()" [disabled]="guardando || !passwordActual || passwordNueva.length < 6">
          {{ guardando ? 'Guardando...' : 'Cambiar' }}
        </button>
      </div>
    </div>
  `
})
export class CambiarPasswordDialogComponent {
  dialogRef = inject(MatDialogRef);
  http = inject(HttpClient);
  snack = inject(MatSnackBar);

  passwordActual = '';
  passwordNueva = '';
  guardando = false;

  guardar() {
    this.guardando = true;
    this.http.post(`${environment.apiUrl}/auth/cambiar-password`, {
      passwordActual: this.passwordActual,
      passwordNueva: this.passwordNueva
    }).subscribe({
      next: () => {
        this.snack.open('Contraseña cambiada exitosamente', 'Cerrar', { duration: 3000 });
        this.dialogRef.close(true);
      },
      error: (err) => {
        this.snack.open(err.error?.mensaje || 'Error al cambiar contraseña', 'Cerrar', { duration: 3000, panelClass: ['snack-error'] });
        this.guardando = false;
      }
    });
  }
}
