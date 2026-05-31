import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { catchError, of } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { CitaService } from '../../core/services/cita.service';
import { AtencionService, AtencionRequest } from '../../core/services/atencion.service';
import { RecetaService, RecetaRequest, DetalleRecetaRequest } from '../../core/services/receta.service';
import { ProductoService, Producto } from '../farmacia/services/producto.service';
import { SedeService } from '../../core/services/sede.service';
import { CitaResponse, PacienteResumen, SedeResponse } from '../../core/models/models';

interface PacienteClinicoData {
  peso: number | null;
  temperatura: number | null;
  frecuenciaCardiaca: number | null;
  sintomas: string;
  diagnostico: string;
  tratamiento: string;
  recetaDetalles: {
    productoId: number | null;
    medicamento: string;
    dosis: string;
    frecuencia: string;
    duracionDias: number;
  }[];
  indicacionesGenerales: string;
  guardado: boolean;
  recetaId?: number;
}

@Component({
  selector: 'app-atenciones',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatSnackBarModule
  ],
  templateUrl: './atenciones.component.html',
  styleUrls: ['./atenciones.component.css']
})
export class AtencionesComponent implements OnInit {
  private citaService = inject(CitaService);
  private atencionService = inject(AtencionService);
  private recetaService = inject(RecetaService);
  private productoService = inject(ProductoService);
  private snack = inject(MatSnackBar);
  private router = inject(Router);
  private SedeService = inject(SedeService);

  sedeId = 1;
  sedes: SedeResponse[] = [];
  filtroEstado: 'TODOS' | 'EN_CONSULTORIO' | 'EN_SALA_ESPERA' = 'TODOS';

  citas: CitaResponse[] = [];
  citasFiltradas: (CitaResponse & { badgeCompletada?: boolean })[] = [];
  citasCargando = signal<boolean>(false);

  citaSeleccionada: CitaResponse | null = null;
  activePacienteIndex = 0;

  // Datos locales de formularios clínicos indexados por pacienteId
  clinicoData: { [pacienteId: number]: PacienteClinicoData } = {};

  // Productos de farmacia para prescripción inteligente
  productos: Producto[] = [];
  filteredProductos: Producto[] = [];
  activeAutocompleteRow: number | null = null;

  guardandoFicha = signal<boolean>(false);

  // Control de éxito e impresión final
  citaCompletadaExito = false;
  recetasGeneradas: { pacienteNombre: string; recetaId: number }[] = [];

  ngOnInit() {
    this.SedeService.listarActivas().subscribe({
      next: (res) => {
        this.sedes = res;
        const storedSede = Number(localStorage.getItem('vet_sede_id'));
        if (storedSede && this.sedes.some(s => s.id === storedSede)) {
          this.sedeId = storedSede;
        } else if (this.sedes.length > 0) {
          this.sedeId = this.sedes[0].id;
          localStorage.setItem('vet_sede_id', this.sedeId.toString());
        }
        this.cargarCitas();
        this.cargarProductos();
      },
      error: () => {
        this.sedes = [];
        this.sedeId = Number(localStorage.getItem('vet_sede_id')) || 1;
        this.cargarCitas();
        this.cargarProductos();
      }
    });
  }

  onSedeChange() {
    localStorage.setItem('vet_sede_id', this.sedeId.toString());
    this.citaSeleccionada = null;
    this.cargarCitas();
    this.cargarProductos();
  }

  cambiarFiltro(estado: 'TODOS' | 'EN_CONSULTORIO' | 'EN_SALA_ESPERA') {
    this.filtroEstado = estado;
    this.aplicarFiltros();
  }

  cargarCitas() {
    this.citasCargando.set(true);
    const d = new Date();
    const hoyStr = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;

    this.citaService.listar(this.sedeId, 0, 100, '', hoyStr).subscribe({
      next: (res) => {
        // Ordenamos las citas por hora
        this.citas = res.content || [];
        this.aplicarFiltros();
        this.citasCargando.set(false);
      },
      error: () => {
        this.snack.open('Error al obtener la agenda de citas', 'Cerrar', { duration: 3000, panelClass: ['snack-error'] });
        this.citasCargando.set(false);
      }
    });
  }

