import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Page } from '../models/models';

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
  fechaCreacion?: string;
  veterinarioNombre?: string;
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

  listarPorPaciente(pacienteId: number, page: number = 0, size: number = 20): Observable<Page<AtencionResponse>> {
    return this.http.get<Page<AtencionResponse>>(`${this.apiUrl}/paciente/${pacienteId}?page=${page}&size=${size}`);
  }
}
