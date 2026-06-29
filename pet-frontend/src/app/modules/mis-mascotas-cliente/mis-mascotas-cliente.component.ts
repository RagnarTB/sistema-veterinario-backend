import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ClienteService } from '../../core/services/cliente.service';
import { PacienteResponse } from '../../core/models/models';
import { MascotaDetalleDialogComponent } from './mascota-detalle-dialog.component';

@Component({
  selector: 'app-mis-mascotas-cliente',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    MatButtonModule,
    MatDialogModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule
  ],
  templateUrl: './mis-mascotas-cliente.component.html',
  styleUrl: './mis-mascotas-cliente.component.css'
})
export class MisMascotasClienteComponent implements OnInit {
  private clienteService = inject(ClienteService);
  private dialog = inject(MatDialog);
  private snack = inject(MatSnackBar);

  mascotas = signal<PacienteResponse[]>([]);
  loading = signal(false);

  ngOnInit(): void {
    this.cargarMascotas();
  }

  cargarMascotas(): void {
    this.loading.set(true);
    this.clienteService.listarMisMascotas().subscribe({
      next: (mascotas) => {
        this.mascotas.set(mascotas);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.snack.open('No se pudieron cargar tus mascotas', 'Cerrar', { duration: 3500 });
      }
    });
  }

  abrirDetalle(mascota: PacienteResponse): void {
    this.dialog.open(MascotaDetalleDialogComponent, {
      width: '920px',
      maxWidth: '95vw',
      data: { mascota },
      autoFocus: false
    });
  }

  inicial(nombre: string): string {
    return nombre?.trim().charAt(0).toUpperCase() || 'M';
  }
}
