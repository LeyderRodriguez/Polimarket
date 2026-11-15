import { Component, OnInit, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ClientesApi } from '../../core/api/clientes.api';
import type { Cliente, ErrorApi } from '../../core/api/tipos';
import { aErrorApi } from '../../core/http/errores.interceptor';
import { AvisoError } from '../../shared/aviso-error/aviso-error';
import { RegistroCliente } from '../../shared/registro-cliente/registro-cliente';

/** Los clientes del vendedor y el registro de uno nuevo (buscar tercero, registrarlo si no existe, preferencias). */
@Component({
  selector: 'app-clientes',
  imports: [AvisoError, RegistroCliente],
  templateUrl: './clientes.html',
})
export class Clientes implements OnInit {
  private readonly api = inject(ClientesApi);

  protected readonly clientes = signal<Cliente[]>([]);
  protected readonly cargando = signal(false);
  protected readonly error = signal<ErrorApi | null>(null);
  protected readonly registrando = signal(false);
  protected readonly aviso = signal<string | null>(null);

  ngOnInit(): void {
    void this.cargar();
  }

  protected async cargar(): Promise<void> {
    this.cargando.set(true);
    this.error.set(null);
    try {
      this.clientes.set(await firstValueFrom(this.api.listar()));
    } catch (fallo) {
      this.clientes.set([]);
      this.error.set(aErrorApi(fallo));
    } finally {
      this.cargando.set(false);
    }
  }

  protected abrirRegistro(): void {
    this.aviso.set(null);
    this.registrando.set(true);
  }

  protected alRegistrar(cliente: Cliente): void {
    this.registrando.set(false);
    this.aviso.set(`Cliente «${cliente.nombre}» registrado.`);
    void this.cargar();
  }
}
