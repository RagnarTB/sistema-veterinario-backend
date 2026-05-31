import { Routes } from '@angular/router';
export const PACIENTES_ROUTES: Routes = [
  { path: '', loadComponent: () => import('./pacientes.component').then(m => m.PacientesComponent) },
  { path: ':id/historial', loadComponent: () => import('./paciente-historial/paciente-historial.component').then(m => m.PacienteHistorialComponent) }
];
