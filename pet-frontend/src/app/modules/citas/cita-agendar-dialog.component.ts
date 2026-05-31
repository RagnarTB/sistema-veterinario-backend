import { Component, Inject, OnInit, signal, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatStepperModule, MatStepper } from '@angular/material/stepper';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { CitaService } from '../../core/services/cita.service';
import { ClienteService } from '../../core/services/cliente.service';
import { PacienteService } from '../../core/services/paciente.service';
import { EmpleadoService } from '../../core/services/empleado.service';
import { ServicioMedicoService } from '../servicios-medicos/servicio-medico.service';
import { ExternoService } from '../../core/services/externo.service';
import { EspecieService } from '../../core/services/especie.service';
import { CitaResponse, ClienteResponse, PacienteResponse, EmpleadoResponse, ServicioMedicoResponse, SlotDisponibilidad, EspecieResponse, MascotaRapidaRequest } from '../../core/models/models';
import { Subject, debounceTime, distinctUntilChanged, switchMap, of, catchError } from 'rxjs';

@Component({
  selector: 'app-cita-agendar-dialog',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatDialogModule, MatStepperModule, MatFormFieldModule,
    MatInputModule, MatSelectModule, MatButtonModule, MatCheckboxModule,
    MatAutocompleteModule, MatProgressSpinnerModule
  ],
  templateUrl: './cita-agendar-dialog.component.html',
  styleUrls: ['./cita-agendar-dialog.component.css']
})
export class CitaAgendarDialogComponent implements OnInit {
  @ViewChild('stepper') stepper!: MatStepper;
  stepIndex = 0;

  clienteQuery = '';
  clientesFiltrados = signal<ClienteResponse[]>([]);
  clienteSeleccionado = signal<ClienteResponse | null>(null);
  pacientesDisponibles = signal<PacienteResponse[]>([]);
  pacientesSeleccionados = signal<PacienteResponse[]>([]);
  loadingPacientes = signal(false);

  servicios = signal<ServicioMedicoResponse[]>([]);
  veterinarios = signal<EmpleadoResponse[]>([]);
  servicioId: number | null = null;
  veterinarioId: number | null = null;
  motivo = '';

  fechaCita = '';
  horaSeleccionada: string | null = null;
  slots = signal<SlotDisponibilidad[]>([]);
  loadingSlots = signal(false);
  slotsBuscados = signal(false);

  submitting = signal(false);
  hoy = '';

  private clienteSearch$ = new Subject<string>();

  nextStep(): void { if (this.canAdvance() && this.stepIndex < 3) { this.stepIndex++; } }
  prevStep(): void { if (this.stepIndex > 0) { this.stepIndex--; } }
  canAdvance(): boolean {
    if (this.stepIndex === 0) return this.pacientesSeleccionados().length > 0;
    if (this.stepIndex === 1) return !!this.servicioId && !!this.veterinarioId && !!this.motivo.trim();
    if (this.stepIndex === 2) return !!this.horaSeleccionada;
    return false;
  }

  // === Quick client state ===
  modoClienteRapido = signal(false);
  buscandoReniec = signal(false);
  reniecConsultado = signal(false);
  registrandoRapido = signal(false);
  especiesDisponibles = signal<EspecieResponse[]>([]);

  rapidoDni = '';
  rapidoNombre = '';
  rapidoApellido = '';
  rapidoTelefono = '';
  rapidoMascotas: { nombre: string; especieId: number; raza: string; sexo: string; fechaNacimiento: string }[] = [];

  constructor(
    public dialogRef: MatDialogRef<CitaAgendarDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { cita: CitaResponse | null, sedeId: number },
    private citaService: CitaService,
    private clienteService: ClienteService,
    private pacienteService: PacienteService,
    private empleadoService: EmpleadoService,
    private servicioMedicoService: ServicioMedicoService,
    private externoService: ExternoService,
    private especieService: EspecieService,
    private snack: MatSnackBar
  ) {
    const d = new Date();
    this.hoy = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
    this.fechaCita = this.hoy;
  }

  ngOnInit(): void {
    this.clienteSearch$.pipe(
      debounceTime(300), distinctUntilChanged(),
      switchMap(q => q.length < 2 ? of({ content: [] }) : this.clienteService.listar(0, 10, q, true))
    ).subscribe(res => this.clientesFiltrados.set(res.content));

    this.servicioMedicoService.listar(true).subscribe(s => this.servicios.set(s));
    this.empleadoService.listar(0, 100, '', true, this.data.sedeId).subscribe(res => {
      this.veterinarios.set(res.content.filter(e => e.nombresRoles?.includes('ROLE_VETERINARIO')));
    });
    this.especieService.listar().subscribe(e => this.especiesDisponibles.set(e.filter(x => x.activo)));

    if (this.data.cita) this.precargarEdicion();
  }

  onClienteSearch(q: string): void { this.clienteQuery = q; this.clienteSearch$.next(q); }

  onClienteSelected(c: ClienteResponse): void {
    this.clienteSeleccionado.set(c);
    this.clienteQuery = `${c.nombre} ${c.apellido}`;
    this.pacientesSeleccionados.set([]);
    this.cargarPacientes(c.id);
  }

