package polimarket.fachada;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import polimarket.compartido.ExcepcionNegocio;

/** Traduce los errores a la respuesta común {codigo, mensaje} con el estado HTTP que corresponde. */
@RestControllerAdvice
class ManejadorErrores {

    record ErrorApi(String codigo, String mensaje) {
    }

    @ExceptionHandler(ExcepcionNegocio.class)
    ResponseEntity<ErrorApi> negocio(ExcepcionNegocio e) {
        HttpStatus estado = switch (e.getCategoria()) {
            case DATOS_INVALIDOS -> HttpStatus.BAD_REQUEST;
            case NO_AUTENTICADO -> HttpStatus.UNAUTHORIZED;
            case SIN_PERMISO -> HttpStatus.FORBIDDEN;
            case NO_ENCONTRADO -> HttpStatus.NOT_FOUND;
            case CONFLICTO -> HttpStatus.CONFLICT;
        };
        return ResponseEntity.status(estado).body(new ErrorApi(e.getCodigo(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorApi> validacion(MethodArgumentNotValidException e) {
        String detalle = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .orElse("Datos inválidos.");
        return ResponseEntity.badRequest().body(new ErrorApi("DATOS_INVALIDOS", detalle));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorApi> cuerpoIlegible(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(new ErrorApi("DATOS_INVALIDOS", "El cuerpo de la solicitud no es válido."));
    }

    /** Red de seguridad: una restricción única de la base que se violó por una carrera entre dos solicitudes. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorApi> integridad(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorApi("CONFLICTO", "La operación entra en conflicto con datos existentes."));
    }
}
