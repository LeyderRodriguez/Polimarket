#include "ui/MainWindow.h"

#include "servicios/rest/ApiCliente.h"
#include "ui/bodega/BajoStockPage.h"
#include "ui/bodega/IngresoProductosPage.h"
#include "ui/bodega/InventarioPage.h"
#include "ui/rh/AutorizacionPage.h"
#include "ui/rh/CrearCuentaPage.h"
#include "ui/rh/CuentasPage.h"
#include "ui/rh/VendedoresPage.h"

#include <QFrame>
#include <QHBoxLayout>
#include <QLabel>
#include <QListWidget>
#include <QMessageBox>
#include <QPushButton>
#include <QStackedWidget>
#include <QStatusBar>
#include <QVBoxLayout>

namespace polimarket {

MainWindow::MainWindow(const UsuarioSesion& sesion, const Servicios& servicios, QWidget* parent)
    : QMainWindow(parent), m_sesion(sesion), m_servicios(servicios) {
    setWindowTitle("PoliMarket - " + textoRol(sesion.rol));
    resize(1180, 720);

    auto* central = new QWidget;
    central->setObjectName("fondo");
    auto* raiz = new QHBoxLayout(central);
    raiz->setContentsMargins(0, 0, 0, 0);
    raiz->setSpacing(0);

    // Barra lateral
    auto* lateral = new QFrame;
    lateral->setObjectName("barraLateral");
    lateral->setFixedWidth(230);
    auto* ll = new QVBoxLayout(lateral);
    ll->setContentsMargins(0, 0, 0, 12);
    auto* marca = new QLabel("PoliMarket");
    marca->setObjectName("marca");
    auto* rol = new QLabel(textoRol(sesion.rol));
    rol->setObjectName("rolLateral");
    m_menu = new QListWidget;
    m_menu->setObjectName("menu");
    auto* salir = new QPushButton("Cerrar sesión");
    salir->setStyleSheet("margin: 0 16px; background: transparent; color: white; border: 1px solid #5F7BB0;");
    ll->addWidget(marca);
    ll->addWidget(rol);
    ll->addWidget(m_menu, 1);
    ll->addWidget(salir);

    // Zona de contenido
    auto* contenido = new QWidget;
    auto* cl = new QVBoxLayout(contenido);
    cl->setContentsMargins(0, 0, 0, 0);
    cl->setSpacing(0);
    auto* encabezado = new QFrame;
    encabezado->setObjectName("encabezado");
    auto* el = new QHBoxLayout(encabezado);
    el->setContentsMargins(24, 12, 24, 12);
    auto* usuario = new QLabel(QString("<b>%1</b>  ·  %2").arg(sesion.nombre, sesion.correo));
    el->addStretch();
    el->addWidget(usuario);
    m_paginas = new QStackedWidget;
    cl->addWidget(encabezado);
    cl->addWidget(m_paginas, 1);

    raiz->addWidget(lateral);
    raiz->addWidget(contenido, 1);
    setCentralWidget(central);

    if (sesion.rol == Rol::RecursosHumanos) construirMenuRecursosHumanos();
    else construirMenuBodega();

    connect(m_menu, &QListWidget::currentRowChanged, m_paginas, &QStackedWidget::setCurrentIndex);
    m_menu->setCurrentRow(0);

    connect(salir, &QPushButton::clicked, this, [this] {
        m_servicios.autenticacion->cerrarSesion();
        terminar(QString());
    });

    // Si el backend responde 401 (sesión vencida o cuenta desactivada), se vuelve al login.
    if (m_servicios.api) {
        connect(m_servicios.api.get(), &ApiCliente::sesionExpirada, this, [this] {
            terminar("La sesión expiró o la cuenta fue desactivada. Inicie sesión de nuevo.");
        });
    }

    statusBar()->showMessage(m_servicios.descripcionModo);
}

void MainWindow::terminar(const QString& aviso) {
    if (m_terminando) return;
    m_terminando = true;
    if (m_servicios.api) disconnect(m_servicios.api.get(), nullptr, this, nullptr);
    emit sesionCerrada(aviso);
    close();
}

void MainWindow::agregarPagina(const QString& nombre, QWidget* pagina) {
    m_menu->addItem(nombre);
    m_paginas->addWidget(pagina);
}

void MainWindow::construirMenuRecursosHumanos() {
    auto* vendedores = new VendedoresPage(m_servicios.recursosHumanos);
    auto* crearCuenta = new CrearCuentaPage(m_servicios.recursosHumanos);
    auto* cuentas = new CuentasPage(m_servicios.recursosHumanos);
    agregarPagina("Avales de vendedores", vendedores);
    agregarPagina("Crear cuenta", crearCuenta);
    agregarPagina("Cuentas", cuentas);
    agregarPagina("Consultar autorización", new AutorizacionPage(m_servicios.recursosHumanos));

    // Una cuenta nueva puede ser un vendedor con aval PENDIENTE: se actualizan las listas.
    connect(crearCuenta, &CrearCuentaPage::cuentaCreada, cuentas, &CuentasPage::recargar);
    connect(crearCuenta, &CrearCuentaPage::cuentaCreada, vendedores, &VendedoresPage::recargar);
}

void MainWindow::construirMenuBodega() {
    auto* inventario = new InventarioPage(m_servicios.bodega);
    auto* bajoStock = new BajoStockPage(m_servicios.bodega);
    auto* ingreso = new IngresoProductosPage(m_servicios.bodega);
    agregarPagina("Inventario", inventario);
    agregarPagina("Bajo stock y proveedores", bajoStock);
    agregarPagina("Registrar entrada", ingreso);

    // Al registrar una entrada, las demás vistas se actualizan.
    connect(ingreso, &IngresoProductosPage::ingresoRegistrado, inventario, &InventarioPage::recargar);
    connect(ingreso, &IngresoProductosPage::ingresoRegistrado, bajoStock, &BajoStockPage::recargar);
}

} // namespace polimarket
