package pl.tomaszko.s03e05.hub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import pl.tomaszko.s03e05.TestAppProperties;

class HubClientResponseTest {

    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private HubClient client;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new HubClient(builder.build(), TestAppProperties.create());
    }

    @Test
    void jsonSuccessIsReadable() {
        server.expect(requestTo("https://hub.ag3nts.org/api/maps"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"tiles\":[]}", MediaType.APPLICATION_JSON));

        HubCallResult result = client.discover("/api/maps", "Skolwin");

        assertTrue(result.success());
        assertEquals("{\"tiles\":[]}", result.textForModel());
        server.verify();
    }

    @Test
    void httpErrorIncludesStatusAndBody() {
        server.expect(requestTo("https://hub.ag3nts.org/api/maps"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withBadRequest().body("{\"error\":\"no map\"}").contentType(MediaType.APPLICATION_JSON));

        HubCallResult result = client.discover("/api/maps", "Skolwin");

        assertFalse(result.success());
        assertTrue(result.textForModel().contains("400"));
        assertTrue(result.textForModel().contains("no map"));
        server.verify();
    }

    @Test
    void binaryImageIncludesContentType() {
        byte[] png = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x01};
        server.expect(requestTo("https://hub.ag3nts.org/api/maps"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(png, MediaType.IMAGE_PNG));

        HubCallResult result = client.discover("/api/maps", "Skolwin");

        assertFalse(result.success());
        assertTrue(result.textForModel().contains("200"));
        assertTrue(result.textForModel().toLowerCase().contains("png"));
        assertTrue(result.textForModel().contains("binary"));
        server.verify();
    }

    @Test
    void octetStreamJsonIsStillReadable() {
        server.expect(requestTo("https://hub.ag3nts.org/api/wehicles"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"vehicles\":[\"horse\"]}", MediaType.APPLICATION_OCTET_STREAM));

        HubCallResult result = client.discover("/api/wehicles", "vehicles");

        assertTrue(result.success());
        assertTrue(result.textForModel().contains("horse"));
        server.verify();
    }
}
