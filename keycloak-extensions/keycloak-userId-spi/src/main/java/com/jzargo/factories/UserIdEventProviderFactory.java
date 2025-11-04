package com.jzargo.factories;

import com.jzargo.events.UserIdEventProvider;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class UserIdEventProviderFactory implements EventListenerProviderFactory {

    public static final String USER_ID_EVENT_LISTENER = "user-id-event-listener";

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new UserIdEventProvider(session);
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return USER_ID_EVENT_LISTENER;
    }
}
