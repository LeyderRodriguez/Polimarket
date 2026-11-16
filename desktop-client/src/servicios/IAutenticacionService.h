#pragma once
// Sesión (componente Seguridad) a través de la fachada.
//   POST /api/auth/login   { "correo", "contrasena" } -> { "id", "nombre", "rol" }
//   POST /api/auth/logout

#include "modelos/Modelos.h"

namespace polimarket {

class IAutenticacionService {
public:
    virtual ~IAutenticacionService() = default;

    // Solo acepta los roles de este cliente: RECURSOS_HUMANOS y BODEGA.
    virtual Respuesta<UsuarioSesion> iniciarSesion(const QString& correo, const QString& contrasena) = 0;
    virtual void cerrarSesion() = 0;
};

} // namespace polimarket
