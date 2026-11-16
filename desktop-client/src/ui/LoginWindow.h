#pragma once

#include "servicios/IAutenticacionService.h"

#include <QWidget>
#include <memory>

class QLineEdit;
class QLabel;

namespace polimarket {

// Pantalla de inicio de sesión. Consume IAutenticacion de la fachada.
class LoginWindow : public QWidget {
    Q_OBJECT
public:
    explicit LoginWindow(std::shared_ptr<IAutenticacionService> autenticacion,
                         QWidget* parent = nullptr);
    void limpiar();
    void mostrarAviso(const QString& mensaje);

signals:
    void sesionIniciada(const polimarket::UsuarioSesion& sesion);

private:
    void ingresar();

    std::shared_ptr<IAutenticacionService> m_autenticacion;
    QLineEdit* m_correo = nullptr;
    QLineEdit* m_clave = nullptr;
    QLabel* m_error = nullptr;
};

} // namespace polimarket
