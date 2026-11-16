#pragma once
// Funcionalidades de Recursos Humanos de la FachadaPoliMarket
// (componentes Terceros, Seguridad y Recursos Humanos).
//
//   buscarTercero        GET  /api/terceros?tipoDocumento=..&numeroDocumento=..
//   registrarTercero     POST /api/terceros
//   crearCuenta          POST /api/usuarios
//   listarCuentas        GET  /api/usuarios
//   desactivarCuenta     POST /api/usuarios/{id}/desactivacion
//   listarVendedores     GET  /api/vendedores
//   consultarAutorizacion GET /api/vendedores/{id}/autorizacion
//   aprobarAval          POST /api/vendedores/{id}/aval/aprobar   { "observacion" }
//   rechazarAval         POST /api/vendedores/{id}/aval/rechazar  { "observacion" }

#include "modelos/Modelos.h"

namespace polimarket {

class IRecursosHumanosService {
public:
    virtual ~IRecursosHumanosService() = default;

    // Si no existe, responde ok = false con codigo NO_ENCONTRADO.
    virtual Respuesta<Tercero> buscarTercero(const QString& tipoDocumento, const QString& numeroDocumento) = 0;
    virtual Respuesta<Tercero> registrarTercero(const Tercero& tercero) = 0;

    virtual Respuesta<CuentaCreada> crearCuenta(const NuevaCuenta& cuenta) = 0;
    virtual Respuesta<QList<Cuenta>> listarCuentas() = 0;
    virtual Respuesta<Cuenta> desactivarCuenta(qint64 idCuenta) = 0;

    virtual Respuesta<QList<Vendedor>> listarVendedores() = 0;
    virtual Respuesta<Aval> consultarAutorizacion(qint64 idVendedor) = 0;
    virtual Respuesta<Aval> aprobarAval(qint64 idVendedor, const QString& observacion) = 0;
    virtual Respuesta<Aval> rechazarAval(qint64 idVendedor, const QString& observacion) = 0;
};

} // namespace polimarket
