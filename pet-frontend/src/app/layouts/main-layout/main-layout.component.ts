import { Component, signal, computed, HostListener, OnInit, OnDestroy } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatBadgeModule } from '@angular/material/badge';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { AuthService } from '../../core/services/auth.service';
import { HospitalizacionWebsocketService } from '../../core/services/hospitalizacion-websocket.service';
import { RolNombre } from '../../core/models/models';
import { CambiarPasswordDialogComponent } from './cambiar-password-dialog.component';
import { Subscription } from 'rxjs';

interface NavItem {
  label: string;
  icon: string;
  route: string;
  roles?: RolNombre[];
}

interface NavGroup {
  label: string;
  icon: string;
  roles?: RolNombre[];
  expanded?: boolean;
  children: NavItem[];
}

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatTooltipModule,
    MatMenuModule,
    MatButtonModule,
    MatDividerModule,
    MatDialogModule,
    MatBadgeModule,
    MatSnackBarModule
  ],

  templateUrl: './main-layout.component.html',
  styleUrls: ['./main-layout.component.css'],

})
export class MainLayoutComponent implements OnInit, OnDestroy {
  isCollapsed = signal(false);
  isMobileOpen = signal(false);
  showMobileOverlay = computed(() => this.isMobileOpen());

  protected readonly navGroups: NavGroup[] = [
    {
      label: 'Panel de Control',
      icon: 'dashboard',
      roles: ['ROLE_ADMIN'],
      expanded: false,
      children: [
        { label: 'Dashboard', route: '/app/dashboard', icon: 'bar_chart' },
        { label: 'Reportes', route: '/app/reportes', icon: 'pie_chart' },
      ]
    },
    {
      label: 'Gestión Clínica',
      icon: 'medical_services',
      expanded: true,
      children: [
        { label: 'Citas', route: '/app/citas', icon: 'calendar_today' },
        { label: 'Atenciones', route: '/app/atenciones', icon: 'healing', roles: ['ROLE_ADMIN', 'ROLE_VETERINARIO'] },
        { label: 'Clientes', route: '/app/clientes', icon: 'people', roles: ['ROLE_ADMIN', 'ROLE_RECEPCIONISTA'] },
        { label: 'Pacientes', route: '/app/pacientes', icon: 'pets' },
      ]
    },
    {
      label: 'Hospitalización',
      icon: 'local_hospital',
      roles: ['ROLE_ADMIN', 'ROLE_VETERINARIO'],
      expanded: false,
      children: [
        { label: 'Panel Internados', route: '/app/hospitalizacion', icon: 'bed' },
        { label: 'Gestión de Jaulas', route: '/app/jaulas', icon: 'grid_view' }
      ]
    },
    {
      label: 'Comercial y Logística',
      icon: 'storefront',
      expanded: false,
      children: [
        { label: 'Punto de Venta', route: '/app/farmacia', icon: 'point_of_sale' },
        { label: 'Inventario', route: '/app/inventario', icon: 'inventory_2', roles: ['ROLE_ADMIN', 'ROLE_RECEPCIONISTA'] },
      ]
    },
    {
      label: 'Finanzas',
      icon: 'account_balance',
      roles: ['ROLE_ADMIN', 'ROLE_RECEPCIONISTA'],
      expanded: false,
      children: [
        { label: 'Caja Diaria', route: '/app/caja', icon: 'payments' },
        { label: 'Cuentas por Cobrar', route: '/app/finanzas/deudas', icon: 'receipt_long' },
        { label: 'Historial de Ventas', route: '/app/finanzas/ventas', icon: 'history' },
      ]
    },
    {
      label: 'Administración',
      icon: 'admin_panel_settings',
      roles: ['ROLE_ADMIN'],
      expanded: false,
      children: [
        { label: 'Empleados', route: '/app/empleados', icon: 'badge' },
        { label: 'Sedes', route: '/app/sedes', icon: 'location_on' },
        { label: 'Catálogo Servicios', route: '/app/servicios-medicos', icon: 'design_services' },
      ]
    }
  ];

