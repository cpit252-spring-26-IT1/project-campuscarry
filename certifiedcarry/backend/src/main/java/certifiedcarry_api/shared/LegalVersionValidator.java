package certifiedcarry_api.shared;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LegalVersionValidator {

  private static final String LEGAL_VERSIONS_OUTDATED_MESSAGE =
      "Accepted legal versions are outdated. Refresh and accept the current policies.";

  private final String activeTermsVersion;
  private final String activePrivacyVersion;

  public LegalVersionValidator(
      @Value("${legal.terms-active-version:cc-terms-2026-04-04}") String activeTermsVersion,
      @Value("${legal.privacy-active-version:cc-privacy-2026-04-04}") String activePrivacyVersion) {
    this.activeTermsVersion = activeTermsVersion;
    this.activePrivacyVersion = activePrivacyVersion;
  }

  public void validateAcceptedVersions(
      String acceptedTermsVersion, String acceptedPrivacyVersion) {
    if (!activeTermsVersion.equals(acceptedTermsVersion)
        || !activePrivacyVersion.equals(acceptedPrivacyVersion)) {
      throw HttpErrors.badRequest(LEGAL_VERSIONS_OUTDATED_MESSAGE);
    }
  }
}
