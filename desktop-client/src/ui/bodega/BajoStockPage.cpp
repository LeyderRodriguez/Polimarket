#include "ui/bodega/BajoStockPage.h"

#include "ui/comun/Estilos.h"
#include "ui/comun/Tablas.h"

#include <QHBoxLayout>
#include <QLabel>
#include <QPushButton>
#include <QSplitter>
#include <QTableWidget>
#include <QVBoxLayout>

namespace polimarket {

BajoStockPage::BajoStockPage(std::shared_ptr<IBodegaService> servicio, QWidget* parent)
    : QWidget(parent), m_servicio(std::move(servicio)) {
    auto* l = new QVBoxLayout(this);
    l->setContentsMargins(24, 20, 24, 20);
    l->addWidget(ui::encabezadoPagina(
        "Bajo stock y proveedores",
        "Productos cuyo disponible está por debajo del mínimo. Seleccione uno para ver a qué proveedores pedirlo."));

    auto* divisor = new QSplitter(Qt::Vertical);

    auto* arriba = new QWidget;
    auto* al = new QVBoxLayout(arriba);
    al->setContentsMargins(0, 0, 0, 0);
    auto* barra = new QHBoxLayout;
    barra->addWidget(new QLabel("<b>Productos por reabastecer</b>"));
    m_resumen = new QLabel;
    m_resumen->setObjectName("subtitulo");
    barra->addWidget(m_resumen);
    barra->addStretch();
    auto* actualizar = new QPushButton("Actualizar");
    barra->addWidget(actualizar);
    al->addLayout(barra);
    m_productos = new QTableWidget;
    ui::configurarTabla(m_productos, {"Producto", "Stock físico", "Reservado", "Disponible", "Stock mínimo", "Faltante"});
    al->addWidget(m_productos);

    auto* abajo = new QWidget;
    auto* bl = new QVBoxLayout(abajo);
    bl->setContentsMargins(0, 8, 0, 0);
    m_tituloProveedores = new QLabel("<b>Proveedores</b>");
    bl->addWidget(m_tituloProveedores);
    m_proveedores = new QTableWidget;
    ui::configurarTabla(m_proveedores, {"Proveedor", "Contacto", "Teléfono"});
    bl->addWidget(m_proveedores);

    divisor->addWidget(arriba);
    divisor->addWidget(abajo);
    l->addWidget(divisor, 1);

    connect(actualizar, &QPushButton::clicked, this, &BajoStockPage::recargar);
    connect(m_productos, &QTableWidget::itemSelectionChanged, this, &BajoStockPage::mostrarProveedores);

    recargar();
}

void BajoStockPage::recargar() {
    const auto r = m_servicio->listarBajoStock();
    m_items = r.ok ? r.valor : QList<ProductoBajoStock>{};
    m_resumen->setText(r.ok ? QString("· %1 producto%2").arg(m_items.size()).arg(m_items.size() == 1 ? "" : "s") : "· Error: " + r.mensaje);

    m_productos->setRowCount(0);
    m_proveedores->setRowCount(0);
    for (const auto& b : m_items) {
        const int f = m_productos->rowCount();
        m_productos->insertRow(f);
        m_productos->setItem(f, 0, ui::celda(b.nombre));
        m_productos->setItem(f, 1, ui::celdaNumero(b.stockFisico));
        m_productos->setItem(f, 2, ui::celdaNumero(b.cantidadReservada));
        m_productos->setItem(f, 3, ui::celdaNumero(b.cantidadDisponible));
        m_productos->setItem(f, 4, ui::celdaNumero(b.stockMinimo));
        auto* faltante = ui::celdaNumero(b.stockMinimo - b.cantidadDisponible);
        faltante->setForeground(QColor(estilos::kColorAlerta));
        m_productos->setItem(f, 5, faltante);
    }
    if (!m_items.isEmpty()) m_productos->selectRow(0);
    else m_tituloProveedores->setText("<b>Proveedores</b>");
}

void BajoStockPage::mostrarProveedores() {
    m_proveedores->setRowCount(0);
    const auto filas = m_productos->selectionModel()->selectedRows();
    if (filas.isEmpty()) return;
    const auto& item = m_items.at(filas.first().row());
    m_tituloProveedores->setText(QString("<b>Proveedores de %1</b>").arg(item.nombre));
    for (const auto& p : item.proveedores) {
        const int f = m_proveedores->rowCount();
        m_proveedores->insertRow(f);
        m_proveedores->setItem(f, 0, ui::celda(p.nombre));
        m_proveedores->setItem(f, 1, ui::celda(p.nombreContacto.isEmpty() ? "—" : p.nombreContacto));
        m_proveedores->setItem(f, 2, ui::celda(p.telefonoContacto.isEmpty() ? "—" : p.telefonoContacto));
    }
}

} // namespace polimarket
