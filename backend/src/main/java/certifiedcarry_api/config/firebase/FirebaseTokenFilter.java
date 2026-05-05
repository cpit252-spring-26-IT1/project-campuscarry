package certifiedcarry_api.config.firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import certifiedcarry_api.user.model.UserEntity;
import certifiedcarry_api.user.model.UserRole;
import certifiedcarry_api.user.repo.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@ConditionalOnBean(com.google.firebase.FirebaseApp.class)
public class FirebaseTokenFilter extends OncePerRequestFilter {

  private final UserRepository userRepository;

  public FirebaseTokenFilter(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

    if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    String idToken = authorizationHeader.substring(7).trim();

    if (idToken.isBlank()) {
      respondUnauthorized(response, "Missing Firebase ID token.");
      return;
    }

    try {
      FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
      String firebaseUid = decodedToken.getUid();
      Object emailClaim = decodedToken.getClaims().get("email");
      boolean emailVerified = readBooleanClaim(decodedToken.getClaims().get("email_verified"));
      Optional<UserEntity> backendUser = resolveBackendUser(firebaseUid);

      List<SimpleGrantedAuthority> authorities = new ArrayList<>();
      authorities.add(new SimpleGrantedAuthority("ROLE_FIREBASE_AUTHENTICATED"));

      backendUser
          .map(UserEntity::getRole)
          .ifPresent(
              role -> {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
                if (role == UserRole.ADMIN) {
                  authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                }
              });

      var authentication =
          new UsernamePasswordAuthenticationToken(
              firebaseUid,
              null,
              authorities);

      authentication.setDetails(decodedToken);
      SecurityContextHolder.getContext().setAuthentication(authentication);

      request.setAttribute("firebaseUid", firebaseUid);
      if (emailClaim != null) {
        request.setAttribute("firebaseEmail", String.valueOf(emailClaim));
      }
      request.setAttribute("firebaseEmailVerified", emailVerified);
      backendUser.ifPresent(
          user -> {
            request.setAttribute("backendUserId", String.valueOf(user.getId()));
            request.setAttribute("backendUserRole", user.getRole().name());
          });

      filterChain.doFilter(request, response);
    } catch (FirebaseAuthException exception) {
      respondUnauthorized(response, "Invalid Firebase ID token.");
    }
  }

  private void respondUnauthorized(HttpServletResponse response, String message) throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.getWriter().write("{\"message\":\"" + message + "\"}");
  }

  private Optional<UserEntity> resolveBackendUser(String firebaseUid) {
    String normalizedFirebaseUid = normalizeFirebaseUid(firebaseUid);

    if (normalizedFirebaseUid == null) {
      return Optional.empty();
    }

    return userRepository.findByFirebaseUid(normalizedFirebaseUid);
  }

  private String normalizeFirebaseUid(String firebaseUid) {
    if (firebaseUid == null) {
      return null;
    }

    String normalized = firebaseUid.trim();
    return normalized.isEmpty() ? null : normalized;
  }

  private boolean readBooleanClaim(Object claimValue) {
    if (claimValue instanceof Boolean booleanValue) {
      return booleanValue;
    }

    if (claimValue instanceof String stringValue) {
      return Boolean.parseBoolean(stringValue.trim());
    }

    return false;
  }
}
