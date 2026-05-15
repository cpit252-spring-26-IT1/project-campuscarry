package certifiedcarry_api.storage.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ObjectStorageConfigTest {

  private final ObjectStorageConfig config = new ObjectStorageConfig();

  @Test
  void presignerBuildsWhenRequiredPropertiesArePresent() {
    ObjectStorageProperties properties = new ObjectStorageProperties();
    properties.setEnabled(true);
    properties.setEndpoint("https://nyc3.digitaloceanspaces.com");
    properties.setRegion("nyc3");
    properties.setAccessKey("key");
    properties.setSecretKey("secret");

    assertNotNull(config.s3Presigner(properties));
  }

  @Test
  void presignerRejectsMissingRequiredValues() {
    ObjectStorageProperties properties = new ObjectStorageProperties();
    properties.setEnabled(true);
    properties.setEndpoint(" ");
    properties.setRegion("nyc3");
    properties.setAccessKey("key");
    properties.setSecretKey("secret");

    assertThrows(IllegalStateException.class, () -> config.s3Presigner(properties));
  }
}
