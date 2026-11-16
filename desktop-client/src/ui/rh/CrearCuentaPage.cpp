#include "ui/rh/CrearCuentaPage.h"

#include "ui/comun/Estilos.h"
#include "ui/comun/Tablas.h"

#include <QComboBox>
#include <QFormLayout>
#include <QFrame>
#include <QHBoxLayout>
#include <QLabel>
#include <QLineEdit>
#include <QPushButton>
#include <QVBoxLayout>

namespace polimarket {

CrearCuentaPage::CrearCuentaPage(std::shared_ptr<IRecursosHumanosService> servicio, QWidget* parent)
    : QWidget(parent), m_servicio(std::move(servicio)) {
    auto* l = new QVBoxLayout(this);
    l->setContentsMargins(24, 20, 24, 20);
    l->setSpacing(12);
    l->addWidget(ui::encabezadoPagina(
        "Crear cuenta",
        "Primero se busca a la persona por documento para no duplicarla. Si es vendedor, la cuenta queda con aval "
        "PENDIENTE y se aprueba en «Avales de vendedores»."));

    // Paso 1: buscar el tercero
    auto* paso1 = ui::tarjeta();
    auto* p1 = new QVBoxLayout(paso1);
    p1->setContentsMargins(20, 16, 20, 16);
    p1->addWidget(new QLabel("<b>1. Buscar a la persona</b>"));
    auto* fila = new QHBoxLayout;
    m_tipoDocumento = new QComboBox;
    m_tipoDocumento->addItem("CC · Cédula de ciudadanía", "CC");
    m_tipoDocumento->addItem("CE · Cédula de extranjería", "CE");
    m_tipoDocumento->addItem("PA · Pasaporte", "PA");
    m_documento = new QLineEdit;
    m_documento->setPlaceholderText("Número de documento");
    m_documento->setMaxLength(30);
    auto* buscarBtn = new QPushButton("Buscar");
    buscarBtn->setObjectName("primario");
    fila->addWidget(m_tipoDocumento);
    fila->addWidget(m_documento, 1);
    fila->addWidget(buscarBtn);
    p1->addLayout(fila);
    m_estadoBusqueda = new QLabel;
    m_estadoBusqueda->setWordWrap(true);
    p1->addWidget(m_estadoBusqueda);
    l->addWidget(paso1);

    // Paso 2: datos personales (solo si es nuevo)
    m_datos = ui::tarjeta();
    auto* p2 = new QFormLayout(m_datos);
    p2->setContentsMargins(20, 16, 20, 16);
    p2->addRow(new QLabel("<b>2. Datos de la persona</b>"));
    m_nombres = new QLineEdit;
    m_apellidos = new QLineEdit;
    m_telefono = new QLineEdit;
    m_correoContacto = new QLineEdit;
    m_direccion = new QLineEdit;
    p2->addRow("Nombres *", m_nombres);
    p2->addRow("Apellidos *", m_apellidos);
    p2->addRow("Teléfono", m_telefono);
    p2->addRow("Correo de contacto", m_correoContacto);
    p2->addRow("Dirección", m_direccion);
    l->addWidget(m_datos);

    // Paso 3: la cuenta
    m_cuenta = ui::tarjeta();
    auto* p3 = new QFormLayout(m_cuenta);
    p3->setContentsMargins(20, 16, 20, 16);
    p3->addRow(new QLabel("<b>3. Cuenta de acceso</b>"));
    m_correo = new QLineEdit;
    m_correo->setPlaceholderText("usuario@polimarket.test");
    m_contrasena = new QLineEdit;
    m_contrasena->setEchoMode(QLineEdit::Password);
    m_contrasena->setPlaceholderText("Mínimo 8 caracteres");
    m_rol = new QComboBox;
    m_rol->addItem("Vendedor", "VENDEDOR");
    m_rol->addItem("Bodega", "BODEGA");
    m_rol->addItem("Entregas", "ENTREGAS");
    p3->addRow("Correo de acceso *", m_correo);
    p3->addRow("Contraseña inicial *", m_contrasena);
    p3->addRow("Rol *", m_rol);
    l->addWidget(m_cuenta);

    auto* acciones = new QHBoxLayout;
    auto* limpiarBtn = new QPushButton("Limpiar");
    m_crear = new QPushButton("Crear cuenta");
    m_crear->setObjectName("primario");
    acciones->addWidget(new QLabel("* Campos obligatorios"));
    acciones->addStretch();
    acciones->addWidget(limpiarBtn);
    acciones->addWidget(m_crear);
    l->addLayout(acciones);
    l->addStretch();

    connect(buscarBtn, &QPushButton::clicked, this, &CrearCuentaPage::buscar);
    connect(m_documento, &QLineEdit::returnPressed, this, &CrearCuentaPage::buscar);
    connect(m_documento, &QLineEdit::textEdited, this, [this] { limpiar(); });
    connect(limpiarBtn, &QPushButton::clicked, this, [this] {
        m_documento->clear();
        limpiar();
    });
    connect(m_crear, &QPushButton::clicked, this, &CrearCuentaPage::crear);

    limpiar();
}

void CrearCuentaPage::habilitarDatosPersonales(bool nuevo) {
    for (QLineEdit* c : {m_nombres, m_apellidos, m_telefono, m_correoContacto, m_direccion}) c->setEnabled(nuevo);
}

void CrearCuentaPage::limpiar() {
    m_tercero.reset();
    m_buscado = false;
    m_estadoBusqueda->setText("Ingrese el documento y pulse Buscar.");
    m_estadoBusqueda->setStyleSheet("color: #5A6478;");
    for (QLineEdit* c : {m_nombres, m_apellidos, m_telefono, m_correoContacto, m_direccion, m_correo, m_contrasena})
        c->clear();
    habilitarDatosPersonales(false);
    m_cuenta->setEnabled(false);
    m_crear->setEnabled(false);
}

void CrearCuentaPage::buscar() {
    const QString numero = m_documento->text().trimmed();
    if (numero.isEmpty()) return;
    limpiar();
    const auto r = m_servicio->buscarTercero(m_tipoDocumento->currentData().toString(), numero);

    if (r.ok) {
        m_tercero = r.valor;
        m_nombres->setText(r.valor.tipo == "JURIDICA" ? r.valor.razonSocial : r.valor.nombres);
        m_apellidos->setText(r.valor.apellidos);
        m_telefono->setText(r.valor.telefono);
        m_correoContacto->setText(r.valor.correo);
        m_direccion->setText(r.valor.direccion);
        m_estadoBusqueda->setText(QString("Encontrado: <b>%1</b>. Se usará este registro.").arg(r.valor.nombreCompleto()));
        m_estadoBusqueda->setStyleSheet(QString("color: %1;").arg(estilos::kColorExito));
    } else if (r.codigo == "NO_ENCONTRADO") {
        habilitarDatosPersonales(true);
        m_estadoBusqueda->setText("No está registrado. Complete sus datos para crearlo.");
        m_estadoBusqueda->setStyleSheet(QString("color: %1;").arg(estilos::kColorPendiente));
        m_nombres->setFocus();
    } else {
        m_estadoBusqueda->setText(r.mensaje);
        m_estadoBusqueda->setStyleSheet(QString("color: %1;").arg(estilos::kColorAlerta));
        return;
    }
    m_buscado = true;
    m_cuenta->setEnabled(true);
    m_crear->setEnabled(true);
}

void CrearCuentaPage::crear() {
    if (!m_buscado) return;
    QStringList faltantes;
    if (!m_tercero && m_nombres->text().trimmed().isEmpty()) faltantes << "nombres";
    if (!m_tercero && m_apellidos->text().trimmed().isEmpty()) faltantes << "apellidos";
    if (!m_correo->text().contains('@')) faltantes << "correo de acceso válido";
    if (m_contrasena->text().size() < 8) faltantes << "contraseña de al menos 8 caracteres";
    if (!faltantes.isEmpty()) {
        ui::mostrarResultado(this, false, "Complete: " + faltantes.join(", ") + ".");
        return;
    }

    // Registrar el tercero si no existía
    if (!m_tercero) {
        Tercero t;
        t.tipo = "NATURAL";
        t.tipoDocumento = m_tipoDocumento->currentData().toString();
        t.numeroDocumento = m_documento->text().trimmed();
        t.nombres = m_nombres->text().trimmed();
        t.apellidos = m_apellidos->text().trimmed();
        t.telefono = m_telefono->text().trimmed();
        t.correo = m_correoContacto->text().trimmed();
        t.direccion = m_direccion->text().trimmed();
        const auto rt = m_servicio->registrarTercero(t);
        if (!rt.ok) {
            ui::mostrarResultado(this, false, rt.mensaje);
            return;
        }
        m_tercero = rt.valor;
        habilitarDatosPersonales(false);
    }

    NuevaCuenta c;
    c.terceroId = m_tercero->id;
    c.correo = m_correo->text().trimmed();
    c.contrasenaInicial = m_contrasena->text();
    c.rol = m_rol->currentData().toString();
    const auto rc = m_servicio->crearCuenta(c);
    if (!rc.ok) {
        ui::mostrarResultado(this, false, rc.mensaje);
        return;
    }

    QString mensaje = QString("Cuenta creada para %1 (%2).").arg(m_tercero->nombreCompleto(), rc.valor.correo);
    if (rc.valor.rol == "VENDEDOR")
        mensaje += "\nEl vendedor quedó con aval PENDIENTE: apruébelo en «Avales de vendedores».";
    ui::mostrarResultado(this, true, mensaje);
    emit cuentaCreada();
    m_documento->clear();
    limpiar();
}

} // namespace polimarket
