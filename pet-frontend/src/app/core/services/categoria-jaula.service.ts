import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Observable } from 'rxjs';

export interface CategoriaJaula {
  id?: number;
  nombre: string;
  descripcion: string;
  activo: boolean;
  precioPorDia: number;
}

@Injectable({
  providedIn: 'root'
})
export class CategoriaJaulaService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl + '/categorias-jaula';

  listar(): Observable<CategoriaJaula[]> {
    return this.http.get<CategoriaJaula[]>(this.apiUrl);
  }

  crear(categoria: CategoriaJaula): Observable<CategoriaJaula> {
    return this.http.post<CategoriaJaula>(this.apiUrl, categoria);
  }

  actualizar(id: number, categoria: CategoriaJaula): Observable<CategoriaJaula> {
    return this.http.put<CategoriaJaula>(this.apiUrl + '/' + id, categoria);
  }

  eliminar(id: number): Observable<any> {
    return this.http.delete(this.apiUrl + '/' + id);
  }
}