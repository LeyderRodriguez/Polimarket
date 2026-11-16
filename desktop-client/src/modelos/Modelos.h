#pragma once
// Modelos que intercambia el cliente de escritorio con la API de PoliMarket.
// Reflejan los DTO del backend (docs/reference/api-rest.md). Los montos llegan
// como texto ("5000.00") y aquí se guardan como double solo para mostrarlos.

#include <QList>
#include <QString>
#include <QStringList>

namespace polimarket {

// ---------- Respuesta genérica de cualquier llamada ----------
// ok = la operación se hizo. Si no, codigo y mensaje vienen del backend
// ({ "codigo": "...", "mensaje": "..." }) o describen el fallo de conexión.
template <typename T>
struct Respuesta {
    bool ok = false;
    T valor{};
    QString codigo;
    QString mensaje;

    static Respuesta exito(T v, QString msg = {}) { return {true, std::move(v), {}, std::move(msg)}; }
    static Respuesta error(QString cod, QString msg) { return {false, T{}, std::move(cod), std::move(msg)}; }
};

struct Vacio {};

// ---------- Seguridad ----------
enum class Rol { RecursosHumanos, Bodega };

inline QString textoRol(Rol rol) {
    return rol == Rol::RecursosHumanos ? QStringLiteral("Recursos Humanos") : QStringLiteral("Bodega");
}

// PerfilDto: { "id", "nombre", "rol" }
struct UsuarioSesion {
    qint64 id = 0;
    QString nombre;
    QString correo;
    Rol rol = Rol::RecursosHumanos;
};

// ---------- Terceros ----------
// TerceroDto
struct Tercero {
    qint64 id = 0;
    QString tipo = "NATURAL"; // NATURAL o JURIDICA
    QString tipoDocumento;
    QString numeroDocumento;
    QString nombres;
    QString apellidos;
    QString razonSocial;
    QString telefono;
    QString correo;
    QString direccion;

    QString nombreCompleto() const {
        return tipo == "JURIDICA" ? razonSocial : (nombres + " " + apellidos).trimmed();
    }
};

// ---------- Cuentas (Recursos Humanos) ----------
// CuentaDto: { "id", "terceroId", "nombre", "correo", "rol", "activo" }
struct Cuenta {
    qint64 id = 0;
    qint64 terceroId = 0;
    QString nombre;
    QString correo;
    QString rol;
    bool activo = true;
};

struct NuevaCuenta {
    qint64 terceroId = 0;
    QString correo;
    QString contrasenaInicial;
    QString rol; // VENDEDOR, BODEGA o ENTREGAS
};

// CuentaCreadaDto
struct CuentaCreada {
    qint64 id = 0;
    QString correo;
    QString rol;
    qint64 vendedorId = 0;   // solo si el rol es VENDEDOR
    QString estadoAval;      // PENDIENTE para un vendedor nuevo
};

// ---------- Vendedores y aval ----------
// VendedorDto: { "id", "terceroId", "nombre", "codigo", "zona", "estadoAval" }
struct Vendedor {
    qint64 id = 0;
    qint64 terceroId = 0;
    QString nombre;
    QString codigo;
    QString zona;
    QString estadoAval; // PENDIENTE, APROBADO o RECHAZADO
};

// AvalDto: { "vendedorId", "estadoAval", "autorizado" }
struct Aval {
    qint64 vendedorId = 0;
    QString estadoAval;
    bool autorizado = false;
};

// ---------- Bodega ----------
// DisponibilidadDto + datos de bajo stock cuando aplica
struct ProductoInventario {
    qint64 id = 0;
    QString codigo;
    QString nombre;
    double precio = 0.0;
    int stockFisico = 0;
    int cantidadReservada = 0;
    int cantidadDisponible = 0;
    bool bajoStock = false;   // aparece en /api/inventario/bajo-stock
    int stockMinimo = -1;     // solo se conoce si está bajo stock
};

// ProveedorDto: { "id", "terceroId", "nombre", "nombreContacto", "telefonoContacto" }
struct Proveedor {
    qint64 id = 0;
    qint64 terceroId = 0;
    QString nombre;
    QString nombreContacto;
    QString telefonoContacto;
};

// BajoStockDto
struct ProductoBajoStock {
    qint64 productoId = 0;
    QString nombre;
    int stockFisico = 0;
    int cantidadReservada = 0;
    int cantidadDisponible = 0;
    int stockMinimo = 0;
    QList<Proveedor> proveedores; // id y nombre del backend, más contacto de /api/proveedores
};

// Solicitud de POST /api/inventario/entradas (una por producto)
struct NuevaEntrada {
    qint64 productoId = 0;
    int cantidad = 0;
    qint64 proveedorId = 0;
    QString referencia;     // número de factura
    double costoUnitario = 0.0;
};

// MovimientoDto
struct Movimiento {
    qint64 movimientoId = 0;
    qint64 productoId = 0;
    QString tipo;
    int cantidad = 0;
    int stockAnterior = 0;
    int stockFisico = 0;
};

} // namespace polimarket
