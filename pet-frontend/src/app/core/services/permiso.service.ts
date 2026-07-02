import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface PermisoDTO {
  nombre: string;
  descripcion: string;
}

@Injectable({ providedIn: 'root' })
export class PermisoService {
  private url = `${environment.apiUrl}/permisos`;
  
  constructor(private http: HttpClient) {}

  listarTodos(): Observable<PermisoDTO[]> {
    return this.http.get<PermisoDTO[]>(this.url);
  }
}
