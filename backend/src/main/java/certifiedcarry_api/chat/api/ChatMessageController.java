package certifiedcarry_api.chat.api;

import certifiedcarry_api.chat.service.ChatService;
import certifiedcarry_api.shared.ActorRequestContext;
import certifiedcarry_api.shared.ActorRequestResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat_messages")
public class ChatMessageController {

  private final ChatService chatService;

  public ChatMessageController(ChatService chatService) {
    this.chatService = chatService;
  }

  @GetMapping
  public List<ChatMessageResponse> getChatMessages(
      Authentication authentication, HttpServletRequest request) {
    ActorRequestContext actor = ActorRequestResolver.requireActor(authentication, request);
    return ChatMessageResponse.fromServiceRows(
        chatService.getChatMessagesForActor(actor.userId(), actor.isAdmin()));
  }

  @PostMapping
  public ResponseEntity<ChatMessageResponse> createChatMessage(
      @RequestBody ChatMessageCreateRequest request,
      Authentication authentication,
      HttpServletRequest servletRequest) {
    ActorRequestContext actor =
        ActorRequestResolver.requireActor(authentication, servletRequest);
    ChatMessageResponse created =
        ChatMessageResponse.fromServiceRow(
            chatService.createChatMessageForActor(
                request.toServiceRequest(), actor.userId(), actor.isAdmin()));
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @PatchMapping("/{messageId}")
  public ResponseEntity<ChatMessageResponse> patchChatMessage(
      @PathVariable String messageId,
      @RequestBody ChatMessagePatchRequest request,
      Authentication authentication,
      HttpServletRequest servletRequest) {
    ActorRequestContext actor =
        ActorRequestResolver.requireActor(authentication, servletRequest);
    ChatMessageResponse updated =
        ChatMessageResponse.fromServiceRow(
            chatService.patchChatMessageForActor(
                messageId, request.toServiceRequest(), actor.userId(), actor.isAdmin()));
    return ResponseEntity.ok(updated);
  }
}
