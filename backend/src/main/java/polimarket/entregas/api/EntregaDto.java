package polimarket.entregas.api;

import polimarket.bodega.api.EstadoReserva;

public record EntregaDto(Long entregaId, Long ventaId, EstadoEntrega estado, EstadoReserva reservaEstado) {
}
