#pragma once

#include "servicios/IRecursosHumanosService.h"

#include <QWidget>
#include <memory>

class QFrame;
class QLabel;
class QLineEdit;

namespace polimarket {

// RS-1: consultar si un vendedor está autorizado.
//   GET /api/vendedores (para encontrarlo por código o nombre)
//   GET /api/vendedores/{id}/autorizacion
class AutorizacionPage : public QWidget {
    Q_OBJECT
public:
    explicit AutorizacionPage(std::shared_ptr<IRecursosHumanosService> servicio, QWidget* parent = nullptr);

private:
    void consultar();
    void mostrar(const QString& color, const QString& estado, const QString& nombre, const QString& detalle);

    std::shared_ptr<IRecursosHumanosService> m_servicio;
    QLineEdit* m_criterio = nullptr;
    QFrame* m_resultado = nullptr;
    QLabel* m_estado = nullptr;
    QLabel* m_nombre = nullptr;
    QLabel* m_detalle = nullptr;
};

} // namespace polimarket
