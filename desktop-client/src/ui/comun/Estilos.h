#pragma once
// Hoja de estilos (QSS) común a toda la aplicación.

#include <QString>

namespace polimarket::estilos {

inline const QString kColorPrimario = "#1F3864";
inline const QString kColorExito = "#2E7D32";
inline const QString kColorAlerta = "#C62828";
inline const QString kColorPendiente = "#B26A00";

inline QString hojaGlobal() {
    return QStringLiteral(R"(
QWidget { font-family: "Segoe UI", "Arial"; font-size: 10pt; color: #1E1E1E; }
QMainWindow, QWidget#fondo { background: #F4F6FA; }

QFrame#barraLateral { background: #1F3864; }
QLabel#marca { color: white; font-size: 15pt; font-weight: 600; padding: 18px 16px 4px 16px; }
QLabel#rolLateral { color: #C9D4EA; padding: 0 16px 16px 16px; }
QListWidget#menu { background: transparent; border: none; color: white; outline: 0; }
QListWidget#menu::item { background: transparent; padding: 10px 16px; border: none; }
QListWidget#menu::item:selected { background: #2E5597; color: white; }
QListWidget#menu::item:hover:!selected { background: #29497F; }

QFrame#encabezado { background: white; border-bottom: 1px solid #DDE2EC; }
QLabel#tituloPagina { font-size: 15pt; font-weight: 600; color: #1F3864; }
QLabel#subtitulo { color: #5A6478; }

QFrame#tarjeta { background: white; border: 1px solid #DDE2EC; border-radius: 8px; }

QPushButton { background: white; border: 1px solid #B8C2D6; border-radius: 6px; padding: 6px 14px; }
QPushButton:hover { background: #EEF2F9; }
QPushButton:disabled { color: #A0A7B4; border-color: #DDE2EC; }
QPushButton#primario { background: #1F3864; color: white; border: none; }
QPushButton#primario:hover { background: #2E5597; }
QPushButton#exito { background: #2E7D32; color: white; border: none; }
QPushButton#exito:disabled, QPushButton#peligro:disabled { background: #C9CED8; color: white; }
QPushButton#peligro { background: #C62828; color: white; border: none; }

QLineEdit, QComboBox, QDateEdit, QSpinBox, QDoubleSpinBox, QTextEdit {
    background: white; border: 1px solid #B8C2D6; border-radius: 6px; padding: 5px 8px; }
QLineEdit:focus, QComboBox:focus, QDateEdit:focus { border: 1px solid #2E5597; }

QTableWidget { background: white; border: 1px solid #DDE2EC; gridline-color: #EEF1F6;
               selection-background-color: #DCE6F2; selection-color: #1E1E1E; }
QHeaderView::section { background: #DCE6F2; border: none; border-right: 1px solid #C9D4EA;
                       padding: 6px; font-weight: 600; }
QStatusBar { background: white; color: #5A6478; }
)");
}

} // namespace polimarket::estilos
