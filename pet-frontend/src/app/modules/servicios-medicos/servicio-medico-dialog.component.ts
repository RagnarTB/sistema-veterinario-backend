import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { ServicioMedicoResponse, TipoServicio } from '../../core/models/models';

@Component({
  selector: 'app-servicio-medico-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule
  ],
  templateUrl: './servicio-medico-dialog.component.html',
  styleUrls: ['./servicio-medico-dialog.component.css']
})
export class ServicioMedicoDialogComponent implements OnInit {
  form: FormGroup;
  tipos: TipoServicio[] = ['CONSULTA', 'VACUNACION', 'CIRUGIA', 'HOSPITALIZACION', 'ESTETICA', 'EXAMEN', 'OTRO'];

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<ServicioMedicoDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ServicioMedicoResponse | null
  ) {
    this.form = this.fb.group({
      nombre: ['', [Validators.required]],
      descripcion: [''],
      tipoServicio: ['CONSULTA', [Validators.required]],
      precio: [0, [Validators.required, Validators.min(0)]],
      duracionMinutos: [30, [Validators.required, Validators.min(1)]],
      bufferMinutos: [0, [Validators.required, Validators.min(0)]]
    });
  }

  ngOnInit(): void {
    if (this.data) {
      this.form.patchValue(this.data);
    }
  }

  onSubmit(): void {
    if (this.form.valid) {
      this.dialogRef.close(this.form.value);
    }
  }

  onCancel(): void {
    if (this.dialogRef.close) {
      this.dialogRef.close();
    }
  }
}