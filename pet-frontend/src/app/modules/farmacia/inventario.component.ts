import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ProductoService, Producto } from './services/producto.service';
import { ModalConfirmacionComponent } from '../../shared/components/modal-confirmacion/modal-confirmacion.component';
import { ProductoDialogComponent } from './components/producto-dialog/producto-dialog.component';
import { GestionCatalogosDialogComponent } from './components/gestion-catalogos-dialog/gestion-catalogos-dialog.component';
import { InventarioService } from './services/inventario.service';
import { StockMinimoDialogComponent } from './components/stock-minimo-dialog/stock-minimo-dialog.component';
import { AuthService } from '../../core/services/auth.service';
import { SedeService } from '../../core/services/sede.service';
import { SedeResponse } from '../../core/models/models';

@Component({
  selector: 'app-inventario',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatSnackBarModule,
    MatTooltipModule
  ],
  templateUrl: './inventario.component.html',
  styleUrls: ['./inventario.component.css']
})
export class InventarioComponent implements OnInit {
  private productoService = inject(ProductoService);
  private inventarioService = inject(InventarioService);
  private authService = inject(AuthService);
  private sedeService = inject(SedeService);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);

  productos: Producto[] = [];
  sedes = signal<SedeResponse[]>([]);
  sedeSeleccionadaId = 1;
  userSedeId = 1;

  esAdmin = () => this.authService.isAdmin();
  puedeOperar = () => {
    if (this.esAdmin()) return true;
    const misSedes = this.authService.currentSedeIds();
    return misSedes.includes(this.sedeSeleccionadaId);
  };
  busqueda = '';
  currentPage = 0;
  pageSize = 10;
  totalElements = 0;
  totalPages = 0;

  private searchTimeout: any;

  ngOnInit() {
    this.userSedeId = Number(localStorage.getItem('vet_sede_id')) || 1;
    this.sedeSeleccionadaId = this.userSedeId;
    this.cargarSedes();
    this.cargarProductos();
  }

  cargarSedes() {
    this.sedeService.listarActivas().subscribe({
      next: (res) => this.sedes.set(res),
      error: () => {}
    });
  }

  onSedeChange() {
    this.sedeSeleccionadaId = Number(this.sedeSeleccionadaId);
    this.currentPage = 0;
    this.cargarProductos();
  }

  cargarProductos() {
    const sedeId = this.sedeSeleccionadaId;
    this.productoService.listar(this.busqueda, this.currentPage, this.pageSize, sedeId)
      .subscribe({
        next: (page) => {
          this.productos = page.content;
          this.totalElements = page.totalElements;
          this.totalPages = page.totalPages;
        },
        error: () => this.mostrarMensaje('Error al cargar productos')
      });
  }

  onSearch() {
    clearTimeout(this.searchTimeout);
    this.searchTimeout = setTimeout(() => {
      this.currentPage = 0;
      this.cargarProductos();
    }, 400);
  }

  cambiarPagina(nuevaPagina: number) {
    this.currentPage = nuevaPagina;
    this.cargarProductos();
  }

  abrirModalNuevo() {
    const dialogRef = this.dialog.open(ProductoDialogComponent, {
      width: '800px',
      disableClose: true,
      data: { 
        isEditing: false,
        sedeId: this.sedeSeleccionadaId,
        readOnly: !this.puedeOperar()
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) this.cargarProductos();
    });
  }

  abrirModalEditar(producto: Producto) {
    const dialogRef = this.dialog.open(ProductoDialogComponent, {
      width: '800px',
      disableClose: true,
      data: { 
        isEditing: true, 
        producto,
        sedeId: this.sedeSeleccionadaId,
        readOnly: !this.puedeOperar()
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) this.cargarProductos();
    });
  }

  confirmarCambioEstado(producto: Producto) {
    const accion = producto.activo ? 'desactivar' : 'activar';
    const nuevoEstado = !producto.activo;

    const dialogRef = this.dialog.open(ModalConfirmacionComponent, {
      width: '400px',
      data: {
        title: 'Confirmar Acción',
        message: '¿Está seguro que desea ' + accion + ' el producto "' + producto.nombre + '"?',
        confirmText: 'Sí, continuar',
        cancelText: 'Cancelar',
        isDestructive: producto.activo
      }
    });

    dialogRef.afterClosed().subscribe(confirmado => {
      if (confirmado) {
        this.productoService.cambiarEstado(producto.id, nuevoEstado).subscribe({
          next: () => {
            this.mostrarMensaje('Producto ' + accion + 'do exitosamente');
            this.cargarProductos();
          },
          error: () => this.mostrarMensaje('Error al cambiar estado')
        });
      }
    });
  }

  private mostrarMensaje(mensaje: string) {
    this.snackBar.open(mensaje, 'Cerrar', { duration: 3000 });
  }

  abrirGestionCatalogos() {
    this.dialog.open(GestionCatalogosDialogComponent, {
      width: '750px',
      disableClose: false
    });
  }

  editarStockMinimo(producto: Producto) {
    if (!this.puedeOperar()) {
      this.mostrarMensaje('No tiene permisos para modificar el stock en esta sede');
      return;
    }

    const dialogRef = this.dialog.open(StockMinimoDialogComponent, {
      width: '400px',
      data: { 
        productoNombre: producto.nombre, 
        stockMinimoActual: producto.stockMinimo || 0 
      }
    });

    dialogRef.afterClosed().subscribe(nuevoStock => {
      if (nuevoStock !== undefined && nuevoStock !== null) {
        const sedeId = this.sedeSeleccionadaId;
        this.inventarioService.actualizarStockMinimo(producto.id, sedeId, nuevoStock).subscribe({
          next: () => {
            this.mostrarMensaje('Stock mínimo actualizado');
            // Forzar actualización inmediata de la lista
            this.cargarProductos();
          },
          error: () => this.mostrarMensaje('Error al actualizar stock mínimo')
        });
      }
    });
  }
}
