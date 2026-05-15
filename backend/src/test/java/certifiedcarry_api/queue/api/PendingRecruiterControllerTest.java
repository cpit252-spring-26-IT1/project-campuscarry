package certifiedcarry_api.queue.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import certifiedcarry_api.queue.PendingQueueFields;
import certifiedcarry_api.queue.service.PendingQueueService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class PendingRecruiterControllerTest {

  @Mock private PendingQueueService pendingQueueService;

  @Test
  void pendingRecruiterControllerSupportsAdminReadsAndRecruiterCreateDeleteFlows() {
    PendingRecruiterController controller = new PendingRecruiterController(pendingQueueService);
    MockHttpServletRequest recruiterRequest = recruiterRequest();
    Map<String, Object> row =
        Map.of(
            PendingQueueFields.ID, "5",
            PendingQueueFields.USER_ID, "9",
            PendingQueueFields.FULL_NAME, "Recruiter Name",
            PendingQueueFields.EMAIL, "recruiter@example.com",
            PendingQueueFields.LINKEDIN_URL, "https://linkedin.com/in/recruiter",
            PendingQueueFields.ORGANIZATION_NAME, "Org");
    PendingRecruiterCreateRequest createRequest =
        new PendingRecruiterCreateRequest(
            "9",
            "Recruiter Name",
            "recruiter@example.com",
            "https://linkedin.com/in/recruiter",
            "Org",
            OffsetDateTime.parse("2026-05-13T00:00:00Z"),
            OffsetDateTime.parse("2026-05-13T00:00:00Z"),
            "en",
            "v1",
            "v1");

    when(pendingQueueService.getPendingRecruitersForActor("9", 0L, "", true)).thenReturn(List.of(row));
    when(pendingQueueService.createPendingRecruiterForActor(createRequest.toServiceRequest(), 9L, "RECRUITER", false))
        .thenReturn(row);

    List<PendingRecruiterResponse> pending =
        controller.getPendingRecruiters("9", adminAuth(), new MockHttpServletRequest());
    var created = controller.createPendingRecruiter(createRequest, recruiterAuth(), recruiterRequest);
    var deleted = controller.deletePendingRecruiter("5", adminAuth(), new MockHttpServletRequest());

    assertEquals(1, pending.size());
    assertEquals("5", pending.getFirst().id());
    assertEquals(HttpStatus.CREATED, created.getStatusCode());
    assertEquals("5", created.getBody().id());
    assertEquals(HttpStatus.NO_CONTENT, deleted.getStatusCode());
    verify(pendingQueueService).deletePendingRecruiterForActor("5", 0L, "", true);
  }

  private MockHttpServletRequest recruiterRequest() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute("backendUserId", "9");
    request.setAttribute("backendUserRole", "RECRUITER");
    return request;
  }

  private UsernamePasswordAuthenticationToken recruiterAuth() {
    return new UsernamePasswordAuthenticationToken(
        "recruiter", "n/a", List.of(new SimpleGrantedAuthority("ROLE_RECRUITER")));
  }

  private UsernamePasswordAuthenticationToken adminAuth() {
    return new UsernamePasswordAuthenticationToken(
        "admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
  }
}
