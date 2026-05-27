import { Component, Inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { CustomValidators } from '../../shared/utils/custom-validators';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatChipsModule } from '@angular/material/chips';
import { MatTabsModule } from '@angular/material/tabs';

import { EmpleadoService } from '../../core/services/empleado.service';
import { SedeService } from '../../core/services/sede.service';
import { RolService } from '../../core/services/rol.service';
import { ExternoService } from '../../core/services/externo.service';
import { ColegiaturaService } from '../../core/services/colegiatura.service';
import { HorariosVeterinarioComponent } from './horarios-veterinario.component';
import { DiasBloqueadosComponent } from './dias-bloqueados.component';
import { HorarioService, HorarioVeterinarioResponse } from '../../core/services/horario.service';
import { DiaBloqueadoService } from '../../core/services/dia-bloqueado.service';
import { EmpleadoResponse, SedeResponse, RolResponse, ColegiaturaValidacion, DiaBloqueadoResponse } from '../../core/models/models';

export interface EmpleadoDialogData {
  empleado?: EmpleadoResponse;
}

@Component({
  selector: 'app-empleado-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    MatFormFieldModule,
    MatInputModule,
    MatChipsModule,
    MatTabsModule,
    HorariosVeterinarioComponent,
    DiasBloqueadosComponent
  ],
  templateUrl: './empleado-dialog.component.html',
  styleUrls: ['./empleado-dialog.component.css']
})
export class EmpleadoDialogComponent implements OnInit {
  isEdit = signal(false);
  loading = signal(false);
  buscandoDni = false;
  form: FormGroup;
  paso = signal<'formulario' | 'confirmacion'>('formulario');
  dniEsCliente = signal(false);
  esVeterinario = signal(false);
  esVeterinarioGuardado = signal(false);
  validandoColegiatura = signal(false);
  colegiaturaResultado = signal<ColegiaturaValidacion | null>(null);

  sedes = signal<SedeResponse[]>([]);
  sedesAsignadas = signal<SedeResponse[]>([]);
  roles = signal<RolResponse[]>([]);

  constructor(
    private dialogRef: MatDialogRef<EmpleadoDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: EmpleadoDialogData,
    private fb: FormBuilder,
    private empleadoService: EmpleadoService,
    private sedeService: SedeService,
    private rolService: RolService,
    private externoService: ExternoService,
    private colegiaturaService: ColegiaturaService,
    private snack: MatSnackBar
  ) {
    this.isEdit.set(!!data.empleado);

    this.form = this.fb.group({
      dni: [{value: data.empleado?.dni || '', disabled: this.isEdit()}, [Validators.required, CustomValidators.dni]],
      nombre: [{value: data.empleado?.nombre || '', disabled: this.isEdit()}, [Validators.required, CustomValidators.noWhitespace]],
      apellido: [{value: data.empleado?.apellido || '', disabled: this.isEdit()}, [Validators.required, CustomValidators.noWhitespace]],
      telefono: [data.empleado?.telefono || '', [Validators.required, CustomValidators.telefono]],
      email: [{value: data.empleado?.email || '', disabled: this.isEdit()}, [Validators.required, Validators.email]],
      especialidad: [data.empleado?.especialidad || ''],
      numeroColegiatura: [data.empleado?.numeroColegiatura || ''],
      sueldoBase: [data.empleado?.sueldoBase || null],
      sedeIds: [data.empleado?.sedeIds || [], Validators.required],
      roles: [data.empleado?.nombresRoles || [], Validators.required]
    });

    // Verificar si ya es veterinario al editar
    if (this.isEdit() && data.empleado?.nombresRoles?.includes('ROLE_VETERINARIO')) {
      this.esVeterinarioGuardado.set(true);
    }
  }

  ngOnInit(): void {
    this.cargarSedes();
    this.cargarRoles();

    if (this.isEdit() && this.data.empleado) {
      // Si el empleado ya es veterinario y tiene sedes asignadas
      if (this.data.empleado.nombresRoles?.includes('ROLE_VETERINARIO')) {
        this.esVeterinario.set(true);
      }
    }
  }

  cargarSedes() {
    this.sedeService.listar(0, 100).subscribe({
      next: (res: any) => this.sedes.set(res.content || []),
      error: () => console.error('Error al cargar sedes')
    });
  }

  cargarRoles() {
    this.rolService.listarTodos().subscribe({
      next: (res: any) => this.roles.set(res || []),
      error: () => console.error('Error al cargar roles')
    });
  }