  visibleNavGroups = computed(() => {
    return this.navGroups.filter(group => {
      // Si el grupo no tiene roles, o el usuario tiene alguno de los roles requeridos
      if (!group.roles || this.authService.hasAnyRole(...group.roles)) {
        // Filtrar también para asegurar que haya al menos 1 hijo visible
        return this.getVisibleChildren(group).length > 0;
      }
      return false;
    });
  });

  getVisibleChildren(group: NavGroup): NavItem[] {
    return group.children.filter(child => {
      if (!child.roles) return true;
      return this.authService.hasAnyRole(...child.roles);
    });
  }

  toggleGroup(group: NavGroup): void {
    if (this.isCollapsed()) {
      // Si está colapsado y hacen clic, lo expandimos globalmente y abrimos este grupo
      this.isCollapsed.set(false);
      group.expanded = true;
    } else {
      group.expanded = !group.expanded;
    }
  }

  constructor(
    public authService: AuthService, 
    private dialog: MatDialog,
    private wsService: HospitalizacionWebsocketService,
    private snackBar: MatSnackBar
  ) { }

  private wsSubscription?: Subscription;
  unreadAlertsCount = signal(0);

  ngOnInit() {
    // Conectar WebSocket si es veterinario
    if (this.authService.hasAnyRole('ROLE_VETERINARIO', 'ROLE_ADMIN')) {
      this.wsService.connect();
      this.wsSubscription = this.wsService.getAlerts().subscribe(mensaje => {
        if (mensaje) {
          this.unreadAlertsCount.update(c => c + 1);
          this.snackBar.open(mensaje, 'Cerrar', {
            duration: 8000,
            horizontalPosition: 'right',
            verticalPosition: 'top',
            panelClass: ['bg-red-500', 'text-white']
          });
        }
      });
    }
  }

  ngOnDestroy() {
    this.wsService.disconnect();
    if (this.wsSubscription) {
      this.wsSubscription.unsubscribe();
    }
  }

  clearAlerts() {
    this.unreadAlertsCount.set(0);
  }

  toggleSidebar(): void {
    if (window.innerWidth <= 768) {
      this.isMobileOpen.update(v => !v);
    } else {
      this.isCollapsed.update(v => !v);
    }
  }

  closeMobileSidebar(): void {
    this.isMobileOpen.set(false);
  }

  logout(): void {
    this.authService.logout();
  }

  abrirDialogoPassword() {
    this.dialog.open(CambiarPasswordDialogComponent, {
      width: '400px'
    });
  }

  getRolLabel(): string {
    const active = this.authService.activeRole();
    if (active) return this.getRoleName(active);
    // fallback
    const roles = this.authService.currentRoles();
    if (roles.includes('ROLE_ADMIN')) return 'Administrador';
    if (roles.includes('ROLE_VETERINARIO')) return 'Veterinario';
    if (roles.includes('ROLE_RECEPCIONISTA')) return 'Recepcionista';
    if (roles.includes('ROLE_CLIENTE')) return 'Cliente';
    return '';
  }

  getRoleName(rol: string): string {
    const names: Record<string, string> = {
      'ROLE_ADMIN': 'Administrador',
      'ROLE_CLIENTE': 'Cliente',
      'ROLE_VETERINARIO': 'Veterinario',
      'ROLE_RECEPCIONISTA': 'Recepcionista'
    };
    return names[rol] || rol.replace('ROLE_', '');
  }

  cambiarRol(rol: string) {
    // Si ya estamos en el rol, no hacer nada
    if (rol === this.authService.activeRole()) return;

    // Llamar al backend para regenerar el token
    this.authService.seleccionarRol(rol).subscribe({
      next: () => {
        this.authService.setActiveRole(rol as RolNombre);
        // Recargar la aplicación para aplicar todos los guards y vistas nuevas
        window.location.href = '/app';
      },
      error: () => {
        alert('Error al cambiar de rol');
      }
    });
  }

  @HostListener('window:resize')
  onResize(): void {
    if (window.innerWidth > 768) {
      this.isMobileOpen.set(false);
    }
  }
}
