import { Component, signal, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { RangoPesoJaulaService, RangoPesoJaula } from '../../../../core/services/rango-peso-jaula.service';
import { EspecieService } from '../../../../core/services/especie.service';
import { EspecieResponse } from '../../../../core/models/models';
import { TamanoJaulaService, TamanoJaula } from '../../../../core/services/tamano-jaula.service';

@Component({
  selector: 'app-rango-peso-jaula-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatButtonModule, MatDialogModule, MatProgressSpinnerModule],
  template: `
    <div style="padding: 24px; font-family: system-ui, -apple-system, sans-serif;">
      <h2 style="margin: 0 0 16px 0; font-size: 20px; font-weight: 600; color: #1e293b;">
        {{ data ? 'Editar Rango de Peso' : 'Nuevo Rango de Peso' }}
      </h2>
      <form [formGroup]="form" (ngSubmit)="guardar()">
        <div style="margin-bottom: 16px;">
          <label style="display: block; font-size: 13px; font-weight: 500; color: #475569; margin-bottom: 6px;">Especie</label>
          <select formControlName="especieId" style="width: 100%; padding: 10px 12px; background: white; border: 1px solid #cbd5e1; border-radius: 8px; outline: none; font-size: 14px; box-sizing: border-box;">
            <option value="">Seleccione una especie</option>
            <option *ngFor="let esp of especies()" [value]="esp.id">{{esp.nombre}}</option>
          </select>
        </div>
        <div style="margin-bottom: 16px; display: flex; gap: 16px;">
          <div style="flex: 1;">
            <label style="display: block; font-size: 13px; font-weight: 500; color: #475569; margin-bottom: 6px;">Peso Mínimo (kg)</label>
            <input formControlName="pesoMinimo" type="number" step="0.01"
              style="width: 100%; padding: 10px 12px; background: white; border: 1px solid #cbd5e1; border-radius: 8px; outline: none; font-size: 14px; box-sizing: border-box;">
          </div>
          <div style="flex: 1;">
            <label style="display: block; font-size: 13px; font-weight: 500; color: #475569; margin-bottom: 6px;">Peso Máximo (kg)</label>
            <input formControlName="pesoMaximo" type="number" step="0.01"
              style="width: 100%; padding: 10px 12px; background: white; border: 1px solid #cbd5e1; border-radius: 8px; outline: none; font-size: 14px; box-sizing: border-box;">
          </div>
        </div>
        <div style="margin-bottom: 24px;">
          <label style="display: block; font-size: 13px; font-weight: 500; color: #475569; margin-bottom: 6px;">Tamaño Sugerido</label>
          <select formControlName="tamanoJaulaId" style="width: 100%; padding: 10px 12px; background: white; border: 1px solid #cbd5e1; border-radius: 8px; outline: none; font-size: 14px; box-sizing: border-box;">
            <option value="">Seleccione un tamaño</option>
            <option *ngFor="let tam of tamanos()" [value]="tam.id">{{tam.nombre}}</option>
          </select>
        </div>
        
        <div style="display: flex; justify-content: flex-end; gap: 12px;">
          <button type="button" mat-button (click)="dialogRef.close()" style="border-radius: 8px;">Cancelar</button>
          <button type="submit" mat-flat-button color="primary" style="border-radius: 8px;" [disabled]="form.invalid || saving()">
            <mat-spinner *ngIf="saving()" diameter="20" style="display: inline-block; margin-right: 8px;"></mat-spinner>
            Guardar
          </button>
        </div>
      </form>
    </div>
  `
})
export class RangoPesoJaulaDialogComponent implements OnInit {
  data = inject(MAT_DIALOG_DATA) as RangoPesoJaula | null;
  dialogRef = inject(MatDialogRef);
  fb = inject(FormBuilder);
  servicio = inject(RangoPesoJaulaService);
  especieService = inject(EspecieService);
  tamanoService = inject(TamanoJaulaService);
  snackBar = inject(MatSnackBar);

  saving = signal(false);
  especies = signal<EspecieResponse[]>([]);
  tamanos = signal<TamanoJaula[]>([]);

  form: FormGroup = this.fb.group({
    especieId: [this.data?.especie?.id || '', Validators.required],
    pesoMinimo: [this.data?.pesoMinimo || 0, [Validators.required, Validators.min(0)]],
    pesoMaximo: [this.data?.pesoMaximo || 0, [Validators.required, Validators.min(0)]],
    tamanoJaulaId: [this.data?.tamanoJaula?.id || '', Validators.required]
  });

  ngOnInit() {
    this.especieService.listar().subscribe(res => this.especies.set(res));
    this.tamanoService.listar().subscribe(res => this.tamanos.set(res.filter(t => t.activo)));
  }

  guardar() {
    if (this.form.invalid) return;
    this.saving.set(true);

    const val = this.form.value;
    const payload: any = {
      especie: { id: val.especieId },
      pesoMinimo: val.pesoMinimo,
      pesoMaximo: val.pesoMaximo,
      tamanoJaula: { id: val.tamanoJaulaId }
    };

    const request = this.data?.id 
      ? this.servicio.actualizar(this.data.id, payload)
      : this.servicio.crear(payload);

    request.subscribe({
      next: (res) => {
        this.snackBar.open('Rango guardado', 'Cerrar', { duration: 3000 });
        this.dialogRef.close(true);
      },
      error: () => {
        this.snackBar.open('Error al guardar', 'Cerrar', { duration: 3000 });
        this.saving.set(false);
      }
    });
  }
}

@Component({
  selector: 'app-rango-peso-jaula',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    MatSnackBarModule
  ],
  templateUrl: './rango-peso-jaula.component.html',
  styleUrls: ['./rango-peso-jaula.component.css']
})
export class RangoPesoJaulaComponent implements OnInit {
  displayedColumns: string[] = ['especie', 'rango', 'tamano', 'acciones'];
  dataSource = signal<RangoPesoJaula[]>([]);
  loading = signal(true);

  private servicio = inject(RangoPesoJaulaService);
  private dialog = inject(MatDialog);

  ngOnInit() {
    this.cargarDatos();
  }

  cargarDatos() {
    this.loading.set(true);
    this.servicio.listar().subscribe({
      next: (data) => {
        this.dataSource.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      }
    });
  }

  eliminar(id: number) {
    if (confirm('¿Está seguro de eliminar este rango de peso?')) {
      this.servicio.eliminar(id).subscribe({
        next: () => this.cargarDatos(),
        error: (err) => alert('No se pudo eliminar.')
      });
    }
  }

  abrirModal(rango?: RangoPesoJaula) {
    const dialogRef = this.dialog.open(RangoPesoJaulaDialogComponent, {
      width: '450px',
      data: rango
    });

    dialogRef.afterClosed().subscribe(res => {
      if (res) this.cargarDatos();
    });
  }
}

