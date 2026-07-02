import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Observable } from 'rxjs';

export interface RangoPesoJaula {
  id?: number;
  especie: any;
  pesoMinimo: number;
  pesoMaximo: number;
  tamanoJaula: any;
}

@Injectable({
  providedIn: 'root'
})
export class RangoPesoJaulaService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl + '/rangos-peso-jaula';

  listar(): Observable<RangoPesoJaula[]> {
    return this.http.get<RangoPesoJaula[]>(this.apiUrl);
  }

  crear(rango: RangoPesoJaula): Observable<RangoPesoJaula> {
    return this.http.post<RangoPesoJaula>(this.apiUrl, rango);
  }

  actualizar(id: number, rango: RangoPesoJaula): Observable<RangoPesoJaula> {
    return this.http.put<RangoPesoJaula>(this.apiUrl + '/' + id, rango);
  }

  eliminar(id: number): Observable<any> {
    return this.http.delete(this.apiUrl + '/' + id);
  }
}