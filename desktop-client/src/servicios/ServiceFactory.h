#pragma once
// Único lugar donde se decide qué implementación de los servicios usa la app.
//   - Por defecto: REST contra la API de la FachadaPoliMarket.
//   - Con --demo (o POLIMARKET_MODO=demo): datos simulados en memoria.
// La URL del backend se cambia con --api=http://servidor:8080 o POLIMARKET_API_URL.
// Las pantallas no cambian en ningún caso, porque solo conocen las interfaces.

#include "servicios/IAutenticacionService.h"
#include "servicios/IBodegaService.h"
#include "servicios/IRecursosHumanosService.h"

#include <QStringList>
#include <memory>

namespace polimarket {

class ApiCliente;

struct Servicios {
    std::shared_ptr<IAutenticacionService> autenticacion;
    std::shared_ptr<IRecursosHumanosService> recursosHumanos;
    std::shared_ptr<IBodegaService> bodega;
    std::shared_ptr<ApiCliente> api; // nulo en modo demostración
    QString descripcionModo;         // se muestra en la barra de estado
};

Servicios crearServicios(const QStringList& argumentos);

} // namespace polimarket
