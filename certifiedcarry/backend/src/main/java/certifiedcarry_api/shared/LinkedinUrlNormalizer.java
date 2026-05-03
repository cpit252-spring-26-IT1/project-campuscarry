package certifiedcarry_api.shared;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.function.Function;
import org.springframework.web.server.ResponseStatusException;

public final class LinkedinUrlNormalizer {

  private LinkedinUrlNormalizer() {
  }

  public static String normalizeValidatedLinkedinUrl(
      String normalized,
      Function<String, ResponseStatusException> badRequestFactory) {
    URI parsedUri;
    try {
      parsedUri = new URI(normalized);
    } catch (URISyntaxException exception) {
      throw badRequestFactory.apply("linkedinUrl must be a valid URL.");
    }

    String scheme = parsedUri.getScheme();
    if (scheme == null || !("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme))) {
      throw badRequestFactory.apply("linkedinUrl must start with http:// or https://.");
    }

    String host = parsedUri.getHost();
    if (host == null || host.isBlank()) {
      throw badRequestFactory.apply("linkedinUrl must include a valid host.");
    }

    String normalizedHost = host.toLowerCase(Locale.ROOT);
    if (!normalizedHost.equals("linkedin.com") && !normalizedHost.endsWith(".linkedin.com")) {
      throw badRequestFactory.apply("linkedinUrl must point to linkedin.com.");
    }

    String path = parsedUri.getPath();
    if (path == null || path.isBlank() || "/".equals(path)) {
      throw badRequestFactory.apply("linkedinUrl must include a LinkedIn profile or company path.");
    }

    return normalized;
  }
}
