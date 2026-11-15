-- Una venta tiene a lo sumo una reserva: evita reservar dos veces para la misma venta.
CREATE UNIQUE INDEX uq_reserva_venta ON bodega.reserva_inventario (venta_id);
