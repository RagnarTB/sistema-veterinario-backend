import { Component, OnInit, HostListener, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { CajaService, CajaEstadoResponse } from '../../core/services/caja.service';
import { VentaService } from '../../core/services/venta.service';
import { AuthService } from '../../core/services/auth.service';
import { SedeService } from '../../core/services/sede.service';
import { VentaResponse, SedeResponse } from '../../core/models/models';

interface ManualMovement {
  id: number;
  tipo: 'INGRESO' | 'EGRESO';
  monto: number;
  concepto: string;
  fecha: string;
  metodo: 'EFECTIVO' | 'YAPE' | 'PLIN';
}

@Component({
  selector: 'app-caja',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatSnackBarModule
  ],
  templateUrl: './caja.component.html',
  styleUrls: ['./caja.component.css']
})
export class CajaComponent implements OnInit {
  private cajaService = inject(CajaService);
  private ventaService = inject(VentaService);
  private snack = inject(MatSnackBar);
  private SedeService = inject(SedeService);
  private authService = inject(AuthService);

  canOperarCaja = this.authService.hasPermission('OPERAR_CAJA');

  sedeId = 1;
  sedes: SedeResponse[] = [];
  cajaAbierta = false;
  cajaCargando = signal<boolean>(false);
  operandoCaja = signal<boolean>(false);

  saldoInicialInput: number | null = null;
  estadoCaja: CajaEstadoResponse | null = null;

  // Movimientos e ingresos de ventas reales
  ventasHoy: VentaResponse[] = [];
  totalVentasReal = 0;

  // Movimientos manuales simulados locales
  manualMovements: ManualMovement[] = [];
  mostrarModalMovimiento = false;
  movimientoModalTipo: 'INGRESO' | 'EGRESO' = 'INGRESO';
  manualConcepto = '';
  manualMonto: number | null = null;
  manualMetodo: 'EFECTIVO' | 'YAPE' | 'PLIN' = 'EFECTIVO';

  // Mezcla ordenada de transacciones
  movimientosMezclados: any[] = [];

  // Cerrar caja modal & resumen final
  mostrarModalCierre = false;
  cierreResumenFinal: any = null;

