package com.jzargo.events;

import com.jzargo.entities.IncrementedValue;
import com.jzargo.messaging.UserRegisterRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class UserIdEventProvider implements EventListenerProvider {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(UserIdEventProvider.class);
    private final KeycloakSession session;

    private static final String KEYCLOAK_HEADER = "X-KEYCLOAK-SECRET";
    private static final String KEYCLOAK_VALUE = "Gz9Tj7cE2Rk1Q4L8aXnYhP7vWb3sQ6Lf";

    public UserIdEventProvider(KeycloakSession session) {
        this.session = session;
    }

    private void sendToUser(Event event, long userId){
        HttpClient build = HttpClient.newBuilder().build();
        try {

            RealmModel realm = session.realms().getRealm(event.getRealmId());
            UserModel user = session.users().getUserById(realm, event.getUserId());


            if(userId < 1) {
                log.error("UserId did not set");
                throw new RuntimeException();
            }

            UserRegisterRequest urr = UserRegisterRequest.builder()
                    .userId(userId)
                    .name(user.getUsername())
                    .email(user.getEmail())
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(urr.toString()))
                    .uri(new URI("http://gateway:8080/api/users/internal"))
                    .headers(KEYCLOAK_HEADER, KEYCLOAK_VALUE)
                    .build();
            build.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        } catch (URISyntaxException e) {
            log.error("Cannot send register");
            throw new RuntimeException(e);
        }
    }

    private long setIdToUser(Event event) {
        try {

            EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
            TypedQuery<IncrementedValue> typedQuery =
                    em.createQuery("SELECT iv FROM IncrementedValue iv WHERE iv.name = :name", IncrementedValue.class);
            typedQuery.setParameter("name", IncrementedValue.USER_ID_COUNTER);
            typedQuery.setLockMode(LockModeType.PESSIMISTIC_WRITE);
            IncrementedValue incrementedValue = typedQuery.getSingleResultOrNull();

            if (incrementedValue == null) {
                log.info("This is first register event");
                incrementedValue = new IncrementedValue();
                incrementedValue.setName(IncrementedValue.USER_ID_COUNTER);
                em.persist(incrementedValue);
            } else if (incrementedValue.getLastValue() == null) {
                incrementedValue.initializeLastValue(0L);
            }

            Long l = incrementedValue.incrementValue();
            log.info("Incremented value was found and changed to {}", l);

            RealmModel realm = session.realms().getRealm(event.getRealmId());
            UserModel user = session.users().getUserById(realm, event.getUserId());
            user.setSingleAttribute("user_id", l.toString());
            log.info("Attribute user_id was added successfully");

            return l;
        } catch (Exception e) {
            log.error("Error happened",e);
            throw e;
        }
    }

    @Override
    public void onEvent(Event event) {
        if (event.getType() == EventType.REGISTER) {
            log.info("caught event register");
            long id = setIdToUser(event);
            sendToUser(event, id);
            log.info("Everything alright");
        }
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean b) {

    }

    @Override
    public void close() {

    }
}
