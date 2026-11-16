#include "ui/rh/VendedoresPage.h"

#include "ui/comun/Estilos.h"
#include "ui/comun/Tablas.h"

#include <QComboBox>
#include <QHBoxLayout>
#include <QInputDialog>
#include <QLabel>
#include <QLineEdit>
#include <QPushButton>
#include <QTableWidget>
#include <QVBoxLayout>

namespace polimarket {

VendedoresPage::VendedoresPage(std::shared_ptr<IRecursosHumanosService> servicio, QWidget* parent)
    : QWidget(parent), m_servicio(std::move(servicio)) {
    auto* l = new QVBoxLayout(this);
    l->setContentsMargins(24, 20, 24, 20);
    l->addWidget(ui::encabezadoPagina(
        "Avales de vendedores",
        "Los vendedores solo pueden vender cuando su aval está APROBADO. Rechazar un aval aprobado lo revoca."));

    auto* filtros = new QHBoxLayout;
    m_buscar = new QLineEdit;
    m_buscar->setPlaceholderText("Buscar por nombre, código o zona");
    m_filtroEstado = new QComboBox;
    m_filtroEstado->addItems({"Todos los estados", "PENDIENTE", "APROBADO", "RECHAZADO"});
    filtros->addWidget(m_buscar, 1);
    filtros->addWidget(m_filtroEstado);
    l->addLayout(filtros);

    m_tabla = new QTableWidget;
    ui::configurarTabla(m_tabla, {"Código", "Nombre", "Zona", "Estado del aval"});
    l->addWidget(m_tabla, 1);

    auto* acciones = new QHBoxLayout;
    m_resumen = new QLabel;
    m_resumen->setObjectName("subtitulo");
    m_aprobar = new QPushButton("Aprobar aval");
    m_aprobar->setObjectName("exito");
    m_rechazar = new QPushButton("Rechazar / revocar");
    m_rechazar->setObjectName("peligro");
    auto* refrescar = new QPushButton("Actualizar");
    acciones->addWidget(m_resumen);
    acciones->addStretch();
    acciones->addWidget(refrescar);
    acciones->addWidget(m_rechazar);
    acciones->addWidget(m_aprobar);
    l->addLayout(acciones);

    connect(m_buscar, &QLineEdit::textChanged, this, &VendedoresPage::pintar);
    connect(m_filtroEstado, &QComboBox::currentIndexChanged, this, &VendedoresPage::pintar);
    connect(m_tabla, &QTableWidget::itemSelectionChanged, this, &VendedoresPage::actualizarBotones);
    connect(refrescar, &QPushButton::clicked, this, &VendedoresPage::recargar);
    connect(m_aprobar, &QPushButton::clicked, this, &VendedoresPage::aprobar);
    connect(m_rechazar, &QPushButton::clicked, this, &VendedoresPage::rechazar);

    recargar();
}

void VendedoresPage::recargar() {
    const auto r = m_servicio->listarVendedores();
    if (!r.ok) {
        m_vendedores.clear();
        pintar();
        m_resumen->setText("No se pudo cargar la lista: " + r.mensaje);
        return;
    }
    m_vendedores = r.valor;
    pintar();
}

void VendedoresPage::pintar() {
    const QString texto = m_buscar->text().trimmed();
    const QString estado = m_filtroEstado->currentIndex() == 0 ? QString() : m_filtroEstado->currentText();

    m_tabla->setRowCount(0);
    int pendientes = 0;
    for (const auto& v : m_vendedores) {
        if (v.estadoAval == "PENDIENTE") ++pendientes;
        const bool coincideTexto = texto.isEmpty() || v.nombre.contains(texto, Qt::CaseInsensitive)
                                   || v.codigo.contains(texto, Qt::CaseInsensitive)
                                   || v.zona.contains(texto, Qt::CaseInsensitive);
        if (!coincideTexto || (!estado.isEmpty() && v.estadoAval != estado)) continue;

        const int fila = m_tabla->rowCount();
        m_tabla->insertRow(fila);
        auto* codigo = ui::celda(v.codigo);
        codigo->setData(Qt::UserRole, v.id);
        m_tabla->setItem(fila, 0, codigo);
        m_tabla->setItem(fila, 1, ui::celda(v.nombre));
        m_tabla->setItem(fila, 2, ui::celda(v.zona.isEmpty() ? "—" : v.zona));
        m_tabla->setItem(fila, 3, ui::celdaEstado(v.estadoAval, ui::colorEstadoAval(v.estadoAval)));
    }
    m_resumen->setText(QString("%1 vendedores · %2 con aval pendiente").arg(m_vendedores.size()).arg(pendientes));
    actualizarBotones();
}

const Vendedor* VendedoresPage::seleccionado() const {
    const auto filas = m_tabla->selectionModel()->selectedRows();
    if (filas.isEmpty()) return nullptr;
    const qint64 id = m_tabla->item(filas.first().row(), 0)->data(Qt::UserRole).toLongLong();
    for (const auto& v : m_vendedores)
        if (v.id == id) return &v;
    return nullptr;
}

void VendedoresPage::actualizarBotones() {
    const Vendedor* v = seleccionado();
    m_aprobar->setEnabled(v && v->estadoAval != "APROBADO");
    m_rechazar->setEnabled(v && v->estadoAval != "RECHAZADO");
}

void VendedoresPage::aprobar() {
    const Vendedor* v = seleccionado();
    if (!v) return;
    const qint64 id = v->id;
    const QString nombre = v->nombre;
    bool ok = false;
    const QString obs = QInputDialog::getText(this, "Aprobar aval", "Observación (opcional):",
                                              QLineEdit::Normal, QString(), &ok);
    if (!ok) return;
    const auto r = m_servicio->aprobarAval(id, obs);
    ui::mostrarResultado(this, r.ok, r.ok ? QString("Aval APROBADO para %1.").arg(nombre) : r.mensaje);
    recargar();
}

void VendedoresPage::rechazar() {
    const Vendedor* v = seleccionado();
    if (!v) return;
    const qint64 id = v->id;
    const QString nombre = v->nombre;
    const bool revocar = v->estadoAval == "APROBADO";
    bool ok = false;
    const QString motivo = QInputDialog::getMultiLineText(
        this, revocar ? "Revocar aval" : "Rechazar aval", "Motivo (opcional):", QString(), &ok);
    if (!ok) return;
    const auto r = m_servicio->rechazarAval(id, motivo);
    ui::mostrarResultado(this, r.ok,
                         r.ok ? QString("Aval %1 para %2.").arg(revocar ? "revocado" : "rechazado", nombre) : r.mensaje);
    recargar();
}

} // namespace polimarket
