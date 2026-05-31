import { Component, signal, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialogRef, MatDialogModule, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

import { HospitalizacionService } from '../../core/services/hospitalizacion.service';
import { PacienteService } from '../../core/services/paciente.service';
import { PacienteResponse } from '../../core/models/models';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-hospitalizacion-ingreso-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatButtonModule, MatDialogModule, MatProgressSpinnerModule, MatIconModule],
  template: `
    <div style="padding: 24px; font-family: system-ui, -apple-system, sans-serif;">
      <h2 style="margin: 0 0 16px 0; font-size: 20px; font-weight: 600; color: #1e293b;">
        Ingresar Paciente a Hospitalización
      </h2>

      <!-- Paso 1: Seleccionar Paciente -->
      <div *ngIf="paso() === 1">
        <div style="margin-bottom: 16px;">
          <label style="display: block; font-size: 13px; font-weight: 500; color: #475569; margin-bottom: 6px;">Paciente</label>
          <select [formControl]="pacienteCtrl" style="width: 100%; padding: 10px 12px; background: white; border: 1px solid #cbd5e1; border-radius: 8px; outline: none; font-size: 14px;">
            <option value="">Seleccione un paciente</option>
            <option *ngFor="let pac of pacientes()" [value]="pac.id">{{pac.nombre}} ({{pac.especie}} - {{pac.clienteNombre}})</option>
          </select>
        </div>
        <div style="display: flex; justify-content: flex-end; gap: 12px; margin-top: 24px;">
            <button type="button" mat-button (click)="dialogRef.close()" style="border-radius: 8px;">Cancelar</button>
            <button type="button" mat-flat-button color="primary" style="border-radius: 8px;" [disabled]="!pacienteCtrl.value || loadingSugerencia()" (click)="obtenerSugerencia()">
                <mat-spinner *ngIf="loadingSugerencia()" diameter="20" style="display: inline-block; margin-right: 8px;"></mat-spinner>
                Continuar
            </button>
        </div>
      </div>

      <!-- Paso 2: Sugerencia Inteligente y Formulario -->
      <div *ngIf="paso() === 2">
          
          <div *ngIf="sugerencia()" style="background: #eef2ff; border: 1px solid #c7d2fe; border-radius: 12px; padding: 16px; margin-bottom: 20px;">
              <div style="display: flex; justify-content: space-between; align-items: flex-start; gap: 8px; margin-bottom: 8px;">
                <div style="display: flex; align-items: center; gap: 8px; color: #4f46e5; font-weight: 600;">
                    <mat-icon>auto_awesome</mat-icon> Sugerencia del Sistema
                </div>
                <div *ngIf="sugerencia().ultimoPeso === 0" style="color: #ef4444; font-size: 12px; font-weight: bold; background: #fee2e2; padding: 2px 8px; border-radius: 4px;">
                  ⚠️ Paciente sin peso registrado
                </div>
              </div>
              
              <div style="margin-bottom: 12px; display: flex; align-items: center; gap: 12px;">
                <label style="font-size: 14px; font-weight: 500; color: #334155;">Peso Actual (kg):</label>
                <div style="display: flex; align-items: center; gap: 8px;">
                  <input type="number" [formControl]="pesoCtrl" step="0.1" min="0.1" style="width: 80px; padding: 6px 8px; background: white; border: 1px solid #cbd5e1; border-radius: 6px; outline: none; font-size: 14px;">
                  <button type="button" mat-button color="primary" (click)="recalcularSugerencia()" [disabled]="pesoCtrl.invalid || loadingSugerencia()" style="min-width: unset; padding: 0 12px; border-radius: 6px; height: 34px;">
                    <mat-icon style="font-size: 18px; width: 18px; height: 18px; margin-right: 4px;">refresh</mat-icon> Recalcular
                  </button>
                </div>
              </div>

              <div *ngIf="loadingSugerencia()" style="display: flex; align-items: center; gap: 8px; color: #64748b; font-size: 13px;">
                <mat-spinner diameter="16"></mat-spinner> Recalculando...
              </div>

              <div *ngIf="!loadingSugerencia()">
                <p style="margin: 0; font-size: 14px; color: #334155;" *ngIf="sugerencia().tamanoSugeridoNombre">
                    Tamaño ideal de jaula: <span style="background: #4f46e5; color: white; padding: 2px 8px; border-radius: 999px; font-size: 12px;">{{ sugerencia().tamanoSugeridoNombre }}</span>
                </p>
                <p style="margin: 0; font-size: 14px; color: #ef4444;" *ngIf="!sugerencia().tamanoSugeridoNombre">
                    No se encontró un tamaño sugerido para este peso.
                </p>
              </div>
          </div>

          <form [formGroup]="form" (ngSubmit)="guardar()">
              <div style="margin-bottom: 16px;">
                  <label style="display: block; font-size: 13px; font-weight: 500; color: #475569; margin-bottom: 6px;">Seleccionar Jaula Disponible</label>
                  <select formControlName="jaulaId" style="width: 100%; padding: 10px 12px; background: white; border: 1px solid #cbd5e1; border-radius: 8px; outline: none; font-size: 14px;">
                      <option value="">Seleccione una jaula</option>
                      <option *ngFor="let j of sugerencia()?.jaulasDisponibles" [value]="j.id">
                          Jaula #{{j.numero}} ({{j.tamanoNombre}} - {{j.categoriaNombre}})
                      </option>
                  </select>
                  <div *ngIf="sugerencia()?.jaulasDisponibles?.length === 0" style="color: #ef4444; font-size: 12px; margin-top: 4px;">
                      No hay jaulas disponibles en la sede actual.
                  </div>
              </div>

              <div style="margin-bottom: 16px;">
                  <label style="display: block; font-size: 13px; font-weight: 500; color: #475569; margin-bottom: 6px;">Motivo de Hospitalización</label>
                  <textarea formControlName="motivo" rows="3" style="width: 100%; padding: 10px 12px; background: white; border: 1px solid #cbd5e1; border-radius: 8px; outline: none; font-size: 14px; resize: vertical;"></textarea>
              </div>

              <div style="margin-bottom: 16px;">
                  <label style="display: block; font-size: 13px; font-weight: 500; color: #475569; margin-bottom: 6px;">Frecuencia de Monitoreo (horas)</label>
                  <input formControlName="frecuenciaMonitoreoHoras" type="number" min="1" max="24" style="width: 100%; padding: 10px 12px; background: white; border: 1px solid #cbd5e1; border-radius: 8px; outline: none; font-size: 14px;">
              </div>
              
              <div style="display: flex; justify-content: space-between; margin-top: 24px;">
                  <button type="button" mat-button (click)="paso.set(1)" style="border-radius: 8px;">Atrás</button>
                  <div style="display: flex; gap: 12px;">
                      <button type="button" mat-button (click)="dialogRef.close()" style="border-radius: 8px;">Cancelar</button>
                      <button type="submit" mat-flat-button color="primary" style="border-radius: 8px;" [disabled]="form.invalid || pesoCtrl.invalid || saving()">
                          <mat-spinner *ngIf="saving()" diameter="20" style="display: inline-block; margin-right: 8px;"></mat-spinner>
                          Ingresar
                      </button>
                  </div>
              </div>
          </form>
      </div>

    </div>
  `
})
export class HospitalizacionIngresoDialogComponent implements OnInit {
  dialogRef = inject(MatDialogRef);
  dialogData: any = inject(MAT_DIALOG_DATA, { optional: true });
  fb = inject(FormBuilder);
  hospitalizacionService = inject(HospitalizacionService);
  pacienteService = inject(PacienteService);
  authService = inject(AuthService);
  snackBar = inject(MatSnackBar);

