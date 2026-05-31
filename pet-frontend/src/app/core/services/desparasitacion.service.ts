import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { DesparasitacionRequest, DesparasitacionResponse } from '../models/models';

@Injectable({
  providedIn: 'root'
})
export class DesparasitacionService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/desparasitaciones`;

  registrarDesparasitacion(request: DesparasitacionRequest): Observable<DesparasitacionResponse> {
    return this.http.post<DesparasitacionResponse>(this.apiUrl, request);
  }

  listarPorPaciente(pacienteId: number): Observable<DesparasitacionResponse[]> {
    return this.http.get<DesparasitacionResponse[]>(`${this.apiUrl}/paciente/${pacienteId}`);
  }

  listarProximasDosis(): Observable<DesparasitacionResponse[]> {
    return this.http.get<DesparasitacionResponse[]>(`${this.apiUrl}/proximas`);
  }
}
