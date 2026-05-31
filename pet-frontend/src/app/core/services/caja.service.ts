import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CajaRequest, CierreCajaResponse } from '../models/models';

export interface CajaEstadoResponse {
  abierta: boolean;
  cajaId: number | null;
  saldoInicial: number | null;
  fechaApertura: string | null;
  responsableNombre: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class CajaService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/caja`;

  abrirCaja(dto: CajaRequest): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/abrir`, dto);
  }

  cerrarCaja(sedeId: number): Observable<CierreCajaResponse> {
    return this.http.put<CierreCajaResponse>(`${this.apiUrl}/cerrar`, null, {
      params: new HttpParams().set('sedeId', sedeId.toString())
    });
  }

  obtenerEstadoCaja(sedeId: number): Observable<CajaEstadoResponse> {
    return this.http.get<CajaEstadoResponse>(`${this.apiUrl}/estado`, {
      params: new HttpParams().set('sedeId', sedeId.toString())
    });
  }
}
