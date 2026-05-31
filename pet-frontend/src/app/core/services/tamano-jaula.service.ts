import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Observable } from 'rxjs';

export interface TamanoJaula {
  id?: number;
  nombre: string;
  activo: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class TamanoJaulaService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl + '/tamanos-jaula';

  listar(): Observable<TamanoJaula[]> {
    return this.http.get<TamanoJaula[]>(this.apiUrl);
  }

  crear(tamano: TamanoJaula): Observable<TamanoJaula> {
    return this.http.post<TamanoJaula>(this.apiUrl, tamano);
  }

  actualizar(id: number, tamano: TamanoJaula): Observable<TamanoJaula> {
    return this.http.put<TamanoJaula>(this.apiUrl + '/' + id, tamano);
  }

  eliminar(id: number): Observable<any> {
    return this.http.delete(this.apiUrl + '/' + id);
  }
}