  limpiarCliente(): void {
    this.clienteSeleccionado.set(null);
    this.clienteQuery = '';
    this.pacientesDisponibles.set([]);
    this.pacientesSeleccionados.set([]);
    this.cancelarModoRapido();
  }

  // === Quick client methods ===
  activarModoRapido(): void {
    this.modoClienteRapido.set(true);
    this.rapidoMascotas = [{ nombre: '', especieId: 0, raza: '', sexo: 'MACHO', fechaNacimiento: '' }];
  }

  cancelarModoRapido(): void {
    this.modoClienteRapido.set(false);
    this.reniecConsultado.set(false);
    this.rapidoDni = '';
    this.rapidoNombre = '';
    this.rapidoApellido = '';
    this.rapidoTelefono = '';
    this.rapidoMascotas = [];
  }

  // Limpiar campos RENIEC al editar DNI
  onRapidoDniChange(): void {
    if (this.rapidoDni.length < 8) {
      this.rapidoNombre = '';
      this.rapidoApellido = '';
      this.reniecConsultado.set(false);
    }
  }

  buscarEnReniec(): void {
    if (this.rapidoDni.length !== 8) return;
    this.buscandoReniec.set(true);
    this.externoService.consultarDni(this.rapidoDni).pipe(
      catchError(() => of(null))
    ).subscribe(res => {
      this.buscandoReniec.set(false);
      this.reniecConsultado.set(true);
      if (res) {
        // Si el cliente ya existe en BD, auto-seleccionarlo
        if (res.existe_en_bd && res.cliente_id) {
          this.clienteService.buscarPorId(res.cliente_id).subscribe({
            next: (cliente) => {
              this.onClienteSelected(cliente);
              this.modoClienteRapido.set(false);
              this.snack.open('Cliente ya registrado. Vinculado automáticamente.', 'Listo', { duration: 4000 });
            },
            error: () => {
              // Fallback: llenar campos manualmente
              const nombres = res.first_name || '';
              const apellidos = [res.first_last_name, res.second_last_name].filter(Boolean).join(' ');
              this.rapidoNombre = nombres || this.rapidoNombre;
              this.rapidoApellido = apellidos || this.rapidoApellido;
            }
          });
        } else {
          const nombres = res.first_name || '';
          const apellidos = [res.first_last_name, res.second_last_name].filter(Boolean).join(' ');
          this.rapidoNombre = nombres || this.rapidoNombre;
          this.rapidoApellido = apellidos || this.rapidoApellido;
          this.snack.open('Datos completados desde RENIEC', 'OK', { duration: 3000 });
        }
      } else {
        this.snack.open('No se encontró en RENIEC. Complete los datos manualmente.', 'OK', { duration: 3000 });
      }
    });
  }

  agregarMascotaRapida(): void {
    this.rapidoMascotas.push({ nombre: '', especieId: 0, raza: '', sexo: 'MACHO', fechaNacimiento: '' });
  }

  quitarMascotaRapida(index: number): void {
    this.rapidoMascotas = this.rapidoMascotas.filter((_, i) => i !== index);
  }

  puedeRegistrarRapido(): boolean {
    return this.rapidoDni.length === 8
      && !!this.rapidoNombre.trim()
      && !!this.rapidoApellido.trim()
      && this.rapidoMascotas.length > 0
      && this.rapidoMascotas.every(m => !!m.nombre.trim() && m.especieId > 0 && !!m.fechaNacimiento);
  }

  registrarClienteRapido(): void {
    if (!this.puedeRegistrarRapido() || this.registrandoRapido()) return;
    this.registrandoRapido.set(true);

    const payload = {
      dni: this.rapidoDni,
      nombre: this.rapidoNombre.trim(),
      apellido: this.rapidoApellido.trim(),
      telefono: this.rapidoTelefono.trim() || undefined,
      mascotas: this.rapidoMascotas.map(m => ({
        nombre: m.nombre.trim(),
        especieId: m.especieId,
        raza: m.raza.trim() || undefined,
        sexo: m.sexo,
        fechaNacimiento: m.fechaNacimiento
      } as MascotaRapidaRequest))
    };

    this.clienteService.crearRapido(payload).subscribe({
      next: res => {
        this.registrandoRapido.set(false);
        this.snack.open(`Cliente ${res.nombre} ${res.apellido} registrado exitosamente`, 'OK', { duration: 4000 });
        // Seleccionar automáticamente el nuevo cliente
        const nuevoCliente: ClienteResponse = {
          id: res.clienteId, nombre: res.nombre, apellido: res.apellido,
          dni: res.dni, telefono: res.telefono || '', email: '',
          activo: true, verificado: false, esInvitado: true
        };
        this.onClienteSelected(nuevoCliente);
        this.cancelarModoRapido();
      },
      error: err => {
        this.registrandoRapido.set(false);
        const msg = err.error?.message || err.error?.mensaje || 'Error al registrar cliente rápido';
        this.snack.open(msg, 'Cerrar', { duration: 5000 });
      }
    });
  }

