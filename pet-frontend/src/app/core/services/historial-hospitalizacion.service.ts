import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Observable } from 'rxjs';

export interface MonitoreoPayload {
  hospitalizacionId: number;
  empleadoId: number;
  descripcion: string;
  tipoAccion: string;
  productoId?: number;
  cantidadAplicada?: number;
  origenMedicamento?: string;
}

@Injectable({
  providedIn: 'root'
})
export class HistorialHospitalizacionService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl + '/hospitalizaciones';

  registrarMonitoreo(id: number, payload: MonitoreoPayload): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/${id}/monitoreo`, payload);
  }
}

