package certifiedcarry_api.config;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class CorsSafetyValidator implements ApplicationRunner {

  private final CorsProperties corsProperties;

  public CorsSafetyValidator(CorsProperties corsProperties) {
    this.corsProperties = corsProperties;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (!corsProperties.isEnforceProductionSafeOrigins()) {
      return;
    }

    List<String> allowedOrigins = sanitizeValues(corsProperties.getAllowedOrigins());
    if (allowedOrigins.isEmpty()) {
      throw new IllegalStateException(
          "security.cors.allowed-origins must be set when production CORS safety checks are enabled.");
    }

    for (String origin : allowedOrigins) {
      validateOrigin(origin);
    }
  }

  private void validateOrigin(String origin) {
    if (origin.contains("*")) {
      throw new IllegalStateException(
          "Wildcard CORS origins are not allowed when production CORS safety checks are enabled: "
              + origin);
    }

    URI parsedOrigin;
    try {
      parsedOrigin = URI.create(origin);
    } catch (IllegalArgumentException exception) {
      throw new IllegalStateException("Invalid CORS origin: " + origin, exception);
    }

    String scheme = normalizeLower(parsedOrigin.getScheme());
    String host = normalizeLower(parsedOrigin.getHost());

    if (!"https".equals(scheme)) {
      throw new IllegalStateException(
          "Only HTTPS CORS origins are allowed when production safety checks are enabled: " + origin);
    }

    if (host == null || host.isBlank()) {
      throw new IllegalStateException("CORS origin must include a valid host: " + origin);
    }

    if (isLoopbackHost(host)) {
      throw new IllegalStateException(
          "Loopback CORS origins are not allowed when production safety checks are enabled: " + origin);
    }
  }

  private boolean isLoopbackHost(String host) {
    return "localhost".equals(host)
        || "127.0.0.1".equals(host)
        || "::1".equals(host)
        || host.endsWith(".localhost");
  }

  private String normalizeLower(String value) {
    if (value == null) {
      return null;
    }

    return value.trim().toLowerCase(Locale.ROOT);
  }

  private List<String> sanitizeValues(List<String> values) {
    if (values == null) {
      return List.of();
    }

    return values.stream().map(String::trim).filter(value -> !value.isEmpty()).toList();
  }
}
