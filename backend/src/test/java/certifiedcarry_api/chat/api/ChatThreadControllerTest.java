package certifiedcarry_api.chat.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import certifiedcarry_api.chat.ChatFields;
import certifiedcarry_api.chat.service.ChatService;
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
class ChatThreadControllerTest {

  @Mock private ChatService chatService;

  @Test
  void chatThreadControllerDelegatesListConversationMessageCreateAndPatchFlows() {
    ChatThreadController controller = new ChatThreadController(chatService);
    MockHttpServletRequest playerRequest = actorRequest("9", "PLAYER");
    MockHttpServletRequest adminRequest = actorRequest("19", "ADMIN");
    Map<String, Object> threadRow =
        Map.of(
            ChatFields.ID, "4",
            ChatFields.PARTICIPANT_IDS, List.of("9", "12"),
            ChatFields.INITIATED_BY_ID, "9",
            ChatFields.INITIATED_BY_ROLE, "PLAYER");
    Map<String, Object> messageRow =
        Map.of(
            ChatFields.ID, "40",
            ChatFields.THREAD_ID, "4",
            ChatFields.SENDER_ID, "9",
            ChatFields.RECIPIENT_ID, "12",
            ChatFields.BODY, "hello");
    Map<String, Object> summaryRow =
        Map.of(
            ChatFields.ID, "4",
            ChatFields.PARTICIPANT_IDS, List.of("9", "12"),
            "unreadCount", 2);
    ChatThreadCreateRequest createRequest =
        new ChatThreadCreateRequest(List.of("9", "12"), null, "PLAYER", null, null, null, null, null);
    ChatThreadPatchRequest patchRequest = new ChatThreadPatchRequest();
    patchRequest.setLastMessagePreview("updated");

    when(chatService.getChatThreadsForActor(9L, false)).thenReturn(List.of(threadRow));
    when(chatService.getConversationSummariesForActor(19L, true)).thenReturn(List.of(summaryRow));
    when(chatService.getConversationMessagesForActor("4", 19L, true)).thenReturn(List.of(messageRow));
    when(chatService.createChatThreadForActor(createRequest.toServiceRequest(), 9L, false)).thenReturn(threadRow);
    when(chatService.patchChatThreadForActor("4", Map.of(ChatFields.LAST_MESSAGE_PREVIEW, "updated"), 9L, false))
        .thenReturn(threadRow);

    List<ChatThreadResponse> threads = controller.getChatThreads(playerAuth(), playerRequest);
    List<ConversationSummaryResponse> summaries =
        controller.getConversationSummaries(true, adminAuth(), adminRequest);
    List<ChatMessageResponse> messages =
        controller.getConversationMessages("4", true, adminAuth(), adminRequest);
    var created = controller.createChatThread(createRequest, playerAuth(), playerRequest);
    var updated = controller.patchChatThread("4", patchRequest, playerAuth(), playerRequest);

    assertEquals(1, threads.size());
    assertEquals(1, summaries.size());
    assertEquals(2, summaries.getFirst().unreadCount());
    assertEquals(1, messages.size());
    assertEquals(HttpStatus.CREATED, created.getStatusCode());
    assertEquals("4", created.getBody().id());
    assertEquals("4", updated.getBody().id());
    verify(chatService).getConversationSummariesForActor(19L, true);
    verify(chatService).getConversationMessagesForActor("4", 19L, true);
  }

  private MockHttpServletRequest actorRequest(String userId, String role) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute("backendUserId", userId);
    request.setAttribute("backendUserRole", role);
    return request;
  }

  private UsernamePasswordAuthenticationToken playerAuth() {
    return new UsernamePasswordAuthenticationToken(
        "player", "n/a", List.of(new SimpleGrantedAuthority("ROLE_PLAYER")));
  }

  private UsernamePasswordAuthenticationToken adminAuth() {
    return new UsernamePasswordAuthenticationToken(
        "admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
  }
}
