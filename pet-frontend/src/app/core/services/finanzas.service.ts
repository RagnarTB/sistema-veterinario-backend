import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { VentaResponse, Page } from '../models/models';

@Injectable({
  providedIn: 'root'
})
export class FinanzasService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/finanzas`;

  listarDeudas(sedeId: number | null, query = '', page = 0, size = 10): Observable<Page<VentaResponse>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (sedeId !== null && sedeId !== undefined) {
      params = params.set('sedeId', sedeId.toString());
    }

    if (query && query.trim()) {
      params = params.set('query', query.trim());
    }

    return this.http.get<Page<VentaResponse>>(`${this.apiUrl}/deudas`, { params });
  }
}
