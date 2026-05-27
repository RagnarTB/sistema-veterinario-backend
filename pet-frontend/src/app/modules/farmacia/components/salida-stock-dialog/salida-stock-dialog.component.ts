import { Component, Inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { InventarioService } from '../../services/inventario.service';

@Component({
  selector: 'app-salida-stock-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatDialogModule],
  templateUrl: './salida-stock-dialog.component.html',
  styleUrls: ['./salida-stock-dialog.component.css']
})
export class SalidaStockDialogComponent {
  form: FormGroup;
  cargando = signal(false);
  confirmando = signal(false);

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<SalidaStockDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { producto: any; sedeId: number },
    private inventarioService: InventarioService,
    private snackBar: MatSnackBar
  ) {
    this.form = this.fb.group({
      tipoMovimiento: ['', Validators.required],
      cantidad: [1, [Validators.required, Validators.min(0.01)]],
      motivo: ['', Validators.required]
    });
  }

  get permiteDecimales(): boolean {
    return this.data.producto.unidadVentaPermiteDecimales !== false;
  }

  esSalida(): boolean {
    const tipo = this.form.get('tipoMovimiento')?.value || '';
    return tipo === 'SALIDA_CONSUMO_INTERNO' || tipo === 'AJUSTE_NEGATIVO' || tipo === 'MERMA_VENCIMIENTO';
  }

  getLabelTipo(tipo: string): string {
    const map: Record<string, string> = {
      'SALIDA_CONSUMO_INTERNO': 'Consumo Interno',
      'AJUSTE_NEGATIVO': 'Ajuste Negativo',
      'MERMA_VENCIMIENTO': 'Merma por Vencimiento',
      'AJUSTE_POSITIVO': 'Ajuste Positivo'
    };
    return map[tipo] || tipo;
  }

  soloEnteros(event: KeyboardEvent) {
    if (!/[0-9]/.test(event.key)) {
      event.preventDefault();
    }
  }

  irAConfirmar() {
    if (this.form.invalid) return;

    if (!this.permiteDecimales) {
      const cant = this.form.get('cantidad')?.value;
      if (cant % 1 !== 0) {
        this.msg('Esta unidad no permite decimales. Ingrese un número entero.');
        return;
      }
    }

    this.confirmando.set(true);
  }

  guardar() {
    this.cargando.set(true);
    const sedeId = this.data.sedeId || Number(localStorage.getItem('vet_sede_id')) || 1;

    const payload = {
      productoId: this.data.producto.id,
      sedeId: sedeId,
      ...this.form.value
    };

    this.inventarioService.registrarSalidaAjuste(payload).subscribe({
      next: () => {
        this.snackBar.open('Movimiento registrado correctamente', 'Cerrar', { duration: 3000 });
        this.dialogRef.close(true);
      },
      error: (err) => {
        this.msg(err.error?.message || 'Error al registrar el movimiento');
        this.cargando.set(false);
      }
    });
  }

  cerrar() {
    this.dialogRef.close();
  }

  private msg(text: string) {
    this.snackBar.open(text, 'Cerrar', { duration: 3000 });
  }
}
