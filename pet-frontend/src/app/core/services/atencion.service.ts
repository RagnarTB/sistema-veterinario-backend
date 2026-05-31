import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface AtencionRequest {
  citaId: number;
  pacienteId: number;
  sintomas: string;
  diagnostico: string;
  tratamiento: string;
  peso: number;
  temperatura: number;
  frecuenciaCardiaca: number;
}

export interface AtencionResponse {
  id: number;
  sintomas: string;
  diagnostico: string;
  tratamiento: string;
  peso: number;
  temperatura: number;
  frecuenciaCardiaca: number;
  resumenIaCliente?: string;
  citaId: number;
  veterinarioId: number;
  activo: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class AtencionService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/atenciones`;

  crear(dto: AtencionRequest): Observable<AtencionResponse> {
    return this.http.post<AtencionResponse>(this.apiUrl, dto);
  }

  actualizar(id: number, dto: AtencionRequest): Observable<AtencionResponse> {
    return this.http.put<AtencionResponse>(`${this.apiUrl}/${id}`, dto);
  }

  buscarPorId(id: number): Observable<AtencionResponse> {
    return this.http.get<AtencionResponse>(`${this.apiUrl}/${id}`);
  }

  buscarPorCitaYPaciente(citaId: number, pacienteId: number): Observable<AtencionResponse> {
    return this.http.get<AtencionResponse>(`${this.apiUrl}/cita/${citaId}/paciente/${pacienteId}`);
  }
}
