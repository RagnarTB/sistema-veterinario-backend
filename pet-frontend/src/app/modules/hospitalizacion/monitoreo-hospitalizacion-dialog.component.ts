import { Component, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialogRef, MatDialogModule, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { HistorialHospitalizacionService } from '../../core/services/historial-hospitalizacion.service';
import { AuthService } from '../../core/services/auth.service';

import { Observable, Subject, of } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';
import { ProductoService, Producto } from '../farmacia/services/producto.service';

@Component({
  selector: 'app-monitoreo-hospitalizacion-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatButtonModule, MatDialogModule, MatProgressSpinnerModule, MatIconModule],
  templateUrl: './monitoreo-hospitalizacion-dialog.component.html'
})
export class MonitoreoHospitalizacionDialogComponent {
  data = inject(MAT_DIALOG_DATA) as any;
  dialogRef = inject(MatDialogRef);
  fb = inject(FormBuilder);
  historialService = inject(HistorialHospitalizacionService);
  authService = inject(AuthService);
  snackBar = inject(MatSnackBar);

  saving = signal(false);

  form: FormGroup = this.fb.group({
    tipoAccion: ['CONTROL_SIGNOS', Validators.required],
    descripcion: ['', Validators.required],
    aplicaMedicamento: [false],
    origenMedicamento: ['CLINICA'],
    nombreMedicamentoTraido: [''],
    productoQuery: [''],
    productoId: [null],
    cantidadAplicada: [null]
  });

  productoService = inject(ProductoService);
  productosFiltrados = signal<Producto[]>([]);
  productoSeleccionado = signal<Producto | null>(null);
  buscandoProducto = signal(false);
  private productoSearch$ = new Subject<string>();

  ngOnInit() {
    this.productoSearch$.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(q => {
        if (q.length < 2) {
          return of({ content: [] });
        }
        this.buscandoProducto.set(true);
        // Suponiendo Sede Central si no tenemos context aquí, o pasamos la SedeId de la data
        const sedeId = this.data.sedeId || 1; 
        return this.productoService.listar(q, 0, 10, sedeId);
      })
    ).subscribe({
      next: (res) => {
        this.productosFiltrados.set(res.content || []);
        this.buscandoProducto.set(false);
      },
      error: () => {
        this.productosFiltrados.set([]);
        this.buscandoProducto.set(false);
      }
    });
  }

  onProductoSearch(query: string) {
    this.productoSeleccionado.set(null);
    this.form.patchValue({ productoId: null });
    this.productoSearch$.next(query);
  }

  seleccionarProducto(prod: Producto) {
    this.productoSeleccionado.set(prod);
    this.form.patchValue({ 
      productoQuery: prod.nombre,
      productoId: prod.id 
    });
    this.productosFiltrados.set([]);
  }

  guardar() {
    if (this.form.invalid) return;

    const isLoggedIn = this.authService.isAuthenticated();
    if (!isLoggedIn) {
      this.snackBar.open('Debe iniciar sesion', 'Cerrar', { duration: 3000 });
      return;
    }

    this.saving.set(true);

    let descripcionFinal = this.form.value.descripcion;
    const aplicaMed = this.form.value.aplicaMedicamento;
    const origen = this.form.value.origenMedicamento;
    
    if (aplicaMed && origen === 'CLIENTE' && this.form.value.nombreMedicamentoTraido) {
      descripcionFinal = `[M. Traído: ${this.form.value.nombreMedicamentoTraido}] ` + descripcionFinal;
    }

    const payload = {
      hospitalizacionId: this.data.id,
      empleadoId: 1, // ToDo: Obtener del AuthService
      tipoAccion: this.form.value.tipoAccion,
      descripcion: descripcionFinal,
      productoId: (aplicaMed && origen === 'CLINICA') ? this.form.value.productoId : null,
      cantidadAplicada: (aplicaMed && origen === 'CLINICA') ? this.form.value.cantidadAplicada : null,
      origenMedicamento: aplicaMed ? origen : null
    };

    this.historialService.registrarMonitoreo(this.data.id, payload).subscribe({
      next: () => {
        this.snackBar.open('Monitoreo registrado con éxito', 'Cerrar', { duration: 3000, panelClass: ['snack-success'] });
        this.dialogRef.close(true);
      },
      error: (err) => {
        const msg = err.error?.mensaje || err.error?.message || 'Error al registrar monitoreo';
        this.snackBar.open(msg, 'Cerrar', { duration: 4000, panelClass: ['snack-error'] });
        this.saving.set(false);
      }
    });
  }
}