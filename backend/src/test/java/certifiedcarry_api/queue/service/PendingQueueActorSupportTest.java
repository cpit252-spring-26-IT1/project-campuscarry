package certifiedcarry_api.queue.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import certifiedcarry_api.queue.PendingQueueFields;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class PendingQueueActorSupportTest {

  @Test
  void enforcePayloadUserOwnershipInjectsMissingUserAndRejectsForeignUser() {
    Map<String, Object> request = new LinkedHashMap<>();

    PendingQueueActorSupport.enforcePayloadUserOwnership(request, 14L);
    assertEquals("14", request.get(PendingQueueFields.USER_ID));

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () ->
                PendingQueueActorSupport.enforcePayloadUserOwnership(
                    new LinkedHashMap<>(Map.of(PendingQueueFields.USER_ID, "15")), 14L));
    assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    assertEquals("You can only submit updates for your own userId.", exception.getReason());
  }

  @Test
  void parseLongValueSupportsNumbersAndRejectsNonNumericStrings() {
    assertEquals(17L, PendingQueueActorSupport.parseLongValue(17, PendingQueueFields.USER_ID));
    assertEquals(18L, PendingQueueActorSupport.parseLongValue(" 18 ", PendingQueueFields.USER_ID));

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> PendingQueueActorSupport.parseLongValue("oops", PendingQueueFields.USER_ID));
    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    assertEquals("userId must be a numeric string.", exception.getReason());
  }
}
