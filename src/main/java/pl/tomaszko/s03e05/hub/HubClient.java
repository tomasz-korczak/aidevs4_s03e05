package pl.tomaszko.s03e05.hub;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import pl.tomaszko.s03e05.config.AppProperties;

public class HubClient {

    private static final int ERROR_BODY_LIMIT = 2000;

    private final RestClient restClient;
    private final AppProperties properties;

    public HubClient(RestClient restClient, AppProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public HubCallResult discover(String path, String query) {
        DiscoverRequest request = new DiscoverRequest(properties.getHub().getApiKey(), query);
        return post(join(properties.getHub().getBaseUrl(), path), request);
    }

    public HubCallResult verify(List<String> answer) {
        VerifyRequest request = new VerifyRequest(
                properties.getHub().getApiKey(),
                properties.getHub().getTask(),
                answer);
        return post(join(properties.getHub().getBaseUrl(), properties.getHub().getVerifyPath()), request);
    }

    private HubCallResult post(URI uri, Object body) {
        try {
            HubCallResult result = restClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.ALL)
                    .body(body)
                    .exchange((request, response) -> toResult(response.getStatusCode(), response.getHeaders().getContentType(), response.getBody()));
            return result != null ? result : HubCallResult.failed("Empty hub response");
        } catch (ResourceAccessException ex) {
            return HubCallResult.failed("Hub unreachable or timed out");
        } catch (RestClientException ex) {
            String message = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            return HubCallResult.failed("Hub request failed: " + message);
        }
    }

    static HubCallResult toResult(HttpStatusCode status, MediaType contentType, InputStream body) {
        byte[] bytes;
        try {
            bytes = body != null ? body.readAllBytes() : new byte[0];
        } catch (Exception ex) {
            return HubCallResult.failed("Hub HTTP " + status.value() + " " + typeLabel(contentType)
                    + ": body could not be read (" + ex.getMessage() + ")");
        }
        if (isBinary(contentType, bytes)) {
            return HubCallResult.failed("Hub HTTP " + status.value() + " " + typeLabel(contentType)
                    + " (" + bytes.length + " bytes, binary). Use a query or path that returns JSON or text.");
        }
        String text = new String(bytes, charsetOf(contentType)).trim();
        if (text.isEmpty()) {
            return HubCallResult.failed("Hub HTTP " + status.value() + " " + typeLabel(contentType) + ": empty body");
        }
        if (status.isError()) {
            return HubCallResult.failed("Hub HTTP " + status.value() + " " + typeLabel(contentType) + ": "
                    + truncate(text));
        }
        return HubCallResult.ok(text);
    }

    private static Charset charsetOf(MediaType contentType) {
        if (contentType != null && contentType.getCharset() != null) {
            return contentType.getCharset();
        }
        return StandardCharsets.UTF_8;
    }

    private static String typeLabel(MediaType contentType) {
        return contentType != null ? contentType.toString() : "unknown";
    }

    private static String truncate(String text) {
        if (text.length() <= ERROR_BODY_LIMIT) {
            return text;
        }
        return text.substring(0, ERROR_BODY_LIMIT) + "...";
    }

    private static boolean isBinary(MediaType contentType, byte[] bytes) {
        if (isBinaryMagic(bytes)) {
            return true;
        }
        if (contentType == null) {
            return !looksLikeText(bytes);
        }
        String type = contentType.getType();
        String subtype = contentType.getSubtype();
        if ("image".equals(type) || "audio".equals(type) || "video".equals(type)) {
            return true;
        }
        if ("application".equals(type) && ("octet-stream".equals(subtype) || "pdf".equals(subtype) || "zip".equals(subtype))) {
            return !looksLikeText(bytes);
        }
        return false;
    }

    private static boolean isBinaryMagic(byte[] bytes) {
        if (bytes.length >= 8 && bytes[0] == (byte) 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) {
            return true;
        }
        if (bytes.length >= 3 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF) {
            return true;
        }
        if (bytes.length >= 4 && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F') {
            return true;
        }
        return bytes.length >= 4 && bytes[0] == 'P' && bytes[1] == 'K';
    }

    private static boolean looksLikeText(byte[] bytes) {
        if (bytes.length == 0) {
            return true;
        }
        int inspect = Math.min(bytes.length, 512);
        int control = 0;
        for (int i = 0; i < inspect; i++) {
            int value = bytes[i] & 0xff;
            if (value == 0) {
                return false;
            }
            if (value < 0x09) {
                control++;
            }
        }
        return control * 10 <= inspect;
    }

    static URI join(URI base, String path) {
        String normalized = path == null || path.isBlank() ? "/" : path;
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        String baseText = base.toString();
        if (baseText.endsWith("/")) {
            baseText = baseText.substring(0, baseText.length() - 1);
        }
        return URI.create(baseText + normalized);
    }
}
