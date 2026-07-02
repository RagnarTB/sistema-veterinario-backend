import { Component, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-home-redirect',
  standalone: true,
  template: '',
})
export class HomeRedirectComponent implements OnInit {
  private authService = inject(AuthService);
  private router = inject(Router);

  ngOnInit() {
    const rol = this.authService.activeRole();
    if (rol === 'ROLE_CLIENTE') {
      this.router.navigate(['/app/panel-cliente']);
    } else if (rol === 'ROLE_ADMIN') {
      this.router.navigate(['/app/dashboard']);
    } else {
      this.router.navigate(['/app/citas']);
    }
  }
}
