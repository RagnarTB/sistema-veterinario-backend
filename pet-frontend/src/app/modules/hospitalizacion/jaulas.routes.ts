import { Routes } from '@angular/router';

export const JAULAS_ROUTES: Routes = [
  { 
    path: '', 
    loadComponent: () => import('./jaulas.component').then(m => m.JaulasComponent) 
  },
  { 
    path: 'configuracion/categorias-jaula', 
    loadComponent: () => import('./configuracion/categoria-jaula/categoria-jaula.component').then(m => m.CategoriaJaulaComponent) 
  },
  { 
    path: 'configuracion/tamanos-jaula', 
    loadComponent: () => import('./configuracion/tamano-jaula/tamano-jaula.component').then(m => m.TamanoJaulaComponent) 
  },
  { 
    path: 'configuracion/rangos-peso', 
    loadComponent: () => import('./configuracion/rango-peso-jaula/rango-peso-jaula.component').then(m => m.RangoPesoJaulaComponent) 
  }
];
