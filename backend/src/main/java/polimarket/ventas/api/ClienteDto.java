package polimarket.ventas.api;

import java.util.List;

public record ClienteDto(Long id, Long terceroId, String nombre, List<String> preferencias) {
}
