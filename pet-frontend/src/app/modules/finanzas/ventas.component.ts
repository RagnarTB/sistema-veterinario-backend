import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { VentaService } from '../../core/services/venta.service';
import { VentaResponse, SedeResponse } from '../../core/models/models';
import { SedeService } from '../../core/services/sede.service';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { VentaDetalleDialogComponent } from './venta-detalle-dialog/venta-detalle-dialog.component';


@Component({
  selector: 'app-ventas',
  standalone: true,
  imports: [CommonModule, FormsModule, MatSnackBarModule, MatDialogModule],
  templateUrl: './ventas.component.html',
  styleUrls: ['./ventas.component.css']
})
export class VentasComponent implements OnInit {
  private ventaService = inject(VentaService);
  private sedeService = inject(SedeService);
  private snack = inject(MatSnackBar);
  private dialog = inject(MatDialog);

  sedes: SedeResponse[] = [];
  ventas: VentaResponse[] = [];
  cargando = signal(false);

  // Paginación
  page = 0;
  size = 15;
  totalElements = 0;
  totalPages = 0;

  // Filtros
  searchQuery = '';
  filtroEstado = '';

  ngOnInit() {
    this.sedeService.listarActivas().subscribe({
      next: (res) => {
        this.sedes = res;
        this.cargarVentas();
      },
      error: () => this.cargarVentas()
    });
  }

  cargarVentas() {
    this.cargando.set(true);
    this.ventaService.listar(this.page, this.size).subscribe({
      next: (res) => {
        this.ventas = (res.content || []).filter(v => {
          const matchQuery = !this.searchQuery.trim() ||
            (v.clienteNombre || '').toLowerCase().includes(this.searchQuery.toLowerCase()) ||
            String(v.id).includes(this.searchQuery);
          const matchEstado = !this.filtroEstado || v.estado === this.filtroEstado;
          return matchQuery && matchEstado;
        });
        this.totalElements = res.totalElements || 0;
        this.totalPages = res.totalPages || 0;
        this.cargando.set(false);
      },
      error: () => {
        this.snack.open('Error al cargar el historial de ventas', 'Cerrar', { duration: 3000, panelClass: ['snack-error'] });
        this.cargando.set(false);
      }
    });
  }

  buscar() {
    this.page = 0;
    this.cargarVentas();
  }

  limpiarFiltros() {
    this.searchQuery = '';
    this.filtroEstado = '';
    this.buscar();
  }

  cambiarPagina(newPage: number) {
    if (newPage >= 0 && newPage < this.totalPages) {
      this.page = newPage;
      this.cargarVentas();
    }
  }

  abrirDetalle(venta: VentaResponse) {
    this.dialog.open(VentaDetalleDialogComponent, {
      width: '600px',
      maxWidth: '95vw',
      data: venta,
      disableClose: false,
      autoFocus: false,
      panelClass: 'modern-dialog'
    });
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

  get pagesArray(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i);
  }

  getTotalVentas(): number {
    return this.ventas.reduce((acc, v) => acc + v.total, 0);
  }

  getTotalCobrado(): number {
    return this.ventas.reduce((acc, v) => acc + v.montoPagado, 0);
  }
}
