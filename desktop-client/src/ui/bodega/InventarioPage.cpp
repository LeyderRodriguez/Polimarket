#include "ui/bodega/InventarioPage.h"

#include "ui/comun/Estilos.h"
#include "ui/comun/Tablas.h"

#include <QHBoxLayout>
#include <QLabel>
#include <QLineEdit>
#include <QPushButton>
#include <QTableWidget>
#include <QVBoxLayout>

namespace polimarket {

namespace {
QLabel* indicador(QHBoxLayout* fila, const QString& titulo) {
    auto* caja = ui::tarjeta();
    auto* l = new QVBoxLayout(caja);
    l->setContentsMargins(16, 10, 16, 10);
    auto* t = new QLabel(titulo);
    t->setObjectName("subtitulo");
    auto* valor = new QLabel("0");
    valor->setStyleSheet(QString("font-size: 18pt; font-weight: 700; color: %1;").arg(estilos::kColorPrimario));
    l->addWidget(t);
    l->addWidget(valor);
    fila->addWidget(caja);
    return valor;
}
QString miles(int n) { return QLocale(QLocale::Spanish, QLocale::Colombia).toString(n); }
} // namespace

InventarioPage::InventarioPage(std::shared_ptr<IBodegaService> servicio, QWidget* parent)
    : QWidget(parent), m_servicio(std::move(servicio)) {
    auto* l = new QVBoxLayout(this);
    l->setContentsMargins(24, 20, 24, 20);
    l->addWidget(ui::encabezadoPagina(
        "Inventario",
        "Disponible = stock físico − unidades reservadas por ventas pendientes. Es la misma disponibilidad que "
        "consulta Ventas antes de registrar una venta."));

    auto* indicadores = new QHBoxLayout;
    m_totalProductos = indicador(indicadores, "Productos");
    m_totalFisico = indicador(indicadores, "Unidades en bodega");
    m_totalReservado = indicador(indicadores, "Reservadas por ventas");
    m_totalBajoStock = indicador(indicadores, "Bajo stock mínimo");
    l->addLayout(indicadores);

    auto* consulta = new QHBoxLayout;
    m_codigo = new QLineEdit;
    m_codigo->setPlaceholderText("Código del producto (ej.: AZU-01)");
    m_codigo->setMinimumWidth(280);
    auto* consultar = new QPushButton("Consultar disponibilidad");
    consultar->setObjectName("primario");
    m_disponibilidad = new QLabel;
    consulta->addWidget(m_codigo);
    consulta->addWidget(consultar);
    consulta->addWidget(m_disponibilidad, 1);
    l->addLayout(consulta);

    auto* filtros = new QHBoxLayout;
    m_buscar = new QLineEdit;
    m_buscar->setPlaceholderText("Filtrar por código o nombre");
    auto* actualizar = new QPushButton("Actualizar");
    filtros->addWidget(m_buscar, 1);
    filtros->addWidget(actualizar);
    l->addLayout(filtros);

    m_tabla = new QTableWidget;
    ui::configurarTabla(m_tabla, {"Código", "Producto", "Precio", "Stock físico", "Reservado", "Disponible", "Estado"});
    l->addWidget(m_tabla, 1);

    m_aviso = new QLabel;
    m_aviso->setWordWrap(true);
    m_aviso->setStyleSheet(QString("color: %1;").arg(estilos::kColorAlerta));
    m_aviso->hide();
    l->addWidget(m_aviso);

    connect(m_buscar, &QLineEdit::textChanged, this, &InventarioPage::pintar);
    connect(actualizar, &QPushButton::clicked, this, &InventarioPage::recargar);
    connect(consultar, &QPushButton::clicked, this, &InventarioPage::consultarDisponibilidad);
    connect(m_codigo, &QLineEdit::returnPressed, this, &InventarioPage::consultarDisponibilidad);

    recargar();
}

void InventarioPage::recargar() {
    const auto r = m_servicio->listarInventario();
    m_productos = r.ok ? r.valor : QList<ProductoInventario>{};
    m_aviso->setVisible(!r.ok);
    if (!r.ok) m_aviso->setText("No se pudo cargar el inventario: " + r.mensaje);
    pintar();
}

void InventarioPage::pintar() {
    const QString texto = m_buscar->text().trimmed();
    int fisico = 0, reservado = 0, bajos = 0;
    m_tabla->setRowCount(0);
    for (const auto& p : m_productos) {
        fisico += p.stockFisico;
        reservado += p.cantidadReservada;
        if (p.bajoStock) ++bajos;
        if (!texto.isEmpty() && !p.codigo.contains(texto, Qt::CaseInsensitive)
            && !p.nombre.contains(texto, Qt::CaseInsensitive))
            continue;

        const int f = m_tabla->rowCount();
        m_tabla->insertRow(f);
        m_tabla->setItem(f, 0, ui::celda(p.codigo));
        m_tabla->setItem(f, 1, ui::celda(p.nombre));
        m_tabla->setItem(f, 2, ui::celda(ui::pesos(p.precio)));
        m_tabla->setItem(f, 3, ui::celdaNumero(p.stockFisico));
        m_tabla->setItem(f, 4, ui::celdaNumero(p.cantidadReservada));
        m_tabla->setItem(f, 5, ui::celdaNumero(p.cantidadDisponible));
        m_tabla->setItem(f, 6, p.bajoStock
                                   ? ui::celdaEstado(QString("Bajo stock (mín. %1)").arg(p.stockMinimo), estilos::kColorAlerta)
                                   : ui::celdaEstado("Disponible", estilos::kColorExito));
    }
    m_totalProductos->setText(QString::number(m_productos.size()));
    m_totalFisico->setText(miles(fisico));
    m_totalReservado->setText(miles(reservado));
    m_totalBajoStock->setText(QString::number(bajos));
    m_totalBajoStock->setStyleSheet(QString("font-size: 18pt; font-weight: 700; color: %1;")
                                        .arg(bajos > 0 ? estilos::kColorAlerta : estilos::kColorExito));
}

void InventarioPage::consultarDisponibilidad() {
    const QString codigo = m_codigo->text().trimmed();
    if (codigo.isEmpty()) return;
    recargar(); // datos frescos del servidor
    for (const auto& p : m_productos) {
        if (p.codigo.compare(codigo, Qt::CaseInsensitive) != 0) continue;
        const QString color = p.bajoStock ? estilos::kColorAlerta : estilos::kColorExito;
        m_disponibilidad->setText(QString("<b>%1</b>: <span style='color:%2'><b>%3 disponibles</b></span> "
                                          "(físico %4 · reservado %5)")
                                      .arg(p.nombre, color, miles(p.cantidadDisponible), miles(p.stockFisico),
                                           miles(p.cantidadReservada)));
        return;
    }
    m_disponibilidad->setText(QString("<span style='color:%1'>No existe un producto con el código %2.</span>")
                                  .arg(estilos::kColorAlerta, codigo.toHtmlEscaped()));
}

} // namespace polimarket
