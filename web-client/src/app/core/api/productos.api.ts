import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import type { ProductoDisponible } from './tipos';

/** Productos y su disponibilidad. Solo habla con la API; el filtrado y la presentación son de cada pantalla. */
@Injectable({ providedIn: 'root' })
export class ProductosApi {
  private readonly http = inject(HttpClient);

  /** `GET /api/productos`: stock físico, reservado y disponible de cada producto. */
  listar() {
    return this.http.get<ProductoDisponible[]>('/api/productos');
  }
}
