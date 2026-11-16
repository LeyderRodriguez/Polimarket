#pragma once

#include "servicios/IBodegaService.h"

#include <QWidget>
#include <memory>

class QComboBox;
class QLabel;
class QLineEdit;
class QTableWidget;

namespace polimarket {

// RF-11: registrar la mercancía recibida de un proveedor.
//   GET /api/proveedores · GET /api/productos · POST /api/inventario/entradas (una por producto)
class IngresoProductosPage : public QWidget {
    Q_OBJECT
public:
    explicit IngresoProductosPage(std::shared_ptr<IBodegaService> servicio, QWidget* parent = nullptr);

public slots:
    void cargarCatalogos();

signals:
    void ingresoRegistrado();

private:
    void agregarFila();
    void quitarFila();
    void recalcularTotal();
    void registrar();
    void limpiar();

    std::shared_ptr<IBodegaService> m_servicio;
    QList<Proveedor> m_listaProveedores;
    QList<ProductoInventario> m_listaProductos;
    QComboBox* m_proveedor = nullptr;
    QLineEdit* m_referencia = nullptr;
    QTableWidget* m_items = nullptr;
    QLabel* m_total = nullptr;
    QLabel* m_aviso = nullptr;
};

} // namespace polimarket
