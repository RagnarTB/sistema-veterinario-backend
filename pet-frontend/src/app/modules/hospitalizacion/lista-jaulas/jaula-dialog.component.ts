import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

import { JaulaService, JaulaResponse } from '../../../core/services/jaula.service';
import { CategoriaJaulaService } from '../../../core/services/categoria-jaula.service';
import { TamanoJaulaService } from '../../../core/services/tamano-jaula.service';

@Component({
  selector: 'app-jaula-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatButtonModule, MatIconModule, MatDialogModule],
  template: `
    <div style="padding: 24px; font-family: system-ui, -apple-system, sans-serif;">
      <h2 style="margin: 0 0 16px 0; font-size: 20px; font-weight: 600; color: #1e293b;">
        {{ data.jaula ? 'Editar' : 'Nueva' }} Jaula
      </h2>
      
      <form [formGroup]="form" (ngSubmit)="guardar()">
        <div style="margin-bottom: 16px;">
          <label style="display: block; font-size: 13px; font-weight: 500; color: #475569; margin-bottom: 6px;">N\u00famero / Identificador</label>
          <input type="text" formControlName="numero"
            style="width: 100%; padding: 10px 12px; background: white; border: 1px solid #cbd5e1; border-radius: 8px; outline: none; font-size: 14px; box-sizing: border-box;"
            placeholder="Ej: J-001">
        </div>

        <div style="margin-bottom: 16px;">
          <label style="display: block; font-size: 13px; font-weight: 500; color: #475569; margin-bottom: 6px;">Categor\u00eda</label>
          <select formControlName="categoriaId"
            style="width: 100%; padding: 10px 12px; background: white; border: 1px solid #cbd5e1; border-radius: 8px; outline: none; font-size: 14px; box-sizing: border-box;">
            <option [ngValue]="null">Seleccione...</option>
            <option *ngFor="let c of categorias()" [ngValue]="c.id">{{ c.nombre }}</option>
          </select>
        </div>

        <div style="margin-bottom: 16px;">
          <label style="display: block; font-size: 13px; font-weight: 500; color: #475569; margin-bottom: 6px;">Tama\u00f1o</label>
          <select formControlName="tamanoId"
            style="width: 100%; padding: 10px 12px; background: white; border: 1px solid #cbd5e1; border-radius: 8px; outline: none; font-size: 14px; box-sizing: border-box;">
            <option [ngValue]="null">Seleccione...</option>
            <option *ngFor="let t of tamanos()" [ngValue]="t.id">{{ t.nombre }}</option>
          </select>
        </div>

        <div style="margin-bottom: 24px; display: flex; align-items: center; gap: 8px;">
          <input formControlName="alertaContagio" type="checkbox" id="alertaContagio" style="width: 16px; height: 16px; cursor: pointer;">
          <label for="alertaContagio" style="font-size: 14px; color: #dc2626; cursor: pointer; user-select: none; font-weight: 500;">
            Jaula para contagiosos (Aislamiento)
          </label>
        </div>

        <div style="display: flex; justify-content: flex-end; gap: 12px;">
          <button type="button" mat-button (click)="dialogRef.close()" style="border-radius: 8px;">Cancelar</button>
          <button type="submit" mat-flat-button color="primary" style="border-radius: 8px;" [disabled]="form.invalid || saving()">
            {{ saving() ? 'Guardando...' : 'Guardar' }}
          </button>
        </div>
      </form>
    </div>
  `
})
export class JaulaDialogComponent implements OnInit {
  dialogRef = inject(MatDialogRef<JaulaDialogComponent>);
  data = inject(MAT_DIALOG_DATA) as { jaula?: JaulaResponse, sedeId: number };
  fb = inject(FormBuilder);
  jaulaService = inject(JaulaService);
  categoriaService = inject(CategoriaJaulaService);
  tamanoService = inject(TamanoJaulaService);
  snackBar = inject(MatSnackBar);

  categorias = signal<any[]>([]);
  tamanos = signal<any[]>([]);
  saving = signal(false);

  form: FormGroup = this.fb.group({
    numero: ['', Validators.required],
    categoriaId: [null, Validators.required],
    tamanoId: [null, Validators.required],
    alertaContagio: [false],
    sedeId: [this.data.sedeId, Validators.required],
    estado: ['DISPONIBLE']
  });

  ngOnInit() {
    this.cargarListas();
    if (this.data?.jaula) {
      this.form.patchValue({
        numero: this.data.jaula.numero,
        tamanoId: this.data.jaula.tamanoId,
        alertaContagio: this.data.jaula.alertaContagio,
        sedeId: this.data.sedeId,
        estado: this.data.jaula.estado
      });
    }
  }

  cargarListas() {
    this.categoriaService.listar().subscribe(res => {
      this.categorias.set(res.filter((c: any) => c.activo !== false));
      if (this.data?.jaula) {
        const cat = res.find((c: any) => c.nombre === this.data.jaula!.categoriaNombre);
        if (cat) this.form.patchValue({ categoriaId: cat.id });
      }
    });
    this.tamanoService.listar().subscribe(res => {
      this.tamanos.set(res.filter((t: any) => t.activo !== false));
      if (this.data?.jaula && this.data.jaula.tamanoId) {
        this.form.patchValue({ tamanoId: this.data.jaula.tamanoId });
      }
    });
  }

  guardar() {
    if (this.form.invalid) return;
    this.saving.set(true);

    const request = this.form.value;

    if (this.data?.jaula) {
      this.jaulaService.actualizar(this.data.jaula.id, request).subscribe({
        next: () => {
          this.snackBar.open('Jaula actualizada', 'Cerrar', { duration: 3000 });
          this.dialogRef.close(true);
        },
        error: (err) => {
          this.snackBar.open('Error al actualizar: ' + (err.error?.message || err.error || 'Error desconocido'), 'Cerrar', { duration: 5000 });
          this.saving.set(false);
        }
      });
    } else {
      this.jaulaService.crear(request).subscribe({
        next: () => {
          this.snackBar.open('Jaula creada exitosamente', 'Cerrar', { duration: 3000 });
          this.dialogRef.close(true);
        },
        error: (err) => {
          this.snackBar.open('Error al crear: ' + (err.error?.message || err.error || 'Error desconocido'), 'Cerrar', { duration: 5000 });
          this.saving.set(false);
        }
      });
    }
  }
}
