#pragma once
// Funcionalidades de Bodega de la FachadaPoliMarket
// (componentes Bodega y Proveedores).
//
//   listarInventario   GET  /api/productos  (+ GET /api/inventario/bajo-stock para marcar los bajos)
//   listarBajoStock    GET  /api/inventario/bajo-stock  (+ GET /api/proveedores para el contacto)
//   listarProveedores  GET  /api/proveedores
//   registrarEntrada   POST /api/inventario/entradas

#include "modelos/Modelos.h"

namespace polimarket {

class IBodegaService {
public:
    virtual ~IBodegaService() = default;

    virtual Respuesta<QList<ProductoInventario>> listarInventario() = 0;
    virtual Respuesta<QList<ProductoBajoStock>> listarBajoStock() = 0;
    virtual Respuesta<QList<Proveedor>> listarProveedores() = 0;
    virtual Respuesta<Movimiento> registrarEntrada(const NuevaEntrada& entrada) = 0;
};

} // namespace polimarket
