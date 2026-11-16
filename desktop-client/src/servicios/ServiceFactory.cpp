#include "servicios/ServiceFactory.h"

#include "servicios/mock/MockServicios.h"
#include "servicios/rest/ApiCliente.h"
#include "servicios/rest/RestServicios.h"

#include <QUrl>

namespace polimarket {

namespace {
constexpr auto kUrlPorDefecto = "http://localhost:8080";
}

Servicios crearServicios(const QStringList& argumentos) {
    Servicios s;

    const bool demo = argumentos.contains("--demo") || qEnvironmentVariable("POLIMARKET_MODO") == "demo";
    if (demo) {
        s.autenticacion = std::make_shared<MockAutenticacionService>();
        s.recursosHumanos = std::make_shared<MockRecursosHumanosService>();
        s.bodega = std::make_shared<MockBodegaService>();
        s.descripcionModo = "Modo demostración: datos simulados, sin conexión al backend.";
        return s;
    }

    QString url = qEnvironmentVariable("POLIMARKET_API_URL", kUrlPorDefecto);
    for (const QString& a : argumentos)
        if (a.startsWith("--api=")) url = a.mid(6);

    s.api = std::make_shared<ApiCliente>(QUrl(url));
    s.autenticacion = std::make_shared<RestAutenticacionService>(s.api);
    s.recursosHumanos = std::make_shared<RestRecursosHumanosService>(s.api);
    s.bodega = std::make_shared<RestBodegaService>(s.api);
    s.descripcionModo = "Conectado a la API de PoliMarket: " + url;
    return s;
}

} // namespace polimarket
