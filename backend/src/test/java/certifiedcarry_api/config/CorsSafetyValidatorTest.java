package certifiedcarry_api.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

class CorsSafetyValidatorTest {

  @Test
  void runSkipsValidationWhenProductionSafetyChecksAreDisabled() {
    CorsProperties properties = new CorsProperties();
    properties.setEnforceProductionSafeOrigins(false);

    CorsSafetyValidator validator = new CorsSafetyValidator(properties);
    assertDoesNotThrow(() -> validator.run(new DefaultApplicationArguments()));
  }

  @Test
  void runRejectsMissingWildcardInsecureAndLoopbackOrigins() {
    CorsProperties missingOrigins = new CorsProperties();
    missingOrigins.setAllowedOrigins(List.of("   "));
    missingOrigins.setEnforceProductionSafeOrigins(true);

    IllegalStateException missingOriginFailure =
        assertThrows(
            IllegalStateException.class,
            () -> new CorsSafetyValidator(missingOrigins).run(new DefaultApplicationArguments()));
    assertEquals(
        "security.cors.allowed-origins must be set when production CORS safety checks are enabled.",
        missingOriginFailure.getMessage());

    CorsProperties wildcardOrigins = new CorsProperties();
    wildcardOrigins.setAllowedOrigins(List.of("https://*.certifiedcarry.me"));
    wildcardOrigins.setEnforceProductionSafeOrigins(true);
    assertThrows(
        IllegalStateException.class,
        () -> new CorsSafetyValidator(wildcardOrigins).run(new DefaultApplicationArguments()));

    CorsProperties insecureOrigins = new CorsProperties();
    insecureOrigins.setAllowedOrigins(List.of("http://certifiedcarry.me"));
    insecureOrigins.setEnforceProductionSafeOrigins(true);
    assertThrows(
        IllegalStateException.class,
        () -> new CorsSafetyValidator(insecureOrigins).run(new DefaultApplicationArguments()));

    CorsProperties loopbackOrigins = new CorsProperties();
    loopbackOrigins.setAllowedOrigins(List.of("https://localhost"));
    loopbackOrigins.setEnforceProductionSafeOrigins(true);
    assertThrows(
        IllegalStateException.class,
        () -> new CorsSafetyValidator(loopbackOrigins).run(new DefaultApplicationArguments()));
  }

  @Test
  void runAcceptsTrimmedHttpsOriginsWithRealHosts() {
    CorsProperties properties = new CorsProperties();
    properties.setAllowedOrigins(List.of("  https://certifiedcarry.me  ", "https://app.certifiedcarry.me"));
    properties.setEnforceProductionSafeOrigins(true);

    CorsSafetyValidator validator = new CorsSafetyValidator(properties);
    assertDoesNotThrow(() -> validator.run(new DefaultApplicationArguments()));
  }
}
