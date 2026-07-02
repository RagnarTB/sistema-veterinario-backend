import { Component, OnInit, ViewChild, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { debounceTime, distinctUntilChanged } from 'rxjs';

import { MatTableModule } from '@angular/material/table';
import { MatPaginator, MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTabsModule } from '@angular/material/tabs';

import { ClienteService } from '../../core/services/cliente.service';
import { AuthService } from '../../core/services/auth.service';
import { ClienteResponse } from '../../core/models/models';
import { ModalConfirmacionComponent } from '../../shared/components/modal-confirmacion/modal-confirmacion.component';
import { ClienteDialogComponent } from './cliente-dialog/cliente-dialog.component';

@Component({
  selector: 'app-clientes',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatFormFieldModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatDialogModule,
    MatSnackBarModule,
    MatTabsModule
  ],
  templateUrl: './clientes.component.html',
  styleUrls: ['./clientes.component.css']
})
export class ClientesComponent implements OnInit {
  private clienteService = inject(ClienteService);
  private authService = inject(AuthService);
  private dialog = inject(MatDialog);
  private snack = inject(MatSnackBar);

  displayedColumns: string[] = ['nombre', 'contacto', 'estado', 'acciones'];
  dataSource = signal<ClienteResponse[]>([]);
  totalElements = signal(0);
  pageSize = signal(10);
  pageIndex = signal(0);
  loading = signal(false);
  estadoActual = signal<boolean | null>(true); // true: Activos, false: Inactivos, null: Todos

  searchControl = new FormControl('');
  isAdmin = this.authService.isAdmin();
  canManageClientes = this.authService.hasPermission('GESTIONAR_CLIENTES');

  ngOnInit(): void {
    this.cargarClientes();

    this.searchControl.valueChanges
      .pipe(debounceTime(400), distinctUntilChanged())
      .subscribe(() => {
        this.pageIndex.set(0);
        this.cargarClientes();
      });
  }

  cargarClientes() {
    this.loading.set(true);
    const search = this.searchControl.value || '';

    // Suponiendo que tu servicio ha sido actualizado para recibir el estado
    // this.clienteService.listar(this.pageIndex(), this.pageSize(), search, this.estadoActual())
    this.clienteService.listar(this.pageIndex(), this.pageSize(), search, this.estadoActual()).subscribe({
      next: (pageData) => {
        this.dataSource.set(pageData.content);
        this.totalElements.set(pageData.totalElements);
        this.loading.set(false);
      },
      error: () => {
        this.snack.open('Error al cargar la lista de clientes', 'Cerrar', { duration: 3000 });
        this.loading.set(false);
      }
    });
  }

  onTabChange(index: number) {
    if (index === 0) {
      this.estadoActual.set(true);
    } else if (index === 1) {
      this.estadoActual.set(false);
    } else {
      this.estadoActual.set(null);
    }
    this.pageIndex.set(0);
    this.cargarClientes();
  }

  onPageChange(event: PageEvent) {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.cargarClientes();
  }

  abrirModalCliente(cliente?: ClienteResponse) {
    const dialogRef = this.dialog.open(ClienteDialogComponent, {
      width: '600px',
      data: cliente || null,
      disableClose: true
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loading.set(true);
        if (cliente) {
          this.clienteService.actualizar(cliente.id, result).subscribe({
            next: () => {
              this.snack.open('Cliente actualizado correctamente', 'Cerrar', { duration: 3000 });
              this.cargarClientes();
            },
            error: (err) => {
              this.loading.set(false);
              this.snack.open(err.error?.mensaje || 'Error al actualizar', 'Cerrar', { duration: 4000 });
            }
          });
        } else {
          this.clienteService.crear(result).subscribe({
            next: () => {
              this.snack.open('Cliente creado correctamente', 'Cerrar', { duration: 3000 });
              this.cargarClientes();
            },
            error: (err) => {
              this.loading.set(false);
              this.snack.open(err.error?.mensaje || 'Error al crear', 'Cerrar', { duration: 4000 });
            }
          });
        }
      }
    });
  }

  cambiarEstado(cliente: ClienteResponse) {
    const nuevoEstado = !cliente.activo;
    const accion = nuevoEstado ? 'concederá acceso' : 'revocará el acceso';

    const dialogRef = this.dialog.open(ModalConfirmacionComponent, {
      width: '400px',
      data: {
        title: nuevoEstado ? 'Activar Cliente' : 'Desactivar Cliente',
        message: `¿Estás seguro de ${nuevoEstado ? 'activar' : 'desactivar'} a ${cliente.nombre} ${cliente.apellido}? Se le ${accion} al sistema.`,
        confirmText: nuevoEstado ? 'Sí, Activar' : 'Sí, Desactivar',
        isDestructive: !nuevoEstado
      }
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed) {
        this.loading.set(true);
        this.clienteService.cambiarEstado(cliente.id, nuevoEstado).subscribe({
          next: () => {
            this.snack.open(`Cliente ${nuevoEstado ? 'activado' : 'desactivado'}`, 'Cerrar', { duration: 3000 });
            this.cargarClientes();
          },
          error: () => {
            this.loading.set(false);
            this.snack.open('Error al cambiar el estado', 'Cerrar', { duration: 3000 });
          }
        });
      }
    });
  }
}