  cargarProductos() {
    // Obtenemos los productos activos de esta sede para la hoja de recetas
    this.productoService.listar('', 0, 100, this.sedeId).subscribe({
      next: (res) => {
        this.productos = res.content || [];
      }
    });
  }

  aplicarFiltros() {
    if (this.filtroEstado === 'TODOS') {
      // Mostramos citas relevantes para el veterinario
      this.citasFiltradas = this.citas.filter(c =>
        c.estado === 'EN_CONSULTORIO' || c.estado === 'EN_SALA_ESPERA' || c.estado === 'COMPLETADA'
      );
    } else {
      this.citasFiltradas = this.citas.filter(c => c.estado === this.filtroEstado);
    }
  }

  seleccionarCita(cita: CitaResponse) {
    this.citaSeleccionada = cita;
    this.activePacienteIndex = 0;
    this.clinicoData = {};
    this.citaCompletadaExito = false;
    this.recetasGeneradas = [];

    // Inicializamos estructura clínica vacía para cada paciente de la cita
    if (cita.pacientes && cita.pacientes.length > 0) {
      cita.pacientes.forEach(pet => {
        this.clinicoData[pet.id] = {
          peso: null,
          temperatura: null,
          frecuenciaCardiaca: null,
          sintomas: '',
          diagnostico: '',
          tratamiento: '',
          recetaDetalles: [],
          indicacionesGenerales: '',
          guardado: false
        };

        // Si la cita ya está completada, intentamos cargar la historia clínica
        if (cita.estado === 'COMPLETADA' || cita.estado === 'EN_CONSULTORIO') {
          this.atencionService.buscarPorCitaYPaciente(cita.id, pet.id).pipe(
            catchError(() => of(null))
          ).subscribe(atencion => {
            if (atencion) {
              this.clinicoData[pet.id] = {
                peso: atencion.peso,
                temperatura: atencion.temperatura,
                frecuenciaCardiaca: atencion.frecuenciaCardiaca,
                sintomas: atencion.sintomas || '',
                diagnostico: atencion.diagnostico || '',
                tratamiento: atencion.tratamiento || '',
                recetaDetalles: [], // La receta se maneja aparte por ahora
                indicacionesGenerales: atencion.resumenIaCliente || '',
                guardado: true
              };
              // Si la cita está completada globalmente, mostramos el success state
              if (cita.estado === 'COMPLETADA') {
                this.citaCompletadaExito = true;
                this.citaSeleccionada!.badgeCompletada = true;
              }
            }
          });
        }
      });
    }
  }

  get activePaciente(): PacienteResumen | null {
    if (this.citaSeleccionada && this.citaSeleccionada.pacientes) {
      return this.citaSeleccionada.pacientes[this.activePacienteIndex];
    }
    return null;
  }

  switchPaciente(index: number) {
    this.activePacienteIndex = index;
    this.activeAutocompleteRow = null;
  }

  pasarAConsultorio() {
    if (!this.citaSeleccionada) return;

    this.citaService.cambiarEstado(this.citaSeleccionada.id, 'EN_CONSULTORIO').subscribe({
      next: () => {
        this.citaSeleccionada!.estado = 'EN_CONSULTORIO';
        this.snack.open('Cita iniciada en box de consulta', 'Listo', { duration: 2500, panelClass: ['snack-success'] });
        this.cargarCitas();
      },
      error: () => {
        this.snack.open('Error al actualizar el estado de la cita', 'Cerrar', { duration: 3000, panelClass: ['snack-error'] });
      }
    });
  }

  agregarMedicamento(pacienteId: number) {
    this.clinicoData[pacienteId].recetaDetalles.push({
      productoId: null,
      medicamento: '',
      dosis: '',
      frecuencia: '',
      duracionDias: 1
    });
  }

