import { Routes } from '@angular/router';

export const PANEL_CLIENTE_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./dashboard-cliente.component').then(
        (m) => m.DashboardClienteComponent
      ),
  },
  {
    path: 'mascotas',
    loadChildren: () =>
      import('../pacientes/pacientes.routes').then((m) => m.PACIENTES_ROUTES),
  },
  {
    path: 'citas',
    loadChildren: () =>
      import('../citas/citas.routes').then((m) => m.CITAS_ROUTES),
  }
];
