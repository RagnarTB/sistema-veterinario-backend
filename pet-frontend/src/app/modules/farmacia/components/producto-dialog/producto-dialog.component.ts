import { Component, Inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTabsModule } from '@angular/material/tabs';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ProductoService } from '../../services/producto.service';
import { CatalogoService, CategoriaProducto, UnidadMedida, Proveedor } from '../../services/catalogo.service';
import { InventarioService, LoteInventario, MovimientoInventario } from '../../services/inventario.service';
import { IngresoStockDialogComponent } from '../ingreso-stock-dialog/ingreso-stock-dialog.component';
import { SalidaStockDialogComponent } from '../salida-stock-dialog/salida-stock-dialog.component';

@Component({
  selector: 'app-producto-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, MatDialogModule, MatTabsModule],
  templateUrl: './producto-dialog.component.html',
  styleUrl: './producto-dialog.component.css'
})
export class ProductoDialogComponent implements OnInit {
  form: FormGroup;
  cargando = signal(false);
  confirmando = signal(false);
  tabIndex = signal(0);
  haCambiadoStock = signal(false);
  editandoLoteId = signal<number | null>(null);
  loteEditData = {
    numeroLote: '',
    fechaVencimiento: '',
    proveedorId: null as number | null,
    motivo: ''
  };
  fechaHoy = new Date().toISOString().split('T')[0];

  categorias = signal<CategoriaProducto[]>([]);
  unidades = signal<UnidadMedida[]>([]);
  proveedores = signal<Proveedor[]>([]);
  lotes = signal<LoteInventario[]>([]);
  movimientos = signal<MovimientoInventario[]>([]);

  // Paginación
  pageSizeLotes = 5;
  currentPageLotes = signal(0);
  lotesPaginados = () => this.lotes().slice(this.currentPageLotes() * this.pageSizeLotes, (this.currentPageLotes() + 1) * this.pageSizeLotes);
  totalPaginasLotes = () => Math.ceil(this.lotes().length / this.pageSizeLotes);

  pageSizeKardex = 5;
  currentPageStock = signal(0);
  movimientosStock = () => this.movimientos().filter(m => m.cantidad > 0 || !m.motivo?.startsWith('EDICIÓN LOTE'));
  movimientosStockPaginados = () => this.movimientosStock().slice(this.currentPageStock() * this.pageSizeKardex, (this.currentPageStock() + 1) * this.pageSizeKardex);
  totalPaginasStock = () => Math.ceil(this.movimientosStock().length / this.pageSizeKardex);

  currentPageEdicion = signal(0);
  movimientosEdicion = () => this.movimientos().filter(m => m.cantidad === 0 && m.motivo?.startsWith('EDICIÓN LOTE'));
  movimientosEdicionPaginados = () => this.movimientosEdicion().slice(this.currentPageEdicion() * this.pageSizeKardex, (this.currentPageEdicion() + 1) * this.pageSizeKardex);
  totalPaginasEdicion = () => Math.ceil(this.movimientosEdicion().length / this.pageSizeKardex);

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<ProductoDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { 
      isEditing: boolean; 
      producto?: any; 
      sedeId: number; 
      readOnly?: boolean 
    },
    private productoService: ProductoService,
    private catalogoService: CatalogoService,
    private inventarioService: InventarioService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {
    this.form = this.fb.group({
      nombre: ['', Validators.required],
      marca: [''],
      descripcion: [''],
      precio: [0, [Validators.required, Validators.min(0)]],
      categoriaId: [null],
      unidadCompraId: [null],
      unidadVentaId: [null],
      factorConversion: [1, [Validators.required, Validators.min(0.01)]]
    });
  }

  ngOnInit() {
    this.cargarCatalogos();

    if (this.data.isEditing && this.data.producto) {
      this.form.patchValue(this.data.producto);
      if (this.data.readOnly) this.form.disable();
      this.cargarLotes();
      this.cargarMovimientos();
    }
  }

