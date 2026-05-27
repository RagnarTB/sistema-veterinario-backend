import { Component, Inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';

@Component({
  selector: 'app-stock-minimo-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, MatDialogModule],
  templateUrl: './stock-minimo-dialog.component.html',
  styleUrls: ['./stock-minimo-dialog.component.css']
})
export class StockMinimoDialogComponent {
  stockMinimo: number;

  constructor(
    private dialogRef: MatDialogRef<StockMinimoDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { productoNombre: string, stockMinimoActual: number }
  ) {
    this.stockMinimo = data.stockMinimoActual;
  }

  cerrar() {
    this.dialogRef.close();
  }

  guardar() {
    this.dialogRef.close(this.stockMinimo);
  }
}
