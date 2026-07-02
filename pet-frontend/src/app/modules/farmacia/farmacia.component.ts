import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Subject, of, Observable } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap, catchError } from 'rxjs/operators';

import { CajaService } from '../../core/services/caja.service';
import { VentaService } from '../../core/services/venta.service';
import { ProductoService, Producto } from './services/producto.service';
import { ClienteService } from '../../core/services/cliente.service';
import { ExternoService } from '../../core/services/externo.service';
import { RecetaService } from '../../core/services/receta.service';
import { EspecieService } from '../../core/services/especie.service';
import { SedeService } from '../../core/services/sede.service';
import { AuthService } from '../../core/services/auth.service';
import {
  ClienteResponse,
  VentaResponse,
  EspecieResponse,
  VentaRequest,
  DetalleVentaRequest,
  PagoRequest,
  MetodoPago,
  TipoComprobante,
  ClienteRapidoRequest,
  MascotaRapidaRequest,
  SedeResponse
} from '../../core/models/models';

interface CartItem {
  id?: number;              // null for service items
  servicioId?: number;      // null for product items
  nombre: string;
  precio: number;
  cantidad: number;
  maxStock: number;
  isRetail: boolean;        // true for products, false for clinical services
}

interface SplitPayment {
  metodoPago: MetodoPago | 'DEUDA';
  monto: number;
  referencia?: string;
}

@Component({
  selector: 'app-farmacia',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatSnackBarModule
  ],
  templateUrl: './farmacia.component.html',
  styleUrls: ['./farmacia.component.css']
})
export class FarmaciaComponent implements OnInit {
  private cajaService = inject(CajaService);
  private ventaService = inject(VentaService);
  private productoService = inject(ProductoService);
  private clienteService = inject(ClienteService);
  private externoService = inject(ExternoService);
  private recetaService = inject(RecetaService);
  private especieService = inject(EspecieService);
  private SedeService = inject(SedeService);
  public authService = inject(AuthService);
  private router = inject(Router);
  private snack = inject(MatSnackBar);

  canRealizarVentas = this.authService.hasPermission('REALIZAR_VENTAS');

  // Router context signals
  private activatedRoute = inject(ActivatedRoute);

  sedeId = 1;
  sedes: SedeResponse[] = [];
  cajaCerradaBlock = signal(false);

  // Catálogo
  searchQuery = '';
  products: Producto[] = [];
  loadingProducts = signal(false);

  currentPage = 0;
  pageSize = 12;
  totalPages = 0;
  hoy = new Date().toISOString().split('T')[0];

  // Carrito Activo
  cartItems: CartItem[] = [];
  citasVentaId: number | null = null; // ID de la venta clínica cargada

  // Tickets Pendientes Drawer
  showPendingDrawer = false;
  pendingTickets: VentaResponse[] = [];
  loadingTickets = signal(false);
  loadedTicket: VentaResponse | null = null;

  // Settle Checkout Dialog
  showCheckoutModal = false;
  tipoComprobante: TipoComprobante = 'BOLETA';

  // Cliente Lookup
  clienteQuery = '';
  filteredClientes: ClienteResponse[] = [];
  selectedCliente: ClienteResponse | null = null;
  showClienteDropdown = false;
  private clienteSearch$ = new Subject<string>();

  // Pagos del Settle
  payments: SplitPayment[] = [];
  newPayMethod: MetodoPago | 'DEUDA' = 'EFECTIVO';
  newPayAmount: number | null = null;
  newPayRef = '';

  // Quick Client State
  modoClienteRapido = signal(false);
  buscandoReniec = signal(false);
  reniecConsultado = signal(false);
  registrandoRapido = signal(false);
  especies: EspecieResponse[] = [];

  rapidoDni = '';
  rapidoNombre = '';
  rapidoApellido = '';
  rapidoTelefono = '';
  rapidoEmail: string = '';
  rapidoPetNombre: string = '';
  rapidoPetEspecieId: number | null = null;
  rapidoPetRaza: string = '';
  rapidoPetSexo: string = 'MACHO';
  rapidoPetNac: string = ''; // yyyy-MM-dd

