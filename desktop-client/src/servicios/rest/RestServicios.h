#pragma once
// Implementaciones REST de las interfaces: llaman a la API de la FachadaPoliMarket.

#include "servicios/IAutenticacionService.h"
#include "servicios/IBodegaService.h"
#include "servicios/IRecursosHumanosService.h"
#include "servicios/rest/ApiCliente.h"

#include <memory>

namespace polimarket {

class RestAutenticacionService : public IAutenticacionService {
public:
    explicit RestAutenticacionService(std::shared_ptr<ApiCliente> api) : m_api(std::move(api)) {}
    Respuesta<UsuarioSesion> iniciarSesion(const QString& correo, const QString& contrasena) override;
    void cerrarSesion() override;

private:
    std::shared_ptr<ApiCliente> m_api;
};

class RestRecursosHumanosService : public IRecursosHumanosService {
public:
    explicit RestRecursosHumanosService(std::shared_ptr<ApiCliente> api) : m_api(std::move(api)) {}
    Respuesta<Tercero> buscarTercero(const QString& tipoDocumento, const QString& numeroDocumento) override;
    Respuesta<Tercero> registrarTercero(const Tercero& tercero) override;
    Respuesta<CuentaCreada> crearCuenta(const NuevaCuenta& cuenta) override;
    Respuesta<QList<Cuenta>> listarCuentas() override;
    Respuesta<Cuenta> desactivarCuenta(qint64 idCuenta) override;
    Respuesta<QList<Vendedor>> listarVendedores() override;
    Respuesta<Aval> consultarAutorizacion(qint64 idVendedor) override;
    Respuesta<Aval> aprobarAval(qint64 idVendedor, const QString& observacion) override;
    Respuesta<Aval> rechazarAval(qint64 idVendedor, const QString& observacion) override;

private:
    Respuesta<Aval> decidirAval(qint64 idVendedor, const QString& accion, const QString& observacion);
    std::shared_ptr<ApiCliente> m_api;
};

class RestBodegaService : public IBodegaService {
public:
    explicit RestBodegaService(std::shared_ptr<ApiCliente> api) : m_api(std::move(api)) {}
    Respuesta<QList<ProductoInventario>> listarInventario() override;
    Respuesta<QList<ProductoBajoStock>> listarBajoStock() override;
    Respuesta<QList<Proveedor>> listarProveedores() override;
    Respuesta<Movimiento> registrarEntrada(const NuevaEntrada& entrada) override;

private:
    std::shared_ptr<ApiCliente> m_api;
};

} // namespace polimarket
