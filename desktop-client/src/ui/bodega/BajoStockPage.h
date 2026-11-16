#pragma once

#include "servicios/IBodegaService.h"

#include <QWidget>
#include <memory>

class QLabel;
class QTableWidget;

namespace polimarket {

// RS-4: productos cuyo disponible está bajo el mínimo y sus proveedores.
//   GET /api/inventario/bajo-stock · GET /api/proveedores
class BajoStockPage : public QWidget {
    Q_OBJECT
public:
    explicit BajoStockPage(std::shared_ptr<IBodegaService> servicio, QWidget* parent = nullptr);

public slots:
    void recargar();

private:
    void mostrarProveedores();

    std::shared_ptr<IBodegaService> m_servicio;
    QList<ProductoBajoStock> m_items;
    QTableWidget* m_productos = nullptr;
    QTableWidget* m_proveedores = nullptr;
    QLabel* m_tituloProveedores = nullptr;
    QLabel* m_resumen = nullptr;
};

} // namespace polimarket