  removerMedicamento(pacienteId: number, index: number) {
    this.clinicoData[pacienteId].recetaDetalles.splice(index, 1);
  }

  // AUTOCOMPLETE LOGIC
  showAutocomplete(pacienteId: number, index: number) {
    this.activeAutocompleteRow = index;
    const term = this.clinicoData[pacienteId].recetaDetalles[index].medicamento.toLowerCase();
    this.filterProducts(term);
  }

  hideAutocomplete(index: number) {
    // Delay simple para permitir la selección de mousedown
    setTimeout(() => {
      if (this.activeAutocompleteRow === index) {
        this.activeAutocompleteRow = null;
      }
    }, 200);
  }

  filterProducts(term: string) {
    if (!term) {
      this.filteredProductos = this.productos.slice(0, 8);
    } else {
      this.filteredProductos = this.productos.filter(p =>
        p.nombre.toLowerCase().includes(term) || (p.marca && p.marca.toLowerCase().includes(term))
      ).slice(0, 8);
    }
  }

  seleccionarProducto(pacienteId: number, medIdx: number, prod: Producto) {
    const row = this.clinicoData[pacienteId].recetaDetalles[medIdx];
    row.productoId = prod.id;
    row.medicamento = prod.nombre;
    this.activeAutocompleteRow = null;
  }

  validarFichaPaciente(pacienteId: number): boolean {
    const f = this.clinicoData[pacienteId];
    if (!f) return false;

    // Validamos signos vitales mayores a cero y textos completados
    return (
      f.peso !== null && f.peso > 0 &&
      f.temperatura !== null && f.temperatura > 0 &&
      f.frecuenciaCardiaca !== null && f.frecuenciaCardiaca > 0 &&
      f.sintomas.trim().length > 2 &&
      f.diagnostico.trim().length > 2 &&
      f.tratamiento.trim().length > 2
    );
  }

  guardarAtencionPaciente(pacienteId: number) {
    if (!this.citaSeleccionada || !this.validarFichaPaciente(pacienteId)) return;

    this.guardandoFicha.set(true);
    const f = this.clinicoData[pacienteId];

    const request: AtencionRequest = {
      citaId: this.citaSeleccionada.id,
      pacienteId: pacienteId,
      sintomas: f.sintomas,
      diagnostico: f.diagnostico,
      tratamiento: f.tratamiento,
      peso: Number(f.peso),
      temperatura: Number(f.temperatura),
      frecuenciaCardiaca: Number(f.frecuenciaCardiaca)
    };

    this.atencionService.crear(request).subscribe({
      next: (atencionRes) => {
        // Comprobar si hay receta por emitir
        if (f.recetaDetalles.length > 0) {
          const reqReceta: RecetaRequest = {
            atencionMedicaId: atencionRes.id,
            indicacionesGenerales: f.indicacionesGenerales,
            detalles: f.recetaDetalles.map(det => ({
              medicamento: det.medicamento,
              dosis: det.dosis,
              frecuencia: det.frecuencia,
              duracionDias: det.duracionDias,
              productoId: det.productoId
            }))
          };

          this.recetaService.generar(reqReceta).subscribe({
            next: (recetaRes) => {
              f.guardado = true;
              f.recetaId = recetaRes.id;

              const petNombre = this.citaSeleccionada!.pacientes.find(p => p.id === pacienteId)?.nombre || 'Paciente';
              this.recetasGeneradas.push({
                pacienteNombre: petNombre,
                recetaId: recetaRes.id
              });

              this.snack.open(`Atención y receta de ${petNombre} guardadas`, 'Listo', { duration: 3000, panelClass: ['snack-success'] });
              this.verificarCitaCompletada();
              this.guardandoFicha.set(false);
            },
            error: () => {
              this.snack.open('Error al generar la receta médica', 'Cerrar', { duration: 3000, panelClass: ['snack-error'] });
              this.guardandoFicha.set(false);
            }
          });
        } else {
          f.guardado = true;
          const petNombre = this.citaSeleccionada!.pacientes.find(p => p.id === pacienteId)?.nombre || 'Paciente';
          this.snack.open(`Atención de ${petNombre} guardada con éxito`, 'Listo', { duration: 3000, panelClass: ['snack-success'] });
          this.verificarCitaCompletada();
          this.guardandoFicha.set(false);
        }
      },
      error: () => {
        this.snack.open('Error al guardar el registro clínico', 'Cerrar', { duration: 3000, panelClass: ['snack-error'] });
        this.guardandoFicha.set(false);
      }
    });
  }

