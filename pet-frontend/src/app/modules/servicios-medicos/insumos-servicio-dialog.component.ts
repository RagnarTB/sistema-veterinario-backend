import { Component, Inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialog, MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { ServicioMedicoService } from './servicio-medico.service';
import { ProductoService, Producto } from '../farmacia/services/producto.service';
import { ServicioMedicoResponse, ServicioMedicoInsumoResponse } from '../../core/models/models';
import { ModalConfirmacionComponent } from '../../shared/components/modal-confirmacion/modal-confirmacion.component';
import { debounceTime, distinctUntilChanged, switchMap, finalize } from 'rxjs/operators';
import { Subject } from 'rxjs';

@Component({
  selector: 'app-insumos-servicio-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatAutocompleteModule
  ],
  templateUrl: './insumos-servicio-dialog.component.html',
  styleUrls: ['./insumos-servicio-dialog.component.css']
})
export class InsumosServicioDialogComponent implements OnInit {
  insumos = signal<ServicioMedicoInsumoResponse[]>([]);
  filteredProducts = signal<Producto[]>([]);
  loadingSearch = signal(false);

  searchQuery = '';
  selectedProduct: Producto | null = null;
  newInsumo = {
    cantidadEstimada: 1,
    notas: ''
  };

  private searchSubject = new Subject<string>();

  constructor(
    private servicioMedicoService: ServicioMedicoService,
    private productoService: ProductoService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog,
    public dialogRef: MatDialogRef<InsumosServicioDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ServicioMedicoResponse
  ) {
    this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(query => {
        if (!query || query.length < 2) return [[]];
        this.loadingSearch.set(true);
        return this.productoService.listar(query, 0, 20).pipe(
          switchMap(res => [res.content]),
          finalize(() => this.loadingSearch.set(false))
        );
      })
    ).subscribe(prods => {
      this.filteredProducts.set(prods);
    });
  }

  ngOnInit(): void {
    this.cargarInsumos();
  }

  cargarInsumos(): void {
    this.servicioMedicoService.listarInsumos(this.data.id).subscribe(res => {
      this.insumos.set(res);
    });
  }

  onSearchInput(event: any): void {
    this.searchSubject.next(event.target.value);
  }

  displayFn(prod: Producto): string {
    return prod ? prod.nombre : '';
  }

  onProductSelected(prod: Producto): void {
    this.selectedProduct = prod;
  }

  addInsumo(): void {
    if (!this.selectedProduct) return;

    this.servicioMedicoService.agregarInsumo(this.data.id, {
      productoId: this.selectedProduct.id,
      cantidadEstimada: this.newInsumo.cantidadEstimada,
      notas: this.newInsumo.notas
    }).subscribe({
      next: () => {
        this.snackBar.open('Insumo agregado a la plantilla', 'OK', { duration: 2000 });
        this.cargarInsumos();
        this.searchQuery = '';
        this.selectedProduct = null;
        this.newInsumo = { cantidadEstimada: 1, notas: '' };
      },
      error: (err) => {
        this.snackBar.open(err.error?.message || 'Error al agregar insumo', 'Cerrar', { duration: 3000 });
      }
    });
  }

  toggleEstado(insumo: ServicioMedicoInsumoResponse): void {
    const dialogRef = this.dialog.open(ModalConfirmacionComponent, {
      width: '400px',
      data: {
        title: insumo.activo ? 'Desactivar Insumo' : 'Reactivar Insumo',
        message: `¿Está seguro de ${insumo.activo ? 'desactivar' : 'reactivar'} el insumo "${insumo.productoNombre}" de la plantilla?`,
        confirmText: insumo.activo ? 'Desactivar' : 'Reactivar',
        isDestructive: insumo.activo
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.servicioMedicoService.cambiarEstadoInsumo(this.data.id, insumo.id, !insumo.activo).subscribe(() => {
          this.cargarInsumos();
          this.snackBar.open('Estado de insumo actualizado', 'OK', { duration: 2000 });
        });
      }
    });
  }

  eliminarInsumoPermanente(insumo: ServicioMedicoInsumoResponse): void {
    const dialogRef = this.dialog.open(ModalConfirmacionComponent, {
      width: '400px',
      data: {
        title: 'Eliminar Insumo',
        message: `¿Está seguro de ELIMINAR PERMANENTEMENTE el insumo "${insumo.productoNombre}" de esta plantilla? Esta acción no se puede deshacer.`,
        confirmText: 'Eliminar',
        isDestructive: true
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.servicioMedicoService.eliminarInsumo(this.data.id, insumo.id).subscribe({
          next: () => {
            this.cargarInsumos();
            this.snackBar.open('Insumo eliminado definitivamente', 'OK', { duration: 2000 });
          },
          error: (err) => {
            this.snackBar.open(err.error?.message || 'Error al eliminar', 'Cerrar', { duration: 3000 });
          }
        });
      }
    });
  }

  onClose(): void {
    this.dialogRef.close();
  }

  trackByProductId(index: number, item: Producto): number {
    return item.id;
  }

  trackByInsumoId(index: number, item: ServicioMedicoInsumoResponse): number {
    return item.id;
  }
}


