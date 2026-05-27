import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogRef, MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTabsModule } from '@angular/material/tabs';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CatalogoService, CategoriaProducto, UnidadMedida, Proveedor } from '../../services/catalogo.service';
import { ModalConfirmacionComponent } from '../../../../shared/components/modal-confirmacion/modal-confirmacion.component';

@Component({
  selector: 'app-gestion-catalogos-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, MatDialogModule, MatTabsModule],
  templateUrl: './gestion-catalogos-dialog.component.html',
  styleUrls: ['./gestion-catalogos-dialog.component.css']
})
export class GestionCatalogosDialogComponent implements OnInit {
  todasCategorias = signal<CategoriaProducto[]>([]);
  todasUnidades = signal<UnidadMedida[]>([]);
  todosProveedores = signal<Proveedor[]>([]);

  // Computed filters
  categoriasActivas = computed(() => this.todasCategorias().filter(c => c.activo));
  categoriasInactivas = computed(() => this.todasCategorias().filter(c => !c.activo));
  
  unidadesActivas = computed(() => this.todasUnidades().filter(u => u.activo));
  unidadesInactivas = computed(() => this.todasUnidades().filter(u => !u.activo));

  proveedoresActivos = computed(() => this.todosProveedores().filter(p => p.activo));
  proveedoresInactivos = computed(() => this.todosProveedores().filter(p => !p.activo));

  // Categorias
  nuevaCategoriaNombre = '';
  editandoCatId: number | null = null;
  editCatNombre = '';
  editCatDesc = '';

  // Unidades
  nuevaUnidadNombre = '';
  nuevaUnidadAbrev = '';
  nuevaUnidadDec = false;
  editandoUniId: number | null = null;
  editUniNombre = '';
  editUniAbrev = '';
  editUniDec = false;

  // Proveedores
  nuevoProvNombre = '';
  nuevoProvRuc = '';
  nuevoProvTelefono = '';
  nuevoProvDireccion = '';
  buscandoRuc = signal(false);
  editandoProvId: number | null = null;
  editProvNombre = '';
  editProvTelefono = '';

