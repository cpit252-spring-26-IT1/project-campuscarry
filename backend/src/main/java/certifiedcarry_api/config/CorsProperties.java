package certifiedcarry_api.config;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.cors")
@Getter
@Setter
public class CorsProperties {

  private List<String> allowedOrigins = List.of("http://localhost:3005", "http://127.0.0.1:3005");
  private List<String> allowedMethods = List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS");
  private List<String> allowedHeaders =
      List.of("Authorization", "Content-Type", "Accept", "X-Requested-With", "X-Request-Id");
  private List<String> exposedHeaders = List.of("X-Request-Id", "Retry-After");
  private boolean allowCredentials = false;
  private long maxAgeSeconds = 3600;
  private boolean enforceProductionSafeOrigins = false;
}
