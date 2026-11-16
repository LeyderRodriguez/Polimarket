#include "ui/bodega/IngresoProductosPage.h"

#include "ui/comun/Estilos.h"
#include "ui/comun/Tablas.h"

#include <QComboBox>
#include <QDoubleSpinBox>
#include <QFormLayout>
#include <QHBoxLayout>
#include <QLabel>
#include <QLineEdit>
#include <QPushButton>
#include <QSpinBox>
#include <QTableWidget>
#include <QVBoxLayout>

namespace polimarket {

namespace {
constexpr int kColProducto = 0;
constexpr int kColCantidad = 1;
constexpr int kColCosto = 2;
constexpr int kColSubtotal = 3;
} // namespace

IngresoProductosPage::IngresoProductosPage(std::shared_ptr<IBodegaService> servicio, QWidget* parent)
    : QWidget(parent), m_servicio(std::move(servicio)) {
    auto* l = new QVBoxLayout(this);
    l->setContentsMargins(24, 20, 24, 20);
    l->addWidget(ui::encabezadoPagina(
        "Registrar entrada de mercancía",
        "Registra lo recibido de un proveedor. Cada producto genera un movimiento de ENTRADA con quién lo hizo y cuándo."));

    auto* caja = ui::tarjeta();
    auto* form = new QFormLayout(caja);
    form->setContentsMargins(20, 16, 20, 16);
    m_proveedor = new QComboBox;
    m_referencia = new QLineEdit;
    m_referencia->setPlaceholderText("Ej.: FAC-001");
    m_referencia->setMaxLength(60);
    form->addRow("Proveedor *", m_proveedor);
    form->addRow("Factura o referencia", m_referencia);
    l->addWidget(caja);

    auto* barra = new QHBoxLayout;
    barra->addWidget(new QLabel("<b>Productos recibidos</b>"));
    barra->addStretch();
    auto* recargarBtn = new QPushButton("Recargar catálogos");
    auto* quitar = new QPushButton("Quitar seleccionado");
    auto* agregar = new QPushButton("+ Agregar producto");
    barra->addWidget(recargarBtn);
    barra->addWidget(quitar);
    barra->addWidget(agregar);
    l->addLayout(barra);

    m_items = new QTableWidget;
    ui::configurarTabla(m_items, {"Producto", "Cantidad", "Costo unitario (opcional)", "Subtotal"});
    l->addWidget(m_items, 1);

    m_aviso = new QLabel;
    m_aviso->setWordWrap(true);
    m_aviso->setStyleSheet(QString("color: %1;").arg(estilos::kColorAlerta));
    m_aviso->hide();
    l->addWidget(m_aviso);

    auto* pie = new QHBoxLayout;
    m_total = new QLabel;
    m_total->setStyleSheet("font-size: 13pt; font-weight: 600;");
    auto* limpiarBtn = new QPushButton("Limpiar");
    auto* registrarBtn = new QPushButton("Registrar entrada");
    registrarBtn->setObjectName("primario");
    pie->addWidget(m_total);
    pie->addStretch();
    pie->addWidget(limpiarBtn);
    pie->addWidget(registrarBtn);
    l->addLayout(pie);

    connect(agregar, &QPushButton::clicked, this, &IngresoProductosPage::agregarFila);
    connect(quitar, &QPushButton::clicked, this, &IngresoProductosPage::quitarFila);
    connect(recargarBtn, &QPushButton::clicked, this, &IngresoProductosPage::cargarCatalogos);
    connect(limpiarBtn, &QPushButton::clicked, this, &IngresoProductosPage::limpiar);
    connect(registrarBtn, &QPushButton::clicked, this, &IngresoProductosPage::registrar);

    cargarCatalogos();
}

void IngresoProductosPage::cargarCatalogos() {
    const auto prov = m_servicio->listarProveedores();
    const auto prod = m_servicio->listarInventario();
    m_listaProveedores = prov.ok ? prov.valor : QList<Proveedor>{};
    m_listaProductos = prod.ok ? prod.valor : QList<ProductoInventario>{};

    m_proveedor->clear();
    for (const auto& p : m_listaProveedores) m_proveedor->addItem(p.nombre, p.id);

    QStringList errores;
    if (!prov.ok) errores << "proveedores: " + prov.mensaje;
    if (!prod.ok) errores << "productos: " + prod.mensaje;
    if (prov.ok && m_listaProveedores.isEmpty()) errores << "no hay proveedores registrados";
    m_aviso->setVisible(!errores.isEmpty());
    m_aviso->setText("No se pudieron cargar los catálogos (" + errores.join("; ") + ").");
    limpiar();
}

void IngresoProductosPage::agregarFila() {
    const int f = m_items->rowCount();
    m_items->insertRow(f);

    auto* producto = new QComboBox;
    for (const auto& p : m_listaProductos)
        producto->addItem(QString("%1 · %2").arg(p.codigo, p.nombre), p.id);
    auto* cantidad = new QSpinBox;
    cantidad->setRange(1, 1000000);
    cantidad->setValue(10);
    auto* costo = new QDoubleSpinBox;
    costo->setRange(0, 9999999999.0);
    costo->setDecimals(2);
    costo->setPrefix("$ ");
    costo->setSingleStep(100);
    costo->setSpecialValueText("Sin costo");

    m_items->setCellWidget(f, kColProducto, producto);
    m_items->setCellWidget(f, kColCantidad, cantidad);
    m_items->setCellWidget(f, kColCosto, costo);
    m_items->setItem(f, kColSubtotal, ui::celda(QString()));

    connect(cantidad, &QSpinBox::valueChanged, this, &IngresoProductosPage::recalcularTotal);
    connect(costo, &QDoubleSpinBox::valueChanged, this, &IngresoProductosPage::recalcularTotal);
    recalcularTotal();
}

void IngresoProductosPage::quitarFila() {
    const int f = m_items->currentRow();
    if (f >= 0) m_items->removeRow(f);
    recalcularTotal();
}

void IngresoProductosPage::recalcularTotal() {
    double total = 0;
    for (int f = 0; f < m_items->rowCount(); ++f) {
        auto* cantidad = qobject_cast<QSpinBox*>(m_items->cellWidget(f, kColCantidad));
        auto* costo = qobject_cast<QDoubleSpinBox*>(m_items->cellWidget(f, kColCosto));
        if (!cantidad || !costo) continue;
        const double subtotal = cantidad->value() * costo->value();
        total += subtotal;
        m_items->item(f, kColSubtotal)->setText(costo->value() > 0 ? ui::pesos(subtotal) : "—");
    }
    m_total->setText("Total: " + ui::pesos(total));
}

void IngresoProductosPage::registrar() {
    if (m_proveedor->count() == 0) {
        ui::mostrarResultado(this, false, "No hay proveedores para seleccionar.");
        return;
    }
    if (m_items->rowCount() == 0) {
        ui::mostrarResultado(this, false, "Agregue al menos un producto.");
        return;
    }

    // El backend registra una entrada por producto: se envían en orden y se informa cada resultado.
    QStringList registrados, fallidos;
    QList<int> filasOk;
    for (int f = 0; f < m_items->rowCount(); ++f) {
        auto* producto = qobject_cast<QComboBox*>(m_items->cellWidget(f, kColProducto));
        auto* cantidad = qobject_cast<QSpinBox*>(m_items->cellWidget(f, kColCantidad));
        auto* costo = qobject_cast<QDoubleSpinBox*>(m_items->cellWidget(f, kColCosto));
        if (!producto || producto->currentIndex() < 0) continue;

        NuevaEntrada e;
        e.productoId = producto->currentData().toLongLong();
        e.cantidad = cantidad->value();
        e.proveedorId = m_proveedor->currentData().toLongLong();
        e.referencia = m_referencia->text().trimmed();
        e.costoUnitario = costo->value();
        const auto r = m_servicio->registrarEntrada(e);
        if (r.ok) {
            registrados << QString("%1: +%2 (stock %3 → %4)")
                               .arg(producto->currentText()).arg(e.cantidad)
                               .arg(r.valor.stockAnterior).arg(r.valor.stockFisico);
            filasOk << f;
        } else {
            fallidos << QString("%1: %2").arg(producto->currentText(), r.mensaje);
        }
    }

    QString mensaje;
    if (!registrados.isEmpty()) mensaje += "Entradas registradas:\n• " + registrados.join("\n• ");
    if (!fallidos.isEmpty()) mensaje += QString(mensaje.isEmpty() ? "" : "\n\n") + "No se registraron:\n• " + fallidos.join("\n• ");
    ui::mostrarResultado(this, fallidos.isEmpty(), mensaje);

    if (!registrados.isEmpty()) {
        emit ingresoRegistrado();
        // Se quitan solo las filas registradas, para poder corregir y reintentar las fallidas.
        for (int i = filasOk.size() - 1; i >= 0; --i) m_items->removeRow(filasOk.at(i));
        if (m_items->rowCount() == 0) limpiar();
        recalcularTotal();
    }
}

void IngresoProductosPage::limpiar() {
    m_referencia->clear();
    m_items->setRowCount(0);
    if (!m_listaProductos.isEmpty()) agregarFila();
    recalcularTotal();
}

} // namespace polimarket
