package ru.ifmo.dolzhanskii.sd.hw9.client;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

@RequiredArgsConstructor
public class RequestParamSearchClient implements SearchClient {

    @Getter
    private final String name;
    private final String host;
    private final long port;
    private final String path;
    private final String method;
    private final String queryParamName;
    private final Map<String, String> queryParams;

    private static final int TIMEOUT = 2000;

    @Override
    public List<String> searchTopResponses(String query) throws IOException {
        try (
            CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(
                    RequestConfig.custom()
                        .setConnectTimeout(TIMEOUT)
                        .setConnectionRequestTimeout(TIMEOUT)
                        .setSocketTimeout(TIMEOUT)
                        .build()
                )
                .build()
        ) {
            HttpRequestBase httpRequest = new HttpRequestBase() {

                @Override
                public String getMethod() {
                    return method;
                }
            };

            URIBuilder uriBuilder = new URIBuilder(host)
                .setPort((int) port)
                .setPath(path)
                .addParameter(queryParamName, query);
            uriBuilder.addParameter(queryParamName, query);
            queryParams.forEach(uriBuilder::addParameter);
            httpRequest.setURI(uriBuilder.build());

            try (CloseableHttpResponse response = httpClient.execute(httpRequest)) {
                HttpEntity entity = response.getEntity();
                JSONObject jsonObject = (JSONObject) new JSONParser()
                    .parse(new InputStreamReader(entity.getContent()));
                return (JSONArray) jsonObject.get("response");
            }
        } catch (ParseException e) {
            throw new IOException("[" + name + "] Failed to parse response", e);
        } catch (URISyntaxException e) {
            throw new IOException("[" + name + "] Malformed URL", e);
        }
    }
}
