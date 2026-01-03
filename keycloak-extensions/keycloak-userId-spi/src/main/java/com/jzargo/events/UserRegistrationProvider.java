package com.jzargo.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jzargo.entities.IncrementedValue;
import com.jzargo.entities.Outbox;
import com.jzargo.entities.Status;
import com.jzargo.messaging.UserRegisterRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.slf4j.LoggerFactory;


public class UserRegistrationProvider implements EventListenerProvider {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(UserRegistrationProvider.class);
    private final KeycloakSession session;


    private final ObjectMapper objectMapper = new ObjectMapper();

    public UserRegistrationProvider(KeycloakSession session) {
        this.session = session;
    }

    private void setIdToUser(Event event, EntityManager em) {
        try {

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

        } catch (Exception e) {
            log.error("Error happened",e);
            throw e;
        }
    }

    @Override
    @Transactional
    public void onEvent(Event event) {
        if (event.getType() == EventType.REGISTER) {
            EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
            log.info("caught event register");
            setIdToUser(event, em);
            try {
                createOutboxRecord(event, em);
            } catch (JsonProcessingException e) {
                log.error("Registration provider threw exception while it parsed a payload for outbox {}", e.getMessage());
                throw new RuntimeException(e);
            }
            log.info("Everything alright");
        }
    }

    private void createOutboxRecord(Event event, EntityManager em) throws JsonProcessingException {

        RealmModel realm = session.realms().getRealm(event.getRealmId());
        UserModel user = session.users().getUserById(realm, event.getUserId());

        String username = user.getUsername();
        long userId = Long.parseLong(user.getFirstAttribute("user_id"));
        String email = user.getEmail();

        UserRegisterRequest urr = new UserRegisterRequest(
                userId, username, email);

        Outbox build = Outbox.builder()
                .payload(
                        objectMapper.writeValueAsString(urr)
                )
                .aggregateId(Long.toString(userId))
                .aggregateType(UserRegisterRequest.class
                        .getSimpleName())
                .status(Status.UNPUBLISHED)
                .eventType(EventType.REGISTER)
                .build();

        em.persist(build);
        log.debug("Saved successfully outbox: {}", build);
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean b) {

    }

    @Override
    public void close() {

    }
}
