import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

import { CajaService } from '../../../core/services/caja.service';
import { SedeService } from '../../../core/services/sede.service';
import { CajaHistorialResponse } from '../../../core/models/models';
import { CajaDetalleDialog } from './caja-detalle-dialog/caja-detalle-dialog';

@Component({
  selector: 'app-historial-cajas',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatTableModule,
    MatPaginatorModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDialogModule
  ],
  templateUrl: './historial-cajas.html',
  styleUrl: './historial-cajas.css',
})
export class HistorialCajas implements OnInit {
  private cajaService = inject(CajaService);
  private sedeService = inject(SedeService);
  private dialog = inject(MatDialog);
  private snack = inject(MatSnackBar);

  cajas = signal<CajaHistorialResponse[]>([]);
  totalElements = signal(0);
  loading = signal(false);
  pageIndex = signal(0);
  pageSize = signal(10);
  sedeId = Number(localStorage.getItem('vet_sede_id')) || 1;

  displayedColumns = ['id', 'fechaApertura', 'fechaCierre', 'empleadoNombre', 'saldoInicial', 'saldoFinal', 'acciones'];

  ngOnInit(): void {
    if (this.sedeId) {
      this.cargarCajas();
    }
  }

  cargarCajas() {
    const sedeId = this.sedeId;
    if (!sedeId) return;

    this.loading.set(true);
    this.cajaService.listarHistorial(sedeId, this.pageIndex(), this.pageSize()).subscribe({
      next: (res) => {
        this.cajas.set(res.content);
        this.totalElements.set(res.totalElements);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.snack.open('Error al cargar historial de cajas', 'Cerrar', { duration: 3000 });
      }
    });
  }

  onPageChange(event: PageEvent) {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.cargarCajas();
  }

  verDetalles(caja: CajaHistorialResponse) {
    this.dialog.open(CajaDetalleDialog, {
      width: '800px',
      maxHeight: '90vh',
      data: { cajaId: caja.id, fecha: caja.fechaApertura }
    });
  }
}
