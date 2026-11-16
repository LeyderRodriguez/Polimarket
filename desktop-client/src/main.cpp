// PoliMarket - Cliente de escritorio (C++ / Qt 6)
// Usuarios: Recursos Humanos y Bodega.

#include "servicios/ServiceFactory.h"
#include "ui/LoginWindow.h"
#include "ui/MainWindow.h"
#include "ui/comun/Estilos.h"

#include <QApplication>

using namespace polimarket;

int main(int argc, char* argv[]) {
    QApplication app(argc, argv);
    QApplication::setApplicationName("PoliMarket Escritorio");
    app.setStyle("Fusion");
    app.setStyleSheet(estilos::hojaGlobal());

    // Las pantallas reciben interfaces; aquí se decide si son simuladas o REST.
    const Servicios servicios = crearServicios(QApplication::arguments());

    LoginWindow login(servicios.autenticacion);
    QObject::connect(&login, &LoginWindow::sesionIniciada, [&](const UsuarioSesion& sesion) {
        auto* ventana = new MainWindow(sesion, servicios);
        ventana->setAttribute(Qt::WA_DeleteOnClose);
        QObject::connect(ventana, &MainWindow::sesionCerrada, &login, [&login](const QString& aviso) {
            login.limpiar();
            if (!aviso.isEmpty()) login.mostrarAviso(aviso);
            login.show();
        });
        ventana->show();
        login.hide();
    });
    login.show();

    return app.exec();
}