  verificarCitaCompletada() {
    if (!this.citaSeleccionada) return;

    // Verificamos si todos los pacientes de la cita fueron guardados en esta sesión
    const todosListos = this.citaSeleccionada.pacientes.every(pet => this.clinicoData[pet.id]?.guardado);

    if (todosListos) {
      this.citaCompletadaExito = true;
      this.citaSeleccionada.badgeCompletada = true;

      // Actualizamos listado para refrescar el estado de las citas en el backend
      this.cargarCitas();
    }
  }

  imprimirReceta(receta: any) {
    const printWindow = window.open('', '_blank');
    if (printWindow) {
      printWindow.document.write(`
        <html>
          <head>
            <title>Receta Médica - ${receta.pacienteNombre}</title>
            <style>
              body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; padding: 40px; color: #333; line-height: 1.6; }
              .header { text-align: center; border-bottom: 2px solid #14b8b8; padding-bottom: 20px; margin-bottom: 30px; }
              .logo { font-size: 24px; font-weight: bold; color: #14b8b8; }
              .subtitle { font-size: 14px; color: #666; }
              .paciente-info { display: flex; justify-content: space-between; background: #f8fafc; padding: 15px; border-radius: 8px; margin-bottom: 30px; border: 1px solid #e2e8f0; }
              .medicamentos { width: 100%; border-collapse: collapse; margin-bottom: 30px; }
              .medicamentos th, .medicamentos td { border: 1px solid #e2e8f0; padding: 12px; text-align: left; }
              .medicamentos th { background-color: #f1f5f9; color: #475569; font-weight: 600; text-transform: uppercase; font-size: 12px; }
              .footer { text-align: center; margin-top: 50px; font-size: 12px; color: #94a3b8; }
              .firma { margin-top: 50px; text-align: right; }
              .firma-linea { display: inline-block; border-top: 1px solid #333; padding-top: 5px; width: 200px; text-align: center; }
            </style>
          </head>
          <body>
            <div class="header">
              <div class="logo">CLÍNICA VETERINARIA</div>
              <div class="subtitle">Hoja de Prescripción Médica</div>
            </div>
            
            <div class="paciente-info">
              <div>
                <strong>Paciente:</strong> ${receta.pacienteNombre}<br>
                <strong>Fecha:</strong> ${new Date().toLocaleDateString()}
              </div>
              <div style="text-align: right;">
                <strong>Receta N°:</strong> ${receta.recetaId}
              </div>
            </div>

            <table class="medicamentos">
              <thead>
                <tr>
                  <th>Medicamento</th>
                  <th>Dosis</th>
                  <th>Frecuencia</th>
                  <th>Duración</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td colspan="4" style="text-align: center; color: #666; font-style: italic;">
                    Consulte el historial clínico para ver los medicamentos específicos recetados.
                  </td>
                </tr>
              </tbody>
            </table>
            
            <div class="firma">
              <div class="firma-linea">
                <strong>Firma y Sello del Médico</strong>
              </div>
            </div>

            <div class="footer">
              Este documento es una receta médica oficial de la clínica. Su validez es de 30 días.
            </div>
          </body>
        </html>
      `);
      printWindow.document.close();
      printWindow.focus();
      printWindow.print();
    }
  }

  cerrarPanelExito() {
    this.citaSeleccionada = null;
    this.citaCompletadaExito = false;
    this.recetasGeneradas = [];
  }
}