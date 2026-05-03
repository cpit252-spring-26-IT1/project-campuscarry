package certifiedcarry_api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import certifiedcarry_api.shared.ApiExceptionHandler;
import certifiedcarry_api.shared.ApiExceptionHandler.ApiErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

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
}