  cargarCatalogos() {
    this.catalogoService.listarCategorias().subscribe(res => this.categorias.set(res));
    this.catalogoService.listarUnidades().subscribe(res => this.unidades.set(res));
    this.catalogoService.listarProveedores().subscribe(res => this.proveedores.set(res));
  }

  normalizar(texto: string): string {
    if (!texto) return '';
    return texto.toLowerCase()
      .replace(/[^a-z0-9áéíóúñ\s]/g, '')
      .replace(/\s+/g, ' ')
      .trim();
  }

  obtenerNombreCat(): string {
    const id = this.form.get('categoriaId')?.value;
    return this.categorias().find(c => c.id === id)?.nombre || 'Sin categoría';
  }

  obtenerNombreUni(control: string): string {
    const id = this.form.get(control)?.value;
    return this.unidades().find(u => u.id === id)?.nombre || 'Sin unidad';
  }

  irAConfirmar() {
    if (this.form.invalid) return;
    this.confirmando.set(true);
  }

  guardar() {
    this.cargando.set(true);

    // Los datos ya se normalizan en el backend, pero los mandamos limpios
    const values = {
      ...this.form.value,
      nombre: this.normalizar(this.form.value.nombre),
      marca: this.normalizar(this.form.value.marca)
    };

    const obs$ = this.data.isEditing
      ? this.productoService.actualizar(this.data.producto.id, values)
      : this.productoService.crear(values);

    obs$.subscribe({
      next: () => {
        this.snackBar.open('Producto guardado exitosamente', 'Cerrar', { duration: 3000 });
        this.dialogRef.close(true);
      },
      error: (err) => {
        this.snackBar.open(err.error?.message || 'Error al guardar el producto', 'Cerrar', { duration: 3000 });
        this.cargando.set(false);
      }
    });
  }

  cargarLotes() {
    const sedeId = this.data.sedeId || Number(localStorage.getItem('vet_sede_id')) || 1;
    this.inventarioService.obtenerLotes(this.data.producto.id, sedeId).subscribe({
      next: (res) => {
        // Enriquecer lotes con el último motivo de edición si existe en el kardex
        const lotesEnriquecidos = res.map(lote => {
          const ultimaEdicion = this.movimientos()
            .filter(m => m.motivo?.includes(` -> ${lote.numeroLote}. Motivo:`))
            .sort((a, b) => new Date(b.fecha).getTime() - new Date(a.fecha).getTime())[0];
          return { ...lote, ultimoMotivo: ultimaEdicion?.motivo };
        });
        this.lotes.set(lotesEnriquecidos);
        
        // Actualizar el stock actual en memoria para que las salidas reflejen el valor correcto
        const totalStock = res.reduce((acc, lote) => acc + (lote.stockRestante || 0), 0);
        if (this.data.producto) {
          this.data.producto.stockActual = totalStock;
        }
      },
      error: () => {}
    });
  }

  abrirIngresoStock() {
    // Buscar la unidad para pasar la info de decimales
    const uniId = this.data.producto.unidadCompraId;
    const unidad = this.unidades().find(u => u.id === uniId);

    const dialogRef = this.dialog.open(IngresoStockDialogComponent, {
      width: '500px',
      data: {
        producto: {
          ...this.data.producto,
          unidadCompraPermiteDecimales: unidad ? unidad.permiteDecimales : false
        },
        proveedores: this.proveedores(),
        sedeId: this.data.sedeId
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.haCambiadoStock.set(true);
        this.cargarLotes();
        this.cargarMovimientos();
      }
    });
  }

  abrirSalidaStock() {
    const uniId = this.data.producto.unidadVentaId;
    const unidad = this.unidades().find(u => u.id === uniId);

    const dialogRef = this.dialog.open(SalidaStockDialogComponent, {
      width: '500px',
      data: {
        producto: {
          ...this.data.producto,
          unidadVentaPermiteDecimales: unidad ? unidad.permiteDecimales : false
        },
        sedeId: this.data.sedeId
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.haCambiadoStock.set(true);
        this.cargarLotes();
        this.cargarMovimientos();
      }
    });
  }

