import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ClientesApi } from '../../../core/api/clientes.api';
import { ProductosApi } from '../../../core/api/productos.api';
import type { Cliente, ErrorApi, ProductoDisponible, Venta } from '../../../core/api/tipos';
import { VentasApi } from '../../../core/api/ventas.api';
import { pesos } from '../../../core/formato';
import { aErrorApi } from '../../../core/http/error-api';
import { AvisoError } from '../../../shared/aviso-error/aviso-error';
import { RegistroCliente } from '../../../shared/registro-cliente/registro-cliente';

interface Linea {
  productoId: number;
  cantidad: number;
}

/** Un entero de al menos 1: lo que escribe la persona en un campo de cantidad. */
const comoCantidad = (evento: Event): number => Math.max(1, Math.floor(Number((evento.target as HTMLInputElement).value)) || 1);

/**
 * RSI-3: registrar una venta. La pantalla arma el pedido (cliente, dirección y productos con cantidades); el backend
 * valida el aval y el stock, calcula el total y reserva todo o nada. Aquí no se calcula ni se decide nada de eso.
 */
@Component({
  selector: 'app-nueva-venta',
  imports: [AvisoError, RegistroCliente],
  templateUrl: './nueva-venta.html',
  styleUrl: './nueva-venta.css',
})
export class NuevaVenta implements OnInit {
  private readonly productosApi = inject(ProductosApi);
  private readonly clientesApi = inject(ClientesApi);
  private readonly ventasApi = inject(VentasApi);

  protected readonly clientes = signal<Cliente[]>([]);
  protected readonly productos = signal<ProductoDisponible[]>([]);
  protected readonly cargando = signal(false);
  protected readonly errorCarga = signal<ErrorApi | null>(null);

  protected readonly clienteId = signal<number | null>(null);
  protected readonly direccion = signal('');
  protected readonly lineas = signal<Linea[]>([]);
  protected readonly seleccion = signal<number | null>(null);
  protected readonly cantidadNueva = signal(1);

  protected readonly enCurso = signal(false);
  protected readonly error = signal<ErrorApi | null>(null);
  protected readonly venta = signal<Venta | null>(null);
  protected readonly registrandoCliente = signal(false);

  protected readonly pesos = pesos;

  /** Productos que todavía se pueden agregar: con disponible y que aún no están en el carrito. */
  protected readonly porAgregar = computed(() =>
    this.productos().filter((p) => p.cantidadDisponible > 0 && !this.lineas().some((l) => l.productoId === p.id)),
  );
  protected readonly productoElegido = computed<ProductoDisponible | null>(() => {
    const lista = this.porAgregar();
    return lista.find((p) => p.id === this.seleccion()) ?? lista[0] ?? null; // con la lista vacía no hay ninguno
  });
  protected readonly detalle = computed(() =>
    this.lineas().map((linea) => ({ ...linea, producto: this.productos().find((p) => p.id === linea.productoId) })),
  );
  protected readonly puedeConfirmar = computed(
    () => !this.enCurso() && this.clienteId() !== null && this.direccion().trim() !== '' && this.lineas().length > 0,
  );

  ngOnInit(): void {
    void this.cargar();
  }

  protected async cargar(): Promise<void> {
    this.cargando.set(true);
    this.errorCarga.set(null);
    try {
      const [clientes, productos] = await Promise.all([
        firstValueFrom(this.clientesApi.listar()),
        firstValueFrom(this.productosApi.listar()),
      ]);
      this.clientes.set(clientes);
      this.productos.set(productos);
      if (!clientes.some((c) => c.id === this.clienteId())) {
        this.clienteId.set(clientes[0]?.id ?? null);
      }
    } catch (fallo) {
      this.errorCarga.set(aErrorApi(fallo));
    } finally {
      this.cargando.set(false);
    }
  }

  protected elegirCliente(evento: Event): void {
    this.clienteId.set(Number((evento.target as HTMLSelectElement).value));
  }

  protected escribirDireccion(evento: Event): void {
    this.direccion.set((evento.target as HTMLInputElement).value);
  }

  protected elegirProducto(evento: Event): void {
    this.seleccion.set(Number((evento.target as HTMLSelectElement).value));
  }

  protected cambiarCantidadNueva(evento: Event): void {
    this.cantidadNueva.set(comoCantidad(evento));
  }

  protected agregar(): void {
    const producto = this.productoElegido();
    if (!producto) {
      return;
    }
    this.lineas.update((l) => [...l, { productoId: producto.id, cantidad: this.cantidadNueva() }]);
    this.seleccion.set(null);
    this.cantidadNueva.set(1);
    this.venta.set(null);
    this.error.set(null);
  }

  protected cambiarCantidad(productoId: number, evento: Event): void {
    const cantidad = comoCantidad(evento);
    this.lineas.update((l) => l.map((linea) => (linea.productoId === productoId ? { ...linea, cantidad } : linea)));
  }

  protected quitar(productoId: number): void {
    this.lineas.update((l) => l.filter((linea) => linea.productoId !== productoId));
  }

  protected async confirmar(): Promise<void> {
    const clienteId = this.clienteId();
    if (!this.puedeConfirmar() || clienteId === null) {
      return;
    }
    this.enCurso.set(true);
    this.error.set(null);
    this.venta.set(null);
    try {
      const venta = await firstValueFrom(
        this.ventasApi.registrar({
          clienteId,
          direccionEntrega: this.direccion().trim(),
          productos: this.lineas().map((l) => ({ productoId: l.productoId, cantidad: l.cantidad })),
        }),
      );
      this.venta.set(venta);
      this.lineas.set([]);
      await this.recargarProductos(); // la venta reservó stock: lo disponible cambió
    } catch (fallo) {
      this.error.set(aErrorApi(fallo));
    } finally {
      this.enCurso.set(false);
    }
  }

  protected alRegistrarCliente(cliente: Cliente): void {
    this.registrandoCliente.set(false);
    this.clientes.update((l) => [...l, cliente]);
    this.clienteId.set(cliente.id);
  }

  private async recargarProductos(): Promise<void> {
    try {
      this.productos.set(await firstValueFrom(this.productosApi.listar()));
    } catch {
      // La venta ya quedó registrada; si no se puede refrescar la lista, se verá al volver a entrar.
    }
  }
}
