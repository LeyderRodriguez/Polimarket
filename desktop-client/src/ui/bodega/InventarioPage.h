#pragma once

#include "servicios/IBodegaService.h"

#include <QWidget>
#include <memory>

class QLabel;
class QLineEdit;
class QTableWidget;

namespace polimarket {

// RS-2: existencias y disponibilidad (disponible = stock físico − reservado por ventas pendientes).
//   GET /api/productos · GET /api/inventario/bajo-stock
class InventarioPage : public QWidget {
    Q_OBJECT
public:
    explicit InventarioPage(std::shared_ptr<IBodegaService> servicio, QWidget* parent = nullptr);

public slots:
    void recargar();

private:
    void pintar();
    void consultarDisponibilidad();

    std::shared_ptr<IBodegaService> m_servicio;
    QList<ProductoInventario> m_productos;
    QLineEdit* m_buscar = nullptr;
    QTableWidget* m_tabla = nullptr;
    QLabel* m_totalProductos = nullptr;
    QLabel* m_totalFisico = nullptr;
    QLabel* m_totalReservado = nullptr;
    QLabel* m_totalBajoStock = nullptr;
    QLineEdit* m_codigo = nullptr;
    QLabel* m_disponibilidad = nullptr;
    QLabel* m_aviso = nullptr;
};

} // namespace polimarket
