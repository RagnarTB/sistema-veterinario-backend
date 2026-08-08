import { Component, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { CustomValidators } from '../../../shared/utils/custom-validators';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../core/services/auth.service';
import { ExternoService, ReniecResponse } from '../../../core/services/externo.service';

@Component({
  selector: 'app-completar-registro',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatSnackBarModule, MatProgressSpinnerModule],
  templateUrl: './completar-registro.component.html',
  styleUrls: ['./completar-registro.component.css'],
})
export class CompletarRegistroComponent implements OnInit {
  form: FormGroup;
  loading = signal(false);
  loadingReniec = signal(false);
  isGoogleFlow = signal(false);
  token: string = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private externoService: ExternoService,
    private router: Router,
    private route: ActivatedRoute,
    private snack: MatSnackBar
  ) {
    this.form = this.fb.group({
      dni: ['', [Validators.required, CustomValidators.dni]],
      nombre: ['', Validators.required,],
      apellido: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      telefono: ['', Validators.required],
      password: ['']
    });
  }

  ngOnInit() {
    // Verificar de dónde venimos
    const state = history.state.data;
    if (state && state.googleToken) {
      this.isGoogleFlow.set(true);
      this.token = state.googleToken;
      this.form.patchValue({
        email: state.email,
        nombre: state.nombre || '',
        apellido: state.apellido || ''
      });
      // La contraseña no es obligatoria para Google
      this.form.get('password')?.clearValidators();
      this.form.get('password')?.updateValueAndValidity();
    } else {
      // Venimos de enlace de correo
      this.route.queryParams.subscribe(params => {
        if (params['token']) {
          this.token = params['token'];
          if (params['email']) {
            this.form.patchValue({ email: params['email'] });
          }
          // Para correo la password es requerida
          this.form.get('password')?.setValidators([Validators.required, Validators.minLength(8)]);
          this.form.get('password')?.updateValueAndValidity();
        } else {
          this.snack.open('Token inválido o no encontrado', 'Cerrar');
          this.router.navigate(['/login']);
        }
      });
    }

    // Limpiar datos RENIEC si el DNI se borra o edita (< 8 dígitos)
    this.form.get('dni')?.valueChanges.subscribe((val: string) => {
      if ((val || '').toString().trim().length < 8) {
        this.form.get('nombre')?.enable();
        this.form.patchValue({ nombre: '', apellido: '' });
      }
    });
  }

  get f() { return this.form.controls; }

  buscarDni() {
    const dni = this.form.get('dni')?.value;
    if (!dni || dni.length !== 8) return;

    this.loadingReniec.set(true);
    this.externoService.consultarDni(dni).subscribe({
      next: (res: ReniecResponse) => {
        this.loadingReniec.set(false);
        this.form.patchValue({
          nombre: res.first_name,
          apellido: res.first_last_name + ' ' + res.second_last_name
        });
        this.form.get('nombre')?.disable();
        this.form.get('apellido')?.disable();
      },
      error: () => {
        this.loadingReniec.set(false);
        this.snack.open('No se encontró el DNI. Ingrese los nombres manualmente si puede o intente de nuevo.', 'Cerrar', { duration: 4000 });
        // Permitir editar manualmente en caso de error
        this.form.get('nombre')?.enable();
        this.form.get('apellido')?.enable();
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
    const patron = /^[0-9]$/;
    if (!teclas_permitidas.includes(event.key) && !patron.test(event.key)) {
      event.preventDefault();
    }
  }

  onSubmit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    const dto = {
      ...this.form.getRawValue(),
      token: this.token
    };

    this.authService.completarRegistro(dto).subscribe({
      next: (res) => {
        this.loading.set(false);
        if (this.isGoogleFlow()) {
          this.snack.open('Registro exitoso. ¡Bienvenido!', 'Cerrar', { duration: 3000 });
          this.router.navigate(['/app']);
        } else {
          const email = this.form.get('email')?.value;
          this.snack.open('Registro exitoso. Por favor, inicia sesión con tu nueva contraseña.', 'Cerrar', { duration: 5000 });
          this.authService.limpiarSesion(false);
          this.router.navigate(['/login'], { state: { email } });
        }
      },
      error: (err) => {
        this.loading.set(false);
        this.snack.open(err.error?.mensaje || 'Error al completar el registro', 'Cerrar', { duration: 4000 });
      }
    });
  }


}
