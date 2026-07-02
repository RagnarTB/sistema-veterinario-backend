import { Component, Inject, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, FormControl } from '@angular/forms';
import { debounceTime, distinctUntilChanged, switchMap, of, map, startWith } from 'rxjs';

import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule, MAT_DATE_LOCALE } from '@angular/material/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { PacienteRequest, PacienteResponse, EspecieResponse, ClienteResponse } from '../../../core/models/models';
import { EspecieService } from '../../../core/services/especie.service';
import { ClienteService } from '../../../core/services/cliente.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-paciente-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatInputModule,
    MatFormFieldModule,
    MatSelectModule,
    MatAutocompleteModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  providers: [
    { provide: MAT_DATE_LOCALE, useValue: 'es-ES' }
  ],
  templateUrl: './paciente-dialog.component.html',
  styleUrls: ['./paciente-dialog.component.css']
})
export class PacienteDialogComponent implements OnInit {
  private fb = inject(FormBuilder);
  private especieService = inject(EspecieService);
  private clienteService = inject(ClienteService);
  public dialogRef = inject(MatDialogRef<PacienteDialogComponent>);
  public data: PacienteResponse | null = inject(MAT_DIALOG_DATA);

  form!: FormGroup;
  isEdit = false;
  isSubmitting = false;

  especiesOptions: EspecieResponse[] = [];
  clientesOptions: ClienteResponse[] = [];

  clienteSearchCtrl = new FormControl<string | ClienteResponse>('');
  searchingClientes = signal(false);

  private authService = inject(AuthService);
  esCliente = false;

  today = new Date(); // Para el max datepicker
  mostrarResumen = signal(false);

  ngOnInit(): void {
    this.isEdit = !!this.data;
    this.esCliente = this.authService.activeRole() === 'ROLE_CLIENTE';

    this.form = this.fb.group({
      nombre: [this.data?.nombre || '', Validators.required],
      especieId: [{ value: null, disabled: this.isEdit }, Validators.required],
      raza: [this.data?.raza || ''],
      sexo: [this.data?.sexo || 'MACHO', Validators.required],
      fechaNacimiento: [{ value: this.data?.fechaNacimiento ? new Date(this.data.fechaNacimiento) : null, disabled: this.isEdit }, Validators.required],
      clienteId: [{ value: this.data?.clienteId || null, disabled: this.isEdit || this.esCliente }, this.esCliente ? [] : Validators.required]
    });

    if (this.isEdit || this.esCliente) {
      this.clienteSearchCtrl.disable();
    }

    this.cargarEspecies();
    this.setupClienteAutocomplete();

    if (this.esCliente && !this.isEdit) {
      this.clienteService.misDatos().subscribe(c => {
         if (c) {
           this.clienteSearchCtrl.setValue(c);
           this.form.patchValue({ clienteId: c.id });
         }
      });
    }
  }

  cargarEspecies() {
    this.especieService.listar().subscribe({
      next: (res) => {
        // Obtenemos el nombre del DTO en mayúsculas de forma segura
        const especieDTO = this.data?.especieNombre?.toUpperCase();

        // Filtramos las especies activas, o la especie actual si estamos editando
        this.especiesOptions = res.filter(e => e.activo || (this.isEdit && e.nombre.toUpperCase() === especieDTO));

        // Emparejamos el ID interno para el formulario
        if (this.isEdit && this.data) {
          const matchedEsp = res.find(e => e.nombre.toUpperCase() === especieDTO);
          if (matchedEsp) {
            this.form.patchValue({ especieId: matchedEsp.id });
          }
        }
      }
    });
  }

  setupClienteAutocomplete() {
    // Si estamos editando, tratamos de precargar el cliente localmente o con el DTO
    if (this.isEdit && this.data) {
      // Mock un cliente solo para el display inicial basado en el "clienteNombre" que agregamos
      const initialClient: ClienteResponse = {
        id: this.data.clienteId!,
        nombre: this.data.clienteNombre || 'Dueño Actual',
        apellido: '',
        dni: '',
        telefono: '',
        email: '',
        activo: true,
        verificado: true
      };
      this.clienteSearchCtrl.setValue(initialClient);
    }

    this.clienteSearchCtrl.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap((value) => {
        if (typeof value !== 'string') {
          // It's an object selection
          this.form.patchValue({ clienteId: value?.id });
          return of([]);
        }

        if (value.length < 2) return of([]);

        this.searchingClientes.set(true);
        // Page 0, 10 items, query = value
        return this.clienteService.listar(0, 10, value).pipe(
          map(page => page.content.filter(c => c.activo))
        );
      })
    ).subscribe({
      next: (clientes) => {
        if (clientes.length > 0) {
          this.clientesOptions = clientes;
        }
        this.searchingClientes.set(false);
      },
      error: () => this.searchingClientes.set(false)
    });
  }

  displayCliente(cliente: ClienteResponse): string {
    return cliente ? `${cliente.nombre} ${cliente.apellido}`.trim() : '';
  }

  soloLetras(event: KeyboardEvent): void {
    const teclas_permitidas = ['Backspace', 'Delete', 'ArrowLeft', 'ArrowRight', 'Tab', 'Home', 'End'];
    const patron = /^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]$/;
    if (!teclas_permitidas.includes(event.key) && !patron.test(event.key)) {
      event.preventDefault();
    }
  }

  irAResumen(): void {
    if (this.form.valid) {
      if (this.isEdit) {
        this.onSubmit(); // Si es edición, guardar directo
      } else {
        this.mostrarResumen.set(true);
      }
    } else {
      this.form.markAllAsTouched();
    }
  }

  volverAFormulario(): void {
    this.mostrarResumen.set(false);
  }

  get resumenDatos() {
    const v = this.form.getRawValue();
    const esp = this.especiesOptions.find(e => e.id === v.especieId);
    let cliNombre = '';
    
    if (this.esCliente) {
      cliNombre = 'Tú (Dueño Actual)';
    } else {
      const cli = this.clienteSearchCtrl.value as ClienteResponse;
      cliNombre = cli ? `${cli.nombre} ${cli.apellido}` : '';
    }
    
    return {
      nombre: v.nombre,
      especie: esp?.nombre || '',
      raza: v.raza || 'No especificada',
      sexo: v.sexo,
      fechaNacimiento: v.fechaNacimiento ? new Date(v.fechaNacimiento).toLocaleDateString() : '',
      dueno: cliNombre
    };
  }

  onSubmit(): void {
    if (this.form.valid) {
      const result: PacienteRequest = { ...this.form.getRawValue() };

      if (result.raza) result.raza = result.raza.trim().toUpperCase();
      if (result.nombre) result.nombre = result.nombre.trim();

      // Convertir fecha a string ISO YYYY-MM-DD
      const dateVal = this.form.get('fechaNacimiento')?.value;
      if (dateVal) {
        const d = new Date(dateVal);
        result.fechaNacimiento = d.toISOString().split('T')[0];
      }

      this.dialogRef.close(result);
    } else {
      // Force display errors
      this.form.markAllAsTouched();
    }
  }

  onCancel(): void {
    this.dialogRef.close();
  }
}
