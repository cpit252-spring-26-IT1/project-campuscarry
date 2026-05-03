package certifiedcarry_api.chat.service;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ChatService {

  private final ChatThreadService chatThreadService;
  private final ChatMessageService chatMessageService;

  public ChatService(ChatThreadService chatThreadService, ChatMessageService chatMessageService) {
    this.chatThreadService = chatThreadService;
    this.chatMessageService = chatMessageService;
  }

  public List<Map<String, Object>> getChatThreadsForActor(long actorUserId, boolean isAdmin) {
    return chatThreadService.getChatThreadsForActor(actorUserId, isAdmin);
  }

  public List<Map<String, Object>> getChatMessagesForActor(long actorUserId, boolean isAdmin) {
    return chatMessageService.getChatMessagesForActor(actorUserId, isAdmin);
  }

  public Map<String, Object> createChatThreadForActor(
      Map<String, Object> request, long actorUserId, boolean isAdmin) {
    return chatThreadService.createChatThreadForActor(request, actorUserId, isAdmin);
  }

  public Map<String, Object> patchChatThreadForActor(
      String threadId, Map<String, Object> request, long actorUserId, boolean isAdmin) {
    return chatThreadService.patchChatThreadForActor(threadId, request, actorUserId, isAdmin);
  }

  public Map<String, Object> createChatMessageForActor(
      Map<String, Object> request, long actorUserId, boolean isAdmin) {
    return chatMessageService.createChatMessageForActor(request, actorUserId, isAdmin);
  }

  public Map<String, Object> patchChatMessageForActor(
      String messageId, Map<String, Object> request, long actorUserId, boolean isAdmin) {
    return chatMessageService.patchChatMessageForActor(messageId, request, actorUserId, isAdmin);
  }

  public List<Map<String, Object>> getChatThreads() {
    return chatThreadService.getChatThreads();
  }

  public List<Map<String, Object>> getChatThreadsForUser(long userId) {
    return chatThreadService.getChatThreadsForUser(userId);
  }

  public Map<String, Object> createChatThread(Map<String, Object> request) {
    return chatThreadService.createChatThread(request);
  }

  public Map<String, Object> patchChatThread(String threadId, Map<String, Object> request) {
    return chatThreadService.patchChatThread(threadId, request);
  }

  public List<Map<String, Object>> getChatMessages() {
    return chatMessageService.getChatMessages();
  }

  public List<Map<String, Object>> getChatMessagesForUser(long userId) {
    return chatMessageService.getChatMessagesForUser(userId);
  }

  public boolean isThreadParticipant(String threadId, long userId) {
    return chatThreadService.isThreadParticipant(threadId, userId);
  }

  public boolean isThreadParticipant(long threadId, long userId) {
    return chatThreadService.isThreadParticipant(threadId, userId);
  }

  public boolean isMessageRecipient(String messageId, long userId) {
    return chatMessageService.isMessageRecipient(messageId, userId);
  }

  public Map<String, Object> createChatMessage(Map<String, Object> request) {
    return chatMessageService.createChatMessage(request);
  }

  public Map<String, Object> patchChatMessage(String messageId, Map<String, Object> request) {
    return chatMessageService.patchChatMessage(messageId, request);
  }
}
