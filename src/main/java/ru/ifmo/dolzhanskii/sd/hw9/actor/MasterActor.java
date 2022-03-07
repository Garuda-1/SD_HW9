package ru.ifmo.dolzhanskii.sd.hw9.actor;

import java.util.Set;
import java.util.stream.Collectors;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedAbstractActor;
import akka.dispatch.sysmsg.Terminate;
import akka.japi.Pair;
import lombok.extern.slf4j.Slf4j;
import ru.ifmo.dolzhanskii.sd.hw9.client.SearchClient;
import ru.ifmo.dolzhanskii.sd.hw9.dto.MasterRequest;
import ru.ifmo.dolzhanskii.sd.hw9.dto.MasterResponse;
import ru.ifmo.dolzhanskii.sd.hw9.dto.SearchRequest;
import ru.ifmo.dolzhanskii.sd.hw9.dto.SearchResponse;
import scala.concurrent.duration.Duration;

@Slf4j
public class MasterActor extends UntypedAbstractActor {

    private ActorRef client;
    private Set<String> children;
    private final MasterResponse masterResponse = new MasterResponse();

    @Override
    public void onReceive(Object message) {
        log.error("MASTER: Received message: " + message.toString());
        if (message instanceof MasterRequest masterRequest) {
            client = sender();
            children = masterRequest.getSearchProcessors().stream()
                .map(SearchClient::getName)
                .collect(Collectors.toSet());
            masterRequest.getSearchProcessors().stream()
                .map(searchProcessor -> Pair.create(
                    searchProcessor,
                    getContext().actorOf(Props.create(SearchActor.class), searchProcessor.getName())
                ))
                .parallel()
                .forEach(pair -> pair.second().tell(
                    new SearchRequest(masterRequest.getQuery(), pair.first()), self())
                );
            getContext().setReceiveTimeout(Duration.create("10000 second"));
        } else if (message instanceof SearchResponse searchResponse) {
            children.remove(searchResponse.getName());
            if (!searchResponse.isOk()) {
                log.error(
                    "Failed to get search results from {}",
                    searchResponse.getName());
            } else {
                masterResponse.getResponse().put(
                    searchResponse.getName(),
                    searchResponse.getResults()
                );
                getContext().stop(sender());
            }
            if (children.isEmpty()) {
                terminate();
            }
        } else if (message instanceof ReceiveTimeout) {
            log.error("Timed-out search results from: {}.", String.join(", ", children));
            terminate();
        } else if (message instanceof Terminate) {
            terminate();
        }
    }

    private void terminate() {
        client.tell(masterResponse, self());
        getContext().stop(self());
    }
}
