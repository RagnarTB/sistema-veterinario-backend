import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  //Ruta raiz 
  {
    path: '',
    redirectTo: 'app',
    pathMatch: 'full',
  },
  {
    path: 'auth/confirmar',
    redirectTo: 'confirmar',
    pathMatch: 'full',
  },

  //Auth Layout (sin sidebar) 
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
    ],
  },

  // â”€â”€â”€ Main Layout (con sidebar + header) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
  {
    path: 'app',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./layouts/main-layout/main-layout.component').then(
        (m) => m.MainLayoutComponent
      ),
    children: [
      // Dashboard â€” solo ADMIN
      {
        path: 'dashboard',
        canActivate: [roleGuard(['VER_REPORTES'])],
        loadChildren: () =>
          import('./modules/dashboard/dashboard.routes').then(
            (m) => m.DASHBOARD_ROUTES
          ),
      },

      // Reportes
      {
        path: 'reportes',
        canActivate: [roleGuard(['VER_REPORTES'])],
        loadComponent: () =>
          import('./modules/reportes/reportes.component').then(
            (m) => m.ReportesComponent
          ),
      },

      // Citas — todos los empleados
      {
        path: 'citas',
        canActivate: [roleGuard(['VER_CITAS', 'VER_PACIENTES'])],
        loadChildren: () =>
          import('./modules/citas/citas.routes').then((m) => m.CITAS_ROUTES),
      },

      // Clientes — ADMIN y RECEPCIONISTA
      {
        path: 'clientes',
        canActivate: [roleGuard(['VER_CLIENTES'])],
        loadChildren: () =>
          import('./modules/clientes/clientes.routes').then(
            (m) => m.CLIENTES_ROUTES
          ),
      },

      // Pacientes — todos los empleados
      {
        path: 'pacientes',
        canActivate: [roleGuard(['VER_CITAS', 'VER_PACIENTES'])],
        loadChildren: () =>
          import('./modules/pacientes/pacientes.routes').then(
            (m) => m.PACIENTES_ROUTES
          ),
      },

      // Atenciones medicas — VETERINARIO y ADMIN
      {
        path: 'atenciones',
        canActivate: [roleGuard(['VER_HISTORIAL', 'GESTIONAR_HOSPITALIZACION'])],
        loadChildren: () =>
          import('./modules/atenciones/atenciones.routes').then(
            (m) => m.ATENCIONES_ROUTES
          ),
      },

      // Hospitalización — VETERINARIO y ADMIN
      {
        path: 'hospitalizacion',
        canActivate: [roleGuard(['VER_HISTORIAL', 'GESTIONAR_HOSPITALIZACION'])],
        loadChildren: () =>
          import('./modules/hospitalizacion/hospitalizacion.routes').then(
            (m) => m.HOSPITALIZACION_ROUTES
          ),
      },

      // Jaulas
      {
        path: 'jaulas',
        canActivate: [roleGuard(['VER_HISTORIAL', 'GESTIONAR_HOSPITALIZACION'])],
        loadChildren: () => import('./modules/hospitalizacion/jaulas.routes').then(m => m.JAULAS_ROUTES),
      },

      // Farmacia (productos + ventas) — todos
      {
        path: 'farmacia',
        canActivate: [roleGuard(['VER_VENTAS', 'REALIZAR_VENTAS'])],
        loadChildren: () =>
          import('./modules/farmacia/farmacia.routes').then(
            (m) => m.FARMACIA_ROUTES
          ),
      },

      // Inventario
      {
        path: 'inventario',
        canActivate: [roleGuard(['VER_INVENTARIO'])],
        loadComponent: () =>
          import('./modules/farmacia/inventario.component').then(
            (m) => m.InventarioComponent
          ),
      },

      // Caja — ADMIN y RECEPCIONISTA
      {
        path: 'caja',
        canActivate: [roleGuard(['VER_CAJA'])],
        loadChildren: () =>
          import('./modules/caja/caja.routes').then((m) => m.CAJA_ROUTES),
      },

      // Finanzas / Cuentas por Cobrar — ADMIN y RECEPCIONISTA
      {
        path: 'finanzas/deudas',
        canActivate: [roleGuard(['VER_FINANZAS'])],
        loadComponent: () =>
          import('./modules/finanzas/deudas.component').then(
            (m) => m.DeudasComponent
          ),
      },

      // Finanzas / Historial de Ventas — ADMIN y RECEPCIONISTA
      {
        path: 'finanzas/ventas',
        canActivate: [roleGuard(['VER_VENTAS'])],
        loadComponent: () =>
          import('./modules/finanzas/ventas.component').then(
            (m) => m.VentasComponent
          ),
      },

      // Finanzas / Historial de Cajas — ADMIN y RECEPCIONISTA
      {
        path: 'finanzas/cajas-cerradas',
        canActivate: [roleGuard(['VER_CAJA'])],
        loadComponent: () =>
          import('./modules/finanzas/historial-cajas/historial-cajas').then(
            (m) => m.HistorialCajas
          ),
      },

      // Empleados — solo ADMIN
      {
        path: 'empleados',
        canActivate: [roleGuard(['GESTIONAR_EMPLEADOS'])],
        loadChildren: () =>
          import('./modules/empleados/empleados.routes').then(
            (m) => m.EMPLEADOS_ROUTES
          ),
      },

      // Sedes — solo ADMIN
      {
        path: 'sedes',
        canActivate: [roleGuard(['GESTIONAR_CONFIGURACION'])],
        loadChildren: () =>
          import('./modules/sedes/sedes.routes').then((m) => m.SEDES_ROUTES),
      },

      // Servicios Médicos — solo ADMIN
      {
        path: 'servicios-medicos',
        canActivate: [roleGuard(['GESTIONAR_CATALOGO'])],
        loadComponent: () =>
          import('./modules/servicios-medicos/servicios-medicos.component').then(
            (m) => m.ServiciosMedicosComponent
          ),
      },

      // --- PANEL DE CLIENTE ---
      {
        path: 'panel-cliente',
        canActivate: [roleGuard(['ROLE_CLIENTE'])],
        loadChildren: () =>
          import('./modules/panel-cliente/panel-cliente.routes').then(
            (m) => m.PANEL_CLIENTE_ROUTES
          ),
      },

      // Redirección por defecto dentro de /app (dinámica según rol)
      {
        path: '',
        loadComponent: () =>
          import('./shared/components/home-redirect/home-redirect.component').then(
            (m) => m.HomeRedirectComponent
          ),
        pathMatch: 'full',
      },

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
  { path: '**', redirectTo: 'app' },
];

