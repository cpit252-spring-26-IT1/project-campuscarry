package certifiedcarry_api.storage.config;

import certifiedcarry_api.shared.TextNormalization;
import java.net.URI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties(ObjectStorageProperties.class)
public class ObjectStorageConfig {

  @Bean
  @ConditionalOnProperty(prefix = "object-storage", name = "enabled", havingValue = "true")
  public S3Presigner s3Presigner(ObjectStorageProperties properties) {
    String endpoint = requireNonBlank(properties.getEndpoint(), "OBJECT_STORAGE_ENDPOINT");
    String region = requireNonBlank(properties.getRegion(), "OBJECT_STORAGE_REGION");
    String accessKey = requireNonBlank(properties.getAccessKey(), "OBJECT_STORAGE_ACCESS_KEY");
    String secretKey = requireNonBlank(properties.getSecretKey(), "OBJECT_STORAGE_SECRET_KEY");

    return S3Presigner.builder()
        .endpointOverride(URI.create(endpoint))
        .region(Region.of(region))
        .credentialsProvider(
            StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
        .build();
  }

  private String requireNonBlank(String value, String envName) {
    String normalized = TextNormalization.trimToNull(value);
    if (normalized == null) {
      throw new IllegalStateException(envName + " must be set when object-storage.enabled=true");
    }

    return normalized;
  }
}
