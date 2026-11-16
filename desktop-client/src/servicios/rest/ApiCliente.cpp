#include "servicios/rest/ApiCliente.h"

#include <QEventLoop>
#include <QJsonObject>
#include <QNetworkAccessManager>
#include <QNetworkCookie>
#include <QNetworkCookieJar>
#include <QNetworkReply>
#include <QNetworkRequest>
#include <QTimer>

namespace polimarket {

namespace {
constexpr int kTiempoMaximoMs = 15000;
}

QString RespuestaHttp::codigo() const {
    if (estado == 0) return QStringLiteral("SIN_CONEXION");
    const QString c = cuerpo.object().value("codigo").toString();
    return c.isEmpty() ? QString("HTTP_%1").arg(estado) : c;
}

QString RespuestaHttp::mensaje() const {
    if (estado == 0)
        return QStringLiteral("No se pudo conectar con el servidor de PoliMarket. Verifique que el backend "
                              "esté encendido (docker compose up). Detalle: %1").arg(errorRed);
    const QString m = cuerpo.object().value("mensaje").toString();
    return m.isEmpty() ? QString("El servidor respondió con el código %1.").arg(estado) : m;
}

ApiCliente::ApiCliente(const QUrl& urlBase, QObject* parent)
    : QObject(parent), m_urlBase(urlBase), m_red(new QNetworkAccessManager(this)) {
    m_red->setCookieJar(new QNetworkCookieJar(m_red)); // guarda JSESSIONID y XSRF-TOKEN
}

RespuestaHttp ApiCliente::get(const QString& ruta) {
    return enviar("GET", ruta, {});
}

RespuestaHttp ApiCliente::post(const QString& ruta, const QJsonDocument& cuerpo) {
    // El token CSRF llega como cookie en cualquier respuesta autenticada.
    // Si todavía no lo tenemos (por ejemplo, justo después del login), lo pedimos.
    if (tokenCsrf().isEmpty() && ruta != "/api/auth/login") get("/api/auth/me");
    return enviar("POST", ruta, cuerpo);
}

QByteArray ApiCliente::tokenCsrf() const {
    const auto cookies = m_red->cookieJar()->cookiesForUrl(m_urlBase);
    for (const QNetworkCookie& c : cookies)
        if (c.name() == "XSRF-TOKEN") return c.value();
    return {};
}

void ApiCliente::olvidarSesion() {
    m_red->setCookieJar(new QNetworkCookieJar(m_red));
}

RespuestaHttp ApiCliente::enviar(const QByteArray& metodo, const QString& ruta, const QJsonDocument& cuerpo) {
    QUrl url = m_urlBase.resolved(QUrl(ruta));
    QNetworkRequest req(url);
    req.setRawHeader("Accept", "application/json");
    req.setTransferTimeout(kTiempoMaximoMs);

    QByteArray datos;
    if (metodo != "GET") {
        req.setHeader(QNetworkRequest::ContentTypeHeader, "application/json");
        const QByteArray token = tokenCsrf();
        if (!token.isEmpty()) req.setRawHeader("X-XSRF-TOKEN", token);
        datos = cuerpo.isNull() ? QByteArray("{}") : cuerpo.toJson(QJsonDocument::Compact);
    }

    QNetworkReply* respuesta = m_red->sendCustomRequest(req, metodo, datos);

    // Espera síncrona: las pantallas llaman al servicio y usan el resultado directamente.
    QEventLoop espera;
    connect(respuesta, &QNetworkReply::finished, &espera, &QEventLoop::quit);
    espera.exec(QEventLoop::ExcludeUserInputEvents);

    RespuestaHttp r;
    r.estado = respuesta->attribute(QNetworkRequest::HttpStatusCodeAttribute).toInt();
    const QByteArray contenido = respuesta->readAll();
    if (!contenido.isEmpty()) r.cuerpo = QJsonDocument::fromJson(contenido);
    if (r.estado == 0) r.errorRed = respuesta->errorString();
    respuesta->deleteLater();

    if (r.estado == 401 && ruta != "/api/auth/login") emit sesionExpirada();
    return r;
}

} // namespace polimarket
