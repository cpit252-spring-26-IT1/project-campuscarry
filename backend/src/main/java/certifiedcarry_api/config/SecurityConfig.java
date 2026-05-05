package certifiedcarry_api.config;

import certifiedcarry_api.config.audit.AuditLoggingFilter;
import certifiedcarry_api.config.firebase.FirebaseTokenFilter;
import certifiedcarry_api.config.ratelimit.RequestRateLimitFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(CorsProperties.class)
public class SecurityConfig {

  private final CorsProperties corsProperties;
  private final ObjectProvider<AuditLoggingFilter> auditLoggingFilterProvider;
  private final ObjectProvider<FirebaseTokenFilter> firebaseTokenFilterProvider;
  private final ObjectProvider<RequestRateLimitFilter> requestRateLimitFilterProvider;

  public SecurityConfig(
      CorsProperties corsProperties,
      ObjectProvider<AuditLoggingFilter> auditLoggingFilterProvider,
      ObjectProvider<FirebaseTokenFilter> firebaseTokenFilterProvider,
      ObjectProvider<RequestRateLimitFilter> requestRateLimitFilterProvider) {
    this.corsProperties = corsProperties;
    this.auditLoggingFilterProvider = auditLoggingFilterProvider;
    this.firebaseTokenFilterProvider = firebaseTokenFilterProvider;
    this.requestRateLimitFilterProvider = requestRateLimitFilterProvider;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    AuditLoggingFilter auditLoggingFilter = auditLoggingFilterProvider.getIfAvailable();
    FirebaseTokenFilter firebaseTokenFilter = firebaseTokenFilterProvider.getIfAvailable();
    RequestRateLimitFilter requestRateLimitFilter = requestRateLimitFilterProvider.getIfAvailable();

    http.cors(Customizer.withDefaults())
      .csrf(AbstractHttpConfigurer::disable)
      .headers(
          headers ->
              headers
                  .contentTypeOptions(Customizer.withDefaults())
                  .frameOptions(frame -> frame.deny())
                  .cacheControl(Customizer.withDefaults())
                  .referrerPolicy(
                      policy ->
                          policy.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                  .httpStrictTransportSecurity(
                      hsts -> hsts.maxAgeInSeconds(31536000).includeSubDomains(true)))
      .authorizeHttpRequests(
        auth ->
          auth.requestMatchers(
              this::isPublicUserRegistrationRequest, this::isPublicSignupCompletionRequest)
            .permitAll()
            .requestMatchers(HttpMethod.OPTIONS, "/**")
            .permitAll()
            .requestMatchers(
                HttpMethod.POST,
                ApiRouteCatalog.USERS,
                ApiRouteCatalog.apiPath(ApiRouteCatalog.USERS))
            .permitAll()
            .requestMatchers(HttpMethod.POST, "/**/users", "/**/users/")
            .permitAll()
            .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**")
            .permitAll()
            .requestMatchers("/error", "/error/**")
            .permitAll()
            .requestMatchers(
                HttpMethod.GET,
                ApiRouteCatalog.USERS_ME_DM_OPENNESS,
                ApiRouteCatalog.apiPath(ApiRouteCatalog.USERS_ME_DM_OPENNESS))
            .hasRole("RECRUITER")
            .requestMatchers(HttpMethod.GET, ApiRouteCatalog.USERS)
            .hasRole("FIREBASE_AUTHENTICATED")
            .requestMatchers(HttpMethod.GET, ApiRouteCatalog.recursive(ApiRouteCatalog.USERS))
            .hasRole("FIREBASE_AUTHENTICATED")
            .requestMatchers(
                HttpMethod.PATCH,
                ApiRouteCatalog.USERS_ME_DM_OPENNESS,
                ApiRouteCatalog.apiPath(ApiRouteCatalog.USERS_ME_DM_OPENNESS))
            .hasRole("RECRUITER")
            .requestMatchers(HttpMethod.PATCH, ApiRouteCatalog.recursive(ApiRouteCatalog.USERS))
            .hasRole("ADMIN")
            .requestMatchers(HttpMethod.DELETE, ApiRouteCatalog.USERS + "/me")
            .hasAnyRole("PLAYER", "RECRUITER")
            .requestMatchers(HttpMethod.DELETE, ApiRouteCatalog.recursive(ApiRouteCatalog.USERS))
            .hasRole("ADMIN")
            .requestMatchers(ApiRouteCatalog.AUTH_ME)
            .hasRole("FIREBASE_AUTHENTICATED")
            .requestMatchers(
              HttpMethod.POST,
              ApiRouteCatalog.AUTH_SESSION_LOGIN,
              ApiRouteCatalog.AUTH_SESSION_LOGOUT)
            .hasRole("FIREBASE_AUTHENTICATED")
            .requestMatchers(HttpMethod.POST, ApiRouteCatalog.MEDIA_UPLOADS_PRESIGN)
            .hasRole("FIREBASE_AUTHENTICATED")
            .requestMatchers(ApiRouteCatalog.recursive(ApiRouteCatalog.PLAYER_PROFILES))
            .hasRole("FIREBASE_AUTHENTICATED")
            .requestMatchers(HttpMethod.GET, ApiRouteCatalog.recursive(ApiRouteCatalog.LEADERBOARD))
            .hasRole("FIREBASE_AUTHENTICATED")
            .requestMatchers(HttpMethod.POST, ApiRouteCatalog.recursive(ApiRouteCatalog.LEADERBOARD))
            .hasRole("FIREBASE_AUTHENTICATED")
            .requestMatchers(HttpMethod.PATCH, ApiRouteCatalog.recursive(ApiRouteCatalog.LEADERBOARD))
            .hasRole("FIREBASE_AUTHENTICATED")
            .requestMatchers(HttpMethod.DELETE, ApiRouteCatalog.recursive(ApiRouteCatalog.LEADERBOARD))
            .hasRole("FIREBASE_AUTHENTICATED")
            .requestMatchers(HttpMethod.GET, ApiRouteCatalog.PENDING_RECRUITERS)
            .hasRole("FIREBASE_AUTHENTICATED")
            .requestMatchers(HttpMethod.POST, ApiRouteCatalog.PENDING_RECRUITERS)
            .hasRole("FIREBASE_AUTHENTICATED")
            .requestMatchers(HttpMethod.DELETE, ApiRouteCatalog.recursive(ApiRouteCatalog.PENDING_RECRUITERS))
            .hasRole("ADMIN")
            .requestMatchers(ApiRouteCatalog.recursive(ApiRouteCatalog.PENDING_RANKS))
            .hasRole("FIREBASE_AUTHENTICATED")
            .requestMatchers(ApiRouteCatalog.recursive(ApiRouteCatalog.CHAT_THREADS))
            .hasRole("FIREBASE_AUTHENTICATED")
            .requestMatchers(ApiRouteCatalog.recursive(ApiRouteCatalog.CHAT_MESSAGES))
            .hasRole("FIREBASE_AUTHENTICATED")
            .requestMatchers(HttpMethod.POST, "/**")
            .hasRole("FIREBASE_AUTHENTICATED")
            .anyRequest()
            .denyAll());

    if (firebaseTokenFilter != null) {
      http.addFilterBefore(firebaseTokenFilter, AnonymousAuthenticationFilter.class);
    }

    if (auditLoggingFilter != null) {
      if (firebaseTokenFilter != null) {
        http.addFilterAfter(auditLoggingFilter, FirebaseTokenFilter.class);
      } else {
        http.addFilterBefore(auditLoggingFilter, AnonymousAuthenticationFilter.class);
      }
    }

    if (requestRateLimitFilter != null) {
      if (auditLoggingFilter != null) {
        http.addFilterAfter(requestRateLimitFilter, AuditLoggingFilter.class);
      } else if (firebaseTokenFilter != null) {
        http.addFilterAfter(requestRateLimitFilter, FirebaseTokenFilter.class);
      } else {
        http.addFilterBefore(requestRateLimitFilter, AnonymousAuthenticationFilter.class);
      }
    }

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(sanitizeValues(corsProperties.getAllowedOrigins()));
    configuration.setAllowedMethods(sanitizeValues(corsProperties.getAllowedMethods()));
    configuration.setAllowedHeaders(sanitizeValues(corsProperties.getAllowedHeaders()));
    configuration.setExposedHeaders(sanitizeValues(corsProperties.getExposedHeaders()));
    configuration.setAllowCredentials(corsProperties.isAllowCredentials());
    configuration.setMaxAge(corsProperties.getMaxAgeSeconds());

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  private List<String> sanitizeValues(List<String> values) {
    if (values == null) {
      return List.of();
    }

    return values.stream().map(String::trim).filter(value -> !value.isEmpty()).toList();
  }

  private boolean isPublicUserRegistrationRequest(HttpServletRequest request) {
    if (!HttpMethod.POST.matches(request.getMethod())) {
      return false;
    }

    return ApiRouteCatalog.isUserRegistrationPath(request.getRequestURI())
        || ApiRouteCatalog.isUserRegistrationPath(request.getServletPath())
        || ApiRouteCatalog.isUserRegistrationPath(ApiRouteCatalog.resolveRequestPath(request));
  }

  private boolean isPublicSignupCompletionRequest(HttpServletRequest request) {
    if (!HttpMethod.POST.matches(request.getMethod())) {
      return false;
    }

    return ApiRouteCatalog.isSignupCompletionPath(request.getRequestURI())
        || ApiRouteCatalog.isSignupCompletionPath(request.getServletPath())
        || ApiRouteCatalog.isSignupCompletionPath(ApiRouteCatalog.resolveRequestPath(request));
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
