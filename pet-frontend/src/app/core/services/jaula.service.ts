import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Observable } from 'rxjs';

export interface JaulaResponse {
  id: number;
  numero: string;
  categoriaNombre: string;
  tamanoNombre: string;
  tamanoId: number;
  alertaContagio: boolean;
  activo: boolean;
  estado: string;
  sedeId: number;
  sedeNombre: string;
}

@Injectable({
  providedIn: 'root'
})
export class JaulaService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/jaulas`;

  listarJaulasPorSede(sedeId: number): Observable<JaulaResponse[]> {
    return this.http.get<JaulaResponse[]>(`${this.apiUrl}/sede/${sedeId}`);
  }

  cambiarEstado(id: number, estado: string): Observable<JaulaResponse> {
    return this.http.put<JaulaResponse>(`${this.apiUrl}/${id}/estado?nuevoEstado=${estado}`, {});
  }

  crear(data: any): Observable<JaulaResponse> {
    return this.http.post<JaulaResponse>(this.apiUrl, data);
  }

  actualizar(id: number, data: any): Observable<JaulaResponse> {
    return this.http.put<JaulaResponse>(`${this.apiUrl}/${id}`, data);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}

