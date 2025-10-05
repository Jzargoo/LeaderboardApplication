package com.jzargo.events;

import com.jzargo.entities.IncrementedValue;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.List;
import java.util.logging.Logger;

import static org.keycloak.events.EventType.REGISTER;

public class UserIdEventProvider implements EventListenerProvider {
    private final KeycloakSession session;
    private static final Logger log =Logger.getLogger(String.valueOf(UserIdEventProvider.class));
    public UserIdEventProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void onEvent(Event event) {
        if(event.getType() == REGISTER){
            log.info("caught event register");
            session.getTransactionManager().begin();
            try{
                EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
                IncrementedValue incrementedValue = em.find(IncrementedValue.class, IncrementedValue.USER_ID_COUNTER, LockModeType.PESSIMISTIC_WRITE);
                if(incrementedValue == null) {
                    log.info("This is first register event");
                    incrementedValue = new IncrementedValue();
                    incrementedValue.setName(IncrementedValue.USER_ID_COUNTER);
                }
                Long l = incrementedValue.incrementValue();
                log.info("Incremented value was found and changed to " + l);
                RealmModel realm = session.realms().getRealm(event.getRealmId());
                UserModel user = session.users().getUserById(realm, event.getUserId());
                user.setAttribute("user_id", List.of(l.toString()));
                log.info("attribute was added successfully");
                em.persist(incrementedValue);
            } catch(Exception e) {
                session.getTransactionManager().rollback();
                log.warning("Error was happened " + e);
                throw e;
            }
            session.getTransactionManager().commit();
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
