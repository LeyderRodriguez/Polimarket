package polimarket.fachada;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import polimarket.compartido.ExcepcionNegocio;

/** Traduce los errores a la respuesta común {codigo, mensaje} con el estado HTTP que corresponde. */
@RestControllerAdvice
class ManejadorErrores {

    private static final Logger LOG = LoggerFactory.getLogger(ManejadorErrores.class);

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

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ErrorApi> parametroFaltante(MissingServletRequestParameterException e) {
        return ResponseEntity.badRequest()
                .body(new ErrorApi("DATOS_INVALIDOS", "Falta el parámetro '" + e.getParameterName() + "'."));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErrorApi> tipoIncorrecto(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest()
                .body(new ErrorApi("DATOS_INVALIDOS", "El valor de '" + e.getName() + "' no es válido."));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErrorApi> metodoNoPermitido(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ErrorApi("METODO_NO_PERMITIDO", "Ese método no está disponible en esta ruta."));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorApi> rutaInexistente(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorApi("NO_ENCONTRADO", "La ruta no existe."));
    }

    /** Los permisos por rol se aplican con @PreAuthorize dentro del controller; aquí se les da el formato común. */
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorApi> sinPermiso(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorApi("SIN_PERMISO", "No tiene permiso para esta acción."));
    }

    /**
     * Último recurso. Los errores estándar de Spring MVC que no tienen manejador propio (por ejemplo 415) implementan
     * {@code ErrorResponse}: conservan su estado y salen en el formato común, no como 500. Todo lo demás es un fallo
     * inesperado: nunca se devuelve el detalle interno, queda en el registro del servidor.
     */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorApi> inesperado(Exception e) {
        if (e instanceof ErrorResponse respuesta && respuesta.getStatusCode().is4xxClientError()) {
            HttpStatusCode estado = respuesta.getStatusCode();
            return ResponseEntity.status(estado).body(switch (estado.value()) {
                case 406, 415 -> new ErrorApi("FORMATO_NO_SOPORTADO", "Use application/json en la solicitud y en la respuesta.");
                default -> new ErrorApi("SOLICITUD_INVALIDA", "La solicitud no es válida.");
            });
        }
        LOG.error("Error inesperado", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorApi("ERROR_INTERNO", "Ocurrió un error inesperado."));
    }

    /** Red de seguridad: una restricción única de la base que se violó por una carrera entre dos solicitudes. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorApi> integridad(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorApi("CONFLICTO", "La operación entra en conflicto con datos existentes."));
    }
}
