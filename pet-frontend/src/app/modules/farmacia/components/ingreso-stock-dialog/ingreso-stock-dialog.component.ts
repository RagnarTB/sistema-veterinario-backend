import { Component, Inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { InventarioService } from '../../services/inventario.service';

@Component({
  selector: 'app-ingreso-stock-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatDialogModule],
  templateUrl: './ingreso-stock-dialog.component.html',
  styleUrls: ['./ingreso-stock-dialog.component.css']
})
export class IngresoStockDialogComponent {
  form: FormGroup;
  cargando = signal(false);
  confirmando = signal(false);
  fechaMinima: string;

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<IngresoStockDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { producto: any; proveedores: any[]; sedeId: number },
    private inventarioService: InventarioService,
    private snackBar: MatSnackBar
  ) {
    const hoy = new Date();
    this.fechaMinima = hoy.toISOString().split('T')[0];

    this.form = this.fb.group({
      proveedorId: [null],
      numeroLote: ['', Validators.required],
      tieneVencimiento: [true],
      fechaVencimiento: [''],
      cantidadComprada: [1, [Validators.required, Validators.min(0.01)]],
      motivo: ['Compra de stock']
    });
  }

  get permiteDecimales(): boolean {
    // Asumimos que data.producto tiene la info de la unidad
    // Si no, podríamos buscarla en la lista global, pero usualmente viene en el objeto producto
    return this.data.producto.unidadCompraPermiteDecimales !== false;
  }

  get nombreProveedorSeleccionado(): string {
    const id = this.form.get('proveedorId')?.value;
    return this.data.proveedores.find(p => p.id === id)?.razonSocial || '';
  }

  get cantidadConvertida(): number {
    const cantidad = this.form.get('cantidadComprada')?.value || 0;
    const factor = this.data.producto.factorConversion || 1;
    return cantidad * factor;
  }

  soloEnteros(event: KeyboardEvent) {
    if (!/[0-9]/.test(event.key)) {
      event.preventDefault();
    }
  }

  irAConfirmar() {
    if (this.form.invalid) return;

    // Validación extra de decimales en frontend
    if (!this.permiteDecimales) {
      const cant = this.form.get('cantidadComprada')?.value;
      if (cant % 1 !== 0) {
        this.msg('Esta unidad no permite decimales. Ingrese un número entero.');
        return;
      }
    }

    if (this.form.get('tieneVencimiento')?.value) {
      const fecha = this.form.get('fechaVencimiento')?.value;
      if (!fecha) {
        this.msg('Debe ingresar una fecha de vencimiento');
        return;
      }
      if (fecha < this.fechaMinima) {
        this.msg('La fecha de vencimiento no puede ser anterior a hoy');
        return;
      }
    }

    this.confirmando.set(true);
  }

  guardar() {
    this.cargando.set(true);

    const sedeId = this.data.sedeId || Number(localStorage.getItem('vet_sede_id')) || 1;
    const values = this.form.value;

    const payload = {
      productoId: this.data.producto.id,
      sedeId: sedeId,
      proveedorId: values.proveedorId,
      numeroLote: values.numeroLote,
      fechaVencimiento: values.tieneVencimiento ? values.fechaVencimiento : null,
      cantidadComprada: values.cantidadComprada,
      motivo: values.motivo
    };

    this.inventarioService.registrarIngreso(payload).subscribe({
      next: () => {
        this.snackBar.open('Stock ingresado correctamente', 'Cerrar', { duration: 3000 });
        this.dialogRef.close(true);
      },
      error: (err) => {
        this.msg(err.error?.message || 'Error al registrar el ingreso');
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
