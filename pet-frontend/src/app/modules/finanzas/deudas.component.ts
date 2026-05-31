import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { FinanzasService } from '../../core/services/finanzas.service';
import { VentaService } from '../../core/services/venta.service';
import { SedeService } from '../../core/services/sede.service';
import { VentaResponse, SedeResponse, MetodoPago } from '../../core/models/models';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { PagoDeudaDialogComponent } from './pago-deuda-dialog/pago-deuda-dialog.component';

@Component({
  selector: 'app-deudas',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatSnackBarModule,
    MatDialogModule
  ],
  templateUrl: './deudas.component.html',
  styleUrls: ['./deudas.component.css']
})
export class DeudasComponent implements OnInit {
  private finanzasService = inject(FinanzasService);
  private ventaService = inject(VentaService);
  private sedeService = inject(SedeService);
  private snack = inject(MatSnackBar);
  private dialog = inject(MatDialog);

  sedeId = 1;
  sedes: SedeResponse[] = [];
  deudas: VentaResponse[] = [];
  cargando = signal<boolean>(false);

  // Paginación
  page = 0;
  size = 10;
  totalElements = 0;
  totalPages = 0;
  searchQuery = '';

  // Modal Pago
  cargandoPago = false;

  ngOnInit() {
    this.sedeService.listarActivas().subscribe({
      next: (res) => {
        this.sedes = res;
        const storedSede = Number(localStorage.getItem('vet_sede_id'));
        if (storedSede && this.sedes.some(s => s.id === storedSede)) {
          this.sedeId = storedSede;
        } else if (this.sedes.length > 0) {
          this.sedeId = this.sedes[0].id;
          localStorage.setItem('vet_sede_id', this.sedeId.toString());
        }
        this.cargarDeudas();
      },
      error: () => {
        this.sedes = [];
        this.sedeId = Number(localStorage.getItem('vet_sede_id')) || 1;
        this.cargarDeudas();
      }
    });
  }

  onSedeChange() {
    localStorage.setItem('vet_sede_id', this.sedeId.toString());
    this.page = 0;
    this.cargarDeudas();
  }

  cargarDeudas() {
    this.cargando.set(true);
    this.finanzasService.listarDeudas(this.sedeId, this.searchQuery, this.page, this.size).subscribe({
      next: (res) => {
        this.deudas = res.content || [];
        this.totalElements = res.totalElements || 0;
        this.totalPages = res.totalPages || 0;
        this.cargando.set(false);
      },
      error: (err) => {
        this.snack.open('Error al cargar las deudas de la sede', 'Cerrar', { duration: 3000, panelClass: ['snack-error'] });
        this.cargando.set(false);
      }
    });
  }

  buscar() {
    this.page = 0;
    this.cargarDeudas();
  }

  limpiarBusqueda() {
    this.searchQuery = '';
    this.buscar();
  }

  cambiarPagina(newPage: number) {
    if (newPage >= 0 && newPage < this.totalPages) {
      this.page = newPage;
      this.cargarDeudas();
    }
  }

  abrirModalPago(venta: VentaResponse) {
    const dialogRef = this.dialog.open(PagoDeudaDialogComponent, {
      width: '500px',
      maxWidth: '95vw',
      data: { venta: venta, sedeId: this.sedeId },
      disableClose: false,
      autoFocus: false,
      panelClass: 'modern-dialog'
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.cargarDeudas();
      }
    });
  }
}