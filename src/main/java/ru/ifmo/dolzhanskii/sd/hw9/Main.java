package ru.ifmo.dolzhanskii.sd.hw9;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.pattern.Patterns;
import akka.util.Timeout;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import ru.ifmo.dolzhanskii.sd.hw9.actor.MasterActor;
import ru.ifmo.dolzhanskii.sd.hw9.client.RequestParamSearchClient;
import ru.ifmo.dolzhanskii.sd.hw9.client.SearchClient;
import ru.ifmo.dolzhanskii.sd.hw9.dto.MasterRequest;
import ru.ifmo.dolzhanskii.sd.hw9.dto.MasterResponse;
import scala.concurrent.Await;

public class Main {
    private List<SearchClient> searchClients;

    public static void main(String[] args) throws Exception {

        Scanner in = new Scanner(System.in);

        while (true) {
            System.out.println("Type search query...");
            String searchQuery = in.nextLine();
            System.out.println("Query result:");
            MasterResponse response = new Main().processQuery(searchQuery, "configuration/search_config.json");
            response.getResponse().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEachOrdered(entry ->
                    System.out.println(entry.getKey() + ":\n" + String.join("\n", entry.getValue()) + "\n\n")
                );
        }
    }

    MasterResponse processQuery(String searchQuery, String configPath) throws Exception {
        try {
            searchClients = readConfiguration(configPath);
        } catch (IOException | ParseException e) {
            System.out.println("Failed to read configuration:\n" + e.getMessage());
        }

        ActorSystem system = ActorSystem.create("ActorSystem");
        ActorRef master = system.actorOf(Props.create(MasterActor.class), "master");
        Timeout timeout = Timeout.create(Duration.ofSeconds(10));

        Object response = Await.result(
            Patterns.ask(
                master,
                new MasterRequest(searchQuery, searchClients, System.out),
                timeout
            ),
            timeout.duration()
        );

        if (response instanceof MasterResponse masterResponse) {
            return masterResponse;
        } else {
            throw new RuntimeException("Failed to read response from master");
        }
    }

    private static List<SearchClient> readConfiguration(String configPath) throws IOException, ParseException {
        InputStream configIs = ClassLoader.getSystemClassLoader().getResourceAsStream(configPath);
        if (configIs == null) {
            throw new IOException("Failed to get input stream of search config");
        }
        JSONObject configuration = (JSONObject) new JSONParser().parse(new InputStreamReader(configIs));
        JSONArray searchSources = (JSONArray) configuration.get("searchClients");
        return ((List<JSONObject>) searchSources).stream()
            .map(searchSource -> {
                if (searchSource.get("type").equals("requestParamSearchClient")) {
                    JSONObject parameters = (JSONObject) searchSource.get("parameters");
                    return new RequestParamSearchClient(
                        (String) parameters.get("name"),
                        (String) parameters.get("host"),
                        (Long) parameters.get("port"),
                        (String) parameters.get("path"),
                        (String) parameters.get("method"),
                        (String) parameters.get("queryParamName"),
                        (Map<String, String>) parameters.get("urlParams")
                    );
                } else {
                    throw new IllegalArgumentException("Unsupported search client type");
                }
            })
            .collect(Collectors.toList());
    }
}
