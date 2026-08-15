package pl.tomaszko.s03e05.hub;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import pl.tomaszko.s03e05.TestAppProperties;
import pl.tomaszko.s03e05.config.AppProperties;

class VerifyPayloadTest {

    @Test
    void verifyJsonUsesConfiguredTaskAndInjectedApiKey() {
        AppProperties properties = TestAppProperties.create();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HubClient client = new HubClient(builder.build(), properties);

        server.expect(requestTo("https://hub.ag3nts.org/verify"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.apikey").value("test-hub-key"))
                .andExpect(jsonPath("$.task").value("savethem"))
                .andExpect(jsonPath("$.answer[0]").value("right"))
                .andRespond(withSuccess("{\"status\":\"ok\"}", MediaType.APPLICATION_JSON));

        client.verify(java.util.List.of("right"));
        server.verify();
    }
}
