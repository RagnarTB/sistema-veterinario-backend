import { Component, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { debounceTime, distinctUntilChanged } from 'rxjs';

import { SedeService } from '../../core/services/sede.service';
import { SedeResponse } from '../../core/models/models';
import { SedeDialogComponent } from './sede-dialog.component';
import { ModalConfirmacionComponent } from '../../shared/components/modal-confirmacion/modal-confirmacion.component';

@Component({
  selector: 'app-sedes',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    MatSnackBarModule 
  ],
  templateUrl: './sedes.component.html',
  styleUrls: ['./sedes.component.css']
})
export class SedesComponent implements OnInit {
  
  displayedColumns: string[] = ['nombre', 'telefono', 'estado', 'acciones'];
  
  dataSource = signal<SedeResponse[]>([]);
  totalElements = signal(0);
  pageSize = signal(10);
  pageIndex = signal(0);
  loading = signal(false);

  searchControl = new FormControl('');

  constructor(
    private sedeService: SedeService,
    private dialog: MatDialog,
    private snack: MatSnackBar
  ) {
    this.searchControl.valueChanges.pipe(
      debounceTime(400),
      distinctUntilChanged()
    ).subscribe(() => {
      this.pageIndex.set(0); 
      this.cargarSedes();
    });
  }

  ngOnInit(): void {
    this.cargarSedes();
  }

  cargarSedes(): void {
    this.loading.set(true);
    const searchTerm = this.searchControl.value || '';
    
    this.sedeService.listar(this.pageIndex(), this.pageSize(), searchTerm).subscribe({
      next: (page: any) => {
        this.dataSource.set(page.content);
        this.totalElements.set(page.totalElements);
        this.loading.set(false);
      },
      error: () => {
        this.snack.open('Error al cargar sedes', 'Cerrar', { duration: 3000 });
        this.loading.set(false);
      }
    });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.cargarSedes();
  }

  abrirModal(sede?: SedeResponse): void {
    const dialogRef = this.dialog.open(SedeDialogComponent, {
      width: '500px',
      data: { sede },
      disableClose: true
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) this.cargarSedes();
    });
  }

  cambiarEstado(sede: SedeResponse): void {
    const nuevoEstado = !sede.activo;
    
    const dialogRef = this.dialog.open(ModalConfirmacionComponent, {
      width: '400px',
      data: {
        title: nuevoEstado ? 'Activar Sede' : 'Desactivar Sede',
        message: `¿Estás seguro de ${nuevoEstado ? 'activar' : 'desactivar'} la sede ${sede.nombre}?`,
        confirmText: nuevoEstado ? 'Sí, Activar' : 'Sí, Desactivar',
        isDestructive: !nuevoEstado
      }
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed) {
        this.loading.set(true);
        this.sedeService.cambiarEstado(sede.id, nuevoEstado).subscribe({
          next: () => {
            this.snack.open(`Sede ${nuevoEstado ? 'activada' : 'desactivada'} con éxito`, 'Cerrar', { duration: 3000 });
            this.cargarSedes();
          },
          error: () => {
            this.snack.open(`Error al cambiar estado de la sede`, 'Cerrar', { duration: 3000 });
            this.loading.set(false);
          }
        });
      }
    });
  }

  eliminarFisico(sede: SedeResponse): void {
    const dialogRef = this.dialog.open(ModalConfirmacionComponent, {
      width: '400px',
      data: {
        title: 'Eliminar Sede',
        message: `¿Está completamente seguro de eliminar la sede ${sede.nombre}? Esta acción no se puede deshacer y fallará si hay empleados asignados.`,
        confirmText: 'Sí, Eliminar',
        isDestructive: true
      }
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed) {
        this.loading.set(true);
        this.sedeService.eliminar(sede.id).subscribe({
          next: () => {
            this.snack.open('Sede eliminada permanentemente', 'Cerrar', { duration: 3000 });
            this.cargarSedes();
          },
          error: (err: any) => {
            this.snack.open(err.error?.mensaje || 'Error al eliminar sede. Es posible que esté en uso.', 'Cerrar', { duration: 4000 });
            this.loading.set(false);
          }
        });
      }
    });
  }
}


