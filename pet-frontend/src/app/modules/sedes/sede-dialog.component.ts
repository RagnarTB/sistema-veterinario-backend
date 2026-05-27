import { Component, Inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

import { SedeService } from '../../core/services/sede.service';
import { SedeResponse } from '../../core/models/models';

export interface SedeDialogData {
  sede?: SedeResponse;
}

@Component({
  selector: 'app-sede-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule
  ],
  templateUrl: './sede-dialog.component.html',
  styleUrls: ['./sede-dialog.component.css']
})
export class SedeDialogComponent implements OnInit {
  isEdit = signal(false);
  loading = signal(false);
  form: FormGroup;

  constructor(
    private dialogRef: MatDialogRef<SedeDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: SedeDialogData,
    private fb: FormBuilder,
    private sedeService: SedeService,
    private snack: MatSnackBar
  ) {
    this.isEdit.set(!!data?.sede);
    
    this.form = this.fb.group({
      nombre: [data?.sede?.nombre || '', Validators.required],
      direccion: [data?.sede?.direccion || '', Validators.required],
      telefono: [data?.sede?.telefono || '', Validators.required]
    });
  }

  ngOnInit() {}

  soloNumeros(event: KeyboardEvent): void {
    const teclas_permitidas = ['Backspace', 'Delete', 'ArrowLeft', 'ArrowRight', 'Tab', 'Home', 'End'];
    const patron = /^[0-9]$/;
    if (!teclas_permitidas.includes(event.key) && !patron.test(event.key)) {
      event.preventDefault();
    }
  }

  guardar() {
    if (this.form.invalid) return;

    this.loading.set(true);
    const dto = this.form.getRawValue();

    const obs = this.isEdit()
      ? this.sedeService.actualizar(this.data.sede!.id, dto)
      : this.sedeService.crear(dto);

    obs.subscribe({
      next: (res: any) => {
        this.snack.open(this.isEdit() ? 'Sede actualizada' : 'Sede registrada', 'Cerrar', { duration: 3000 });
        this.dialogRef.close(res);
      },
      error: (err: any) => {
        this.snack.open(err.error?.mensaje || 'Error al guardar', 'Cerrar', { duration: 3000 });
        this.loading.set(false);
      }
    });
  }
}