#pragma once
// Modo demostración: implementaciones en memoria de las mismas interfaces,
// con los datos de prueba que carga el backend (CargadorDatosDemo).
// Sirven para presentar el cliente sin el backend encendido (opción --demo).

#include "servicios/IAutenticacionService.h"
#include "servicios/IBodegaService.h"
#include "servicios/IRecursosHumanosService.h"

namespace polimarket {

class MockAutenticacionService : public IAutenticacionService {
public:
    Respuesta<UsuarioSesion> iniciarSesion(const QString& correo, const QString& contrasena) override;
    void cerrarSesion() override {}
};

class MockRecursosHumanosService : public IRecursosHumanosService {
public:
    MockRecursosHumanosService();
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
    Vendedor* vendedor(qint64 id);
    QList<Tercero> m_terceros;
    QList<Cuenta> m_cuentas;
    QList<Vendedor> m_vendedores;
};

class MockBodegaService : public IBodegaService {
public:
    MockBodegaService();
    Respuesta<QList<ProductoInventario>> listarInventario() override;
    Respuesta<QList<ProductoBajoStock>> listarBajoStock() override;
    Respuesta<QList<Proveedor>> listarProveedores() override;
    Respuesta<Movimiento> registrarEntrada(const NuevaEntrada& entrada) override;

private:
    struct Item { ProductoInventario producto; int stockMinimo; };
    QList<Item> m_items;
    QList<Proveedor> m_proveedores;
    qint64 m_siguienteMovimiento = 4;
};

} // namespace polimarket
