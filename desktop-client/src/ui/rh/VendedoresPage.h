#pragma once

#include "servicios/IRecursosHumanosService.h"

#include <QWidget>
#include <memory>

class QComboBox;
class QLabel;
class QLineEdit;
class QPushButton;
class QTableWidget;

namespace polimarket {

// RS-1: listar vendedores y aprobar, rechazar o revocar su aval.
//   GET /api/vendedores · POST /api/vendedores/{id}/aval/aprobar · .../rechazar
class VendedoresPage : public QWidget {
    Q_OBJECT
public:
    explicit VendedoresPage(std::shared_ptr<IRecursosHumanosService> servicio, QWidget* parent = nullptr);

public slots:
    void recargar();

private:
    void pintar();
    const Vendedor* seleccionado() const;
    void actualizarBotones();
    void aprobar();
    void rechazar();

    std::shared_ptr<IRecursosHumanosService> m_servicio;
    QList<Vendedor> m_vendedores;
    QLineEdit* m_buscar = nullptr;
    QComboBox* m_filtroEstado = nullptr;
    QTableWidget* m_tabla = nullptr;
    QPushButton* m_aprobar = nullptr;
    QPushButton* m_rechazar = nullptr;
    QLabel* m_resumen = nullptr;
};

} // namespace polimarket
