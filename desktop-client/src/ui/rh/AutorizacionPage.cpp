#include "ui/rh/AutorizacionPage.h"

#include "ui/comun/Estilos.h"
#include "ui/comun/Tablas.h"

#include <QHBoxLayout>
#include <QLabel>
#include <QLineEdit>
#include <QPushButton>
#include <QVBoxLayout>

namespace polimarket {

AutorizacionPage::AutorizacionPage(std::shared_ptr<IRecursosHumanosService> servicio, QWidget* parent)
    : QWidget(parent), m_servicio(std::move(servicio)) {
    auto* l = new QVBoxLayout(this);
    l->setContentsMargins(24, 20, 24, 20);
    l->addWidget(ui::encabezadoPagina(
        "Consultar autorización",
        "Es la misma consulta que hace Ventas antes de permitir que un vendedor registre una venta."));

    auto* busqueda = new QHBoxLayout;
    m_criterio = new QLineEdit;
    m_criterio->setPlaceholderText("Código o nombre del vendedor (ej.: VEN-0002 o Luis)");
    auto* boton = new QPushButton("Consultar");
    boton->setObjectName("primario");
    busqueda->addWidget(m_criterio, 1);
    busqueda->addWidget(boton);
    l->addLayout(busqueda);

    m_resultado = ui::tarjeta();
    auto* rl = new QVBoxLayout(m_resultado);
    rl->setContentsMargins(20, 18, 20, 18);
    m_estado = new QLabel;
    m_nombre = new QLabel;
    m_detalle = new QLabel;
    m_detalle->setWordWrap(true);
    rl->addWidget(m_estado);
    rl->addWidget(m_nombre);
    rl->addWidget(m_detalle);
    m_resultado->hide();
    l->addSpacing(8);
    l->addWidget(m_resultado);
    l->addStretch();

    connect(boton, &QPushButton::clicked, this, &AutorizacionPage::consultar);
    connect(m_criterio, &QLineEdit::returnPressed, this, &AutorizacionPage::consultar);
}

void AutorizacionPage::mostrar(const QString& color, const QString& estado, const QString& nombre,
                               const QString& detalle) {
    m_estado->setText(estado);
    m_nombre->setText(nombre);
    m_detalle->setText(detalle);
    m_estado->setStyleSheet(QString("font-size: 16pt; font-weight: 700; color: %1;").arg(color));
    m_resultado->setStyleSheet(QString("QFrame#tarjeta { border-left: 6px solid %1; }").arg(color));
    m_resultado->show();
}

void AutorizacionPage::consultar() {
    const QString criterio = m_criterio->text().trimmed();
    if (criterio.isEmpty()) return;

    const auto lista = m_servicio->listarVendedores();
    if (!lista.ok) {
        mostrar(estilos::kColorAlerta, "Error", QString(), lista.mensaje);
        return;
    }
    const Vendedor* encontrado = nullptr;
    for (const auto& v : lista.valor) {
        if (v.codigo.compare(criterio, Qt::CaseInsensitive) == 0 || v.nombre.contains(criterio, Qt::CaseInsensitive)) {
            encontrado = &v;
            break;
        }
    }
    if (!encontrado) {
        mostrar(estilos::kColorPendiente, "No se encontró el vendedor", QString(),
                "Verifique el código o el nombre. Para un vendedor nuevo, cree su cuenta con el rol Vendedor.");
        return;
    }

    const auto aval = m_servicio->consultarAutorizacion(encontrado->id);
    if (!aval.ok) {
        mostrar(estilos::kColorAlerta, "Error", encontrado->nombre, aval.mensaje);
        return;
    }
    const QString nombre = QString("<b>%1</b> (%2) · Estado del aval: %3")
                               .arg(encontrado->nombre, encontrado->codigo, aval.valor.estadoAval);
    if (aval.valor.autorizado)
        mostrar(estilos::kColorExito, "AUTORIZADO", nombre, "Puede registrar ventas en PoliMarket.");
    else
        mostrar(estilos::kColorAlerta, "NO AUTORIZADO", nombre,
                "No puede registrar ventas hasta que su aval esté APROBADO.");
}

} // namespace polimarket
