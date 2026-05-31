import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { JaulaService, JaulaResponse } from '../../../core/services/jaula.service';
import { AuthService } from '../../../core/services/auth.service';
import { JaulaDialogComponent } from './jaula-dialog.component';
import { SedeService } from '../../../core/services/sede.service';
import { SedeResponse } from '../../../core/models/models';
import { ModalConfirmacionComponent } from '../../../shared/components/modal-confirmacion/modal-confirmacion.component';
import { FormsModule } from '@angular/forms';
import { signal } from '@angular/core';

@Component({
  selector: 'app-lista-jaulas',
  standalone: true,
  imports: [CommonModule, MatDialogModule, FormsModule],
  templateUrl: './lista-jaulas.html',
  styleUrls: ['./lista-jaulas.css']
})
export class ListaJaulasComponent implements OnInit {
  jaulas: JaulaResponse[] = [];
  cargando = true;
  sedeActualId: number = 1;
  sedes = signal<SedeResponse[]>([]);

  private jaulaService = inject(JaulaService);
  private authService = inject(AuthService);
  private sedeService = inject(SedeService);
  private dialog = inject(MatDialog);

  ngOnInit(): void {
    this.sedeActualId = Number(localStorage.getItem('vet_sede_id')) || 1;
    this.cargarSedes();
    this.cargarJaulas();
  }

  cargarSedes() {
    this.sedeService.listarActivas().subscribe({
      next: (res) => this.sedes.set(res),
      error: () => {}
    });
  }

  onSedeChange() {
    this.sedeActualId = Number(this.sedeActualId);
    this.cargarJaulas();
  }

  cargarJaulas(): void {
    if (!this.sedeActualId) return;
    this.cargando = true;
    this.jaulaService.listarJaulasPorSede(this.sedeActualId).subscribe({
      next: (data) => {
        this.jaulas = data;
        this.cargando = false;
      },
      error: (err) => {
        console.error('Error cargando jaulas', err);
        this.cargando = false;
      }
    });
  }

  abrirDialogoJaula(jaula?: JaulaResponse) {
    const dialogRef = this.dialog.open(JaulaDialogComponent, {
      width: '500px',
      data: { jaula, sedeId: this.sedeActualId }
    });

    dialogRef.afterClosed().subscribe(res => {
      if (res) this.cargarJaulas();
    });
  }

  cambiarEstado(jaulaId: number, nuevoEstado: string): void {
    const dialogRef = this.dialog.open(ModalConfirmacionComponent, {
      width: '400px',
      data: {
        title: 'Confirmar Acción',
        message: `¿Está seguro de cambiar la jaula a estado ${nuevoEstado}?`,
        confirmText: 'Mover'
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.jaulaService.cambiarEstado(jaulaId, nuevoEstado).subscribe({
          next: () => this.cargarJaulas(),
          error: (err) => console.error('Error al cambiar estado', err)
        });
      }
    });
  }

  eliminar(jaulaId: number): void {
    const dialogRef = this.dialog.open(ModalConfirmacionComponent, {
      width: '400px',
      data: {
        title: 'Eliminar Jaula',
        message: '¿Está seguro de eliminar esta jaula?',
        confirmText: 'Eliminar',
        isDestructive: true
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.jaulaService.eliminar(jaulaId).subscribe({
          next: () => this.cargarJaulas(),
          error: (err) => alert('No se puede eliminar la jaula. Puede que tenga historiales asociados.')
        });
      }
    });
  }

  getEstadoStyle(estado: string): Record<string, string> {
    switch(estado) {
      case 'DISPONIBLE': return { 'background-color': '#dcfce7', 'color': '#166534', 'border': '1px solid #bbf7d0' };
      case 'OCUPADA': return { 'background-color': '#dbeafe', 'color': '#1e40af', 'border': '1px solid #bfdbfe' };
      case 'EN_DESINFECCION': return { 'background-color': '#ffedd5', 'color': '#9a3412', 'border': '1px solid #fed7aa' };
      case 'MANTENIMIENTO': return { 'background-color': '#f1f5f9', 'color': '#475569', 'border': '1px solid #e2e8f0' };
      case 'BLOQUEO_CUARENTENA': return { 'background-color': '#fee2e2', 'color': '#991b1b', 'border': '1px solid #fecaca' };
      default: return { 'background-color': '#f1f5f9', 'color': '#475569', 'border': '1px solid #e2e8f0' };
    }
  }
}