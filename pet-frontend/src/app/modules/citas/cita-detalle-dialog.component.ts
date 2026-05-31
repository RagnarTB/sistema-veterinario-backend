import { Component, Inject, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';

import { CitaService } from '../../core/services/cita.service';
import { AuthService } from '../../core/services/auth.service';
import { CitaResponse, EstadoCita } from '../../core/models/models';
import { ESTADO_LABELS, ESTADO_COLORS, ESTADO_ICONS, getTransicionesPermitidas, esEstadoFinal } from './cita-estado.util';
import { ModalConfirmacionComponent } from '../../shared/components/modal-confirmacion/modal-confirmacion.component';

const ESTADOS_ORDEN: EstadoCita[] = ['AGENDADA', 'CONFIRMADA', 'EN_SALA_ESPERA', 'EN_CONSULTORIO', 'COMPLETADA'];

@Component({
  selector: 'app-cita-detalle-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatTooltipModule],
  templateUrl: './cita-detalle-dialog.component.html',
  styleUrls: ['./cita-detalle-dialog.component.css']
})
export class CitaDetalleDialogComponent {
  private citaService = inject(CitaService);
  private authService = inject(AuthService);
  private matDialog = inject(MatDialog);
  private snack = inject(MatSnackBar);

  cita: CitaResponse;
  transiciones: EstadoCita[];
  estadosOrden = ESTADOS_ORDEN;

  getEstadoLabel = (e: EstadoCita) => ESTADO_LABELS[e];
  getEstadoColor = (e: EstadoCita) => ESTADO_COLORS[e];
  getEstadoIcon = (e: EstadoCita) => ESTADO_ICONS[e];

  constructor(
    public dialogRef: MatDialogRef<CitaDetalleDialogComponent>,
    @Inject(MAT_DIALOG_DATA) data: CitaResponse
  ) {
    this.cita = data;
    this.transiciones = getTransicionesPermitidas(data.estado);
  }

  getEstadoIndex(e: EstadoCita): number { return ESTADOS_ORDEN.indexOf(e); }
  isEstadoCancelado(): boolean { return this.cita.estado === 'CANCELADA' || this.cita.estado === 'NO_ASISTIO'; }
  isDestructive(e: EstadoCita): boolean { return e === 'CANCELADA' || e === 'NO_ASISTIO'; }

  cambiarEstado(nuevo: EstadoCita): void {
    const destructive = this.isDestructive(nuevo);
    const ref = this.matDialog.open(ModalConfirmacionComponent, {
      width: '400px',
      data: {
        title: `Cambiar a ${ESTADO_LABELS[nuevo]}`,
        message: destructive
          ? `¿Está seguro de marcar esta cita como "${ESTADO_LABELS[nuevo]}"? Esta acción no se puede revertir.`
          : `¿Confirma cambiar el estado de la cita a "${ESTADO_LABELS[nuevo]}"?`,
        confirmText: ESTADO_LABELS[nuevo],
        isDestructive: destructive
      }
    });

    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.citaService.cambiarEstado(this.cita.id, nuevo).subscribe({
        next: () => {
          this.snack.open(`Estado cambiado a ${ESTADO_LABELS[nuevo]}`, 'OK', { duration: 2000 });
          this.dialogRef.close('refresh');
        },
        error: err => this.snack.open(err.error?.message || 'Error al cambiar estado', 'Cerrar', { duration: 3000 })
      });
    });
  }

  retirarDeAgenda(): void {
    const ref = this.matDialog.open(ModalConfirmacionComponent, {
      width: '400px',
      data: {
        title: 'Retirar Cita de Agenda',
        message: '¿Está seguro de cancelar esta cita agendada? Se retirará de la agenda.',
        confirmText: 'Sí, Retirar',
        isDestructive: true
      }
    });

    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.citaService.eliminar(this.cita.id).subscribe({
        next: () => {
          this.snack.open('Cita retirada de la agenda', 'OK', { duration: 2000 });
          this.dialogRef.close('refresh');
        },
        error: err => this.snack.open(err.error?.message || 'Error al cancelar', 'Cerrar', { duration: 3000 })
      });
    });
  }
}
