import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CitaRequest, CitaResponse, SlotDisponibilidad, Page, EstadoCita } from '../models/models';

@Injectable({ providedIn: 'root' })
export class CitaService {
  private url = `${environment.apiUrl}/citas`;
  constructor(private http: HttpClient) {}

  listar(sedeId: number, page = 0, size = 10, buscar = '', fecha?: string, estado?: EstadoCita, veterinarioId?: number): Observable<Page<CitaResponse>> {
    let params = new HttpParams().set('sedeId', sedeId).set('page', page).set('size', size);
    if (buscar) params = params.set('buscar', buscar);
    if (fecha) params = params.set('fecha', fecha);
    if (estado) params = params.set('estado', estado);
    if (veterinarioId) params = params.set('veterinarioId', veterinarioId);
    return this.http.get<Page<CitaResponse>>(this.url, { params });
  }

  buscarPorId(id: number): Observable<CitaResponse> {
    return this.http.get<CitaResponse>(`${this.url}/${id}`);
  }

  crear(dto: CitaRequest): Observable<CitaResponse> {
    return this.http.post<CitaResponse>(this.url, dto);
  }

  actualizar(id: number, dto: CitaRequest): Observable<CitaResponse> {
    return this.http.put<CitaResponse>(`${this.url}/${id}`, dto);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }

  cambiarEstado(id: number, estado: EstadoCita): Observable<void> {
    return this.http.patch<void>(`${this.url}/${id}/estado`, null, {
      params: new HttpParams().set('estado', estado),
    });
  }

  getDisponibilidad(veterinarioId: number, fecha: string, servicioId: number, sedeId: number, cantidadPacientes = 1, citaIdExcluir?: number): Observable<SlotDisponibilidad[]> {
    let params = new HttpParams()
      .set('veterinarioId', veterinarioId)
      .set('fecha', fecha)
      .set('servicioId', servicioId)
      .set('sedeId', sedeId)
      .set('cantidadPacientes', cantidadPacientes);
    if (citaIdExcluir) params = params.set('citaIdExcluir', citaIdExcluir);
    return this.http.get<SlotDisponibilidad[]>(`${this.url}/disponibilidad`, { params });
  }
}
