#pragma once

#include "servicios/ServiceFactory.h"

#include <QMainWindow>

class QListWidget;
class QStackedWidget;

namespace polimarket {

// Ventana principal. Muestra el menú y las pantallas según el rol del usuario:
// Recursos Humanos o Bodega.
class MainWindow : public QMainWindow {
    Q_OBJECT
public:
    MainWindow(const UsuarioSesion& sesion, const Servicios& servicios, QWidget* parent = nullptr);

signals:
    void sesionCerrada(const QString& aviso);

private:
    void terminar(const QString& aviso);
    void agregarPagina(const QString& nombre, QWidget* pagina);
    void construirMenuRecursosHumanos();
    void construirMenuBodega();

    UsuarioSesion m_sesion;
    Servicios m_servicios;
    QListWidget* m_menu = nullptr;
    QStackedWidget* m_paginas = nullptr;
    bool m_terminando = false;
};

} // namespace polimarket