  estaPorVencer(fecha: string): boolean {
    if (!fecha) return false;
    const hoy = new Date();
    const fVencimiento = new Date(fecha);
    const diffTime = fVencimiento.getTime() - hoy.getTime();
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    return diffDays <= 30;
  }

  cerrar() {
    this.dialogRef.close(this.haCambiadoStock());
  }

  cargarMovimientos() {
    const sedeId = this.data.sedeId || Number(localStorage.getItem('vet_sede_id')) || 1;
    this.inventarioService.obtenerMovimientos(this.data.producto.id, sedeId).subscribe({
      next: (res) => this.movimientos.set(res),
      error: () => {}
    });
  }

  esEntrada(tipo: string): boolean {
    return tipo === 'ENTRADA_COMPRA' || tipo === 'AJUSTE_POSITIVO';
  }

  traducirTipo(tipo: string): string {
    const map: Record<string, string> = {
      'ENTRADA_COMPRA': 'Compra',
      'SALIDA_VENTA': 'Venta',
      'SALIDA_CONSUMO_INTERNO': 'Consumo Interno',
      'AJUSTE_POSITIVO': 'Ajuste (+)',
      'AJUSTE_NEGATIVO': 'Ajuste (-)',
      'MERMA_VENCIMIENTO': 'Merma'
    };
    return map[tipo] || tipo;
  }

  loteOriginal: any = null;

  iniciarEdicionLote(lote: any) {
    this.editandoLoteId.set(lote.id);
    this.loteOriginal = { ...lote };
    this.loteEditData = {
      numeroLote: lote.numeroLote,
      fechaVencimiento: lote.fechaVencimiento || '',
      proveedorId: null,
      motivo: ''
    };
    // Intentar buscar el proveedor ID por nombre si no viene
    const prov = this.proveedores().find(p => p.razonSocial === lote.proveedorNombre);
    if (prov) this.loteEditData.proveedorId = prov.id;
  }

  cancelarEdicionLote() {
    this.editandoLoteId.set(null);
  }

  guardarEdicionLote() {
    // 1. Verificar si hubo cambios reales
    const provOriginal = this.proveedores().find(p => p.razonSocial === this.loteOriginal.proveedorNombre);
    const idProvOriginal = provOriginal ? provOriginal.id : null;

    const huboCambios = 
      this.loteEditData.numeroLote !== this.loteOriginal.numeroLote ||
      this.loteEditData.fechaVencimiento !== (this.loteOriginal.fechaVencimiento || '') ||
      this.loteEditData.proveedorId !== idProvOriginal;

    if (!huboCambios) {
      this.snackBar.open('No se detectaron cambios en el lote. Puede cerrar con (X).', 'Entendido', { duration: 3000 });
      this.cancelarEdicionLote();
      return;
    }

    if (this.loteEditData.fechaVencimiento && this.loteEditData.fechaVencimiento < this.fechaHoy) {
      this.snackBar.open('La fecha de vencimiento no puede ser anterior a hoy', 'Cerrar', { duration: 3000 });
      return;
    }

    if (!this.loteEditData.motivo) {
      this.snackBar.open('Debe indicar un motivo para el cambio de datos', 'Cerrar', { duration: 3000 });
      return;
    }

    this.inventarioService.actualizarLote(this.editandoLoteId()!, this.loteEditData).subscribe({
      next: () => {
        this.snackBar.open('Lote actualizado correctamente', 'Cerrar', { duration: 3000 });
        this.editandoLoteId.set(null);
        this.cargarLotes();
        this.cargarMovimientos();
      },
      error: () => this.snackBar.open('Error al actualizar el lote', 'Cerrar', { duration: 3000 })
    });
  }
}
