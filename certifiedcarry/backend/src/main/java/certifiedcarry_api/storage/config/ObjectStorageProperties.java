package certifiedcarry_api.storage.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "object-storage")
@Getter
@Setter
public class ObjectStorageProperties {

  private boolean enabled;
  private String endpoint;
  private String region;
  private String bucket;
  private String accessKey;
  private String secretKey;
  private String keyPrefix = "player-assets";
  private String publicBaseUrl;
  private int presignExpirySeconds = 600;
  private long maxUploadBytes = 6L * 1024L * 1024L;
}
