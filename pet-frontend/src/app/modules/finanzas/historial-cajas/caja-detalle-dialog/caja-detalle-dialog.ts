import { Component, Inject, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { CajaService } from '../../../../core/services/caja.service';
import { MovimientoCajaResponse } from '../../../../core/models/models';

@Component({
  selector: 'app-caja-detalle-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatTableModule,
    MatProgressSpinnerModule,
    MatIconModule
  ],
  templateUrl: './caja-detalle-dialog.html',
  styleUrl: './caja-detalle-dialog.css',
})
export class CajaDetalleDialog implements OnInit {
  private cajaService = inject(CajaService);
  
  movimientos = signal<MovimientoCajaResponse[]>([]);
  loading = signal(true);
  
  displayedColumns = ['fechaHora', 'concepto', 'tipoMovimiento', 'monto'];

  constructor(
    public dialogRef: MatDialogRef<CajaDetalleDialog>,
    @Inject(MAT_DIALOG_DATA) public data: { cajaId: number, fecha: string }
  ) {}

  ngOnInit(): void {
    this.cajaService.listarMovimientos(this.data.cajaId).subscribe({
      next: (movs) => {
        this.movimientos.set(movs);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      }
    });
  }

  cerrar(): void {
    this.dialogRef.close();
  }
}
