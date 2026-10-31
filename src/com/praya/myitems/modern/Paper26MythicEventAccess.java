package com.praya.myitems.modern;

import java.util.List;

/**
 * @deprecated Use the LyMeowzItem MythicMobs event API adapter.
 * The exact historical public constructor and methods are preserved.
 */
@Deprecated
public final class Paper26MythicEventAccess {
    private final com.lymeowz.item.integration.MythicEventAccess delegate;

    public Paper26MythicEventAccess(final Class<?> eventType)
            throws ReflectiveOperationException {
        this.delegate = new com.lymeowz.item.integration.MythicEventAccess(eventType);
    }

    public Class<?> entityType() {
        return delegate.entityType();
    }

    public Object entity(final Object event) throws ReflectiveOperationException {
        return delegate.entity(event);
    }

    public List<String> configLines(final Object event, final String key)
            throws ReflectiveOperationException {
        return delegate.configLines(event, key);
    }
}
