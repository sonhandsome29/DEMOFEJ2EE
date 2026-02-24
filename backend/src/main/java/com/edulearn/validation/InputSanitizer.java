package com.edulearn.validation;

import com.edulearn.exception.BadRequestException;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

@Component
public class InputSanitizer {

    private static final Safelist NO_HTML = Safelist.none();
    private static final Set<String> ALLOWED_URL_SCHEMES = Set.of("http", "https");

    public String sanitizePlainText(String value) {
        if (value == null) {
            return null;
        }

        String cleaned = Jsoup.clean(value, NO_HTML).trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    public String sanitizeHttpUrl(String value, String fieldName) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        try {
            URI uri = new URI(trimmed);
            String scheme = uri.getScheme();
            if (scheme == null || !ALLOWED_URL_SCHEMES.contains(scheme.toLowerCase())) {
                throw new BadRequestException(fieldName + " must use http or https");
            }

            return trimmed;
        } catch (URISyntaxException ex) {
            throw new BadRequestException("Invalid URL for " + fieldName);
        }
    }
}
