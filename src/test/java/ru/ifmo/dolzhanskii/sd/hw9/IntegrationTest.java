package ru.ifmo.dolzhanskii.sd.hw9;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.io.Resources;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import ru.ifmo.dolzhanskii.sd.hw9.dto.MasterResponse;

import static com.google.common.truth.Truth.assertThat;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.matchers.Times.exactly;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class IntegrationTest {

    private Main main;
    private ClientAndServer mockServer;

    private static final int MOCK_SERVER_PORT = 8008;

    @BeforeEach
    void beforeEach() {
        mockServer = startClientAndServer(MOCK_SERVER_PORT);
        main = new Main();
    }

    @AfterEach
    void afterEach() {
        mockServer.stop();
    }

    @Test
    void testAllUp() throws Exception {
        String query = "Some query";
        createStubs(query, 200, 200, 200);

        MasterResponse masterResponse = main.processQuery(query, "configuration/test_search_config.json");
        assertThat(masterResponse.getResponse()).isEqualTo(Map.of(
            "Googel", List.of("Pootis", "Pootus", "Pootin"),
            "Beng", List.of("Gottam", "Gottum", "Gottim"),
            "Yondix", List.of("saas", "soos", "sees")
        ));
    }

    @Test
    void testOneHung() throws Exception {
        String query = "Some query";
        createStubs(query, 200, 200, 10_000);

        MasterResponse masterResponse = main.processQuery(query, "configuration/test_search_config.json");
        assertThat(masterResponse.getResponse()).isEqualTo(Map.of(
            "Googel", List.of("Pootis", "Pootus", "Pootin"),
            "Beng", List.of("Gottam", "Gottum", "Gottim")
        ));
    }

    @Test
    void testAllHung() throws Exception {
        String query = "Some query";
        createStubs(query, 10_000, 10_000, 10_000);

        MasterResponse masterResponse = main.processQuery(query, "configuration/test_search_config.json");
        assertThat(masterResponse.getResponse()).isEmpty();
    }

    private void createStubs(
        String query,
        long googelDelayMillis,
        long bengDelayMillis,
        long yondixDelayMillis
    ) throws Exception{
        stubSearch(
            "googel",
            "GET",
            "query",
            query,
            Map.of("foo", "bar"),
            "response/googel.json",
            googelDelayMillis
        );
        stubSearch(
            "beng",
            "GET",
            "query",
            query,
            Map.of(),
            "response/beng.json",
            bengDelayMillis
        );
        stubSearch(
            "yondix",
            "POST",
            "search",
            query,
            Map.of("baz", "qux", "quux", "quuz"),
            "response/yondix.json",
            yondixDelayMillis
        );
    }

    private void stubSearch(
        String name,
        String method,
        String queryParamName,
        String query,
        Map<String, String> parameters,
        String responsePath,
        long delayMillis
    ) throws Exception {
        HttpRequest request = request()
            .withMethod(method)
            .withPath("/" + name)
            .withQueryStringParameter(queryParamName, query);
        parameters.forEach(request::withQueryStringParameter);

        String response = Resources.toString(ClassLoader.getSystemResource(responsePath), StandardCharsets.UTF_8);

        new MockServerClient("localhost", MOCK_SERVER_PORT)
            .when(request, exactly(1))
            .respond(
                response()
                    .withStatusCode(200)
                    .withHeader("Content-Type", "application/json; charset=utf-8")
                    .withBody(response)
                    .withDelay(TimeUnit.MILLISECONDS, delayMillis)
            );
    }
}