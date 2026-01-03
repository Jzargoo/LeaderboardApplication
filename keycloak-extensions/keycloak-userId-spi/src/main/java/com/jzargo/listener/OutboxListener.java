package com.jzargo.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jzargo.entities.Outbox;
import com.jzargo.entities.Status;
import com.jzargo.messaging.UserRegisterRequest;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.events.EventType;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;


@Slf4j
@ApplicationScoped
public class OutboxListener {

    public static final String KEYCLOAK_HEADER = "X-KEYCLOAK-SECRET";
    public static final String KEYCLOAK_VALUE = "Gz9Tj7cE2Rk1Q4L8aXnYhP7vWb3sQ6Lf";

    private volatile boolean running = true;
    private Thread worker;

    private final String selectRecords = """
            SELECT o.id, o.aggregateType, o.aggregateId, o.eventType,
                o.payload, o.status, o.createdAt
                    FROM Outbox o
                        WHERE o.status = :status
                        AND o.eventType = :eventType
                            ORDER BY o.createdAt LIMIT 10
            """;

    @Inject
    EntityManager em;
    private final ObjectMapper objectMapper = new ObjectMapper();

    void onStart(@Observes StartupEvent e) {
        worker = new Thread(this::listen, "outbox-listener");
        worker.start();
    }


    void onStop(@Observes ShutdownEvent e) {
        running = false;
        worker.interrupt();
    }

    private void listen() {
        while (running) {
            try {
                pollOutbox();
                Thread.sleep(60_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Polling error", e);
                sleepQuiet(120_000);
            }
        }
    }

    private void sleepQuiet(int i) {
        try {
            Thread.sleep(i);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Transactional
    void pollOutbox() throws JsonProcessingException, URISyntaxException {

        TypedQuery<Outbox> query = em.createQuery(selectRecords, Outbox.class);

        query.setParameter("status",Status.UNPUBLISHED);
        query.setParameter("eventType", EventType.REGISTER);

        List<Outbox> resultList = query.getResultList();

        HttpClient build = HttpClient.newBuilder().build();

        for(Outbox record: resultList){

            UserRegisterRequest urr = objectMapper.readValue(record.getPayload(), UserRegisterRequest.class);

            HttpRequest request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(urr.toString()))
                    .uri(new URI("http://host.docker.internal:8080/api/users/internal"))
                    .headers(KEYCLOAK_HEADER, KEYCLOAK_VALUE)
                    .build();
            CompletableFuture<HttpResponse<String>> futureResponse =
                    build.sendAsync(request, HttpResponse.BodyHandlers.ofString());

            futureResponse.whenComplete(
                    (resp, e) -> {
                        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                            if(e != null){
                                log.error("Processed request with exception", e);
                                throw new RuntimeException(e);
                            } else{
                                log.error("Internal error");
                                throw new RuntimeException("Internal error");
                            }
                        }
                    }
            );
        }
        log.info("10 entities were sent");
    }
}
