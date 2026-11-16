#pragma once

#include "servicios/IRecursosHumanosService.h"

#include <QWidget>
#include <memory>
#include <optional>

class QComboBox;
class QFrame;
class QLabel;
class QLineEdit;
class QPushButton;

namespace polimarket {

// Crear la cuenta de un empleado, siguiendo el flujo del backend:
//   1. GET  /api/terceros?tipoDocumento&numeroDocumento   (buscar para no duplicar)
//   2. POST /api/terceros                                  (si no existe)
//   3. POST /api/usuarios                                  (cuenta con rol; un VENDEDOR queda con aval PENDIENTE)
class CrearCuentaPage : public QWidget {
    Q_OBJECT
public:
    explicit CrearCuentaPage(std::shared_ptr<IRecursosHumanosService> servicio, QWidget* parent = nullptr);

signals:
    void cuentaCreada();

private:
    void buscar();
    void crear();
    void limpiar();
    void habilitarDatosPersonales(bool nuevo);

    std::shared_ptr<IRecursosHumanosService> m_servicio;
    std::optional<Tercero> m_tercero; // tercero encontrado en la búsqueda
    bool m_buscado = false;

    QComboBox* m_tipoDocumento = nullptr;
    QLineEdit* m_documento = nullptr;
    QLabel* m_estadoBusqueda = nullptr;

    QFrame* m_datos = nullptr;
    QLineEdit* m_nombres = nullptr;
    QLineEdit* m_apellidos = nullptr;
    QLineEdit* m_telefono = nullptr;
    QLineEdit* m_correoContacto = nullptr;
    QLineEdit* m_direccion = nullptr;

    QFrame* m_cuenta = nullptr;
    QLineEdit* m_correo = nullptr;
    QLineEdit* m_contrasena = nullptr;
    QComboBox* m_rol = nullptr;
    QPushButton* m_crear = nullptr;
};

} // namespace polimarket
