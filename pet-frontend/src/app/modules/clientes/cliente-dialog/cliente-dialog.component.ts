import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { CustomValidators } from '../../../shared/utils/custom-validators';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { ClienteRequest, ClienteResponse } from '../../../core/models/models';
import { ExternoService } from '../../../core/services/externo.service';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../../core/services/auth.service';
@Component({
  selector: 'app-cliente-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatInputModule,
    MatFormFieldModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule
  ],
  templateUrl: './cliente-dialog.component.html',
  styleUrls: ['./cliente-dialog.component.css'],
})
export class ClienteDialogComponent implements OnInit {
  form!: FormGroup;
  isEdit = false;
  isSubmitting = false;
  buscandoDni = false;
  pasoActual = 1;
  datosResumen: any = null

  constructor(
    private fb: FormBuilder,
    private externoService: ExternoService,
    private snack: MatSnackBar,
    private authService: AuthService,
    public dialogRef: MatDialogRef<ClienteDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ClienteResponse | null
  ) {
    this.isEdit = !!data;
  }

  ngOnInit(): void {
    this.form = this.fb.group({
      nombre: [{ value: this.data?.nombre || '', disabled: this.isEdit }, [Validators.required, CustomValidators.noWhitespace]],
      apellido: [{ value: this.data?.apellido || '', disabled: this.isEdit }, [Validators.required, CustomValidators.noWhitespace]],
      dni: [{ value: this.data?.dni || '', disabled: this.isEdit }, [Validators.required, CustomValidators.dni]],
      telefono: [this.data?.telefono || '', [Validators.required, CustomValidators.telefono]],
      email: [{ value: this.data?.email || '', disabled: this.isEdit && !!this.data?.email }, [Validators.email]],
      direccion: [this.data?.direccion || '']
    });

    // Limpiar datos RENIEC si el DNI se edita (< 8 dígitos)
    if (!this.isEdit) {
      this.form.get('dni')?.valueChanges.subscribe((val: string) => {
        if ((val || '').toString().trim().length < 8) {
          this.form.get('nombre')?.enable();
          this.form.get('apellido')?.enable();
          this.form.patchValue({ nombre: '', apellido: '' });
        }
      });
    }
  }

  buscarDni() {
    const dni = this.form.get('dni')?.value;
    if (!dni || dni.length !== 8) {
      this.snack.open('Por favor ingresa un DNI válido de 8 dígitos', 'Cerrar', { duration: 3000 });
      return;
    }

    this.buscandoDni = true;
    this.externoService.consultarDni(dni).subscribe({
      next: (res) => {
        if (res && res.first_name) {
          this.form.patchValue({
            nombre: res.first_name,
            apellido: `${res.first_last_name} ${res.second_last_name}`.trim()
          });
          
          if (res.existe_en_bd) {
            if (res.email) this.form.patchValue({ email: res.email });
            if (res.telefono) this.form.patchValue({ telefono: res.telefono });
            
            if (res.cliente_id) {
               this.snack.open('Este DNI ya pertenece a un cliente registrado.', 'Entendido', { duration: 4000 });
               this.buscandoDni = false;
               return; 
            } else {
               // Es empleado (o usuario) sin rol de cliente
               if (this.authService.hasRole('ROLE_ADMIN')) {
                  const confirmar = window.confirm('Este DNI pertenece a un empleado activo en el sistema. ¿Desea asignarle el rol de Cliente?');
                  if (!confirmar) {
                     this.form.patchValue({ dni: '', nombre: '', apellido: '', email: '', telefono: '' });
                     this.form.get('nombre')?.enable();
                     this.form.get('apellido')?.enable();
                     this.buscandoDni = false;
                     return;
                  } else {
                     this.form.get('nombre')?.disable();
                     this.form.get('apellido')?.disable();
                     this.form.get('email')?.disable();
                     this.form.get('telefono')?.disable();
                     this.snack.open('Datos del empleado cargados. Guarde para asignarle el rol Cliente.', 'Entendido', { duration: 5000 });
                  }
               } else {
                  this.snack.open('Este DNI pertenece a un empleado. Solo un Administrador puede asignarle el rol de Cliente.', 'Entendido', { duration: 5000 });
                  this.form.patchValue({ dni: '', nombre: '', apellido: '', email: '', telefono: '' });
                  this.form.get('nombre')?.enable();
                  this.form.get('apellido')?.enable();
                  this.buscandoDni = false;
                  return;
               }
            }
          } else {
            this.form.get('nombre')?.disable();
            this.form.get('apellido')?.disable();
            this.snack.open('DNI encontrado exitosamente', 'Cerrar', { duration: 3000 });
          }
        }
        this.buscandoDni = false;
      },
      error: (err) => {
        this.buscandoDni = false;
        this.snack.open('No se pudo encontrar información para este documento', 'Cerrar', { duration: 4000 });
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

  soloNumeros(event: KeyboardEvent): void {
    const teclas_permitidas = ['Backspace', 'Delete', 'ArrowLeft', 'ArrowRight', 'Tab', 'Home', 'End'];
    // Permitir atajos standard con Ctrl o Meta (Cmd en Mac)
    if (event.ctrlKey || event.metaKey) {
      const shortcuts = ['a', 'c', 'v', 'x', 'z', 'A', 'C', 'V', 'X', 'Z'];
      if (shortcuts.includes(event.key)) {
        return;
      }
    }
    const patron = /^[0-9]$/;
    if (!teclas_permitidas.includes(event.key) && !patron.test(event.key)) {
      event.preventDefault();
    }
  }

  // Si estamos editando y el email / dni se usa para login, a veces no se debería poder editar, 
  // pero lo dejamos habilitado a menos que el backend lo restrinja.

  irAResumen(): void {
    if (this.form.valid) {
      this.datosResumen = this.form.getRawValue();
      this.pasoActual = 2;
    }
  }

  regresarAEditar(): void {
    this.pasoActual = 1;
  }

  confirmarYGuardar(): void {
    this.dialogRef.close(this.datosResumen);
  }

  onCancel(): void {
    this.dialogRef.close();
  }
}