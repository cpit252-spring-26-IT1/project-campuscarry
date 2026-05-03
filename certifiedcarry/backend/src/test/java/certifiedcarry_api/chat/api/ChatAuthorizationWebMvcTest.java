package certifiedcarry_api.chat.api;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import certifiedcarry_api.chat.service.ChatService;
import certifiedcarry_api.config.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(controllers = { ChatThreadController.class, ChatMessageController.class })
@Import(SecurityConfig.class)
class ChatAuthorizationWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ChatService chatService;

    @Test
    void participantReadWithoutBackendUserIdIsForbidden() throws Exception {
        mockMvc
                .perform(get("/chat_threads").with(user("participant").roles("FIREBASE_AUTHENTICATED")))
                .andExpect(status().isForbidden());

        verify(chatService, never()).getChatThreadsForActor(anyLong(), anyBoolean());
    }

    @Test
    void participantReadWithNonNumericBackendUserIdIsBadRequest() throws Exception {
        mockMvc
                .perform(
                        get("/chat_messages")
                                .with(user("participant").roles("FIREBASE_AUTHENTICATED"))
                                .requestAttr("backendUserId", "not-a-number"))
                .andExpect(status().isBadRequest());

        verify(chatService, never()).getChatMessagesForActor(anyLong(), anyBoolean());
    }

    @Test
    void adminReadWithoutBackendUserIdIsForbidden() throws Exception {
        mockMvc
                .perform(get("/chat_messages").with(user("admin").roles("FIREBASE_AUTHENTICATED", "ADMIN")))
                .andExpect(status().isForbidden());

        verify(chatService, never()).getChatMessagesForActor(anyLong(), anyBoolean());
    }

    @Test
    void adminCanListAllThreadsAndMessages() throws Exception {
        when(chatService.getChatThreadsForActor(1L, true))
                .thenReturn(List.of(threadRow("2", "12", "13"), threadRow("5", "11", "14")));
        when(chatService.getChatMessagesForActor(1L, true))
                .thenReturn(
                        List.of(
                                messageRow("2", "2", "12", "13"),
                                messageRow("3", "2", "13", "12")));

        mockMvc
                .perform(
                        get("/chat_threads")
                                .with(user("admin").roles("FIREBASE_AUTHENTICATED", "ADMIN"))
                                .requestAttr("backendUserId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc
                .perform(
                        get("/chat_messages")
                                .with(user("admin").roles("FIREBASE_AUTHENTICATED", "ADMIN"))
                                .requestAttr("backendUserId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(chatService).getChatThreadsForActor(1L, true);
        verify(chatService).getChatMessagesForActor(1L, true);
    }

    @Test
    void participantOnlySeesOwnThreadsAndMessages() throws Exception {
        long participantUserId = 13L;

        when(chatService.getChatThreadsForActor(participantUserId, false))
                .thenReturn(List.of(threadRow("2", "12", "13")));
        when(chatService.getChatMessagesForActor(participantUserId, false))
                .thenReturn(
                        List.of(
                                messageRow("2", "2", "12", "13"),
                                messageRow("3", "2", "13", "12")));

        mockMvc
                .perform(
                        get("/chat_threads")
                                .with(user("participant").roles("FIREBASE_AUTHENTICATED"))
                                .requestAttr("backendUserId", String.valueOf(participantUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc
                .perform(
                        get("/chat_messages")
                                .with(user("participant").roles("FIREBASE_AUTHENTICATED"))
                                .requestAttr("backendUserId", String.valueOf(participantUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(chatService).getChatThreadsForActor(participantUserId, false);
        verify(chatService).getChatMessagesForActor(participantUserId, false);
    }

    @Test
    void nonParticipantCannotPatchThreadOrPostMessage() throws Exception {
        long actorUserId = 4L;
        long threadId = 2L;

        when(chatService.patchChatThreadForActor(eq(String.valueOf(threadId)), anyMap(), eq(actorUserId), eq(false)))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "You can only modify conversations that include your own user id."));
        when(chatService.createChatMessageForActor(anyMap(), eq(actorUserId), eq(false)))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "You can only send messages in conversations that include your own user id."));

        mockMvc
                .perform(
                        patch("/chat_threads/{threadId}", threadId)
                                .with(user("outsider").roles("FIREBASE_AUTHENTICATED"))
                                .requestAttr("backendUserId", String.valueOf(actorUserId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"lastSenderId\":\"4\"}"))
                .andExpect(status().isForbidden());

        Map<String, Object> unauthorizedPostPayload = Map.of(
                "threadId", String.valueOf(threadId),
                "senderId", String.valueOf(actorUserId),
                "recipientId", "13",
                "body", "not allowed");

        mockMvc
                .perform(
                        post("/chat_messages")
                                .with(user("outsider").roles("FIREBASE_AUTHENTICATED"))
                                .requestAttr("backendUserId", String.valueOf(actorUserId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(unauthorizedPostPayload)))
                .andExpect(status().isForbidden());

        verify(chatService)
                .patchChatThreadForActor(eq(String.valueOf(threadId)), anyMap(), eq(actorUserId), eq(false));
        verify(chatService)
                .createChatMessageForActor(anyMap(), eq(actorUserId), eq(false));
        verify(chatService, never()).patchChatThread(eq(String.valueOf(threadId)), anyMap());
        verify(chatService, never()).createChatMessage(anyMap());
    }

    @Test
    void nonRecipientCannotPatchMessageReadState() throws Exception {
        long actorUserId = 4L;
        String messageId = "2";

        when(chatService.patchChatMessageForActor(eq(messageId), anyMap(), eq(actorUserId), eq(false)))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "You can only mark messages addressed to your own user id."));

        mockMvc
                .perform(
                        patch("/chat_messages/{messageId}", messageId)
                                .with(user("outsider").roles("FIREBASE_AUTHENTICATED"))
                                .requestAttr("backendUserId", String.valueOf(actorUserId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"readAt\":\"2026-04-04T00:00:00Z\"}"))
                .andExpect(status().isForbidden());

        verify(chatService)
                .patchChatMessageForActor(eq(messageId), anyMap(), eq(actorUserId), eq(false));
        verify(chatService, never()).patchChatMessage(eq(messageId), anyMap());
    }

    private Map<String, Object> threadRow(String id, String participantUserId1, String participantUserId2) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("participantIds", List.of(participantUserId1, participantUserId2));
        row.put("initiatedById", participantUserId1);
        row.put("initiatedByRole", "PLAYER");
        row.put("lastSenderId", participantUserId1);
        row.put("lastMessageAt", "2026-04-03T20:00:00Z");
        row.put("lastMessagePreview", "preview");
        row.put("createdAt", "2026-04-03T20:00:00Z");
        row.put("updatedAt", "2026-04-03T20:00:00Z");
        return row;
    }

    private Map<String, Object> messageRow(
            String id, String threadId, String senderId, String recipientId) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("threadId", threadId);
        row.put("senderId", senderId);
        row.put("recipientId", recipientId);
        row.put("body", "hello");
        row.put("createdAt", "2026-04-03T20:00:00Z");
        row.put("readAt", null);
        return row;
    }
}
