import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTabsModule } from '@angular/material/tabs';

// Importar los componentes hijos que irán en las pestañas
import { ListaJaulasComponent } from './lista-jaulas/lista-jaulas';
import { CategoriaJaulaComponent } from './configuracion/categoria-jaula/categoria-jaula.component';
import { TamanoJaulaComponent } from './configuracion/tamano-jaula/tamano-jaula.component';
import { RangoPesoJaulaComponent } from './configuracion/rango-peso-jaula/rango-peso-jaula.component';

@Component({
  selector: 'app-jaulas',
  standalone: true,
  imports: [
    CommonModule, 
    MatTabsModule,
    ListaJaulasComponent,
    CategoriaJaulaComponent,
    TamanoJaulaComponent,
    RangoPesoJaulaComponent
  ],
  templateUrl: './jaulas.component.html',
  styleUrls: ['./jaulas.component.css']
})
export class JaulasComponent {}
