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
@RequestMapping("/chat_threads")
public class ChatThreadController {

  private final ChatService chatService;

  public ChatThreadController(ChatService chatService) {
    this.chatService = chatService;
  }

  @GetMapping
  public List<ChatThreadResponse> getChatThreads(
      Authentication authentication, HttpServletRequest request) {
    ActorRequestContext actor = ActorRequestResolver.requireActor(authentication, request);
    return ChatThreadResponse.fromServiceRows(
        chatService.getChatThreadsForActor(actor.userId(), actor.isAdmin()));
  }

  @PostMapping
  public ResponseEntity<ChatThreadResponse> createChatThread(
      @RequestBody ChatThreadCreateRequest request,
      Authentication authentication,
      HttpServletRequest servletRequest) {
    ActorRequestContext actor =
        ActorRequestResolver.requireActor(authentication, servletRequest);
    ChatThreadResponse created =
        ChatThreadResponse.fromServiceRow(
            chatService.createChatThreadForActor(
                request.toServiceRequest(), actor.userId(), actor.isAdmin()));
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @PatchMapping("/{threadId}")
  public ResponseEntity<ChatThreadResponse> patchChatThread(
      @PathVariable String threadId,
      @RequestBody ChatThreadPatchRequest request,
      Authentication authentication,
      HttpServletRequest servletRequest) {
    ActorRequestContext actor =
        ActorRequestResolver.requireActor(authentication, servletRequest);
    ChatThreadResponse updated =
        ChatThreadResponse.fromServiceRow(
            chatService.patchChatThreadForActor(
                threadId, request.toServiceRequest(), actor.userId(), actor.isAdmin()));
    return ResponseEntity.ok(updated);
  }
}
