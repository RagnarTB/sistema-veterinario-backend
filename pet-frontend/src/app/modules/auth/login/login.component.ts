import { Component, signal, OnInit, NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
} from '@angular/forms';
import { Router } from '@angular/router';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../core/services/auth.service';
import { RolNombre } from '../../../core/models/models';
import { environment } from '../../../../environments/environment';

declare var google: any;

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
})
export class LoginComponent implements OnInit {
  step = signal(1); // 1 = Login, 2 = Select Role, 3 = Register Email
  loading = signal(false);
  showPass = signal(false);
  errorMsg = signal('');
  availableRoles = signal<RolNombre[]>([]);

  form: FormGroup;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private snack: MatSnackBar,
    private ngZone: NgZone
  ) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required],
    });
  }

  setStep(newStep: number) {
    this.step.set(newStep);
    if (newStep === 1 || newStep === 3) {
      setTimeout(() => this.renderGoogleButton(), 100);
    }
  }

  ngOnInit() {
    if (this.authService.isAuthenticated()) {
      const activeRole = this.authService.activeRole();
      if (activeRole === 'ROLE_CLIENTE') {
        this.router.navigate(['/app/panel-cliente']);
      } else {
        this.router.navigate(['/app/citas']);
      }
      return;
    }

    if (typeof google === 'undefined' || !google.accounts) {
      const script = document.createElement('script');
      script.src = 'https://accounts.google.com/gsi/client';
      script.async = true;
      script.defer = true;
      script.onload = () => {
        this.renderGoogleButton();
      };
      document.head.appendChild(script);
    } else {
      this.renderGoogleButton();
    }
  }

  renderGoogleButton(retries = 0) {
    if (typeof google !== 'undefined' && google.accounts) {
      if (!(window as any).googleGsiInitialized) {
        google.accounts.id.initialize({
          client_id: environment.googleClientId,
          callback: (response: any) => this.ngZone.run(() => this.handleGoogleCredential(response)),
          auto_select: false,
          cancel_on_tap_outside: true
        });
        (window as any).googleGsiInitialized = true;
      }

      let rendered = false;
      const btn1 = document.getElementById('googleButton');
      if (btn1) {
        google.accounts.id.renderButton(btn1, { theme: 'outline', size: 'large', width: 350 });
        rendered = true;
      }

      if (!rendered && retries < 10) {
        setTimeout(() => this.renderGoogleButton(retries + 1), 150);
      }
    } else {
      if (retries < 20) {
        setTimeout(() => this.renderGoogleButton(retries + 1), 500);
      }
    }
  }

  handleGoogleCredential(response: any) {
    this.loading.set(true);
    this.authService.loginConGoogle(response.credential).subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res.requireRegistration) {
          // Navegar a /completar-registro con los datos
          this.router.navigate(['/completar-registro'], {
            state: { data: res }
          });
        } else {
          // Login exitoso
          const roles = res.roles as RolNombre[];
          if (res.requiresRoleSelection && roles.length > 1) {
            this.availableRoles.set(roles.filter(r => r !== 'ROLE_PRE_AUTH'));
            this.step.set(2);
          } else {
            // Find actual role starting with ROLE_ to navigate correctly
            const activeRole = roles.find(r => r.startsWith('ROLE_')) || roles[0];
            this.navigateToDashboard(activeRole);
          }
        }
      },
      error: (err) => {
        this.loading.set(false);
        this.snack.open(err.error?.mensaje || err.error?.message || 'Error al iniciar sesión con Google', 'Cerrar', { duration: 3000 });
      }
    });
  }

  solicitarRegistro(email: string) {
    if (!email) return;
    this.loading.set(true);
    this.authService.solicitarRegistroCorreo(email).subscribe({
      next: (res) => {
        this.loading.set(false);
        this.setStep(1);
        this.snack.open(res.mensaje || 'Enlace enviado a tu correo', 'Cerrar', { duration: 5000 });
      },
      error: (err) => {
        this.loading.set(false);
        this.snack.open(err.error?.message || err.error?.mensaje || 'Error al solicitar registro', 'Cerrar', { duration: 3000 });
      }
    });
  }

  solicitarRecuperacion(email: string) {
    if (!email) return;
    this.loading.set(true);
    this.authService.solicitarResetPassword(email).subscribe({
      next: (res) => {
        this.loading.set(false);
        this.setStep(1);
        this.snack.open(res.mensaje || 'Enlace enviado a tu correo', 'Cerrar', { duration: 5000 });
      },
      error: (err) => {
        this.loading.set(false);
        this.snack.open(err.error?.message || err.error?.mensaje || 'Error al solicitar recuperación', 'Cerrar', { duration: 3000 });
      }
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

    this.authService.login(this.form.value).subscribe({
      next: (res) => {
        this.loading.set(false);
        const roles = res.roles as RolNombre[];
        if (res.requiresRoleSelection && roles.length > 1) {
          this.availableRoles.set(roles.filter(r => r !== 'ROLE_PRE_AUTH'));
          this.step.set(2);
        } else {
          const activeRole = roles.find(r => r.startsWith('ROLE_')) || roles[0];
          this.navigateToDashboard(activeRole);
        }
      },
      error: (err) => {
        this.loading.set(false);
        const msg =
          err?.error?.mensaje || err?.error?.message ||
          'Credenciales incorrectas. Verifica tu email y contraseña.';
        this.errorMsg.set(msg);
      },
    });
  }

  selectRole(rol: RolNombre): void {
    this.loading.set(true);
    this.authService.seleccionarRol(rol).subscribe({
      next: () => {
        this.loading.set(false);
        this.authService.setActiveRole(rol);
        this.navigateToDashboard(rol);
      },
      error: (err) => {
        this.loading.set(false);
        this.snack.open(err.error?.mensaje || 'Error al seleccionar rol', 'Cerrar', { duration: 3000 });
      }
    });
  }

  private navigateToDashboard(rol?: RolNombre): void {
    if (rol === 'ROLE_CLIENTE') {
      this.router.navigate(['/app/panel-cliente']);
    } else {
      // Send to /app to let the HomeRedirectComponent handle role-specific default routes
      this.router.navigate(['/app']);
    }
  }

  getRoleName(rol: string): string {
    const names: Record<string, string> = {
      'ROLE_ADMIN': 'Administrador',
      'ROLE_CLIENTE': 'Cliente',
      'ROLE_VETERINARIO': 'Veterinario',
      'ROLE_RECEPCIONISTA': 'Recepcionista'
    };
    if (names[rol]) return names[rol];
    const name = rol.replace('ROLE_', '').toLowerCase();
    return name.charAt(0).toUpperCase() + name.slice(1);
  }

  getRoleIcon(rol: string): string {
    const icons: Record<string, string> = {
      'ROLE_ADMIN': 'admin_panel_settings',
      'ROLE_CLIENTE': 'person',
      'ROLE_VETERINARIO': 'medical_services',
      'ROLE_RECEPCIONISTA': 'support_agent'
    };
    return icons[rol] || 'badge';
  }
}
