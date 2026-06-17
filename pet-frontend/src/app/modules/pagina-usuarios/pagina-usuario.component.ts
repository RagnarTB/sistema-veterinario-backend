import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ClienteService } from '../../core/services/cliente.service';

@Component({
    selector: 'app-pagina-usuario',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './pagina-usuario.component.html',
    styleUrls: ['./pagina-usuario.component.scss']
})
export class PaginaUsuarioComponent implements OnInit {

    nombreUsuario = '';

    mascotas: any[] = [];

    proximaCita: any = null;

    resumenSalud: any = null;

    mascotaSeleccionada: any = null;

    cargando = true;

    tieneNotificaciones = true;

    constructor(private clienteService: ClienteService) { }

    ngOnInit(): void {
        this.clienteService.obtenerMiPerfil().subscribe({
            next: (perfil) => {
                this.nombreUsuario = perfil.nombreCliente;
                this.mascotas = perfil.mascotas;
                this.proximaCita = perfil.proximaCita;

                if (this.mascotas.length > 0) {
                    this.seleccionarMascota(this.mascotas[0]);
                }

                this.cargando = false;
            },
            error: (err) => {
                console.error('Error al cargar datos del perfil:', err);
                this.cargando = false;
            }
        });
    }

    seleccionarMascota(mascota: any): void {
        this.mascotas.forEach(m => m.seleccionada = (m.id === mascota.id));
        this.mascotaSeleccionada = mascota;
        this.resumenSalud = mascota.resumenSalud;
    }

    agregarMascota(): void {
        console.log('Agregar nueva mascota');
    }

    verDetallesCita(): void {
        console.log('Ver detalles de la cita');
    }

    agendarCita(): void {
        console.log('Agendar nueva cita');
    }

    verHistorialCompleto(): void {
        console.log('Ver historial completo');
    }

    verTodasMascotas(): void {
        console.log('Ver todas las mascotas');
    }

    get pesoPositivo(): boolean {
        return this.resumenSalud?.peso?.cambio > 0;
    }

    get pesoNegativo(): boolean {
        return this.resumenSalud?.peso?.cambio < 0;
    }

    get cambioAbsoluto(): string {
        if (!this.resumenSalud?.peso) return '';

        const abs = Math.abs(this.resumenSalud.peso.cambio);
        return `${this.pesoNegativo ? '-' : '+'}${abs}kg`;
    }
}