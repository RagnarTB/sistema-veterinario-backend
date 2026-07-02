import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { RouterModule } from '@angular/router';
import { PacienteService } from '../../core/services/paciente.service';
import { CitaService } from '../../core/services/cita.service';
import { SedeService } from '../../core/services/sede.service';
import { PacienteResponse, CitaResponse } from '../../core/models/models';

@Component({
  selector: 'app-dashboard-cliente',
  standalone: true,
  imports: [
    CommonModule, 
    MatCardModule, 
    MatIconModule, 
    MatButtonModule, 
    RouterModule
  ],
  templateUrl: './dashboard-cliente.component.html',
  styleUrls: ['./dashboard-cliente.component.css']
})
export class DashboardClienteComponent implements OnInit {
  private pacienteService = inject(PacienteService);
  private citaService = inject(CitaService);
  private sedeService = inject(SedeService);

  totalMascotas = signal(0);
  proximasCitas = signal(0);
  mascotas = signal<PacienteResponse[]>([]);
  citasRecientes = signal<CitaResponse[]>([]);

  ngOnInit() {
    this.cargarEstadisticas();
  }

  cargarEstadisticas() {
    // El backend filtra automáticamente por el cliente autenticado
    this.pacienteService.listar(0, 5).subscribe({
      next: (res) => {
        this.totalMascotas.set(res.totalElements);
        this.mascotas.set(res.content);
      },
      error: () => {} // silenciar errores
    });

    // Obtener la primera sede activa y buscar citas del cliente
    this.sedeService.listarActivas().subscribe({
      next: (sedes) => {
        if (sedes.length > 0) {
          this.citaService.listar(sedes[0].id, 0, 5).subscribe({
            next: (res) => {
              this.proximasCitas.set(res.totalElements);
              this.citasRecientes.set(res.content);
            },
            error: () => {}
          });
        }
      },
      error: () => {}
    });
  }
}
