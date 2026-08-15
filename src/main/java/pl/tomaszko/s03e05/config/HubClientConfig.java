package pl.tomaszko.s03e05.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import pl.tomaszko.s03e05.hub.HubClient;

@Configuration
public class HubClientConfig {

    @Bean
    HubClient hubClient(AppProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(15));
        RestClient restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
        return new HubClient(restClient, properties);
    }
}
