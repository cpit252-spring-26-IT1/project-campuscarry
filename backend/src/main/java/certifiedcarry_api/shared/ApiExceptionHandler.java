package certifiedcarry_api.shared;

import certifiedcarry_api.config.ApiRouteCatalog;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);
  private static final String DEFAULT_INTERNAL_ERROR_MESSAGE = "Internal server error.";
  private static final String DEFAULT_BAD_REQUEST_MESSAGE = "Request validation failed.";

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ApiErrorResponse> handleResponseStatusException(
      ResponseStatusException exception, HttpServletRequest request) {
    HttpStatusCode statusCode = exception.getStatusCode();
    String message = fallbackMessage(exception.getReason(), statusCode);
    return buildResponse(statusCode, message, request, null);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    List<ApiErrorDetail> details =
        exception.getBindingResult().getFieldErrors().stream()
            .map(this::toErrorDetail)
            .toList();

    return buildResponse(HttpStatus.BAD_REQUEST, DEFAULT_BAD_REQUEST_MESSAGE, request, details);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(
      HttpMessageNotReadableException exception, HttpServletRequest request) {
    return buildResponse(HttpStatus.BAD_REQUEST, "Malformed request body.", request, null);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(
      IllegalArgumentException exception, HttpServletRequest request) {
    return buildResponse(
        HttpStatus.BAD_REQUEST,
        fallbackMessage(exception.getMessage(), HttpStatus.BAD_REQUEST),
        request,
        null);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(
      DataIntegrityViolationException exception, HttpServletRequest request) {
    String message =
        SqlErrorMapper.extractSqlErrorMessage(exception.getMostSpecificCause(), DEFAULT_BAD_REQUEST_MESSAGE);
    return buildResponse(HttpStatus.BAD_REQUEST, message, request, null);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
      Exception exception, HttpServletRequest request) {
    LOGGER.error(
        "Unhandled API exception for {} {}",
        request != null ? request.getMethod() : "UNKNOWN",
        resolvePath(request),
        exception);
    return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, DEFAULT_INTERNAL_ERROR_MESSAGE, request, null);
  }

  private ResponseEntity<ApiErrorResponse> buildResponse(
      HttpStatusCode statusCode,
      String message,
      HttpServletRequest request,
      List<ApiErrorDetail> details) {
    ApiErrorResponse body =
        new ApiErrorResponse(
            OffsetDateTime.now(),
            statusCode.value(),
            resolveError(statusCode),
            message,
            resolvePath(request),
            details == null || details.isEmpty() ? null : details);
    return ResponseEntity.status(statusCode).body(body);
  }

  private ApiErrorDetail toErrorDetail(FieldError fieldError) {
    return new ApiErrorDetail(
        fieldError.getField(),
        fallbackMessage(fieldError.getDefaultMessage(), HttpStatus.BAD_REQUEST));
  }

  private String fallbackMessage(String message, HttpStatusCode statusCode) {
    if (message == null || message.isBlank()) {
      return resolveError(statusCode);
    }

    return message;
  }

  private String resolveError(HttpStatusCode statusCode) {
    HttpStatus httpStatus = HttpStatus.resolve(statusCode.value());
    if (httpStatus == null) {
      return "Error";
    }

    return httpStatus.getReasonPhrase();
  }

  private String resolvePath(HttpServletRequest request) {
    if (request == null) {
      return null;
    }

    return ApiRouteCatalog.resolveRequestPath(request);
  }

  public record ApiErrorResponse(
      OffsetDateTime timestamp,
      int status,
      String error,
      String message,
      String path,
      List<ApiErrorDetail> details) {}

  public record ApiErrorDetail(String field, String message) {}
}