  cargarPacientes(clienteId: number): void {
    this.loadingPacientes.set(true);
    this.pacienteService.listar(0, 100, '', true).subscribe(res => {
      this.pacientesDisponibles.set(res.content.filter(p => p.clienteId === clienteId));
      this.loadingPacientes.set(false);
    });
  }

  togglePaciente(p: PacienteResponse): void {
    const current = this.pacientesSeleccionados();
    if (current.some(x => x.id === p.id)) {
      this.pacientesSeleccionados.set(current.filter(x => x.id !== p.id));
    } else {
      this.pacientesSeleccionados.set([...current, p]);
    }
  }

  isPacienteSelected(id: number): boolean { return this.pacientesSeleccionados().some(p => p.id === id); }

  onServicioChange(): void { this.horaSeleccionada = null; this.slotsBuscados.set(false); }

  getServicio(): ServicioMedicoResponse | undefined { return this.servicios().find(s => s.id === this.servicioId); }
  getVet(): EmpleadoResponse | undefined { return this.veterinarios().find(v => v.id === this.veterinarioId); }
  getDuracionTotal(): number { const s = this.getServicio(); return s ? s.duracionMinutos + s.bufferMinutos : 0; }

  buscarSlots(): void {
    if (!this.veterinarioId || !this.servicioId || !this.fechaCita) return;
    this.loadingSlots.set(true);
    this.horaSeleccionada = null;
    const excludeId = this.data.cita?.id;
    this.citaService.getDisponibilidad(
      this.veterinarioId, this.fechaCita, this.servicioId, this.data.sedeId,
      this.pacientesSeleccionados().length || 1, excludeId
    ).subscribe({
      next: s => { this.slots.set(s); this.loadingSlots.set(false); this.slotsBuscados.set(true); },
      error: () => { this.loadingSlots.set(false); this.snack.open('Error al buscar disponibilidad', 'Cerrar', { duration: 3000 }); }
    });
  }

  isSlotPast(slot: SlotDisponibilidad): boolean {
    if (this.fechaCita !== this.hoy) return false;
    const now = new Date();
    const [h, m] = slot.horaInicio.split(':').map(Number);
    return h < now.getHours() || (h === now.getHours() && m <= now.getMinutes());
  }

  isFormValid(): boolean {
    return this.pacientesSeleccionados().length > 0 && !!this.servicioId && !!this.veterinarioId
      && !!this.motivo.trim() && !!this.horaSeleccionada;
  }

  confirmar(): void {
    if (this.submitting() || !this.isFormValid()) return;
    this.submitting.set(true);
    const payload = {
      fecha: this.fechaCita,
      horaInicio: this.horaSeleccionada!.length === 5 ? this.horaSeleccionada + ':00' : this.horaSeleccionada!,
      servicioId: this.servicioId!,
      veterinarioId: this.veterinarioId!,
      motivo: this.motivo,
      pacienteIds: this.pacientesSeleccionados().map(p => p.id),
      sedeId: this.data.sedeId
    };

    const obs = this.data.cita
      ? this.citaService.actualizar(this.data.cita.id, payload)
      : this.citaService.crear(payload);

    obs.subscribe({
      next: () => {
        this.snack.open(this.data.cita ? 'Cita reprogramada' : 'Cita agendada exitosamente', 'OK', { duration: 3000 });
        this.dialogRef.close(true);
      },
      error: err => {
        this.submitting.set(false);
        const msg = err.error?.message || err.error?.mensaje || 'Error al guardar la cita';
        this.snack.open(msg, 'Cerrar', { duration: 5000 });
        if (msg.includes('horario') || msg.includes('cruce')) { this.buscarSlots(); }
      }
    });
  }

  private precargarEdicion(): void {
    const c = this.data.cita!;
    this.servicioId = c.servicioId;
    this.veterinarioId = c.veterinarioId;
    this.motivo = c.motivo;
    this.fechaCita = c.fecha;
    this.horaSeleccionada = c.horaInicio;

    if (c.pacientes?.length > 0 && c.pacientes[0].clienteId) {
      this.clienteService.buscarPorId(c.pacientes[0].clienteId).subscribe(cl => {
        this.onClienteSelected(cl);
        setTimeout(() => {
          this.pacienteService.listar(0, 100, '', true).subscribe(res => {
            const pacDueno = res.content.filter(p => p.clienteId === cl.id);
            this.pacientesDisponibles.set(pacDueno);
            this.pacientesSeleccionados.set(pacDueno.filter(p => c.pacienteIds.includes(p.id)));
          });
        }, 200);
      });
    }
  }

  // =============================================
  // AGRUPACIÓN DE HORARIOS POR PERÍODO DEL DÍA
  // =============================================
  getSlotsByPeriod(period: 'morning' | 'afternoon' | 'evening'): SlotDisponibilidad[] {
    return this.slots().filter(slot => {
      const hora = parseInt(slot.horaInicio.substring(0, 2), 10);
      if (period === 'morning')   return hora < 12;
      if (period === 'afternoon') return hora >= 12 && hora < 18;
      if (period === 'evening')   return hora >= 18;
      return false;
    });
  }
}
