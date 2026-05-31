import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface DetalleRecetaRequest {
  medicamento: string;
  dosis: string;
  frecuencia: string;
  duracionDias: number;
  productoId?: number | null;
}

export interface RecetaRequest {
  indicacionesGenerales?: string;
  atencionMedicaId: number;
  detalles: DetalleRecetaRequest[];
}

export interface DetalleRecetaResponse {
  id: number;
  medicamento: string;
  dosis: string;
  frecuencia: string;
  duracionDias: number;
  productoId?: number | null;
  productoNombre?: string | null;
  cantidad?: number;
}

export interface RecetaResponse {
  id: number;
  indicacionesGenerales?: string;
  atencionMedicaId: number;
  detalles: DetalleRecetaResponse[];
}

@Injectable({
  providedIn: 'root'
})
export class RecetaService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/recetas`;

  generar(dto: RecetaRequest): Observable<RecetaResponse> {
    return this.http.post<RecetaResponse>(this.apiUrl, dto);
  }

  obtenerPorAtencion(atencionId: number): Observable<RecetaResponse> {
    return this.http.get<RecetaResponse>(`${this.apiUrl}/atencion/${atencionId}`);
  }

  obtenerPorId(id: number): Observable<RecetaResponse> {
    return this.http.get<RecetaResponse>(`${this.apiUrl}/${id}`);
  }
}