  // Variables para facturación RUC (SUNAT)
  rucNumero = '';
  rucRazonSocial = '';
  rucDireccion = '';
  buscandoRuc = signal(false);
  rucValidado = false;

  // Final Receipt printing
  completedVentaRes: VentaResponse | null = null;
  checkingOut = signal(false);

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
      },
      error: () => {
        this.sedes = [];
        this.sedeId = Number(localStorage.getItem('vet_sede_id')) || 1;
        this.verificarCaja();
      }
    });

    // Configurar buscador reactivo de clientes
    this.clienteSearch$.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(q => q.length < 2 ? of({ content: [] }) : this.clienteService.listar(0, 10, q, true))
    ).subscribe({
      next: (res) => {
        this.filteredClientes = res.content || [];
        this.showClienteDropdown = true;
      }
    });

    // Cargar especies para quick register
    this.especieService.listar().subscribe({
      next: (res) => this.especies = res.filter(e => e.activo)
    });

    // Escuchar el query parameter recetaId para pre-cargar la receta
    this.activatedRoute.queryParams.subscribe(params => {
      const recetaId = params['recetaId'];
      if (recetaId) {
        this.preCargarReceta(Number(recetaId));
      }
    });
  }

  verificarCaja() {
    this.cajaService.obtenerEstadoCaja(this.sedeId).subscribe({
      next: (res) => {
        // Bloquear si la caja está cerrada
        this.cajaCerradaBlock.set(!res.abierta);
        if (res.abierta) {
          this.cargarCatalogo();
          this.cargarTicketsClinicosPendientes();
        }
      },
      error: () => {
        this.snack.open('Error al verificar estado de caja en sede', 'Cerrar', { duration: 3000, panelClass: ['snack-error'] });
      }
    });
  }

  onSedeChange() {
    localStorage.setItem('vet_sede_id', this.sedeId.toString());
    this.verificarCaja();
  }

  obtenerNombreSedeActual(): string {
    const activeSede = this.sedes.find(s => s.id === this.sedeId);
    return activeSede ? activeSede.nombre : 'Sede Central';
  }

  obtenerDireccionSedeActual(): string {
    const activeSede = this.sedes.find(s => s.id === this.sedeId);
    return activeSede ? activeSede.direccion : 'Av. Primavera 123, Santiago de Surco - Lima';
  }

  irACaja() {
    this.router.navigate(['/app/caja']);
  }

  // ===================================
  // CATÁLOGO DE PRODUCTOS
  // ===================================
  cargarCatalogo() {
    this.loadingProducts.set(true);
    this.productoService.listar(this.searchQuery, this.currentPage, this.pageSize, this.sedeId).subscribe({
      next: (res) => {
        this.products = res.content || [];
        this.totalPages = res.totalPages;
        this.loadingProducts.set(false);
      },
      error: () => {
        this.snack.open('Error al cargar catálogo de productos', 'Cerrar', { duration: 3000, panelClass: ['snack-error'] });
        this.loadingProducts.set(false);
      }
    });
  }

  onSearchChange(q: string) {
    this.currentPage = 0;
    this.cargarCatalogo();
  }

  clearSearch() {
    this.searchQuery = '';
    this.currentPage = 0;
    this.cargarCatalogo();
  }

  goToPage(p: number) {
    this.currentPage = p;
    this.cargarCatalogo();
  }

  // ===================================
  // TICKET CLÍNICO Y DETALLES
  // ===================================
  cargarTicketsClinicosPendientes() {
    this.loadingTickets.set(true);
    this.ventaService.listar(0, 100).subscribe({
      next: (res) => {
        // Filtrar localmente por estado ACTIVA y que correspondan a una cita O a hospitalizacion
        // Para Hospitalización, se mantiene visible incluso si la venta está PAGADA o PAGADA_PARCIAL, siempre que la hospitalización siga ACTIVA.
        this.pendingTickets = (res.content || []).filter(v => {
          const esCitaPendiente = v.citaId !== null && v.estado === 'ACTIVA';
          const esHospitalizacionActiva = v.hospitalizacionId !== null && (v.estado === 'ACTIVA' || v.estado === 'PAGADA_PARCIAL' || v.estado === 'PAGADA') && v.estadoHospitalizacion === 'ACTIVA';
          return esCitaPendiente || esHospitalizacionActiva;
        });
        this.loadingTickets.set(false);
      },
      error: () => {
        this.loadingTickets.set(false);
      }
    });
  }

  toggleDrawerPending() {
    this.showPendingDrawer = !this.showPendingDrawer;
    if (this.showPendingDrawer) {
      this.cargarTicketsClinicosPendientes();
    }
  }

  loadPendingTicket(ticket: VentaResponse) {
    // Vaciar carrito previo
    this.cartItems = [];
    this.citasVentaId = ticket.id;
    this.loadedTicket = ticket;

    // Cargar cliente asociado al ticket
    if (ticket.clienteId) {
      this.clienteService.buscarPorId(ticket.clienteId).subscribe({
        next: (c) => {
          this.selectedCliente = c;
          this.clienteQuery = `${c.nombre} ${c.apellido}`;
        }
      });
    }

    // Cargar los servicios del ticket clínico
    ticket.detalles.forEach(line => {
      this.cartItems.push({
        id: line.productoId || undefined,
        servicioId: line.servicioId || undefined,
        nombre: line.nombreItem,
        precio: line.precioUnitario,
        cantidad: line.cantidad,
        maxStock: 9999,
        isRetail: line.productoId ? true : false
      });
    });

    this.showPendingDrawer = false;
    this.snack.open(
      ticket.hospitalizacionId 
        ? `🏥 Ticket de Hospitalización #${ticket.id} cargado con éxito.` 
        : `🎟️ Ticket Clínico #${ticket.id} cargado con éxito.`, 
      'Listo', 
      { duration: 3000, panelClass: ['snack-success'] }
    );
  }

  removeClinicalTicket() {
    // Remover del carrito solo los items clínicos
    this.cartItems = this.cartItems.filter(item => item.isRetail);
    this.citasVentaId = null;
    this.loadedTicket = null;
    this.clearClienteSearch();
    this.snack.open('Ticket clínico removido de la canasta.', 'Listo', { duration: 2500 });
  }

  activeClienteName(): string | null {
    return this.selectedCliente ? `${this.selectedCliente.nombre} ${this.selectedCliente.apellido}` : null;
  }

  // ===================================
  // PRECARGAR RECETA DESDE ?recetaId=X
  // ===================================
  preCargarReceta(recetaId: number) {
    this.recetaService.obtenerPorId(recetaId).subscribe({
      next: (receta) => {
        if (!receta.detalles || receta.detalles.length === 0) {
          this.snack.open('La receta no contiene medicamentos pre-configurados.', 'Listo', { duration: 3000 });
          return;
        }

        // Buscar cada producto de la receta en el catálogo de esta sede para verificar stock y precio actual
        let count = 0;
        receta.detalles.forEach(det => {
          if (det.productoId) {
            this.productoService.buscarPorId(det.productoId).subscribe({
              next: (prod) => {
                // Agregar al carrito
                this.addToCart(prod, det.cantidad || 1);
                count++;
                if (count === receta.detalles.filter(d => d.productoId).length) {
                  this.snack.open(`💊 Receta #${recetaId} pre-cargada con éxito en el carrito.`, 'Excelente', { duration: 3000, panelClass: ['snack-success'] });
                }
              }
            });
          }
        });
      },
      error: () => {
        this.snack.open('Error al pre-cargar la receta médica', 'Cerrar', { duration: 3000, panelClass: ['snack-error'] });
      }
    });
  }

  // ===================================
  // CANASTA INTERACTIVA
  // ===================================
  addToCart(prod: Producto, cantidad = 1) {
    const existing = this.cartItems.find(item => item.id === prod.id);
    if (existing) {
      if (existing.cantidad + cantidad <= (prod.stockActual || 0)) {
        existing.cantidad += cantidad;
      } else {
        this.snack.open(`Stock insuficiente de ${prod.nombre}`, 'Cerrar', { duration: 2000 });
      }
    } else {
      if (cantidad <= (prod.stockActual || 0)) {
        this.cartItems.push({
          id: prod.id,
          nombre: prod.nombre,
          precio: prod.precio,
          cantidad: cantidad,
          maxStock: prod.stockActual || 0,
          isRetail: true
        });
      } else {
        this.snack.open(`Stock insuficiente de ${prod.nombre}`, 'Cerrar', { duration: 2000 });
      }
    }
  }

  incrementQty(item: CartItem) {
    if (item.cantidad < item.maxStock) {
      item.cantidad++;
    }
  }

  decrementQty(item: CartItem) {
    if (item.cantidad > 1) {
      item.cantidad--;
    }
  }

  removeFromCart(index: number) {
    this.cartItems.splice(index, 1);
  }

  clearCart() {
    this.cartItems = [];
    this.citasVentaId = null;
    this.loadedTicket = null;
    this.clearClienteSearch();
  }

  obtenerTotalAPagar(): number {
    if (this.esTicketHospitalizacion() && this.loadedTicket) {
      return this.loadedTicket.saldoPendiente;
    }
    return this.getCartTotal();
  }

  obtenerSaldoRestante(): number {
    return this.obtenerTotalAPagar() - this.getPaymentsTotal();
  }

  getCartTotal(): number {
    return this.cartItems.reduce((acc, item) => acc + (item.precio * item.cantidad), 0);
  }

  // ===================================
  // SETTLE CHECKOUT DIALOG
  // ===================================
  openCheckoutModal() {
    this.payments = [];
    this.newPayMethod = 'EFECTIVO';
    this.newPayAmount = null;
    this.newPayRef = '';
    this.modoClienteRapido.set(false);

    // Auto asignar el monto de pago restante
    this.newPayAmount = this.obtenerTotalAPagar();

    this.showCheckoutModal = true;
  }

  closeCheckoutModal() {
    this.showCheckoutModal = false;
  }

  // CLIENT LOOKUP IN SETTLE
  onClienteSearchChange(q: string) {
    this.clienteSearch$.next(q);
  }

  selectCliente(c: ClienteResponse) {
    this.selectedCliente = c;
    this.clienteQuery = `${c.nombre} ${c.apellido}`;
    this.showClienteDropdown = false;
  }

  clearClienteSearch() {
    this.selectedCliente = null;
    this.clienteQuery = '';
    this.filteredClientes = [];
    this.showClienteDropdown = false;
  }

  // PAGO FRACCIONADO METODOS
  getPaymentsTotal(): number {
    return this.payments.reduce((acc, p) => acc + p.monto, 0);
  }

  addPayment() {
    if (this.newPayAmount === null || this.newPayAmount <= 0) return;

    // Validar monto no mayor al saldo restante
    const saldo = this.obtenerTotalAPagar() - this.getPaymentsTotal();
    let valAmount = Number(this.newPayAmount);

    if (this.esTicketHospitalizacion() && valAmount > saldo) {
      this.snack.open('El monto del abono no puede exceder el saldo pendiente.', 'Cerrar', { duration: 2500 });
      return;
    }

    if (!this.esTicketHospitalizacion() && valAmount > saldo && this.newPayMethod !== 'EFECTIVO') {
      this.snack.open('El monto del pago excede el total a pagar.', 'Cerrar', { duration: 2500 });
      return;
    }

    // Efectivo si puede exceder para vuelto
    if (this.newPayMethod === 'EFECTIVO' && valAmount > saldo) {
      // Ajustamos el efectivo al saldo real y guardamos la diferencia en la UI para vuelto
    }

    const pay: SplitPayment = {
      metodoPago: this.newPayMethod,
      monto: valAmount,
      referencia: this.newPayRef.trim() || undefined
    };

    this.payments.push(pay);
    this.newPayAmount = null;
    this.newPayRef = '';

    // Asignar el nuevo saldo por pagar
    const nuevoSaldo = this.obtenerTotalAPagar() - this.getPaymentsTotal();
    if (nuevoSaldo > 0) {
      this.newPayAmount = nuevoSaldo;
    }
  }

  removePayment(index: number) {
    this.payments.splice(index, 1);
    this.newPayAmount = this.obtenerTotalAPagar() - this.getPaymentsTotal();
  }

  getDebtAmount(): number {
    // Retorna el monto diferido como DEUDA
    return this.payments
      .filter(p => p.metodoPago === 'DEUDA')
      .reduce((acc, p) => acc + p.monto, 0);
  }

  exceedsDebtLimit(): boolean {
    if (!this.selectedCliente) return false;
    const currentDebt = this.selectedCliente.deudaAcumulada || 0;
    const additionalDebt = this.getDebtAmount();
    return (currentDebt + additionalDebt) > 200;
  }

  isCheckoutValid(): boolean {
    const totalPayments = this.getPaymentsTotal();
    const totalCart = this.obtenerTotalAPagar();

    // Si hay deuda ingresada, el cliente es OBLIGATORIO
    const debtAmount = this.getDebtAmount();
    if (debtAmount > 0) {
      if (!this.selectedCliente) return false;
      if (this.exceedsDebtLimit()) return false;
    }

    if (this.esTicketHospitalizacion()) {
      // Para Hospitalización (Abono Libre), no se exige cuadrar con el total, solo que haya un pago > 0 (sin contar DEUDA).
      const pagosReales = this.payments.filter(p => p.metodoPago !== 'DEUDA').reduce((acc, p) => acc + p.monto, 0);
      return pagosReales > 0 && pagosReales <= totalCart;
    }

    // Los pagos deben cubrir el total
    // Si se paga en Efectivo, se permite exceder (vuelto)
    const hasEfectivo = this.payments.some(p => p.metodoPago === 'EFECTIVO');
    if (hasEfectivo) {
      return totalPayments >= totalCart;
    }

    // Si no hay efectivo, debe ser exactamente igual
    return Math.abs(totalPayments - totalCart) < 0.01;
  }

  esTicketHospitalizacion(): boolean {
    const ticket = this.pendingTickets.find(t => t.id === this.citasVentaId);
    return ticket ? ticket.hospitalizacionId != null : false;
  }

  // ===================================
  // PROCESAMIENTO DEL COBRO & CHECKOUT
  // ===================================
  confirmCheckout() {
    if (!this.isCheckoutValid()) return;

    this.checkingOut.set(true);

    // Separamos los items de farmacia (Retail) y los del servicio clínico
    const retailItems = this.cartItems.filter(item => item.isRetail);
    const serviceItems = this.cartItems.filter(item => !item.isRetail);

    // Flujo 1: Liquidar el ticket clínico preexistente en el backend si lo hay
    if (this.citasVentaId) {
      const servicePayments: PagoRequest[] = [];
      if (this.esTicketHospitalizacion()) {
        // En Abono Libre de hospitalización, todos los pagos (excepto DEUDA) se abonan directamente al ticket "EN CURSO"
        this.payments.forEach(pay => {
          if (pay.metodoPago !== 'DEUDA' && pay.monto > 0) {
            servicePayments.push({
              monto: pay.monto,
              metodoPago: pay.metodoPago as MetodoPago,
              referencia: pay.referencia,
              sedeId: this.sedeId
            });
          }
        });
      } else {
        // Para tickets normales, calculamos la porción de los pagos aplicados que se imputan a pagar el ticket clínico
        const serviceTotal = serviceItems.reduce((acc, item) => acc + (item.precio * item.cantidad), 0);
        let distributed = 0;

        this.payments.forEach(pay => {
          if (distributed < serviceTotal && pay.metodoPago !== 'DEUDA') {
            const available = pay.monto;
            const remainingToCover = serviceTotal - distributed;
            const toApply = Math.min(available, remainingToCover);

            servicePayments.push({
              monto: toApply,
              metodoPago: pay.metodoPago as MetodoPago,
              referencia: pay.referencia,
              sedeId: this.sedeId
            });

            distributed += toApply;
          }
        });
      }

      // Si quedan remanentes impagos en Efectivo, Plin o Yape para la parte de servicios clínicos
      // el resto se asume como deuda (ya que la deuda se calcula automáticamente en el backend como saldoPendiente)

      // Llamamos en serie al backend para liquidar
      // Si el ticket se pagó parcialmente o en su totalidad, registramos cada pago individual
      let sequentialPayments$: Observable<any> = of(null);

      servicePayments.forEach(p => {
        sequentialPayments$ = sequentialPayments$.pipe(
          switchMap(() => this.ventaService.registrarPago(this.citasVentaId!, p))
        );
      });

      sequentialPayments$.subscribe({
        next: () => {
          if (this.esTicketHospitalizacion()) {
            // El abono se registró, mostramos mensaje de éxito y vaciamos el carrito sin emitir boleta de cierre
            this.snack.open('Abono registrado correctamente en el ticket de Hospitalización.', 'Éxito', { duration: 4000, panelClass: ['snack-success'] });
            this.checkingOut.set(false);
            this.showCheckoutModal = false;
            this.clearCart();
            this.verificarCaja();
          } else {
            // Si ADEMÁS se agregaron productos de farmacia físicos, los creamos como una Venta RETAIL separada!
            if (retailItems.length > 0) {
              this.crearVentaRetailSeparada(retailItems);
            } else {
              // Completado solo servicios clínicos
              this.finalizarVentaSettle(this.citasVentaId!);
            }
          }
        },
        error: (err) => {
          this.snack.open('Error al procesar el pago del ticket clínico', 'Cerrar', { duration: 4000 });
          this.checkingOut.set(false);
        }
      });

    } else {
      // Flujo 2: Solo contiene productos de farmacia retail (venta directa estándar)
      this.crearVentaRetailSeparada(retailItems);
    }
  }

  crearVentaRetailSeparada(retailItems: CartItem[]) {
    // Calculamos el total retail y los pagos correspondientes a esta venta
    const retailTotal = retailItems.reduce((acc, item) => acc + (item.precio * item.cantidad), 0);

    // Obtenemos los pagos que no se distribuyeron al servicio, o si es venta directa, todos
    const retailPayments: PagoRequest[] = [];

    // Si hubo cobro conjunto de cita + farmacia:
    if (this.citasVentaId) {
      const serviceTotal = this.cartItems.filter(item => !item.isRetail).reduce((acc, item) => acc + (item.precio * item.cantidad), 0);
      let distributed = 0;

      this.payments.forEach(pay => {
        if (pay.metodoPago !== 'DEUDA') {
          // Ya consumido por servicios
          const toService = Math.min(pay.monto, Math.max(0, serviceTotal - distributed));
          distributed += toService;
          const remainderForRetail = pay.monto - toService;

          if (remainderForRetail > 0) {
            retailPayments.push({
              monto: remainderForRetail,
              metodoPago: pay.metodoPago as MetodoPago,
              referencia: pay.referencia,
              sedeId: this.sedeId
            });
          }
        }
      });
    } else {
      // Venta directa estándar
      this.payments.forEach(p => {
        if (p.metodoPago !== 'DEUDA') {
          retailPayments.push({
            monto: p.monto,
            metodoPago: p.metodoPago as MetodoPago,
            referencia: p.referencia,
            sedeId: this.sedeId
          });
        }
      });
    }

    // DTO para la creación de la venta retail
    const requestDTO: VentaRequest = {
      sedeId: this.sedeId,
      clienteId: this.selectedCliente ? this.selectedCliente.id : undefined,
      tipoComprobante: this.tipoComprobante,
      ruc: this.tipoComprobante === 'FACTURA' && this.rucValidado ? this.rucNumero : undefined,
      razonSocial: this.tipoComprobante === 'FACTURA' && this.rucValidado ? this.rucRazonSocial : undefined,
      direccionFacturacion: this.tipoComprobante === 'FACTURA' && this.rucValidado ? this.rucDireccion : undefined,
      pagos: retailPayments,
      detalles: retailItems.map(item => ({
        productoId: item.id,
        cantidad: item.cantidad
      }))
    };

    this.ventaService.crear(requestDTO).subscribe({
      next: (res) => {
        this.finalizarVentaSettle(res.id);
      },
      error: (err) => {
        const errorMsg = err.error?.message || err.error?.mensaje || 'Error al emitir el comprobante de farmacia';
        this.snack.open(errorMsg, 'Cerrar', { duration: 4000, panelClass: ['snack-error'] });
        this.checkingOut.set(false);
      }
    });
  }

  finalizarVentaSettle(ventaId: number) {
    this.ventaService.buscarPorId(ventaId).subscribe({
      next: (ventaCompleta) => {
        this.completedVentaRes = ventaCompleta;
        this.checkingOut.set(false);
        this.showCheckoutModal = false;
        this.clearCart();
        this.verificarCaja(); // Actualizar catálogo de stocks
        this.snack.open('Venta procesada y comprobante emitido.', 'Éxito', { duration: 3000, panelClass: ['snack-success'] });
      },
      error: () => {
        this.checkingOut.set(false);
        this.showCheckoutModal = false;
        this.clearCart();
        this.verificarCaja();
      }
    });
  }

  cerrarComprobanteFinal() {
    this.completedVentaRes = null;
  }

  imprimirComprobante() {
    window.print();
  }

  // ===================================
  // QUICK CLIENT REGISTRATION
  // ===================================
  enableQuickClientMode() {
    this.modoClienteRapido.set(true);
    this.rapidoDni = '';
    this.rapidoNombre = '';
    this.rapidoApellido = '';
    this.rapidoTelefono = '';
    this.rapidoEmail = '';
    this.rapidoPetNombre = '';
    this.rapidoPetEspecieId = 0;
    this.rapidoPetRaza = '';
    this.rapidoPetNac = '';
  }

  // Limpiar campos al borrar dígitos del DNI (menos de 8 dígitos)
  onDniChange() {
    this.rapidoDni = this.rapidoDni.replace(/[^0-9]/g, '');
    if (this.rapidoDni.length < 8) {
      this.rapidoNombre = '';
      this.rapidoApellido = '';
      this.rapidoEmail = '';
      this.reniecConsultado.set(false);
    }
  }

  onPhoneChange() {
    this.rapidoTelefono = this.rapidoTelefono.replace(/[^0-9]/g, '');
  }

  // Limpiar RUC si el comprobante cambia a boleta
  onTipoComprobanteChange() {
    if (this.tipoComprobante === 'BOLETA') {
      this.rucNumero = '';
      this.rucRazonSocial = '';
      this.rucDireccion = '';
      this.rucValidado = false;
    }
  }

  // Limpiar validacion de ruc si el usuario borra algun numero
  onRucChange() {
    if (this.rucNumero.length !== 11) {
      this.rucRazonSocial = '';
      this.rucDireccion = '';
      this.rucValidado = false;
    }
  }

  cancelQuickClientMode() {
    this.modoClienteRapido.set(false);
  }

  buscarEnReniec() {
    if (this.rapidoDni.length !== 8) return;
    this.buscandoReniec.set(true);
    this.externoService.consultarDni(this.rapidoDni).pipe(
      catchError(() => of(null))
    ).subscribe({
      next: (res) => {
        this.buscandoReniec.set(false);
        if (res) {
          // Si ya está en BD, auto-seleccionar y salir del modo rápido
          if (res.existe_en_bd && res.cliente_id) {
            this.snack.open('DNI ya registrado. Vinculando automáticamente...', 'Listo', { duration: 4000 });
            const mappedClient: ClienteResponse = {
              id: res.cliente_id,
              nombre: res.first_name || '',
              apellido: [res.first_last_name, res.second_last_name].filter(Boolean).join(' '),
              dni: res.document_number || this.rapidoDni,
              telefono: res.telefono || '',
              email: res.email || '',
              activo: true,
              verificado: false,
              esInvitado: false,
              deudaAcumulada: 0
            };
            this.selectCliente(mappedClient);
            this.modoClienteRapido.set(false);
            return;
          }

          const nombres = res.first_name || '';
          const apellidos = [res.first_last_name, res.second_last_name].filter(Boolean).join(' ');
          this.rapidoNombre = nombres;
          this.rapidoApellido = apellidos;
          this.snack.open('RENIEC: Cliente localizado con éxito', 'OK', { duration: 2500 });
        } else {
          this.snack.open('DNI no localizado en RENIEC. Ingrese los datos manualmente.', 'Cerrar', { duration: 3000 });
        }
      },
      error: () => {
        this.buscandoReniec.set(false);
      }
    });
  }

  // Búsqueda SUNAT por RUC para Facturas
  buscarRuc() {
    if (this.rucNumero.length !== 11) {
      this.snack.open('El RUC debe tener 11 dígitos.', 'OK', { duration: 3000 });
      return;
    }
    this.buscandoRuc.set(true);
    this.rucValidado = false;
    this.externoService.consultarRuc(this.rucNumero).subscribe({
      next: (res) => {
        this.buscandoRuc.set(false);
        if (res && res.razon_social) {
          this.rucRazonSocial = res.razon_social;
          this.rucDireccion = res.direccion || '';
          this.rucValidado = true;
          this.snack.open('SUNAT: RUC Validado con éxito', 'Listo', { duration: 3000, panelClass: ['snack-success'] });
        } else {
          this.rucRazonSocial = '';
          this.rucDireccion = '';
          this.snack.open('RUC no localizado en SUNAT. Verifique el número.', 'Cerrar', { duration: 3000 });
        }
      },
      error: () => {
        this.buscandoRuc.set(false);
        this.snack.open('Error al consultar SUNAT. Intente nuevamente.', 'Cerrar', { duration: 3000 });
      }
    });
  }

  registrarClienteRapido() {
    // Validar DNI (8 digitos numericos)
    if (!this.rapidoDni || !/^\d{8}$/.test(this.rapidoDni)) {
      this.snack.open('El DNI debe tener exactamente 8 números.', 'Cerrar', { duration: 3000 });
      return;
    }
    // Validar nombre y apellido
    if (!this.rapidoNombre.trim() || !this.rapidoApellido.trim()) {
      this.snack.open('El nombre y apellido son obligatorios.', 'Cerrar', { duration: 3000 });
      return;
    }
    // Validar Telefono si se ingresó
    if (this.rapidoTelefono && !/^\d{0,9}$/.test(this.rapidoTelefono)) {
      this.snack.open('El teléfono solo debe contener números (máx 9 dígitos).', 'Cerrar', { duration: 3000 });
      return;
    }
    // Validar Email si se ingresó
    if (this.rapidoEmail) {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailRegex.test(this.rapidoEmail)) {
        this.snack.open('El formato del correo electrónico es inválido.', 'Cerrar', { duration: 3000 });
        return;
      }
    }
    // Validar Mascota
    if (!this.rapidoPetNombre.trim() || !this.rapidoPetEspecieId || !this.rapidoPetNac) {
      this.snack.open('Debe ingresar el nombre de la mascota, la especie y la fecha de nacimiento.', 'Cerrar', { duration: 3000 });
      return;
    }

    this.registrandoRapido.set(true);

    const mascota: MascotaRapidaRequest = {
      nombre: this.rapidoPetNombre,
      especieId: this.rapidoPetEspecieId,
      raza: this.rapidoPetRaza || undefined,
      sexo: this.rapidoPetSexo,
      fechaNacimiento: this.rapidoPetNac
    };

    const req: ClienteRapidoRequest = {
      dni: this.rapidoDni,
      nombre: this.rapidoNombre,
      apellido: this.rapidoApellido,
      telefono: this.rapidoTelefono || undefined,
      email: this.rapidoEmail || undefined, // Vincular email para futura cuenta formal
      mascotas: [mascota]
    };

    this.clienteService.crearRapido(req).subscribe({
      next: (res) => {
        this.registrandoRapido.set(false);
        this.modoClienteRapido.set(false);

        // Auto-seleccionar al cliente creado en la UI
        const mappedClient: ClienteResponse = {
          id: res.clienteId,
          nombre: res.nombre,
          apellido: res.apellido,
          dni: res.dni,
          telefono: res.telefono || '',
          email: '',
          activo: true,
          verificado: false,
          esInvitado: res.esInvitado,
          deudaAcumulada: 0
        };

        this.selectCliente(mappedClient);
        this.snack.open('¡Cliente Rápido registrado y vinculado!', 'Éxito', { duration: 3000, panelClass: ['snack-success'] });
      },
      error: (err) => {
        const errorMsg = err.error?.message || err.error?.mensaje || 'Error al registrar cliente rápido';
        this.snack.open(errorMsg, 'Cerrar', { duration: 4000, panelClass: ['snack-error'] });
        this.registrandoRapido.set(false);
      }
    });
  }
}