  buscarDni() {
    const dni = this.form.get('dni')?.value;
    if (!dni || dni.toString().length !== 8) return;

    this.buscandoDni = true;
    this.externoService.consultarDni(dni).subscribe({
      next: (res: any) => {
        if (res && res.nombre) {
          this.form.patchValue({ nombre: res.nombre, apellido: res.apellido, email: res.email || '' });
          this.form.get('nombre')?.disable();
          this.form.get('apellido')?.disable();

          if (res.existe_en_bd && res.email) {
            const patchData: any = { email: res.email };
            if (res.telefono) patchData.telefono = res.telefono;

            this.form.patchValue(patchData);
            this.form.get('email')?.disable();
            this.dniEsCliente.set(true);
            this.snack.open('DNI registrado en el sistema. Se reutilizarán sus datos.', 'Entendido', { duration: 4000 });
          } else {
            this.snack.open('DNI encontrado exitosamente', 'Cerrar', { duration: 3000 });
          }
        }
        this.buscandoDni = false;
      },
      error: () => {
        this.buscandoDni = false;
        this.snack.open('No se pudo encontrar información para este documento', 'Cerrar', { duration: 4000 });
      }
    });
  }

  onRolesChange() {
    const rolesSeleccionados: string[] = this.form.get('roles')?.value || [];
    this.esVeterinario.set(rolesSeleccionados.includes('ROLE_VETERINARIO'));

    if (!this.esVeterinario()) {
      this.form.get('numeroColegiatura')?.setValue('');
      this.colegiaturaResultado.set(null);
    }
  }

  validarColegiatura() {
    const numero = this.form.get('numeroColegiatura')?.value;
    if (!numero) return;

    this.validandoColegiatura.set(true);
    this.colegiaturaResultado.set(null);

    this.colegiaturaService.validar(numero).subscribe({
      next: (res: ColegiaturaValidacion) => {
        this.colegiaturaResultado.set(res);
        this.validandoColegiatura.set(false);
        if (res.habilitado) {
          this.snack.open('Colegiatura verificada: HABILITADO', 'Cerrar', { duration: 3000 });
        } else {
          this.snack.open(res.error || 'Colegiatura no válida', 'Cerrar', { duration: 4000 });
        }
      },
      error: (err: any) => {
        this.colegiaturaResultado.set({
          numeroColegiatura: numero,
          habilitado: false,
          error: err.error?.error || 'No se pudo validar la colegiatura'
        });
        this.validandoColegiatura.set(false);
      }
    });
  }

  getSedesNombres(): string {
    const ids: number[] = this.form.getRawValue().sedeIds || [];
    return this.sedes().filter(s => ids.includes(s.id)).map(s => s.nombre).join(', ') || 'N/A';
  }

  mostrarConfirmacion() {
    if (this.form.invalid) return;
    this.paso.set('confirmacion');
  }

  guardar() {
    if (this.form.invalid) return;

    this.loading.set(true);
    const dto = this.form.getRawValue();

    if (dto.especialidad) {
      dto.especialidad = dto.especialidad.toUpperCase().trim();
    }

    const obs = this.isEdit()
      ? this.empleadoService.actualizar(this.data.empleado!.id, dto)
      : this.empleadoService.crear(dto);

    obs.subscribe({
      next: (res: any) => {
        this.snack.open(this.isEdit() ? 'Empleado actualizado' : 'Empleado registrado y correo enviado', 'Cerrar', { duration: 3000 });
        this.dialogRef.close(res);
      },
      error: (err: any) => {
        let msg = err.error?.mensaje || err.error?.message || 'Error al guardar';
        if (err.error?.detalles && Array.isArray(err.error.detalles)) {
          msg += ': ' + err.error.detalles.join(', ');
        }

        // Detectar si el backend dice que el DNI pertenece a un cliente
        if (msg.toLowerCase().includes('cliente') || msg.toLowerCase().includes('dni')) {
          this.dniEsCliente.set(true);
          this.paso.set('formulario');
          this.snack.open('Este DNI pertenece a un cliente existente. Se reutilizarán sus datos.', 'Entendido', { duration: 5000 });
        } else {
          this.snack.open(msg, 'Cerrar', { duration: 6000 });
        }
        this.loading.set(false);
      }
    });
  }

  soloNumeros(event: KeyboardEvent): void {
    const teclas_permitidas = ['Backspace', 'Delete', 'ArrowLeft', 'ArrowRight', 'Tab', 'Home', 'End'];
    const patron = /^[0-9]$/;
    if (!teclas_permitidas.includes(event.key) && !patron.test(event.key)) {
      event.preventDefault();
    }
  }

  soloLetras(event: KeyboardEvent): void {
    const teclas_permitidas = ['Backspace', 'Delete', 'ArrowLeft', 'ArrowRight', 'Tab', 'Home', 'End'];
    const patron = /^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]$/;
    if (!teclas_permitidas.includes(event.key) && !patron.test(event.key)) {
      event.preventDefault();
    }
  }
}
