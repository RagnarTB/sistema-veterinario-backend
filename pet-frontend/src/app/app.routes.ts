import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'app/citas',
    pathMatch: 'full',
  },
  {
    path: 'auth/confirmar',
    redirectTo: 'confirmar',
    pathMatch: 'full',
  },

  {
    path: '',
    loadComponent: () =>
      import('./layouts/auth-layout/auth-layout.component').then(
        (m) => m.AuthLayoutComponent
      ),
    children: [
      {
        path: 'login',
        loadComponent: () =>
          import('./modules/auth/login/login.component').then(
            (m) => m.LoginComponent
          ),
      },
      {
        path: 'confirmar',
        loadComponent: () =>
          import('./modules/auth/confirmar-cuenta/confirmar-cuenta.component').then(
            (m) => m.ConfirmarCuentaComponent
          ),
      },
      {
        path: 'completar-registro',
        loadComponent: () =>
          import('./modules/auth/completar-registro/completar-registro.component').then(
            (m) => m.CompletarRegistroComponent
          ),
      },
      {
        path: 'olvide-password',
        loadComponent: () =>
          import('./modules/auth/olvide-password/olvide-password.component').then(
            (m) => m.OlvidePasswordComponent
          ),
      },
      {
        path: 'reset-password',
        loadComponent: () =>
          import('./modules/auth/reset-password/reset-password.component').then(
            (m) => m.ResetPasswordComponent
          ),
      },
    ],
  },


  {
    path: 'app',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./layouts/main-layout/main-layout.component').then(
        (m) => m.MainLayoutComponent
      ),
    children: [

      {
        path: 'dashboard',
        canActivate: [roleGuard(['ROLE_ADMIN'])],
        loadChildren: () =>
          import('./modules/dashboard/dashboard.routes').then(
            (m) => m.DASHBOARD_ROUTES
          ),
      },

     
      {
        path: 'reportes',
        canActivate: [roleGuard(['ROLE_ADMIN'])],
        loadComponent: () =>
          import('./modules/reportes/reportes.component').then(
            (m) => m.ReportesComponent
          ),
      },

    
      {
        path: 'citas',
        loadChildren: () =>
          import('./modules/citas/citas.routes').then((m) => m.CITAS_ROUTES),
      },

      // PÃ¡gina de usuarios
      {
        path: 'pagina-usuarios',
        loadComponent: () =>
          import('./modules/pagina-usuarios/pagina-usuario.component').then(
            (m) => m.PaginaUsuarioComponent
          ),
      },

      {
        path: 'mis-mascotas',
        canActivate: [roleGuard(['ROLE_CLIENTE'])],
        loadComponent: () =>
          import('./modules/mis-mascotas-cliente/mis-mascotas-cliente.component').then(
            (m) => m.MisMascotasClienteComponent
          ),
      },

      // Clientes â€” ADMIN y RECEPCIONISTA
      {
        path: 'clientes',
        canActivate: [roleGuard(['ROLE_ADMIN', 'ROLE_RECEPCIONISTA'])],
        loadChildren: () =>
          import('./modules/clientes/clientes.routes').then(
            (m) => m.CLIENTES_ROUTES
          ),
      },

      // Pacientes â€” todos los autenticados
      {
        path: 'pacientes',
        canActivate: [roleGuard(['ROLE_ADMIN', 'ROLE_RECEPCIONISTA', 'ROLE_VETERINARIO'])],
        loadChildren: () =>
          import('./modules/pacientes/pacientes.routes').then(
            (m) => m.PACIENTES_ROUTES
          ),
      },

      // Atenciones medicas â€” VETERINARIO y ADMIN
      {
        path: 'atenciones',
        canActivate: [roleGuard(['ROLE_ADMIN', 'ROLE_VETERINARIO'])],
        loadChildren: () =>
          import('./modules/atenciones/atenciones.routes').then(
            (m) => m.ATENCIONES_ROUTES
          ),
      },

      // HospitalizaciÃ³n â€” VETERINARIO y ADMIN
      {
        path: 'hospitalizacion',
        canActivate: [roleGuard(['ROLE_ADMIN', 'ROLE_VETERINARIO'])],
        loadChildren: () =>
          import('./modules/hospitalizacion/hospitalizacion.routes').then(
            (m) => m.HOSPITALIZACION_ROUTES
          ),
      },

      // Jaulas
      {
        path: 'jaulas',
        canActivate: [roleGuard(['ROLE_ADMIN', 'ROLE_VETERINARIO'])],
        loadChildren: () => import('./modules/hospitalizacion/jaulas.routes').then(m => m.JAULAS_ROUTES),
      },

      // Farmacia (productos + ventas) â€” todos
      {
        path: 'farmacia',
        loadChildren: () =>
          import('./modules/farmacia/farmacia.routes').then(
            (m) => m.FARMACIA_ROUTES
          ),
      },

      // Inventario
      {
        path: 'inventario',
        canActivate: [roleGuard(['ROLE_ADMIN', 'ROLE_RECEPCIONISTA'])],
        loadComponent: () =>
          import('./modules/farmacia/inventario.component').then(
            (m) => m.InventarioComponent
          ),
      },

      // Caja â€” ADMIN y RECEPCIONISTA
      {
        path: 'caja',
        canActivate: [roleGuard(['ROLE_ADMIN', 'ROLE_RECEPCIONISTA'])],
        loadChildren: () =>
          import('./modules/caja/caja.routes').then((m) => m.CAJA_ROUTES),
      },

      // Finanzas / Cuentas por Cobrar â€” ADMIN y RECEPCIONISTA
      {
        path: 'finanzas/deudas',
        canActivate: [roleGuard(['ROLE_ADMIN', 'ROLE_RECEPCIONISTA'])],
        loadComponent: () =>
          import('./modules/finanzas/deudas.component').then(
            (m) => m.DeudasComponent
          ),
      },

      // Finanzas / Historial de Ventas â€” ADMIN y RECEPCIONISTA
      {
        path: 'finanzas/ventas',
        canActivate: [roleGuard(['ROLE_ADMIN', 'ROLE_RECEPCIONISTA'])],
        loadComponent: () =>
          import('./modules/finanzas/ventas.component').then(
            (m) => m.VentasComponent
          ),
      },

      // Empleados â€” solo ADMIN
      {
        path: 'empleados',
        canActivate: [roleGuard(['ROLE_ADMIN'])],
        loadChildren: () =>
          import('./modules/empleados/empleados.routes').then(
            (m) => m.EMPLEADOS_ROUTES
          ),
      },

      // Sedes â€” solo ADMIN
      {
        path: 'sedes',
        canActivate: [roleGuard(['ROLE_ADMIN'])],
        loadChildren: () =>
          import('./modules/sedes/sedes.routes').then((m) => m.SEDES_ROUTES),
      },

      // Servicios MÃ©dicos â€” solo ADMIN
      {
        path: 'servicios-medicos',
        canActivate: [roleGuard(['ROLE_ADMIN'])],
        loadComponent: () =>
          import('./modules/servicios-medicos/servicios-medicos.component').then(
            (m) => m.ServiciosMedicosComponent
          ),
      },

      // RedirecciÃ³n por defecto dentro de /app
      { path: '', redirectTo: 'citas', pathMatch: 'full' },

      // 403 Forbidden
      {
        path: 'forbidden',
        loadComponent: () =>
          import('./shared/components/forbidden/forbidden.component').then(
            (m) => m.ForbiddenComponent
          ),
      },
    ],
  },

  // Wildcard
  { path: '**', redirectTo: 'app/citas' },
];
