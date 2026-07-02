import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { VacunaRequest, VacunaResponse } from '../models/models';

@Injectable({
  providedIn: 'root'
})
export class VacunaService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/vacunas`;

  registrarVacuna(request: VacunaRequest): Observable<VacunaResponse> {
    return this.http.post<VacunaResponse>(this.apiUrl, request);
  }

  listarPorPaciente(pacienteId: number): Observable<VacunaResponse[]> {
    return this.http.get<VacunaResponse[]>(`${this.apiUrl}/paciente/${pacienteId}`);
  }

  listarProximasDosis(): Observable<VacunaResponse[]> {
    return this.http.get<VacunaResponse[]>(`${this.apiUrl}/proximas`);
  }

  actualizarProximaDosis(id: number, fecha: string): Observable<VacunaResponse> {
    return this.http.put<VacunaResponse>(`${this.apiUrl}/${id}/proxima-dosis`, { fecha });
  }

  eliminar(id: number, motivo: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}?motivo=${encodeURIComponent(motivo)}`);
  }
}
