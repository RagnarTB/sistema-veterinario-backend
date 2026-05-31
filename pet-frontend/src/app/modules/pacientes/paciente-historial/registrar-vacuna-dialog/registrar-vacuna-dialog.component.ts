import { Component, Inject, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { VacunaService } from '../../../../core/services/vacuna.service';
import { VacunaRequest } from '../../../../core/models/models';

@Component({
  selector: 'app-registrar-vacuna-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './registrar-vacuna-dialog.component.html'
})
export class RegistrarVacunaDialogComponent {
  private fb = inject(FormBuilder);
  private vacunaService = inject(VacunaService);
  private snack = inject(MatSnackBar);
  private dialogRef = inject(MatDialogRef<RegistrarVacunaDialogComponent>);

  form: FormGroup;
  loading = signal(false);
  pacienteId: number;
  today = new Date();

  constructor(@Inject(MAT_DIALOG_DATA) public data: { pacienteId: number }) {
    this.pacienteId = data.pacienteId;
    this.form = this.fb.group({
      nombreVacuna: ['', [Validators.required]],
      fechaAplicacion: [new Date(), [Validators.required]],
      fechaProximaDosis: [null],
      observaciones: ['']
    });
  }

  guardar() {
    if (this.form.invalid) return;

    this.loading.set(true);
    const val = this.form.value;

    const request: VacunaRequest = {
      nombreVacuna: val.nombreVacuna,
      fechaAplicacion: this.formatDate(val.fechaAplicacion),
      fechaProximaDosis: val.fechaProximaDosis ? this.formatDate(val.fechaProximaDosis) : undefined,
      observaciones: val.observaciones,
      pacienteId: this.pacienteId
    };

    this.vacunaService.registrarVacuna(request).subscribe({
      next: () => {
        this.snack.open('Vacuna registrada correctamente', 'Cerrar', { duration: 3000 });
        this.loading.set(false);
        this.dialogRef.close(true);
      },
      error: (err) => {
        this.snack.open(err.error?.mensaje || 'Error al registrar la vacuna', 'Cerrar', { duration: 4000 });
        this.loading.set(false);
      }
    });
  }

  private formatDate(date: Date): string {
    const d = new Date(date);
    let month = '' + (d.getMonth() + 1);
    let day = '' + d.getDate();
    const year = d.getFullYear();

    if (month.length < 2) month = '0' + month;
    if (day.length < 2) day = '0' + day;

    return [year, month, day].join('-');
  }
}
