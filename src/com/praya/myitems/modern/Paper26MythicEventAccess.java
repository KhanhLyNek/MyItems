package com.praya.myitems.modern;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Precomputed public MythicMobs 5 event/config ABI. Constructed once per
 * registered event type, rather than performing four getMethod() lookups for
 * every spawn/death. Has no dependency on MythicMobs or Bukkit at class load.
 */
public final class Paper26MythicEventAccess {
    private final Method getEntity;
    private final Method getMob;
    private final Method getConfig;
    private final Method getLines;

    public Paper26MythicEventAccess(final Class<?> eventType)
            throws ReflectiveOperationException {
        this.getEntity = eventType.getMethod("getEntity");
        this.getMob = eventType.getMethod("getMobType");
        this.getConfig = getMob.getReturnType().getMethod("getConfig");
        this.getLines = getConfig.getReturnType().getMethod("getStringList", String.class);
        if (!List.class.isAssignableFrom(getLines.getReturnType())) {
            throw new NoSuchMethodException("MythicConfig#getStringList must return List");
        }
    }

    public Class<?> entityType() {
        return getEntity.getReturnType();
    }

    public Object entity(final Object event) throws ReflectiveOperationException {
        return getEntity.invoke(event);
    }

    /**
     * Return only literal user YAML entries. Never expand named drop tables:
     * the modern MythicMobs loot generation system owns those tables.
     */
    public List<String> configLines(final Object event, final String key)
            throws ReflectiveOperationException {
        if (event == null || key == null || key.isBlank()) return List.of();
        final Object mob = getMob.invoke(event);
        if (mob == null) return List.of();
        final Object config = getConfig.invoke(mob);
        if (config == null) return List.of();
        final Object result = getLines.invoke(config, key);
        if (!(result instanceof List<?> values) || values.isEmpty()) return List.of();
        final List<String> lines = new ArrayList<>(values.size());
        for (final Object value : values) {
            if (value instanceof String text) lines.add(text);
        }
        return lines;
    }
}
