#include "servicios/mock/MockServicios.h"

namespace polimarket {

namespace {
const QString kClaveDemo = "Demo-1234";
}

// ---------------------------------------------------------------- Sesión
Respuesta<UsuarioSesion> MockAutenticacionService::iniciarSesion(const QString& correo, const QString& contrasena) {
    const QString c = correo.trimmed().toLower();
    if (contrasena != kClaveDemo)
        return Respuesta<UsuarioSesion>::error("CREDENCIALES_INVALIDAS", "Correo o contraseña incorrectos.");
    if (c == "rh@polimarket.test") return Respuesta<UsuarioSesion>::exito({1, "Rosa Herrera", c, Rol::RecursosHumanos});
    if (c == "bodega@polimarket.test") return Respuesta<UsuarioSesion>::exito({2, "Bruno Salazar", c, Rol::Bodega});
    if (c == "ana@polimarket.test" || c == "luis@polimarket.test" || c == "entregas@polimarket.test")
        return Respuesta<UsuarioSesion>::error("ROL_NO_SOPORTADO",
                                               "Este cliente de escritorio es para Recursos Humanos y Bodega; use el cliente web.");
    return Respuesta<UsuarioSesion>::error("CREDENCIALES_INVALIDAS", "Correo o contraseña incorrectos.");
}

// ---------------------------------------------------------------- Recursos Humanos
MockRecursosHumanosService::MockRecursosHumanosService() {
    auto persona = [this](qint64 id, const QString& doc, const QString& n, const QString& a) {
        Tercero t;
        t.id = id; t.tipoDocumento = "CC"; t.numeroDocumento = doc; t.nombres = n; t.apellidos = a;
        m_terceros.append(t);
    };
    persona(1, "1000000001", "Rosa", "Herrera");
    persona(2, "1000000002", "Bruno", "Salazar");
    persona(3, "1000000003", "Elena", "Castro");
    persona(4, "1000000004", "Ana", "Pérez");
    persona(5, "1000000005", "Luis", "Mora");

    m_cuentas = {
        {1, 1, "Rosa Herrera", "rh@polimarket.test", "RECURSOS_HUMANOS", true},
        {2, 2, "Bruno Salazar", "bodega@polimarket.test", "BODEGA", true},
        {3, 3, "Elena Castro", "entregas@polimarket.test", "ENTREGAS", true},
        {4, 4, "Ana Pérez", "ana@polimarket.test", "VENDEDOR", true},
        {5, 5, "Luis Mora", "luis@polimarket.test", "VENDEDOR", true},
    };
    m_vendedores = {
        {1, 4, "Ana Pérez", "VEN-0001", "Norte", "APROBADO"},
        {2, 5, "Luis Mora", "VEN-0002", "Sur", "PENDIENTE"},
    };
}

Vendedor* MockRecursosHumanosService::vendedor(qint64 id) {
    for (auto& v : m_vendedores)
        if (v.id == id) return &v;
    return nullptr;
}

Respuesta<Tercero> MockRecursosHumanosService::buscarTercero(const QString& tipo, const QString& numero) {
    for (const auto& t : m_terceros)
        if (t.tipoDocumento == tipo && t.numeroDocumento == numero.trimmed()) return Respuesta<Tercero>::exito(t);
    return Respuesta<Tercero>::error("NO_ENCONTRADO", "No existe un tercero con ese documento.");
}

Respuesta<Tercero> MockRecursosHumanosService::registrarTercero(const Tercero& tercero) {
    if (buscarTercero(tercero.tipoDocumento, tercero.numeroDocumento).ok)
        return Respuesta<Tercero>::error("DOCUMENTO_DUPLICADO", "Ya existe un tercero con ese documento.");
    Tercero t = tercero;
    t.id = m_terceros.size() + 1;
    m_terceros.append(t);
    return Respuesta<Tercero>::exito(t);
}

Respuesta<CuentaCreada> MockRecursosHumanosService::crearCuenta(const NuevaCuenta& c) {
    for (const auto& x : m_cuentas) {
        if (x.correo.compare(c.correo, Qt::CaseInsensitive) == 0)
            return Respuesta<CuentaCreada>::error("CORREO_DUPLICADO", "Ya existe una cuenta con ese correo.");
        if (x.terceroId == c.terceroId)
            return Respuesta<CuentaCreada>::error("TERCERO_CON_CUENTA", "Esa persona ya tiene una cuenta.");
    }
    if (c.contrasenaInicial.size() < 8)
        return Respuesta<CuentaCreada>::error("DATOS_INVALIDOS", "La contraseña inicial debe tener al menos 8 caracteres.");

    QString nombre;
    for (const auto& t : m_terceros)
        if (t.id == c.terceroId) nombre = t.nombreCompleto();
    Cuenta nueva{m_cuentas.size() + 1, c.terceroId, nombre, c.correo, c.rol, true};
    m_cuentas.append(nueva);

    CuentaCreada cc{nueva.id, nueva.correo, nueva.rol, 0, {}};
    if (c.rol == "VENDEDOR") {
        Vendedor v{m_vendedores.size() + 1, c.terceroId, nombre, {}, {}, "PENDIENTE"};
        v.codigo = QString("VEN-%1").arg(v.id, 4, 10, QChar('0'));
        m_vendedores.append(v);
        cc.vendedorId = v.id;
        cc.estadoAval = v.estadoAval;
    }
    return Respuesta<CuentaCreada>::exito(cc);
}

Respuesta<QList<Cuenta>> MockRecursosHumanosService::listarCuentas() {
    return Respuesta<QList<Cuenta>>::exito(m_cuentas);
}

Respuesta<Cuenta> MockRecursosHumanosService::desactivarCuenta(qint64 id) {
    for (auto& c : m_cuentas) {
        if (c.id != id) continue;
        if (c.id == 1) return Respuesta<Cuenta>::error("SIN_PERMISO", "No puede desactivar su propia cuenta.");
        if (!c.activo) return Respuesta<Cuenta>::error("ESTADO_INVALIDO", "La cuenta ya estaba desactivada.");
        c.activo = false;
        return Respuesta<Cuenta>::exito(c);
    }
    return Respuesta<Cuenta>::error("NO_ENCONTRADO", "La cuenta no existe.");
}

Respuesta<QList<Vendedor>> MockRecursosHumanosService::listarVendedores() {
    return Respuesta<QList<Vendedor>>::exito(m_vendedores);
}

Respuesta<Aval> MockRecursosHumanosService::consultarAutorizacion(qint64 id) {
    Vendedor* v = vendedor(id);
    if (!v) return Respuesta<Aval>::error("NO_ENCONTRADO", "El vendedor no existe.");
    return Respuesta<Aval>::exito({v->id, v->estadoAval, v->estadoAval == "APROBADO"});
}

Respuesta<Aval> MockRecursosHumanosService::aprobarAval(qint64 id, const QString&) {
    Vendedor* v = vendedor(id);
    if (!v) return Respuesta<Aval>::error("NO_ENCONTRADO", "El vendedor no existe.");
    if (v->estadoAval == "APROBADO") return Respuesta<Aval>::error("ESTADO_INVALIDO", "El aval ya está aprobado.");
    v->estadoAval = "APROBADO";
    return Respuesta<Aval>::exito({v->id, v->estadoAval, true});
}

Respuesta<Aval> MockRecursosHumanosService::rechazarAval(qint64 id, const QString&) {
    Vendedor* v = vendedor(id);
    if (!v) return Respuesta<Aval>::error("NO_ENCONTRADO", "El vendedor no existe.");
    v->estadoAval = "RECHAZADO";
    return Respuesta<Aval>::exito({v->id, v->estadoAval, false});
}

// ---------------------------------------------------------------- Bodega
MockBodegaService::MockBodegaService() {
    auto producto = [](qint64 id, const QString& codigo, const QString& nombre, double precio, int stock) {
        ProductoInventario p;
        p.id = id; p.codigo = codigo; p.nombre = nombre; p.precio = precio;
        p.stockFisico = stock; p.cantidadDisponible = stock;
        return p;
    };
    m_items = {
        {producto(1, "ARR-01", "Arroz", 5000, 10), 5},
        {producto(2, "AZU-01", "Azúcar", 3500, 4), 5},
        {producto(3, "ACE-01", "Aceite", 12000, 20), 3},
    };
    m_proveedores = {{1, 6, "Distribuidora Ejemplo", "Lucía", "3000000000"}};
}

Respuesta<QList<ProductoInventario>> MockBodegaService::listarInventario() {
    QList<ProductoInventario> lista;
    for (auto item : m_items) {
        item.producto.bajoStock = item.producto.cantidadDisponible < item.stockMinimo;
        item.producto.stockMinimo = item.producto.bajoStock ? item.stockMinimo : -1;
        lista.append(item.producto);
    }
    return Respuesta<QList<ProductoInventario>>::exito(lista);
}

Respuesta<QList<ProductoBajoStock>> MockBodegaService::listarBajoStock() {
    QList<ProductoBajoStock> lista;
    for (const auto& item : m_items) {
        const auto& p = item.producto;
        if (p.cantidadDisponible >= item.stockMinimo) continue;
        lista.append({p.id, p.nombre, p.stockFisico, p.cantidadReservada, p.cantidadDisponible,
                      item.stockMinimo, m_proveedores});
    }
    return Respuesta<QList<ProductoBajoStock>>::exito(lista);
}

Respuesta<QList<Proveedor>> MockBodegaService::listarProveedores() {
    return Respuesta<QList<Proveedor>>::exito(m_proveedores);
}

Respuesta<Movimiento> MockBodegaService::registrarEntrada(const NuevaEntrada& e) {
    if (e.cantidad < 1) return Respuesta<Movimiento>::error("DATOS_INVALIDOS", "La cantidad debe ser al menos 1.");
    for (auto& item : m_items) {
        auto& p = item.producto;
        if (p.id != e.productoId) continue;
        Movimiento m{m_siguienteMovimiento++, p.id, "ENTRADA", e.cantidad, p.stockFisico, p.stockFisico + e.cantidad};
        p.stockFisico += e.cantidad;
        p.cantidadDisponible += e.cantidad;
        return Respuesta<Movimiento>::exito(m);
    }
    return Respuesta<Movimiento>::error("NO_ENCONTRADO", "El producto no existe.");
}

} // namespace polimarket
