#include "servicios/rest/RestServicios.h"

#include <QHash>
#include <QJsonArray>
#include <QJsonObject>
#include <QUrlQuery>

namespace polimarket {

namespace {

// ---- Lectura de JSON a modelos ----
qint64 entero(const QJsonObject& o, const char* campo) {
    return o.value(QLatin1String(campo)).toVariant().toLongLong();
}
QString texto(const QJsonObject& o, const char* campo) {
    return o.value(QLatin1String(campo)).toString();
}
double monto(const QJsonObject& o, const char* campo) {
    // Los montos llegan como texto ("5000.00"); se aceptan también como número.
    const QJsonValue v = o.value(QLatin1String(campo));
    return v.isString() ? v.toString().toDouble() : v.toDouble();
}

Tercero aTercero(const QJsonObject& o) {
    Tercero t;
    t.id = entero(o, "id");
    t.tipo = texto(o, "tipo");
    t.tipoDocumento = texto(o, "tipoDocumento");
    t.numeroDocumento = texto(o, "numeroDocumento");
    t.nombres = texto(o, "nombres");
    t.apellidos = texto(o, "apellidos");
    t.razonSocial = texto(o, "razonSocial");
    t.telefono = texto(o, "telefono");
    t.correo = texto(o, "correo");
    t.direccion = texto(o, "direccion");
    return t;
}

Cuenta aCuenta(const QJsonObject& o) {
    return {entero(o, "id"), entero(o, "terceroId"), texto(o, "nombre"), texto(o, "correo"),
            texto(o, "rol"), o.value("activo").toBool()};
}

Aval aAval(const QJsonObject& o) {
    return {entero(o, "vendedorId"), texto(o, "estadoAval"), o.value("autorizado").toBool()};
}

Proveedor aProveedor(const QJsonObject& o) {
    return {entero(o, "id"), entero(o, "terceroId"), texto(o, "nombre"),
            texto(o, "nombreContacto"), texto(o, "telefonoContacto")};
}

// Agrega un campo de texto solo si tiene valor (el backend trata null como "no enviado").
void siTiene(QJsonObject& o, const char* campo, const QString& valor) {
    if (!valor.trimmed().isEmpty()) o.insert(QLatin1String(campo), valor.trimmed());
}

} // namespace

// =====================================================================
// Sesión
// =====================================================================
Respuesta<UsuarioSesion> RestAutenticacionService::iniciarSesion(const QString& correo, const QString& contrasena) {
    m_api->olvidarSesion();
    QJsonObject cuerpo{{"correo", correo.trimmed()}, {"contrasena", contrasena}};
    const RespuestaHttp r = m_api->post("/api/auth/login", QJsonDocument(cuerpo));
    if (!r.exitosa()) return ApiCliente::error<UsuarioSesion>(r);

    const QJsonObject o = r.cuerpo.object();
    const QString rol = texto(o, "rol");
    UsuarioSesion s;
    s.id = entero(o, "id");
    s.nombre = texto(o, "nombre");
    s.correo = correo.trimmed();
    if (rol == "RECURSOS_HUMANOS") {
        s.rol = Rol::RecursosHumanos;
    } else if (rol == "BODEGA") {
        s.rol = Rol::Bodega;
    } else {
        cerrarSesion();
        return Respuesta<UsuarioSesion>::error(
            "ROL_NO_SOPORTADO",
            QString("La cuenta tiene el rol %1. Este cliente de escritorio es para Recursos Humanos y Bodega; "
                    "use el cliente web.").arg(rol));
    }
    return Respuesta<UsuarioSesion>::exito(s);
}

void RestAutenticacionService::cerrarSesion() {
    m_api->post("/api/auth/logout");
    m_api->olvidarSesion();
}

// =====================================================================
// Recursos Humanos
// =====================================================================
Respuesta<Tercero> RestRecursosHumanosService::buscarTercero(const QString& tipoDocumento,
                                                             const QString& numeroDocumento) {
    QUrlQuery q;
    q.addQueryItem("tipoDocumento", tipoDocumento);
    q.addQueryItem("numeroDocumento", numeroDocumento.trimmed());
    const RespuestaHttp r = m_api->get("/api/terceros?" + q.toString(QUrl::FullyEncoded));
    if (!r.exitosa()) return ApiCliente::error<Tercero>(r);
    return Respuesta<Tercero>::exito(aTercero(r.cuerpo.object()));
}

Respuesta<Tercero> RestRecursosHumanosService::registrarTercero(const Tercero& t) {
    QJsonObject o{{"tipo", t.tipo}, {"tipoDocumento", t.tipoDocumento}, {"numeroDocumento", t.numeroDocumento.trimmed()}};
    siTiene(o, "nombres", t.nombres);
    siTiene(o, "apellidos", t.apellidos);
    siTiene(o, "razonSocial", t.razonSocial);
    siTiene(o, "telefono", t.telefono);
    siTiene(o, "correo", t.correo);
    siTiene(o, "direccion", t.direccion);
    const RespuestaHttp r = m_api->post("/api/terceros", QJsonDocument(o));
    if (!r.exitosa()) return ApiCliente::error<Tercero>(r);
    return Respuesta<Tercero>::exito(aTercero(r.cuerpo.object()));
}

Respuesta<CuentaCreada> RestRecursosHumanosService::crearCuenta(const NuevaCuenta& c) {
    QJsonObject o{{"terceroId", c.terceroId}, {"correo", c.correo.trimmed()},
                  {"contrasenaInicial", c.contrasenaInicial}, {"rol", c.rol}};
    const RespuestaHttp r = m_api->post("/api/usuarios", QJsonDocument(o));
    if (!r.exitosa()) return ApiCliente::error<CuentaCreada>(r);
    const QJsonObject j = r.cuerpo.object();
    CuentaCreada cc;
    cc.id = entero(j, "id");
    cc.correo = texto(j, "correo");
    cc.rol = texto(j, "rol");
    cc.vendedorId = entero(j, "vendedorId");
    cc.estadoAval = texto(j, "estadoAval");
    return Respuesta<CuentaCreada>::exito(cc);
}

Respuesta<QList<Cuenta>> RestRecursosHumanosService::listarCuentas() {
    const RespuestaHttp r = m_api->get("/api/usuarios");
    if (!r.exitosa()) return ApiCliente::error<QList<Cuenta>>(r);
    QList<Cuenta> lista;
    for (const QJsonValue v : r.cuerpo.array()) lista.append(aCuenta(v.toObject()));
    return Respuesta<QList<Cuenta>>::exito(lista);
}

Respuesta<Cuenta> RestRecursosHumanosService::desactivarCuenta(qint64 idCuenta) {
    const RespuestaHttp r = m_api->post(QString("/api/usuarios/%1/desactivacion").arg(idCuenta));
    if (!r.exitosa()) return ApiCliente::error<Cuenta>(r);
    // UsuarioDto: { id, terceroId, correo, rol, activo }
    return Respuesta<Cuenta>::exito(aCuenta(r.cuerpo.object()));
}

Respuesta<QList<Vendedor>> RestRecursosHumanosService::listarVendedores() {
    const RespuestaHttp r = m_api->get("/api/vendedores");
    if (!r.exitosa()) return ApiCliente::error<QList<Vendedor>>(r);
    QList<Vendedor> lista;
    for (const QJsonValue v : r.cuerpo.array()) {
        const QJsonObject o = v.toObject();
        lista.append({entero(o, "id"), entero(o, "terceroId"), texto(o, "nombre"), texto(o, "codigo"),
                      texto(o, "zona"), texto(o, "estadoAval")});
    }
    return Respuesta<QList<Vendedor>>::exito(lista);
}

Respuesta<Aval> RestRecursosHumanosService::consultarAutorizacion(qint64 idVendedor) {
    const RespuestaHttp r = m_api->get(QString("/api/vendedores/%1/autorizacion").arg(idVendedor));
    if (!r.exitosa()) return ApiCliente::error<Aval>(r);
    return Respuesta<Aval>::exito(aAval(r.cuerpo.object()));
}

Respuesta<Aval> RestRecursosHumanosService::decidirAval(qint64 idVendedor, const QString& accion,
                                                        const QString& observacion) {
    QJsonObject o;
    siTiene(o, "observacion", observacion);
    const RespuestaHttp r = m_api->post(QString("/api/vendedores/%1/aval/%2").arg(idVendedor).arg(accion),
                                        QJsonDocument(o));
    if (!r.exitosa()) return ApiCliente::error<Aval>(r);
    return Respuesta<Aval>::exito(aAval(r.cuerpo.object()));
}

Respuesta<Aval> RestRecursosHumanosService::aprobarAval(qint64 idVendedor, const QString& observacion) {
    return decidirAval(idVendedor, "aprobar", observacion);
}

Respuesta<Aval> RestRecursosHumanosService::rechazarAval(qint64 idVendedor, const QString& observacion) {
    return decidirAval(idVendedor, "rechazar", observacion);
}

// =====================================================================
// Bodega
// =====================================================================
Respuesta<QList<ProductoInventario>> RestBodegaService::listarInventario() {
    const RespuestaHttp r = m_api->get("/api/productos");
    if (!r.exitosa()) return ApiCliente::error<QList<ProductoInventario>>(r);

    QList<ProductoInventario> lista;
    for (const QJsonValue v : r.cuerpo.array()) {
        const QJsonObject o = v.toObject();
        ProductoInventario p;
        p.id = entero(o, "id");
        p.codigo = texto(o, "codigo");
        p.nombre = texto(o, "nombre");
        p.precio = monto(o, "precio");
        p.stockFisico = o.value("stockFisico").toInt();
        p.cantidadReservada = o.value("cantidadReservada").toInt();
        p.cantidadDisponible = o.value("cantidadDisponible").toInt();
        lista.append(p);
    }

    // /api/productos no trae el stock mínimo: se marcan los que reporta el bajo stock.
    const RespuestaHttp bajos = m_api->get("/api/inventario/bajo-stock");
    if (bajos.exitosa()) {
        QHash<qint64, int> minimos;
        for (const QJsonValue v : bajos.cuerpo.array())
            minimos.insert(entero(v.toObject(), "productoId"), v.toObject().value("stockMinimo").toInt());
        for (auto& p : lista) {
            if (minimos.contains(p.id)) {
                p.bajoStock = true;
                p.stockMinimo = minimos.value(p.id);
            }
        }
    }
    return Respuesta<QList<ProductoInventario>>::exito(lista);
}

Respuesta<QList<ProductoBajoStock>> RestBodegaService::listarBajoStock() {
    const RespuestaHttp r = m_api->get("/api/inventario/bajo-stock");
    if (!r.exitosa()) return ApiCliente::error<QList<ProductoBajoStock>>(r);

    // El bajo stock trae id y nombre del proveedor; el contacto se toma de /api/proveedores.
    QHash<qint64, Proveedor> contactos;
    const auto proveedores = listarProveedores();
    if (proveedores.ok)
        for (const auto& p : proveedores.valor) contactos.insert(p.id, p);

    QList<ProductoBajoStock> lista;
    for (const QJsonValue v : r.cuerpo.array()) {
        const QJsonObject o = v.toObject();
        ProductoBajoStock b;
        b.productoId = entero(o, "productoId");
        b.nombre = texto(o, "nombre");
        b.stockFisico = o.value("stockFisico").toInt();
        b.cantidadReservada = o.value("cantidadReservada").toInt();
        b.cantidadDisponible = o.value("cantidadDisponible").toInt();
        b.stockMinimo = o.value("stockMinimo").toInt();
        for (const QJsonValue pv : o.value("proveedores").toArray()) {
            const QJsonObject po = pv.toObject();
            Proveedor p = contactos.value(entero(po, "id"));
            p.id = entero(po, "id");
            p.nombre = texto(po, "nombre");
            b.proveedores.append(p);
        }
        lista.append(b);
    }
    return Respuesta<QList<ProductoBajoStock>>::exito(lista);
}

Respuesta<QList<Proveedor>> RestBodegaService::listarProveedores() {
    const RespuestaHttp r = m_api->get("/api/proveedores");
    if (!r.exitosa()) return ApiCliente::error<QList<Proveedor>>(r);
    QList<Proveedor> lista;
    for (const QJsonValue v : r.cuerpo.array()) lista.append(aProveedor(v.toObject()));
    return Respuesta<QList<Proveedor>>::exito(lista);
}

Respuesta<Movimiento> RestBodegaService::registrarEntrada(const NuevaEntrada& e) {
    QJsonObject o{{"productoId", e.productoId}, {"cantidad", e.cantidad}, {"proveedorId", e.proveedorId}};
    siTiene(o, "referencia", e.referencia);
    if (e.costoUnitario > 0) o.insert("costoUnitario", QString::number(e.costoUnitario, 'f', 2));
    const RespuestaHttp r = m_api->post("/api/inventario/entradas", QJsonDocument(o));
    if (!r.exitosa()) return ApiCliente::error<Movimiento>(r);
    const QJsonObject j = r.cuerpo.object();
    Movimiento m;
    m.movimientoId = entero(j, "movimientoId");
    m.productoId = entero(j, "productoId");
    m.tipo = texto(j, "tipo");
    m.cantidad = j.value("cantidad").toInt();
    m.stockAnterior = j.value("stockAnterior").toInt();
    m.stockFisico = j.value("stockFisico").toInt();
    return Respuesta<Movimiento>::exito(m);
}

} // namespace polimarket
