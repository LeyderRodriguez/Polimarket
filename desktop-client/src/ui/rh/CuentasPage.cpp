#include "ui/rh/CuentasPage.h"

#include "ui/comun/Estilos.h"
#include "ui/comun/Tablas.h"

#include <QHBoxLayout>
#include <QLabel>
#include <QLineEdit>
#include <QMessageBox>
#include <QPushButton>
#include <QTableWidget>
#include <QVBoxLayout>

namespace polimarket {

namespace {
QString textoRolCuenta(const QString& rol) {
    if (rol == "RECURSOS_HUMANOS") return "Recursos Humanos";
    if (rol == "VENDEDOR") return "Vendedor";
    if (rol == "BODEGA") return "Bodega";
    if (rol == "ENTREGAS") return "Entregas";
    return rol;
}
} // namespace

CuentasPage::CuentasPage(std::shared_ptr<IRecursosHumanosService> servicio, QWidget* parent)
    : QWidget(parent), m_servicio(std::move(servicio)) {
    auto* l = new QVBoxLayout(this);
    l->setContentsMargins(24, 20, 24, 20);
    l->addWidget(ui::encabezadoPagina(
        "Cuentas",
        "Las cuentas no se borran: se desactivan y sus sesiones abiertas se cierran. Para cambiar el rol de alguien, "
        "se desactiva su cuenta y se crea otra."));

    m_buscar = new QLineEdit;
    m_buscar->setPlaceholderText("Buscar por nombre, correo o rol");
    l->addWidget(m_buscar);

    m_tabla = new QTableWidget;
    ui::configurarTabla(m_tabla, {"Nombre", "Correo", "Rol", "Estado"});
    l->addWidget(m_tabla, 1);

    auto* acciones = new QHBoxLayout;
    m_resumen = new QLabel;
    m_resumen->setObjectName("subtitulo");
    auto* refrescar = new QPushButton("Actualizar");
    m_desactivar = new QPushButton("Desactivar cuenta");
    m_desactivar->setObjectName("peligro");
    acciones->addWidget(m_resumen);
    acciones->addStretch();
    acciones->addWidget(refrescar);
    acciones->addWidget(m_desactivar);
    l->addLayout(acciones);

    connect(m_buscar, &QLineEdit::textChanged, this, &CuentasPage::pintar);
    connect(refrescar, &QPushButton::clicked, this, &CuentasPage::recargar);
    connect(m_desactivar, &QPushButton::clicked, this, &CuentasPage::desactivar);
    connect(m_tabla, &QTableWidget::itemSelectionChanged, this, [this] {
        const Cuenta* c = seleccionada();
        m_desactivar->setEnabled(c && c->activo);
    });

    recargar();
}

void CuentasPage::recargar() {
    const auto r = m_servicio->listarCuentas();
    m_cuentas = r.ok ? r.valor : QList<Cuenta>{};
    pintar();
    if (!r.ok) m_resumen->setText("No se pudo cargar la lista: " + r.mensaje);
}

void CuentasPage::pintar() {
    const QString texto = m_buscar->text().trimmed();
    m_tabla->setRowCount(0);
    int activas = 0;
    for (const auto& c : m_cuentas) {
        if (c.activo) ++activas;
        const QString rol = textoRolCuenta(c.rol);
        if (!texto.isEmpty() && !c.nombre.contains(texto, Qt::CaseInsensitive)
            && !c.correo.contains(texto, Qt::CaseInsensitive) && !rol.contains(texto, Qt::CaseInsensitive))
            continue;
        const int f = m_tabla->rowCount();
        m_tabla->insertRow(f);
        auto* nombre = ui::celda(c.nombre);
        nombre->setData(Qt::UserRole, c.id);
        m_tabla->setItem(f, 0, nombre);
        m_tabla->setItem(f, 1, ui::celda(c.correo));
        m_tabla->setItem(f, 2, ui::celda(rol));
        m_tabla->setItem(f, 3, c.activo ? ui::celdaEstado("Activa", estilos::kColorExito)
                                        : ui::celdaEstado("Desactivada", "#8A93A6"));
    }
    m_resumen->setText(QString("%1 cuentas · %2 activas").arg(m_cuentas.size()).arg(activas));
    m_desactivar->setEnabled(false);
}

const Cuenta* CuentasPage::seleccionada() const {
    const auto filas = m_tabla->selectionModel()->selectedRows();
    if (filas.isEmpty()) return nullptr;
    const qint64 id = m_tabla->item(filas.first().row(), 0)->data(Qt::UserRole).toLongLong();
    for (const auto& c : m_cuentas)
        if (c.id == id) return &c;
    return nullptr;
}

void CuentasPage::desactivar() {
    const Cuenta* c = seleccionada();
    if (!c) return;
    const qint64 id = c->id;
    const QString texto = QString("¿Desactivar la cuenta de %1 (%2)?\nNo podrá iniciar sesión y sus sesiones abiertas "
                                  "se cerrarán.").arg(c->nombre, c->correo);
    if (QMessageBox::question(this, "Desactivar cuenta", texto) != QMessageBox::Yes) return;
    const auto r = m_servicio->desactivarCuenta(id);
    ui::mostrarResultado(this, r.ok, r.ok ? "Cuenta desactivada." : r.mensaje);
    recargar();
}

} // namespace polimarket
