import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ProductosApi } from '../../core/api/productos.api';
import type { ErrorApi, ProductoDisponible } from '../../core/api/tipos';
import { normalizar, pesos } from '../../core/formato';
import { aErrorApi } from '../../core/http/error-api';
import { AvisoError } from '../../shared/aviso-error/aviso-error';

/** RSI-2: el vendedor consulta qué hay disponible antes de vender. Todo viene del backend; aquí solo se muestra y se filtra. */
@Component({
  selector: 'app-disponibilidad',
  imports: [AvisoError],
  templateUrl: './disponibilidad.html',
  styleUrl: './disponibilidad.css',
})
export class Disponibilidad implements OnInit {
  private readonly api = inject(ProductosApi);

  protected readonly productos = signal<ProductoDisponible[]>([]);
  protected readonly cargando = signal(false);
  protected readonly error = signal<ErrorApi | null>(null);
  protected readonly busqueda = signal('');

  protected readonly filtrados = computed(() => {
    const texto = normalizar(this.busqueda().trim());
    return this.productos().filter((p) => normalizar(p.nombre).includes(texto) || normalizar(p.codigo).includes(texto));
  });

  protected readonly mensajeVacio = computed(() => {
    if (this.cargando()) return 'Cargando…';
    if (this.error()) return 'No se pudieron cargar los productos.';
    if (this.productos().length === 0) return 'No hay productos registrados.';
    return `Ningún producto coincide con «${this.busqueda().trim()}».`;
  });

  protected readonly pesos = pesos;

  ngOnInit(): void {
    void this.cargar();
  }

  protected buscar(evento: Event): void {
    this.busqueda.set((evento.target as HTMLInputElement).value);
  }

  protected async cargar(): Promise<void> {
    this.cargando.set(true);
    this.error.set(null);
    try {
      this.productos.set(await firstValueFrom(this.api.listar()));
    } catch (fallo) {
      // Se vacía la lista: mostrar existencias viejas junto a un error llevaría a vender stock que quizá ya no está.
      this.productos.set([]);
      this.error.set(aErrorApi(fallo));
    } finally {
      this.cargando.set(false);
    }
  }
}
