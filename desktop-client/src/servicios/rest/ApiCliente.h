#pragma once
// Cliente HTTP de la API de PoliMarket con Qt Network.
// Cumple las reglas del backend (docs/guides/clients.md):
//   1. Sesión con la cookie JSESSIONID: la guarda y reenvía el QNetworkCookieJar.
//   2. En cada POST/PATCH copia la cookie XSRF-TOKEN al encabezado X-XSRF-TOKEN.
//   3. Envía y recibe JSON. Los errores llegan como { "codigo", "mensaje" }.
//   4. Si una llamada responde 401, emite sesionExpirada() para volver al login.

#include "modelos/Modelos.h"

#include <QJsonDocument>
#include <QObject>
#include <QUrl>

class QNetworkAccessManager;
class QNetworkReply;

namespace polimarket {

struct RespuestaHttp {
    int estado = 0;          // código HTTP; 0 si no hubo conexión
    QJsonDocument cuerpo;    // JSON de la respuesta (puede estar vacío)
    QString errorRed;        // descripción del fallo de red, si lo hubo

    bool exitosa() const { return estado >= 200 && estado < 300; }
    QString codigo() const;  // "codigo" del error del backend
    QString mensaje() const; // "mensaje" del error, o una explicación del fallo
};

class ApiCliente : public QObject {
    Q_OBJECT
public:
    explicit ApiCliente(const QUrl& urlBase, QObject* parent = nullptr);

    QUrl urlBase() const { return m_urlBase; }

    RespuestaHttp get(const QString& ruta);
    RespuestaHttp post(const QString& ruta, const QJsonDocument& cuerpo = {});

    // Convierte un error HTTP en la Respuesta<T> que usan las pantallas.
    template <typename T>
    static Respuesta<T> error(const RespuestaHttp& r) { return Respuesta<T>::error(r.codigo(), r.mensaje()); }

    void olvidarSesion();

signals:
    void sesionExpirada();

private:
    RespuestaHttp enviar(const QByteArray& metodo, const QString& ruta, const QJsonDocument& cuerpo);
    QByteArray tokenCsrf() const;

    QUrl m_urlBase;
    QNetworkAccessManager* m_red = nullptr;
};

} // namespace polimarket