  @HostListener('window:focus', ['$event'])
  onFocus(event: FocusEvent): void {
    if (this.cajaAbierta && this.sedeId) {
      this.cargarDiario();
    }
  }

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
        this.verificarCaja();
        this.cargarManualMovementsFromStorage();
      },
      error: () => {
        this.sedes = [];
        this.sedeId = Number(localStorage.getItem('vet_sede_id')) || 1;
        this.verificarCaja();
        this.cargarManualMovementsFromStorage();
      }
    });
  }

  onSedeChange() {
    localStorage.setItem('vet_sede_id', this.sedeId.toString());
    this.verificarCaja();
  }

  verificarCaja() {
    this.cajaCargando.set(true);
    this.cajaService.obtenerEstadoCaja(this.sedeId).subscribe({
      next: (res) => {
        this.estadoCaja = res;
        this.cajaAbierta = res.abierta;
        this.cajaCargando.set(false);

        if (this.cajaAbierta) {
          this.cargarDiario();
        }
      },
      error: () => {
        this.snack.open('Error al obtener estado de caja', 'Cerrar', { duration: 3000, panelClass: ['snack-error'] });
        this.cajaCargando.set(false);
      }
    });
  }

  abrirCaja() {
    if (this.saldoInicialInput === null || this.saldoInicialInput < 0) return;

    this.operandoCaja.set(true);
    this.cajaService.abrirCaja({
      sedeId: this.sedeId,
      saldoInicial: this.saldoInicialInput
    }).subscribe({
      next: () => {
        this.snack.open('¡Caja Diaria abierta exitosamente!', 'Listo', { duration: 3000, panelClass: ['snack-success'] });
        this.operandoCaja.set(false);
        this.saldoInicialInput = null;
        this.verificarCaja();
      },
      error: (err) => {
        const errorMsg = err.error?.message || err.error?.mensaje || 'Error al abrir la caja diaria';
        this.snack.open(errorMsg, 'Cerrar', { duration: 4000, panelClass: ['snack-error'] });
        this.operandoCaja.set(false);
      }
    });
  }

  cargarDiario() {
    this.ventaService.listar(0, 500).subscribe({
      next: (res) => {
        const activaCajaId = this.estadoCaja?.cajaId;
        if (activaCajaId) {
          // Filtrar ventas que nacieron en esta caja, o ventas de otra caja pero que tienen pagos hechos en ESTA caja
          this.ventasHoy = (res.content || []).filter(v => 
            v.cajaId === activaCajaId || (v.pagos && v.pagos.some(p => p.cajaId === activaCajaId))
          );
        } else {
          const hoy = new Date().toDateString();
          this.ventasHoy = (res.content || []).filter(v => new Date(v.fechaHora).toDateString() === hoy);
        }

        // Para sumar ventasReal, solo sumamos los pagos hechos EN ESTA CAJA
        // Si no hay activaCajaId (por ejemplo caja cerrada viendo historial global) usamos el viejo comportamiento.
        this.totalVentasReal = 0;
        if (activaCajaId) {
          this.ventasHoy.forEach(v => {
            if (v.pagos && v.pagos.length > 0) {
              const pagosEstaCaja = v.pagos.filter(p => p.cajaId === activaCajaId);
              this.totalVentasReal += pagosEstaCaja.reduce((sum, p) => sum + p.monto, 0);
            } else if (v.cajaId === activaCajaId) {
               // En caso de que no tenga pagos registrados pero haya sido creada en esta caja (ej deuda total)
               // (aunque en teoria deuda total es montoPagado 0)
               this.totalVentasReal += (v.montoPagado !== undefined && v.montoPagado !== null ? v.montoPagado : (v.total || 0));
            }
          });
        } else {
          this.totalVentasReal = this.ventasHoy.reduce((acc, v) => acc + (v.montoPagado !== undefined && v.montoPagado !== null ? v.montoPagado : (v.total || 0)), 0);
        }

        this.mezclarMovimientos();
      },
      error: () => {
        this.snack.open('Error al sincronizar transacciones diarias', 'Cerrar', { duration: 3000, panelClass: ['snack-error'] });
      }
    });
  }

  mezclarMovimientos() {
    const activaCajaId = this.estadoCaja?.cajaId;
    const ventasFormateadas: any[] = [];
    
    this.ventasHoy.forEach(v => {
      // Si tenemos caja activa, solo mostraremos los pagos que se hicieron EN ESTA caja
      if (activaCajaId && v.pagos && v.pagos.length > 0) {
        const pagosEstaCaja = v.pagos.filter(p => p.cajaId === activaCajaId);
        
        pagosEstaCaja.forEach((p, idx) => {
          // Si el pago pertenece a una caja actual, pero la venta se generó antes, es un cobro de deuda
          let concepto = v.cajaId === activaCajaId ? `Cobro Venta #${v.id}` : `Cobro Deuda Venta #${v.id}`;
          if (v.cajaId === activaCajaId && v.pagos!.length > 1) {
            concepto += ` (Pago ${idx + 1})`;
          }
          
          ventasFormateadas.push({
            fecha: p.fechaPago,
            concepto,
            comprobante: v.tipoComprobante || 'BOLETA',
            detalle: v.clienteNombre ? `Cliente: ${v.clienteNombre}` : 'Venta General',
            metodo: p.metodoPago || 'EFECTIVO',
            monto: p.monto,
            tipo: 'INGRESO'
          });
        });

        // Si la venta se creo en ESTA caja pero se dejo deuda parcial (montoPagado < total)
        if (v.cajaId === activaCajaId && v.saldoPendiente > 0 && pagosEstaCaja.length === 0) {
           ventasFormateadas.push({
             fecha: v.fechaHora,
             concepto: `Cobro Venta #${v.id} (A Deuda)`,
             comprobante: v.tipoComprobante || 'BOLETA',
             detalle: v.clienteNombre ? `Cliente: ${v.clienteNombre}` : 'Venta General',
             metodo: 'DEUDA',
             monto: 0,
             tipo: 'INGRESO'
           });
        }
      } else {
        // Lógica antigua (fallback para cuando no hay cajaId o no hay pagos definidos)
        let concepto = `Cobro Venta #${v.id}`;
        let metodo: string = v.metodoPago || (v.pagos && v.pagos.length > 0 ? v.pagos[0].metodoPago : 'EFECTIVO');
        let monto = v.montoPagado !== undefined && v.montoPagado !== null ? v.montoPagado : v.total;

        if (v.saldoPendiente > 0 && (v.montoPagado === 0 || v.montoPagado === null || v.montoPagado === undefined)) {
          concepto = `Cobro Venta #${v.id} (A Deuda)`;
          metodo = 'DEUDA';
          monto = 0;
        } else if (v.montoPagado > 0 && v.saldoPendiente > 0) {
          concepto = `Cobro Venta #${v.id} (Pago Parcial)`;
        }

        ventasFormateadas.push({
          fecha: v.fechaHora,
          concepto,
          comprobante: v.tipoComprobante || 'BOLETA',
          detalle: v.clienteNombre ? `Cliente: ${v.clienteNombre}` : 'Venta General',
          metodo,
          monto,
          tipo: 'INGRESO'
        });
      }
    });

    // Formateamos movimientos manuales
    const manualesFormateados = this.manualMovements.map(m => ({
      fecha: m.fecha,
      concepto: m.concepto,
      comprobante: '-',
      detalle: m.tipo === 'INGRESO' ? 'Ingreso Manual Caja' : 'Salida Manual Caja',
      metodo: m.metodo,
      monto: m.monto,
      tipo: m.tipo
    }));

    // Combinamos y ordenamos por hora descendente (más recientes arriba)
    this.movimientosMezclados = [...ventasFormateadas, ...manualesFormateados].sort((a, b) =>
      new Date(b.fecha).getTime() - new Date(a.fecha).getTime()
    );
  }

  // MANUAL MOVEMENTS LOGIC
  cargarManualMovementsFromStorage() {
    const key = `vet_manual_movements_sede_${this.sedeId}`;
    const raw = localStorage.getItem(key);
    if (raw) {
      this.manualMovements = JSON.parse(raw);
    } else {
      this.manualMovements = [];
    }
  }

  guardarManualMovementsToStorage() {
    const key = `vet_manual_movements_sede_${this.sedeId}`;
    localStorage.setItem(key, JSON.stringify(this.manualMovements));
  }

  abrirModalMovimiento(tipo: 'INGRESO' | 'EGRESO') {
    this.movimientoModalTipo = tipo;
    this.manualConcepto = '';
    this.manualMonto = null;
    this.manualMetodo = 'EFECTIVO';
    this.mostrarModalMovimiento = true;
  }

  cerrarModalMovimiento() {
    this.mostrarModalMovimiento = false;
  }

  registrarMovimientoManual() {
    if (!this.manualConcepto.trim() || this.manualMonto === null || this.manualMonto <= 0) return;

    const dto = {
      sedeId: this.sedeId,
      tipo: this.movimientoModalTipo,
      monto: Number(this.manualMonto),
      concepto: this.manualConcepto,
      metodoPago: this.manualMetodo
    };

    this.cajaService.registrarMovimiento(dto).subscribe({
      next: () => {
        const nuevoMov: ManualMovement = {
          id: Date.now(),
          tipo: this.movimientoModalTipo,
          monto: Number(this.manualMonto),
          concepto: this.manualConcepto,
          fecha: new Date().toISOString(),
          metodo: this.manualMetodo
        };

        this.manualMovements.push(nuevoMov);
        this.guardarManualMovementsToStorage();
        this.snack.open(`Movimiento registrado con éxito`, 'Ok', { duration: 2500, panelClass: ['snack-success'] });

        this.mostrarModalMovimiento = false;
        this.cargarDiario();
      },
      error: () => {
        this.snack.open(`Error al registrar movimiento en el servidor`, 'Ok', { duration: 2500, panelClass: ['snack-error'] });
      }
    });
  }

  // SUMMARIES
  get sumManualIngresos(): number {
    return this.manualMovements
      .filter(m => m.tipo === 'INGRESO')
      .reduce((acc, m) => acc + m.monto, 0);
  }

  get sumManualEgresos(): number {
    return this.manualMovements
      .filter(m => m.tipo === 'EGRESO')
      .reduce((acc, m) => acc + m.monto, 0);
  }

  get totalMovimientosNeto(): number {
    return this.sumManualIngresos - this.sumManualEgresos;
  }

  get saldoActualEstimado(): number {
    if (!this.estadoCaja) return 0;
    return (this.estadoCaja.saldoInicial || 0) + this.totalVentasReal + this.totalMovimientosNeto;
  }

  // CLOSE BOX
  abrirModalCierre() {
    this.mostrarModalCierre = true;
  }

  cerrarModalCierre() {
    this.mostrarModalCierre = false;
  }

  cerrarCaja() {
    this.operandoCaja.set(true);
    this.cajaService.cerrarCaja(this.sedeId).subscribe({
      next: (res) => {
        this.operandoCaja.set(false);
        this.mostrarModalCierre = false;

        // Almacenamos el resumen de cierre devuelto por el backend
        this.cierreResumenFinal = res;

        // Limpiamos los movimientos locales
        this.manualMovements = [];
        this.guardarManualMovementsToStorage();

        // Refrescar estado de caja inmediatamente
        this.verificarCaja();
      },
      error: (err) => {
        const errorMsg = err.error?.message || err.error?.mensaje || 'Error al realizar el cierre de caja';
        this.snack.open(errorMsg, 'Cerrar', { duration: 4000, panelClass: ['snack-error'] });
        this.operandoCaja.set(false);
        this.mostrarModalCierre = false;
      }
    });
  }

  finalizarCierreVista() {
    this.cierreResumenFinal = null;
    this.cajaAbierta = false;
    this.estadoCaja = null;
    this.verificarCaja();
  }
}