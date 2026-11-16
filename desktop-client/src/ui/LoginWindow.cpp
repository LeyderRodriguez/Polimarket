#include "ui/LoginWindow.h"

#include "ui/comun/Estilos.h"

#include <QFrame>
#include <QLabel>
#include <QLineEdit>
#include <QPushButton>
#include <QVBoxLayout>

namespace polimarket {

LoginWindow::LoginWindow(std::shared_ptr<IAutenticacionService> autenticacion, QWidget* parent)
    : QWidget(parent), m_autenticacion(std::move(autenticacion)) {
    setObjectName("fondo");
    setWindowTitle("PoliMarket - Iniciar sesión");
    resize(460, 520);

    auto* raiz = new QVBoxLayout(this);
    raiz->setAlignment(Qt::AlignCenter);

    auto* caja = new QFrame;
    caja->setObjectName("tarjeta");
    caja->setFixedWidth(360);
    auto* l = new QVBoxLayout(caja);
    l->setContentsMargins(28, 28, 28, 28);
    l->setSpacing(10);

    auto* titulo = new QLabel("PoliMarket");
    titulo->setStyleSheet(QString("font-size: 22pt; font-weight: 700; color: %1;").arg(estilos::kColorPrimario));
    titulo->setAlignment(Qt::AlignCenter);
    auto* sub = new QLabel("Aplicación de escritorio\nRecursos Humanos y Bodega");
    sub->setObjectName("subtitulo");
    sub->setAlignment(Qt::AlignCenter);

    m_correo = new QLineEdit;
    m_correo->setPlaceholderText("correo@polimarket.test");
    m_clave = new QLineEdit;
    m_clave->setPlaceholderText("Contraseña");
    m_clave->setEchoMode(QLineEdit::Password);

    m_error = new QLabel;
    m_error->setStyleSheet(QString("color: %1;").arg(estilos::kColorAlerta));
    m_error->setWordWrap(true);
    m_error->hide();

    auto* boton = new QPushButton("Ingresar");
    boton->setObjectName("primario");
    boton->setDefault(true);

    auto* ayuda = new QLabel("Cuentas de prueba del backend:\nrh@polimarket.test · bodega@polimarket.test\nClave: Demo-1234");
    ayuda->setObjectName("subtitulo");
    ayuda->setAlignment(Qt::AlignCenter);
    ayuda->setStyleSheet("font-size: 8.5pt; color: #8A93A6;");

    l->addWidget(titulo);
    l->addWidget(sub);
    l->addSpacing(12);
    l->addWidget(new QLabel("Correo"));
    l->addWidget(m_correo);
    l->addWidget(new QLabel("Contraseña"));
    l->addWidget(m_clave);
    l->addWidget(m_error);
    l->addSpacing(6);
    l->addWidget(boton);
    l->addSpacing(10);
    l->addWidget(ayuda);

    raiz->addWidget(caja);

    connect(boton, &QPushButton::clicked, this, &LoginWindow::ingresar);
    connect(m_clave, &QLineEdit::returnPressed, this, &LoginWindow::ingresar);
    connect(m_correo, &QLineEdit::returnPressed, this, &LoginWindow::ingresar);
}

void LoginWindow::limpiar() {
    m_clave->clear();
    m_error->hide();
    m_correo->setFocus();
}

void LoginWindow::mostrarAviso(const QString& mensaje) {
    m_error->setText(mensaje);
    m_error->show();
}

void LoginWindow::ingresar() {
    if (m_correo->text().trimmed().isEmpty() || m_clave->text().isEmpty()) {
        mostrarAviso("Ingrese correo y contraseña.");
        return;
    }
    setEnabled(false);
    const auto r = m_autenticacion->iniciarSesion(m_correo->text(), m_clave->text());
    setEnabled(true);
    if (!r.ok) {
        mostrarAviso(r.mensaje);
        return;
    }
    m_error->hide();
    emit sesionIniciada(r.valor);
}

} // namespace polimarket
