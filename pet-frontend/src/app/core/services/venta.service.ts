import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { VentaRequest, VentaResponse, PagoRequest, Page, MensajeResponse } from '../models/models';

@Injectable({
  providedIn: 'root'
})
export class VentaService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/ventas`;

  crear(dto: VentaRequest): Observable<VentaResponse> {
    return this.http.post<VentaResponse>(this.apiUrl, dto);
  }

  listar(page = 0, size = 10): Observable<Page<VentaResponse>> {
    return this.http.get<Page<VentaResponse>>(this.apiUrl, {
      params: { page: page.toString(), size: size.toString() }
    });
  }

  buscarPorId(id: number): Observable<VentaResponse> {
    return this.http.get<VentaResponse>(`${this.apiUrl}/${id}`);
  }

  registrarPago(id: number, dto: PagoRequest): Observable<VentaResponse> {
    return this.http.post<VentaResponse>(`${this.apiUrl}/${id}/pago`, dto);
  }

  anular(id: number): Observable<MensajeResponse> {
    return this.http.patch<MensajeResponse>(`${this.apiUrl}/${id}/anular`, {});
  }
}
