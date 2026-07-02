import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { RolNombre } from '../models/models';

/**
 * Guard de rol o permiso. Uso:
 *   canActivate: [roleGuard(['VER_CITAS', 'ROLE_CLIENTE'])]
 */
export function roleGuard(permisos: string[]): CanActivateFn {
  return () => {
    const authService = inject(AuthService);
    const router = inject(Router);

    if (!authService.isAuthenticated()) {
      router.navigate(['/login']);
      return false;
    }

    if (permisos.some(p => authService.hasPermission(p))) {
      return true;
    }

    // Autenticado pero sin el permiso requerido → página 403
    router.navigate(['/app/forbidden']);
    return false;
  };
}
