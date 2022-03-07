package ru.ifmo.dolzhanskii.sd.hw9.actor;

import java.io.IOException;
import java.util.List;

import akka.actor.UntypedAbstractActor;
import lombok.extern.slf4j.Slf4j;
import ru.ifmo.dolzhanskii.sd.hw9.client.SearchClient;
import ru.ifmo.dolzhanskii.sd.hw9.dto.SearchRequest;
import ru.ifmo.dolzhanskii.sd.hw9.dto.SearchResponse;

@Slf4j
public class SearchActor extends UntypedAbstractActor {

    @Override
    public void onReceive(Object message) {
        log.error("SEARCH: Received message: " + message.toString());
        if (message instanceof SearchRequest searchRequest) {
            SearchClient searchClient = searchRequest.getSearchClient();
            try {
                List<String> results = searchClient.searchTopResponses(searchRequest.getQuery());
                log.error("RESPONSE: " + String.join(", ", results));
                sender().tell(new SearchResponse(true, searchClient.getName(), results), self());
            } catch (IOException e) {
                sender().tell(new SearchResponse(false, searchClient.getName(), null), self());
            }
        }
    }
}
