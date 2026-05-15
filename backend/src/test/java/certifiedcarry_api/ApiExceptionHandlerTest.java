package certifiedcarry_api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import certifiedcarry_api.shared.ApiExceptionHandler;
import certifiedcarry_api.shared.ApiExceptionHandler.ApiErrorDetail;
import certifiedcarry_api.shared.ApiExceptionHandler.ApiErrorResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.core.MethodParameter;
import org.springframework.web.server.ResponseStatusException;
import java.lang.reflect.Method;

class ApiExceptionHandlerTest {

  private final ApiExceptionHandler exceptionHandler = new ApiExceptionHandler();

  @Test
  void responseStatusExceptionsUseConsistentErrorPayload() {
    MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/player_profiles/9");

    ApiErrorResponse body =
        exceptionHandler
            .handleResponseStatusException(
                new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "You can only update your own player profile."),
                request)
            .getBody();

    assertNotNull(body);
    assertEquals(403, body.status());
    assertEquals("Forbidden", body.error());
    assertEquals("You can only update your own player profile.", body.message());
    assertEquals("/player_profiles/9", body.path());
  }

  @Test
  void unexpectedExceptionsDoNotLeakInternalMessages() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/users");

    ApiErrorResponse body =
        exceptionHandler.handleUnexpectedException(new RuntimeException("db exploded"), request).getBody();

    assertNotNull(body);
    assertEquals(500, body.status());
    assertEquals("Internal Server Error", body.error());
    assertEquals("Internal server error.", body.message());
    assertEquals("/users", body.path());
  }

  @Test
  void validationExceptionsReturnFieldLevelDetails() throws Exception {
    BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
    bindingResult.addError(new FieldError("request", "email", "Email is required."));
    Method method = SampleController.class.getDeclaredMethod("submit", SamplePayload.class);
    MethodArgumentNotValidException exception =
        new MethodArgumentNotValidException(new MethodParameter(method, 0), bindingResult);

    ApiErrorResponse body =
        exceptionHandler
            .handleMethodArgumentNotValid(exception, new MockHttpServletRequest("POST", "/users"))
            .getBody();

    assertNotNull(body);
    assertEquals(400, body.status());
    assertEquals("Request validation failed.", body.message());
    assertEquals(List.of(new ApiErrorDetail("email", "Email is required.")), body.details());
  }

  @Test
  void malformedJsonAndIntegrityViolationsProduceSafeMessages() {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/leaderboard");

    ApiErrorResponse parseBody =
        exceptionHandler
            .handleHttpMessageNotReadable(new HttpMessageNotReadableException("bad json"), request)
            .getBody();
    ApiErrorResponse integrityBody =
        exceptionHandler
            .handleDataIntegrityViolation(
                new DataIntegrityViolationException("duplicate key"),
                new MockHttpServletRequest("POST", "/users"))
            .getBody();

    assertNotNull(parseBody);
    assertEquals("Malformed request body.", parseBody.message());
    assertEquals("/leaderboard", parseBody.path());
    assertNotNull(integrityBody);
    assertEquals(400, integrityBody.status());
    assertEquals("duplicate key", integrityBody.message());
  }

  @Test
  void responseStatusExceptionsFallBackToReasonPhraseWhenReasonIsBlank() {
    ApiErrorResponse body =
        exceptionHandler
            .handleResponseStatusException(
                new ResponseStatusException(HttpStatus.BAD_REQUEST, null), null)
            .getBody();

    assertNotNull(body);
    assertEquals("Bad Request", body.message());
    assertNull(body.path());
    assertTrue(body.details() == null || body.details().isEmpty());
  }

  private static final class SampleController {
    @SuppressWarnings("unused")
    void submit(SamplePayload payload) {}
  }

  private record SamplePayload(String email) {}
}
