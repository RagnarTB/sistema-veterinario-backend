import { Component, Inject } from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { VentaResponse } from '../../../core/models/models';

@Component({
  selector: 'app-venta-detalle-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule
  ],
  templateUrl: './venta-detalle-dialog.component.html',
  styleUrls: ['./venta-detalle-dialog.component.css']
})
export class VentaDetalleDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<VentaDetalleDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: VentaResponse
  ) {}

  onClose(): void {
    this.dialogRef.close();
  }

  getEstadoBadgeClass(estado: string): string {
    switch (estado) {
      case 'PAGADA': return 'badge-success';
      case 'PAGADA_PARCIAL': return 'badge-warning';
      case 'ACTIVA': return 'badge-info';
      case 'ANULADA': return 'badge-danger';
      default: return 'badge-muted';
    }
  }
}
