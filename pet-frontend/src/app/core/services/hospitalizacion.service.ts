import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Observable } from 'rxjs';

export interface HospitalizacionResponse {
  id: number;
  pacienteId: number;
  pacienteNombre: string;
  pacienteEspecie: string;
  motivoIngreso: string;
  fechaIngreso: string;
  fechaAlta?: string;
  estado: string;
  nivelGravedad: string;
  proximoMonitoreo: string;
  monitoreoAtrasado: boolean;
  jaulaId: number;
  jaulaNumero: string;
  frecuenciaMonitoreoHoras: number;
  empleadoId: number;
  empleadoNombre: string;
}

@Injectable({
  providedIn: 'root'
})
export class HospitalizacionService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl + '/hospitalizaciones';

  listarActivas(sedeId?: number): Observable<HospitalizacionResponse[]> {
    return this.http.get<HospitalizacionResponse[]>(`${this.apiUrl}/activas`, { params: sedeId ? { sedeId: sedeId.toString() } : {} });
  }

  sugerirJaula(pacienteId: number, sedeId: number, pesoActual?: number): Observable<any> {
    let params: any = { pacienteId: pacienteId.toString(), sedeId: sedeId.toString() };
    if (pesoActual !== undefined && pesoActual !== null) {
      params.pesoActual = pesoActual.toString();
    }
    return this.http.get<any>(`${this.apiUrl}/sugerencia-jaula`, { params });
  }

  ingresar(payload: any): Observable<HospitalizacionResponse> {
    return this.http.post<HospitalizacionResponse>(`${this.apiUrl}/ingreso`, payload);
  }

  darDeAlta(id: number): Observable<HospitalizacionResponse> {
    return this.http.put<HospitalizacionResponse>(`${this.apiUrl}/${id}/alta`, {});
  }

  cambiarGravedad(id: number, nivel: string): Observable<HospitalizacionResponse> {
    return this.http.patch<HospitalizacionResponse>(`${this.apiUrl}/${id}/gravedad`, null, {
      params: { nivel }
    });
  }

  obtenerHistorial(id: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/${id}/historial`);
  }
}





