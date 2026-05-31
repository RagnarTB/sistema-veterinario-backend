import { Component, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    RouterModule
  ],
  templateUrl: './reset-password.component.html',
  styleUrls: ['../login/login.component.css'],
})
export class ResetPasswordComponent implements OnInit {
  loading = signal(false);
  successMsg = signal('');
  errorMsg = signal('');
  token = signal<string | null>(null);

  showPass1 = signal(false);
  showPass2 = signal(false);

  form: FormGroup;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private snack: MatSnackBar
  ) {
    this.form = this.fb.group({
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', Validators.required],
    }, { validators: this.passwordMatchValidator });
  }

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      if (params['token']) {
        this.token.set(params['token']);
      } else {
        this.errorMsg.set('Enlace inválido. No se encontró el token de seguridad.');
      }
    });
  }

  passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const p1 = control.get('password')?.value;
    const p2 = control.get('confirmPassword')?.value;
    if (p1 !== p2) {
      control.get('confirmPassword')?.setErrors({ mismatch: true });
      return { mismatch: true };
    } else {
      const currentErrors = control.get('confirmPassword')?.errors;
      if (currentErrors) {
        delete currentErrors['mismatch'];
        if (Object.keys(currentErrors).length === 0) {
          control.get('confirmPassword')?.setErrors(null);
        } else {
          control.get('confirmPassword')?.setErrors(currentErrors);
        }
      }
      return null;
    }
  }

  get f() {
    return this.form.controls;
  }

  onSubmit(): void {
    if (!this.token()) {
      this.errorMsg.set('Enlace inválido. Vuelve a solicitar el cambio de contraseña.');
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorMsg.set('');
    this.successMsg.set('');

    const newPassword = this.form.value.password;

    this.authService.resetearPassword(this.token()!, newPassword).subscribe({
      next: (res) => {
        this.loading.set(false);
        this.successMsg.set(res.mensaje || 'Contraseña actualizada correctamente.');
        this.snack.open('Contraseña actualizada correctamente', 'Cerrar', { duration: 5000 });
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 2000);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMsg.set(err.error?.mensaje || 'Error al cambiar la contraseña. Es posible que el enlace haya expirado.');
      },
    });
  }
}