  paso = signal(1);
  pacientes = signal<any[]>([]);
  sugerencia = signal<any>(null);
  
  loadingSugerencia = signal(false);
  saving = signal(false);

  pacienteCtrl = this.fb.control('', Validators.required);
  pesoCtrl = this.fb.control<number | null>(null, [Validators.required, Validators.min(0.1)]);

  form: FormGroup = this.fb.group({
    jaulaId: ['', Validators.required],
    motivo: ['', Validators.required],
    frecuenciaMonitoreoHoras: [4, [Validators.required, Validators.min(1)]]
  });

  ngOnInit() {
    this.pacienteService.listar().subscribe(res => this.pacientes.set(res.content));
  }

  obtenerSugerencia() {
    if (this.pacienteCtrl.invalid) return;
    this.cargarDatosSugerencia();
  }

  recalcularSugerencia() {
    if (this.pesoCtrl.invalid) return;
    this.cargarDatosSugerencia(this.pesoCtrl.value);
  }

  private cargarDatosSugerencia(pesoForzado?: number | null) {
    this.loadingSugerencia.set(true);
    
    // Obtener la sede desde localStorage
    const sedeId = Number(localStorage.getItem('vet_sede_id')) || this.authService.currentSedeIds()[0] || 1; 

    // Enviar el pesoForzado al backend si existe
    this.hospitalizacionService.sugerirJaula(Number(this.pacienteCtrl.value), sedeId, pesoForzado || undefined).subscribe({
        next: (data) => {
            // Actualizar el control de peso con lo devuelto por el backend
            this.pesoCtrl.setValue(data.ultimoPeso || 0);
            
            this.sugerencia.set(data);
            
            if (data.jaulasDisponibles && data.jaulasDisponibles.length > 0) {
                const jaulaIdeal = data.jaulasDisponibles.find((j: any) => j.tamanoId === data.tamanoSugeridoId);
                if (jaulaIdeal) {
                    this.form.patchValue({ jaulaId: jaulaIdeal.id });
                } else {
                    this.form.patchValue({ jaulaId: '' });
                }
            }

            this.loadingSugerencia.set(false);
            if (this.paso() === 1) this.paso.set(2);
        },
        error: () => {
            this.snackBar.open('No se pudo obtener sugerencias', 'Cerrar', { duration: 3000 });
            this.loadingSugerencia.set(false);
        }
    });
  }

  guardar() {
    if (this.form.invalid || this.pesoCtrl.invalid) return;
    this.saving.set(true);

    const empleadoId = 1; // Por defecto o extraer del Auth Token JWT userId

    const payload = {
        pacienteId: Number(this.pacienteCtrl.value),
        jaulaId: Number(this.form.value.jaulaId),
        motivoIngreso: this.form.value.motivo,
        frecuenciaMonitoreoHoras: this.form.value.frecuenciaMonitoreoHoras,
        pesoActual: this.pesoCtrl.value,
        empleadoId: empleadoId
    };

    this.hospitalizacionService.ingresar(payload).subscribe({
        next: () => {
            this.snackBar.open('Paciente ingresado exitosamente', 'Cerrar', { duration: 3000 });
            this.dialogRef.close(true);
        },
        error: () => {
            this.snackBar.open('Error al ingresar', 'Cerrar', { duration: 3000 });
            this.saving.set(false);
        }
    });
  }
}