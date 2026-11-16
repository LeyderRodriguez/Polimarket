cmake_minimum_required(VERSION 3.16)
project(PoliMarketEscritorio VERSION 0.1.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_AUTOMOC ON)

find_package(Qt6 REQUIRED COMPONENTS Widgets Network)

add_executable(polimarket_escritorio
    src/main.cpp
    src/modelos/Modelos.h
    src/servicios/IAutenticacionService.h
    src/servicios/IRecursosHumanosService.h
    src/servicios/IBodegaService.h
    src/servicios/ServiceFactory.h
    src/servicios/ServiceFactory.cpp
    src/servicios/rest/ApiCliente.h
    src/servicios/rest/ApiCliente.cpp
    src/servicios/rest/RestServicios.h
    src/servicios/rest/RestServicios.cpp
    src/servicios/mock/MockServicios.h
    src/servicios/mock/MockServicios.cpp
    src/ui/comun/Estilos.h
    src/ui/comun/Tablas.h
    src/ui/LoginWindow.h
    src/ui/LoginWindow.cpp
    src/ui/MainWindow.h
    src/ui/MainWindow.cpp
    src/ui/rh/VendedoresPage.h
    src/ui/rh/VendedoresPage.cpp
    src/ui/rh/CrearCuentaPage.h
    src/ui/rh/CrearCuentaPage.cpp
    src/ui/rh/CuentasPage.h
    src/ui/rh/CuentasPage.cpp
    src/ui/rh/AutorizacionPage.h
    src/ui/rh/AutorizacionPage.cpp
    src/ui/bodega/InventarioPage.h
    src/ui/bodega/InventarioPage.cpp
    src/ui/bodega/BajoStockPage.h
    src/ui/bodega/BajoStockPage.cpp
    src/ui/bodega/IngresoProductosPage.h
    src/ui/bodega/IngresoProductosPage.cpp
)

target_include_directories(polimarket_escritorio PRIVATE src)
target_link_libraries(polimarket_escritorio PRIVATE Qt6::Widgets Qt6::Network)

if(WIN32)
    set_target_properties(polimarket_escritorio PROPERTIES WIN32_EXECUTABLE ON)
endif()

# Los textos del código están en UTF-8 (tildes y ñ)
if(MSVC)
    target_compile_options(polimarket_escritorio PRIVATE /utf-8)
endif()