  constructor(
    public dialogRef: MatDialogRef<GestionCatalogosDialogComponent>,
    private dialog: MatDialog,
    private catalogoService: CatalogoService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit() {
    this.cargar();
  }

  cargar() {
    this.catalogoService.listarCategoriasTodas().subscribe(r => this.todasCategorias.set(r));
    this.catalogoService.listarUnidadesTodas().subscribe(r => this.todasUnidades.set(r));
    this.catalogoService.listarProveedoresTodas().subscribe(r => this.todosProveedores.set(r));
  }

  soloNumeros(event: KeyboardEvent) {
    const char = event.key;
    if (!/[0-9]/.test(char)) {
      event.preventDefault();
    }
  }

  // ========== CATEGORÍAS ==========
  agregarCategoria() {
    this.catalogoService.crearCategoria({ nombre: this.nuevaCategoriaNombre.trim() }).subscribe({
      next: () => { this.nuevaCategoriaNombre = ''; this.cargar(); this.msg('Categoría creada'); },
      error: () => this.msg('Error al crear categoría')
    });
  }

  iniciarEditCategoria(cat: CategoriaProducto) {
    this.editandoCatId = cat.id;
    this.editCatNombre = cat.nombre;
    this.editCatDesc = cat.descripcion || '';
  }

  guardarCategoria(cat: CategoriaProducto) {
    this.catalogoService.actualizarCategoria(cat.id, { nombre: this.editCatNombre, descripcion: this.editCatDesc }).subscribe({
      next: () => { this.editandoCatId = null; this.cargar(); this.msg('Categoría actualizada'); },
      error: () => this.msg('Error al actualizar')
    });
  }

  activarCat(cat: CategoriaProducto) {
    this.catalogoService.activarCategoria(cat.id).subscribe({
      next: () => { this.cargar(); this.msg('Categoría reactivada'); },
      error: () => this.msg('Error al reactivar')
    });
  }

  eliminarCat(cat: CategoriaProducto) {
    const dialogRef = this.dialog.open(ModalConfirmacionComponent, {
      width: '400px',
      data: {
        title: cat.activo ? 'Desactivar Categoría' : 'Eliminar Permanentemente',
        message: cat.activo 
          ? `¿Está seguro de desactivar la categoría "${cat.nombre}"?` 
          : `¿Está seguro de ELIMINAR PERMANENTEMENTE la categoría "${cat.nombre}"? Esta acción no se puede deshacer.`,
        confirmText: cat.activo ? 'Desactivar' : 'Eliminar',
        isDestructive: true
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.catalogoService.eliminarCategoria(cat.id).subscribe({
          next: (res) => { this.cargar(); this.msg(res.mensaje); },
          error: (err) => {
            const errorMsg = err?.error?.message || 'Error al eliminar';
            this.msg(errorMsg);
          }
        });
      }
    });
  }

  // ========== UNIDADES ==========
  agregarUnidad() {
    this.catalogoService.crearUnidad({ 
      nombre: this.nuevaUnidadNombre.trim(), 
      abreviatura: this.nuevaUnidadAbrev.trim() || '?',
      permiteDecimales: this.nuevaUnidadDec
    }).subscribe({
      next: () => { 
        this.nuevaUnidadNombre = ''; this.nuevaUnidadAbrev = ''; this.nuevaUnidadDec = false;
        this.cargar(); this.msg('Unidad creada'); 
      },
      error: () => this.msg('Error al crear unidad')
    });
  }

  iniciarEditUnidad(uni: UnidadMedida) {
    this.editandoUniId = uni.id;
    this.editUniNombre = uni.nombre;
    this.editUniAbrev = uni.abreviatura;
    this.editUniDec = uni.permiteDecimales || false;
  }

  guardarUnidad(uni: UnidadMedida) {
    this.catalogoService.actualizarUnidad(uni.id, { 
      nombre: this.editUniNombre, 
      abreviatura: this.editUniAbrev,
      permiteDecimales: this.editUniDec
    }).subscribe({
      next: () => { this.editandoUniId = null; this.cargar(); this.msg('Unidad actualizada'); },
      error: () => this.msg('Error al actualizar')
    });
  }

  activarUni(uni: UnidadMedida) {
    this.catalogoService.activarUnidad(uni.id).subscribe({
      next: () => { this.cargar(); this.msg('Unidad reactivada'); },
      error: () => this.msg('Error al reactivar')
    });
  }

  eliminarUni(uni: UnidadMedida) {
    const dialogRef = this.dialog.open(ModalConfirmacionComponent, {
      width: '400px',
      data: {
        title: uni.activo ? 'Desactivar Unidad' : 'Eliminar Permanentemente',
        message: uni.activo 
          ? `¿Está seguro de desactivar la unidad "${uni.nombre}"?` 
          : `¿Está seguro de ELIMINAR PERMANENTEMENTE la unidad "${uni.nombre}"? Esta acción no se puede deshacer.`,
        confirmText: uni.activo ? 'Desactivar' : 'Eliminar',
        isDestructive: true
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.catalogoService.eliminarUnidad(uni.id).subscribe({
          next: (res) => { this.cargar(); this.msg(res.mensaje); },
          error: (err) => {
            const errorMsg = err?.error?.message || 'Error al eliminar';
            this.msg(errorMsg);
          }
        });
      }
    });
  }

  // ========== PROVEEDORES ==========
  buscarRuc() {
    if (this.nuevoProvRuc.length !== 11) return;
    this.buscandoRuc.set(true);
    this.catalogoService.consultarRuc(this.nuevoProvRuc).subscribe({
      next: (res) => {
        this.nuevoProvNombre = res.razon_social || '';
        this.nuevoProvDireccion = res.direccion || '';
        this.buscandoRuc.set(false);
        this.msg('Datos del RUC obtenidos correctamente');
      },
      error: () => {
        this.buscandoRuc.set(false);
        this.msg('No se encontró el RUC o hubo un error');
      }
    });
  }

  agregarProveedor() {
    this.catalogoService.crearProveedor({
      razonSocial: this.nuevoProvNombre.trim(),
      ruc: this.nuevoProvRuc.trim(),
      telefono: this.nuevoProvTelefono.trim(),
      direccion: this.nuevoProvDireccion.trim()
    }).subscribe({
      next: () => {
        this.nuevoProvNombre = ''; this.nuevoProvRuc = ''; this.nuevoProvTelefono = ''; this.nuevoProvDireccion = '';
        this.cargar(); this.msg('Proveedor creado');
      },
      error: () => this.msg('Error al crear proveedor')
    });
  }

  iniciarEditProveedor(prov: Proveedor) {
    this.editandoProvId = prov.id;
    this.editProvNombre = prov.razonSocial;
    this.editProvTelefono = prov.telefono || '';
  }

  guardarProveedor(prov: Proveedor) {
    this.catalogoService.actualizarProveedor(prov.id, {
      razonSocial: this.editProvNombre,
      ruc: prov.ruc,
      telefono: this.editProvTelefono,
      direccion: prov.direccion
    }).subscribe({
      next: () => { this.editandoProvId = null; this.cargar(); this.msg('Proveedor actualizado'); },
      error: () => this.msg('Error al actualizar')
    });
  }

  activarProv(prov: Proveedor) {
    this.catalogoService.activarProveedor(prov.id).subscribe({
      next: () => { this.cargar(); this.msg('Proveedor reactivado'); },
      error: () => this.msg('Error al reactivar')
    });
  }

  eliminarProv(prov: Proveedor) {
    const dialogRef = this.dialog.open(ModalConfirmacionComponent, {
      width: '400px',
      data: {
        title: prov.activo ? 'Desactivar Proveedor' : 'Eliminar Permanentemente',
        message: prov.activo 
          ? `¿Está seguro de desactivar al proveedor "${prov.razonSocial}"?` 
          : `¿Está seguro de ELIMINAR PERMANENTEMENTE al proveedor "${prov.razonSocial}"? Esta acción no se puede deshacer.`,
        confirmText: prov.activo ? 'Desactivar' : 'Eliminar',
        isDestructive: true
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.catalogoService.eliminarProveedor(prov.id).subscribe({
          next: (res) => { this.cargar(); this.msg(res.mensaje); },
          error: (err) => {
            const errorMsg = err?.error?.message || 'Error al eliminar';
            this.msg(errorMsg);
          }
        });
      }
    });
  }

  private msg(text: string) {
    this.snackBar.open(text, 'Cerrar', { duration: 3000 });
  }
}
