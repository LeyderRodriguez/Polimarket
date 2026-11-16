#pragma once
// Utilidades compartidas por las pantallas: tablas, encabezados y mensajes.

#include "ui/comun/Estilos.h"

#include <QColor>
#include <QFrame>
#include <QHeaderView>
#include <QLabel>
#include <QLocale>
#include <QMessageBox>
#include <QTableWidget>
#include <QVBoxLayout>

namespace polimarket::ui {

inline void configurarTabla(QTableWidget* tabla, const QStringList& columnas) {
    tabla->setColumnCount(columnas.size());
    tabla->setHorizontalHeaderLabels(columnas);
    tabla->horizontalHeader()->setSectionResizeMode(QHeaderView::Stretch);
    tabla->verticalHeader()->setVisible(false);
    tabla->setSelectionBehavior(QAbstractItemView::SelectRows);
    tabla->setSelectionMode(QAbstractItemView::SingleSelection);
    tabla->setEditTriggers(QAbstractItemView::NoEditTriggers);
    tabla->setAlternatingRowColors(false);
}

inline QTableWidgetItem* celda(const QString& texto) {
    return new QTableWidgetItem(texto);
}

inline QTableWidgetItem* celdaNumero(double valor, int decimales = 0) {
    auto* item = new QTableWidgetItem(QLocale(QLocale::Spanish, QLocale::Colombia).toString(valor, 'f', decimales));
    item->setTextAlignment(Qt::AlignRight | Qt::AlignVCenter);
    return item;
}

inline QTableWidgetItem* celdaEstado(const QString& texto, const QString& colorHex) {
    auto* item = new QTableWidgetItem(texto);
    item->setForeground(QColor(colorHex));
    QFont f = item->font();
    f.setBold(true);
    item->setFont(f);
    return item;
}

inline QString pesos(double valor) {
    return "$ " + QLocale(QLocale::Spanish, QLocale::Colombia).toString(valor, 'f', 0);
}

// Encabezado de página: título y descripción breve.
inline QWidget* encabezadoPagina(const QString& titulo, const QString& subtitulo) {
    auto* w = new QWidget;
    auto* l = new QVBoxLayout(w);
    l->setContentsMargins(0, 0, 0, 8);
    auto* t = new QLabel(titulo);
    t->setObjectName("tituloPagina");
    auto* s = new QLabel(subtitulo);
    s->setObjectName("subtitulo");
    s->setWordWrap(true);
    l->addWidget(t);
    l->addWidget(s);
    return w;
}

inline QFrame* tarjeta() {
    auto* f = new QFrame;
    f->setObjectName("tarjeta");
    return f;
}

inline QString colorEstadoAval(const QString& estado) {
    if (estado == "APROBADO") return estilos::kColorExito;
    if (estado == "RECHAZADO") return estilos::kColorAlerta;
    return estilos::kColorPendiente;
}

inline void mostrarResultado(QWidget* padre, bool ok, const QString& mensaje) {
    if (ok) QMessageBox::information(padre, "PoliMarket", mensaje);
    else QMessageBox::warning(padre, "PoliMarket", mensaje);
}

} // namespace polimarket::ui
