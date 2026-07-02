import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { VentaResponse, MetodoPago } from '../../../core/models/models';
import { VentaService } from '../../../core/services/venta.service';

export interface PagoDeudaData {
  venta: VentaResponse;
  sedeId: number;
}

@Component({
  selector: 'app-pago-deuda-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule
  ],
  templateUrl: './pago-deuda-dialog.component.html',
  styleUrls: ['./pago-deuda-dialog.component.css']
})
export class PagoDeudaDialogComponent implements OnInit {
  montoAbonar: number | null = null;
  metodoPagoAbonar: MetodoPago = 'EFECTIVO';
  referenciaAbonar = '';
  cargandoPago = false;

  constructor(
    public dialogRef: MatDialogRef<PagoDeudaDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: PagoDeudaData,
    private ventaService: VentaService,
    private snack: MatSnackBar
  ) {}

  ngOnInit() {
    this.montoAbonar = this.data.venta.saldoPendiente;
  }

  setMontoTotal() {
    this.montoAbonar = this.data.venta.saldoPendiente;
  }

  guardarPago() {
    if (this.montoAbonar === null || this.montoAbonar <= 0) return;

    if (this.montoAbonar > this.data.venta.saldoPendiente) {
      this.snack.open('El monto no puede ser mayor al saldo pendiente', 'Cerrar', { duration: 3000, panelClass: ['snack-error'] });
      return;
    }

    this.cargandoPago = true;
    const pagoRequest = {
      monto: this.montoAbonar,
      metodoPago: this.metodoPagoAbonar,
      referencia: this.referenciaAbonar ? this.referenciaAbonar.trim() : undefined,
      sedeId: this.data.sedeId
    };

    this.ventaService.registrarPago(this.data.venta.id, pagoRequest).subscribe({
      next: (res) => {
        this.snack.open('¡Pago de amortización registrado con éxito!', 'Cerrar', { duration: 3000, panelClass: ['snack-success'] });
        this.dialogRef.close(true);
      },
      error: (err) => {
        const errorMsg = err.error?.mensaje || 'Error al registrar el cobro de deuda';
        this.snack.open(errorMsg, 'Cerrar', { duration: 3000, panelClass: ['snack-error'] });
        this.cargandoPago = false;
      }
    });
  }

  onClose(): void {
    this.dialogRef.close(false);
  }
}
