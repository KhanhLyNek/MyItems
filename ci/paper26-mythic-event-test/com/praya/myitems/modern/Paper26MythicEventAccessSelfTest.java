package com.praya.myitems.modern;

import java.util.ArrayList;
import java.util.List;

public final class Paper26MythicEventAccessSelfTest {
    public interface MobApi { ConfigApi getConfig(); }
    public interface ConfigApi { List<?> getStringList(String key); }
    public static final class Event {
        private final MobApi mob;
        public Event(MobApi mob) { this.mob = mob; }
        public Object getEntity() { return "test-entity"; }
        public MobApi getMobType() { return mob; }
    }
    // Mimic Mythic's nonpublic internal implementation classes.
    private static final class HiddenMob implements MobApi {
        private final ConfigApi config;
        HiddenMob(ConfigApi config) { this.config = config; }
        public ConfigApi getConfig() { return config; }
    }
    private static final class HiddenConfig implements ConfigApi {
        private final List<?> values;
        HiddenConfig(List<?> values) { this.values = values; }
        public List<?> getStringList(String key) { return values; }
    }
    public static final class MissingMob { public Object getEntity() { return null; } }
    public interface BadConfig { Object getStringList(String path); }
    public interface BadMob { BadConfig getConfig(); }
    public static final class BadReturn {
        public Object getEntity() { return null; }
        public BadMob getMobType() { return null; }
    }
    private static int checks;
    private static void check(boolean result, String msg) {
        checks++;
        if (!result) throw new AssertionError(msg);
    }
    public static void main(String[] args) throws Exception {
        var access = new Paper26MythicEventAccess(Event.class);
        var canonical = new com.lymeowz.item.integration.MythicEventAccess(Event.class);
        check(canonical.entityType() == access.entityType(),
                "new/old event descriptor agrees");
        check(canonical.entity(new Event(null)).equals(access.entity(new Event(null))),
                "new/old event entity lookup agrees");
        check(canonical.configLines(new Event(null), "Drops")
                .equals(access.configLines(new Event(null), "Drops")),
                "new/old empty mob semantics agree");
        check(access.entityType() == Object.class, "entity return type preserved");
        check("test-entity".equals(access.entity(new Event(null))), "cached entity method");
        check(access.configLines(null, "Drops").isEmpty(), "null event");
        check(access.configLines(new Event(null), "Drops").isEmpty(), "null mob");
        check(access.configLines(new Event(new HiddenMob(null)), "Drops").isEmpty(), "null config");
        check(access.configLines(new Event(new HiddenMob(new HiddenConfig(null))), "Drops").isEmpty(), "null lines");
        check(access.configLines(new Event(new HiddenMob(new HiddenConfig(List.of()))), "Drops").isEmpty(), "empty lines");

        List<Object> raw = new ArrayList<>();
        raw.add("MyItems Custom Sword 1 0.25");
        raw.add(42);
        raw.add(null);
        raw.add("HEAD MyItems Generator Sword 2");
        Event event = new Event(new HiddenMob(new HiddenConfig(raw)));
        List<String> lines = access.configLines(event, "Equipment");
        check(lines.equals(canonical.configLines(event, "Equipment")),
                "old/new MythicMobs config raw string extraction parity");
        check(lines.size() == 2, "only raw string entries");
        check("MyItems Custom Sword 1 0.25".equals(lines.get(0)), "legacy chance kept");
        check("HEAD MyItems Generator Sword 2".equals(lines.get(1)), "legacy equipment kept");
        raw.clear();
        check(lines.size() == 2, "provider list mutation does not affect snapshot");
        check(access.configLines(event, "").isEmpty(), "blank key ignored");

        try {
            new Paper26MythicEventAccess(MissingMob.class);
            throw new AssertionError("missing getMobType must fail early");
        } catch (NoSuchMethodException expected) { checks++; }
        try {
            new Paper26MythicEventAccess(BadReturn.class);
            throw new AssertionError("unexpected config list return type must fail early");
        } catch (NoSuchMethodException expected) { checks++; }
        try {
            new com.lymeowz.item.integration.MythicEventAccess(MissingMob.class);
            throw new AssertionError("new event API must reject missing Mythic methods");
        } catch (NoSuchMethodException expected) { checks++; }

        System.out.println("PASS: " + checks + " cached MythicMobs event/config ABI checks");
    }
}
