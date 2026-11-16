#pragma once

#include "servicios/IRecursosHumanosService.h"

#include <QWidget>
#include <memory>

class QLabel;
class QLineEdit;
class QPushButton;
class QTableWidget;

namespace polimarket {

// Listado de cuentas y desactivación.
//   GET /api/usuarios · POST /api/usuarios/{id}/desactivacion
class CuentasPage : public QWidget {
    Q_OBJECT
public:
    explicit CuentasPage(std::shared_ptr<IRecursosHumanosService> servicio, QWidget* parent = nullptr);

public slots:
    void recargar();

private:
    void pintar();
    const Cuenta* seleccionada() const;
    void desactivar();

    std::shared_ptr<IRecursosHumanosService> m_servicio;
    QList<Cuenta> m_cuentas;
    QLineEdit* m_buscar = nullptr;
    QTableWidget* m_tabla = nullptr;
    QPushButton* m_desactivar = nullptr;
    QLabel* m_resumen = nullptr;
};

} // namespace polimarket
