import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-olvide-password',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    RouterModule
  ],
  templateUrl: './olvide-password.component.html',
  styleUrls: ['../login/login.component.css'],
})
export class OlvidePasswordComponent {
  loading = signal(false);
  successMsg = signal('');
  errorMsg = signal('');

  form: FormGroup;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private snack: MatSnackBar
  ) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
    });
  }

  get f() {
    return this.form.controls;
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorMsg.set('');
    this.successMsg.set('');

    const email = this.form.value.email;

    this.authService.solicitarRecuperacionPassword(email).subscribe({
      next: (res) => {
        this.loading.set(false);
        this.successMsg.set(res.mensaje || 'Se ha enviado un enlace de recuperación a tu correo.');
        this.form.reset();
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMsg.set(err.error?.mensaje || 'Error al procesar la solicitud.');
      },
    });
  }
}
