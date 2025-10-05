package com.jzargo.events;

import com.jzargo.entities.IncrementedValue;
import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;

import java.util.Collections;
import java.util.List;

public class EntityProvider implements JpaEntityProvider {
    @Override
    public List<Class<?>> getEntities() {
        return Collections.singletonList(IncrementedValue.class);
    }

    @Override
    public String getChangelogLocation() {
        return "db/changelog/db-changelog.xml";
    }

    @Override
    public String getFactoryId() {
        return "";
    }

    @Override
    public void close() {

    }
}
