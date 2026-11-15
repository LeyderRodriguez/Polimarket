import { Component, ElementRef, Injector, afterNextRender, inject, output, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { ClientesApi } from '../../core/api/clientes.api';
import { TercerosApi } from '../../core/api/terceros.api';
import type { Cliente, ErrorApi, SolicitudTercero, Tercero, TipoPersona } from '../../core/api/tipos';
import { aErrorApi } from '../../core/http/error-api';
import { AvisoError } from '../aviso-error/aviso-error';
import { TrampaFoco } from '../trampa-foco/trampa-foco';

type Fase = 'buscar' | 'datos' | 'preferencias';

/** Nombre para mostrar de un tercero: la razón social si es empresa, nombres y apellidos si es persona. */
export function nombreDe(tercero: Tercero): string {
  return tercero.tipo === 'JURIDICA' ? tercero.razonSocial : `${tercero.nombres} ${tercero.apellidos}`.trim();
}

/**
 * Registra un cliente en tres pasos: busca el tercero por documento, lo registra si no existe y le pone sus preferencias.
 * Lo usan Clientes y Nueva venta; por eso vive en `shared`. Todas las reglas (duplicados, permisos) las decide el backend.
 */
@Component({
  selector: 'app-registro-cliente',
  imports: [ReactiveFormsModule, AvisoError, TrampaFoco],
  templateUrl: './registro-cliente.html',
  styleUrl: './registro-cliente.css',
  host: { '(document:keydown.escape)': 'cerrar()' },
})
export class RegistroCliente {
  /** Se emite con el cliente ya creado en el backend. */
  readonly registrado = output<Cliente>();
  readonly cerrado = output<void>();

  private readonly terceros = inject(TercerosApi);
  private readonly clientes = inject(ClientesApi);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly injector = inject(Injector);
  private readonly fb = inject(NonNullableFormBuilder);

  protected readonly tiposDocumento = ['CC', 'CE', 'NIT', 'PA'] as const;
  protected readonly fase = signal<Fase>('buscar');
  protected readonly tercero = signal<Tercero | null>(null);
  protected readonly yaExistia = signal(false);
  protected readonly enCurso = signal(false);
  protected readonly error = signal<ErrorApi | null>(null);
  protected readonly nombreDe = nombreDe;

  protected readonly busqueda = this.fb.group({
    tipoDocumento: ['CC'],
    numeroDocumento: ['', Validators.required],
  });
  protected readonly datos = this.fb.group({
    tipo: ['NATURAL' as TipoPersona],
    nombres: [''],
    apellidos: [''],
    razonSocial: [''],
    telefono: [''],
    correo: ['', Validators.email],
    direccion: [''],
  });
  protected readonly preferencias = this.fb.group({ texto: [''] });

  constructor() {
    this.enfocarPrimerCampo();
  }

  protected cerrar(): void {
    this.cerrado.emit();
  }

  protected volver(): void {
    this.irA('buscar');
  }

  protected buscarTercero(): Promise<void> {
    if (this.busqueda.invalid) {
      this.busqueda.markAllAsTouched();
      return Promise.resolve();
    }
    const { tipoDocumento, numeroDocumento } = this.busqueda.getRawValue();
    return this.ejecutar(async () => {
      try {
        this.tercero.set(await firstValueFrom(this.terceros.buscar(tipoDocumento, numeroDocumento.trim())));
        this.yaExistia.set(true);
        this.irA('preferencias');
      } catch (fallo) {
        const error = aErrorApi(fallo);
        if (error.codigo !== 'NO_ENCONTRADO') {
          throw error;
        }
        this.yaExistia.set(false);
        this.irA('datos');
      }
    });
  }

  /** Para el mensaje «este dato es obligatorio»: solo aplica al campo que corresponde al tipo de persona. */
  protected falta(campo: 'nombres' | 'apellidos' | 'razonSocial'): boolean {
    const { tipo } = this.datos.getRawValue();
    const aplica = campo === 'razonSocial' ? tipo === 'JURIDICA' : tipo === 'NATURAL';
    return aplica && this.datos.touched && this.datos.controls[campo].value.trim() === '';
  }

  protected registrarTercero(): Promise<void> {
    const d = this.datos.getRawValue();
    const completo = d.tipo === 'NATURAL' ? d.nombres.trim() && d.apellidos.trim() : d.razonSocial.trim();
    if (!completo || this.datos.invalid) {
      this.datos.markAllAsTouched();
      return Promise.resolve();
    }
    const { tipoDocumento, numeroDocumento } = this.busqueda.getRawValue();
    const solicitud: SolicitudTercero = {
      tipo: d.tipo,
      tipoDocumento,
      numeroDocumento: numeroDocumento.trim(),
      nombres: d.tipo === 'NATURAL' ? d.nombres.trim() : undefined,
      apellidos: d.tipo === 'NATURAL' ? d.apellidos.trim() : undefined,
      razonSocial: d.tipo === 'JURIDICA' ? d.razonSocial.trim() : undefined,
      telefono: d.telefono.trim() || undefined,
      correo: d.correo.trim() || undefined,
      direccion: d.direccion.trim() || undefined,
    };
    return this.ejecutar(async () => {
      this.tercero.set(await firstValueFrom(this.terceros.registrar(solicitud)));
      this.irA('preferencias');
    });
  }

  protected registrarCliente(): Promise<void> {
    const tercero = this.tercero();
    if (!tercero) {
      return Promise.resolve();
    }
    const preferencias = this.preferencias.controls.texto.value
      .split(',')
      .map((p) => p.trim())
      .filter((p) => p.length > 0);
    return this.ejecutar(async () => {
      this.registrado.emit(await firstValueFrom(this.clientes.registrar({ terceroId: tercero.id, preferencias })));
    });
  }

  /** Un solo lugar para «en curso» y para mostrar el error del backend, igual en los tres pasos. */
  private async ejecutar(accion: () => Promise<void>): Promise<void> {
    if (this.enCurso()) {
      return; // ya hay una petición en vuelo: un segundo Enter no debe registrar dos veces al mismo tercero o cliente
    }
    this.enCurso.set(true);
    this.error.set(null);
    try {
      await accion();
    } catch (fallo) {
      this.error.set(aErrorApi(fallo));
    } finally {
      this.enCurso.set(false);
    }
  }

  private irA(fase: Fase): void {
    this.fase.set(fase);
    this.error.set(null);
    this.enfocarPrimerCampo();
  }

  private enfocarPrimerCampo(): void {
    afterNextRender(() => this.host.nativeElement.querySelector<HTMLElement>('input, select')?.focus(), {
      injector: this.injector,
    });
  }
}
