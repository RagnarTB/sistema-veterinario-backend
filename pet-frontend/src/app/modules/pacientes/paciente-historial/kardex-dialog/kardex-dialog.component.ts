import { Component, Inject, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../../environments/environment';

@Component({
  selector: 'app-kardex-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule, MatTableModule, MatProgressSpinnerModule],
  template: `
    <h2 mat-dialog-title class="flex items-center gap-2">
      <mat-icon color="primary">history</mat-icon> Kardex de Auditoría
    </h2>
    <mat-dialog-content>
      @if(loading()) {
        <div class="flex justify-center p-8">
          <mat-spinner diameter="40"></mat-spinner>
        </div>
      } @else {
        <table mat-table [dataSource]="kardex()" class="w-full">
          <ng-container matColumnDef="fechaHora">
            <th mat-header-cell *matHeaderCellDef> Fecha </th>
            <td mat-cell *matCellDef="let element"> {{ element.fechaHora | date:'medium' }} </td>
          </ng-container>

          <ng-container matColumnDef="usuario">
            <th mat-header-cell *matHeaderCellDef> Usuario </th>
            <td mat-cell *matCellDef="let element"> {{ element.usuario }} </td>
          </ng-container>

          <ng-container matColumnDef="modulo">
            <th mat-header-cell *matHeaderCellDef> Acción </th>
            <td mat-cell *matCellDef="let element">
               <span class="px-2 py-1 text-xs font-semibold rounded-full"
                     [ngClass]="{'bg-blue-100 text-blue-800': element.accion === 'EDICION', 'bg-red-100 text-red-800': element.accion === 'ELIMINACION'}">
                 {{ element.modulo }} - {{ element.accion }}
               </span>
            </td>
          </ng-container>

          <ng-container matColumnDef="detalle">
            <th mat-header-cell *matHeaderCellDef> Detalle </th>
            <td mat-cell *matCellDef="let element"> {{ element.detalle }} </td>
          </ng-container>
          
          <ng-container matColumnDef="motivo">
            <th mat-header-cell *matHeaderCellDef> Motivo </th>
            <td mat-cell *matCellDef="let element"> {{ element.motivo || '-' }} </td>
          </ng-container>

          <tr mat-header-row *matHeaderRowDef="columns"></tr>
          <tr mat-row *matRowDef="let row; columns: columns;"></tr>
          
          <tr class="mat-row" *matNoDataRow>
              <td class="mat-cell p-4 text-center text-gray-500" colspan="5">
                  No hay registros de auditoría para este paciente.
              </td>
          </tr>
        </table>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cerrar</button>
    </mat-dialog-actions>
  `
})
export class KardexDialogComponent implements OnInit {
  private http = inject(HttpClient);
  public data: { pacienteId: number } = inject(MAT_DIALOG_DATA);
  
  kardex = signal<any[]>([]);
  loading = signal(true);
  columns = ['fechaHora', 'usuario', 'modulo', 'detalle', 'motivo'];

  ngOnInit() {
    this.http.get<any[]>(`${environment.apiUrl}/pacientes/${this.data.pacienteId}/kardex`).subscribe({
      next: (res) => {
        this.kardex.set(res);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }
}
