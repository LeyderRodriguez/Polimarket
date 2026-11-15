import { Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import type { Rol } from '../../api/tipos';
import { DemostracionServicio } from '../../demostracion/demostracion.servicio';
import { PanelDemostracion } from '../../demostracion/panel-demostracion';
import { SelectorTema } from '../../../shared/selector-tema/selector-tema';
import { SesionServicio } from '../../auth/sesion.servicio';

interface Opcion {
  ruta: string;
  texto: string;
  /** Resalta la opción solo cuando la dirección coincide exactamente (para que «/ventas» no se encienda en «/ventas/nueva»). */
  exacta?: boolean;
}

const MENUS: Partial<Record<Rol, Opcion[]>> = {
  VENDEDOR: [
    { ruta: '/disponibilidad', texto: 'Disponibilidad' },
    { ruta: '/ventas/nueva', texto: 'Nueva venta' },
    { ruta: '/ventas', texto: 'Mis ventas', exacta: true },
    { ruta: '/clientes', texto: 'Clientes' },
  ],
  ENTREGAS: [{ ruta: '/entregas', texto: 'Entregas pendientes' }],
};

const ETIQUETAS: Partial<Record<Rol, string>> = { VENDEDOR: 'Vendedor', ENTREGAS: 'Entregas' };

/** Marco de la aplicación una vez dentro: menú lateral según el rol y el espacio donde se muestra cada pantalla. */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, SelectorTema, PanelDemostracion],
  templateUrl: './shell.html',
  styleUrl: './shell.css',
})
export class Shell {
  private readonly sesion = inject(SesionServicio);

  protected readonly demo = inject(DemostracionServicio);
  protected readonly usuario = this.sesion.usuario;
  protected readonly opciones = computed(() => {
    const rol = this.usuario()?.rol;
    return (rol && MENUS[rol]) || [];
  });
  protected readonly etiquetaRol = computed(() => {
    const rol = this.usuario()?.rol;
    return (rol && ETIQUETAS[rol]) || '';
  });

  protected salir(): void {
    void this.sesion.cerrarSesion();
  }
}
