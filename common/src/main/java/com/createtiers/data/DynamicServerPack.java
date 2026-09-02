package com.createtiers.data;

import com.google.gson.JsonObject;
import com.createtiers.Compat;
import com.createtiers.CreateTiers;
import com.createtiers.api.Tier;
import com.createtiers.api.TierRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Dynamic Forge 1.20.1 server data for registered tier components. */
public class DynamicServerPack implements PackResources {

    private static final String NAME = "createtiers:dynamic_server";
    private static final Map<ResourceLocation, JsonObject> TAGS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, JsonObject> LOOT_TABLES = new ConcurrentHashMap<>();
    private static volatile boolean resourcesGenerated = false;

    private final PackMetadataSection metadata;

    public DynamicServerPack() {
        this.metadata = new PackMetadataSection(
                net.minecraft.network.chat.Component.literal("Dynamic server data for Create Tiers"),
                CreateTiers.SERVER_PACK_FORMAT);
    }

    public static void generateResources() {
        if (resourcesGenerated) {
            return;
        }
        if (TierRegistry.size() == 0) {
            CreateTiers.LOGGER.debug("TierRegistry is empty - skipping server data generation for now");
            return;
        }

        TAGS.clear();
        LOOT_TABLES.clear();
        generateMiningTags();
        generateLootTables();
        resourcesGenerated = true;
        CreateTiers.LOGGER.info("Dynamic server data generated. Tags: {}, Loot Tables: {}", TAGS.size(), LOOT_TABLES.size());
    }

    private static void ensureResourcesGenerated() {
        if (!resourcesGenerated && TierRegistry.size() > 0) {
            synchronized (DynamicServerPack.class) {
                if (!resourcesGenerated && TierRegistry.size() > 0) {
                    generateResources();
                }
            }
        }
    }

    private static void generateMiningTags() {
        JsonObject mineablePickaxe = new JsonObject();
        mineablePickaxe.addProperty("replace", false);
        var blocks = new com.google.gson.JsonArray();

        for (Tier tier : TierRegistry.getAllTiers()) {
            addTierBlocks(blocks, tier);
        }

        mineablePickaxe.add("values", blocks);
        TAGS.put(Compat.rl("minecraft", "tags/blocks/mineable/pickaxe"), mineablePickaxe);

        for (Tier tier : TierRegistry.getAllTiers()) {
            JsonObject shaftTag = new JsonObject();
            shaftTag.addProperty("replace", false);
            var values = new com.google.gson.JsonArray();
            values.add("createtiers:shaft_" + tier.getName());
            shaftTag.add("values", values);
            TAGS.put(Compat.rl(CreateTiers.MOD_ID, "tags/blocks/" + tier.getName() + "_shafts"), shaftTag);
        }
    }

    private static void addTierBlocks(com.google.gson.JsonArray blocks, Tier tier) {
        String name = tier.getName();
        blocks.add("createtiers:shaft_" + name);
        blocks.add("createtiers:cogwheel_" + name);
        blocks.add("createtiers:large_cogwheel_" + name);
        blocks.add("createtiers:andesite_encased_shaft_" + name);
        blocks.add("createtiers:brass_encased_shaft_" + name);
        blocks.add("createtiers:andesite_encased_cogwheel_" + name);
        blocks.add("createtiers:brass_encased_cogwheel_" + name);
        blocks.add("createtiers:andesite_encased_large_cogwheel_" + name);
        blocks.add("createtiers:brass_encased_large_cogwheel_" + name);
        blocks.add("createtiers:gearbox_" + name);
    }

    private static void generateLootTables() {
        for (Tier tier : TierRegistry.getAllTiers()) {
            String name = tier.getName();
            generateBlockLootTable("shaft_" + name);
            generateBlockLootTable("cogwheel_" + name);
            generateBlockLootTable("large_cogwheel_" + name);
            generateBlockLootTable("gearbox_" + name);

            generateEncasedBlockLootTable("andesite_encased_shaft_" + name, "shaft_" + name);
            generateEncasedBlockLootTable("brass_encased_shaft_" + name, "shaft_" + name);
            generateEncasedBlockLootTable("andesite_encased_cogwheel_" + name, "cogwheel_" + name);
            generateEncasedBlockLootTable("brass_encased_cogwheel_" + name, "cogwheel_" + name);
            generateEncasedBlockLootTable("andesite_encased_large_cogwheel_" + name, "large_cogwheel_" + name);
            generateEncasedBlockLootTable("brass_encased_large_cogwheel_" + name, "large_cogwheel_" + name);
        }
    }

    private static void generateBlockLootTable(String blockName) {
        LOOT_TABLES.put(
                Compat.rl(CreateTiers.MOD_ID, "loot_tables/blocks/" + blockName),
                createSingleDropLootTable(CreateTiers.MOD_ID + ":" + blockName));
    }

    private static void generateEncasedBlockLootTable(String encasedBlockName, String dropBlockName) {
        LOOT_TABLES.put(
                Compat.rl(CreateTiers.MOD_ID, "loot_tables/blocks/" + encasedBlockName),
                createSingleDropLootTable(CreateTiers.MOD_ID + ":" + dropBlockName));
    }

    private static JsonObject createSingleDropLootTable(String itemId) {
        JsonObject lootTable = new JsonObject();
        lootTable.addProperty("type", "minecraft:block");

        var pools = new com.google.gson.JsonArray();
        var pool = new JsonObject();
        pool.addProperty("rolls", 1);
        pool.addProperty("bonus_rolls", 0);

        var entries = new com.google.gson.JsonArray();
        var entry = new JsonObject();
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", itemId);
        entries.add(entry);
        pool.add("entries", entries);

        var conditions = new com.google.gson.JsonArray();
        var condition = new JsonObject();
        condition.addProperty("condition", "minecraft:survives_explosion");
        conditions.add(condition);
        pool.add("conditions", conditions);

        pools.add(pool);
        lootTable.add("pools", pools);
        return lootTable;
    }

    public static void clear() {
        TAGS.clear();
        LOOT_TABLES.clear();
        resourcesGenerated = false;
    }

    public static boolean isResourcesGenerated() {
        return resourcesGenerated;
    }

    public static Map<ResourceLocation, JsonObject> getTags() {
        return TAGS;
    }

    public static Map<ResourceLocation, JsonObject> getLootTables() {
        return LOOT_TABLES;
    }

    @Override
    public @NotNull String packId() {
        return NAME;
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getRootResource(String... elements) {
        return null;
    }

    @Override
    public @Nullable IoSupplier<InputStream> getResource(@NotNull PackType type, @NotNull ResourceLocation location) {
        if (type != PackType.SERVER_DATA) {
            return null;
        }
        ensureResourcesGenerated();

        String namespace = location.getNamespace();
        String path = location.getPath();
        if (path.equals(PackResources.PACK_META)) {
            JsonObject packJson = new JsonObject();
            JsonObject packMeta = new JsonObject();
            packMeta.addProperty("description", "Dynamic server data for Create Tiers");
            packMeta.addProperty("pack_format", CreateTiers.SERVER_PACK_FORMAT);
            packJson.add("pack", packMeta);
            return () -> stream(packJson);
        }

        if (!namespace.equals(CreateTiers.MOD_ID) && !namespace.equals("minecraft")) {
            return null;
        }

        if (path.startsWith("tags/") && path.endsWith(".json")) {
            JsonObject json = TAGS.get(Compat.rl(namespace, path.substring(0, path.length() - 5)));
            return json == null ? null : () -> stream(json);
        }
        if (namespace.equals(CreateTiers.MOD_ID) && path.startsWith("loot_tables/") && path.endsWith(".json")) {
            JsonObject json = LOOT_TABLES.get(Compat.rl(namespace, path.substring(0, path.length() - 5)));
            return json == null ? null : () -> stream(json);
        }
        return null;
    }

    @Override
    public void listResources(@NotNull PackType type, @NotNull String namespace, @NotNull String path,
            @NotNull ResourceOutput output) {
        if (type != PackType.SERVER_DATA) {
            return;
        }
        ensureResourcesGenerated();

        if (!namespace.equals(CreateTiers.MOD_ID) && !namespace.equals("minecraft")) {
            return;
        }

        if (path.startsWith("tags/") || path.equals("tags")) {
            TAGS.forEach((loc, json) -> {
                if (loc.getNamespace().equals(namespace) && loc.getPath().startsWith(path)) {
                    output.accept(Compat.withSuffix(loc, ".json"), () -> stream(json));
                }
            });
        }
        if (namespace.equals(CreateTiers.MOD_ID) && (path.startsWith("loot_tables/") || path.equals("loot_tables"))) {
            LOOT_TABLES.forEach((loc, json) -> {
                if (loc.getPath().startsWith(path)) {
                    output.accept(Compat.withSuffix(loc, ".json"), () -> stream(json));
                }
            });
        }
    }

    private static ByteArrayInputStream stream(JsonObject json) {
        return new ByteArrayInputStream(json.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public @NotNull Set<String> getNamespaces(@NotNull PackType type) {
        return type == PackType.SERVER_DATA ? Set.of(CreateTiers.MOD_ID, "minecraft") : Set.of();
    }

    @SuppressWarnings("unchecked")
    @Override
    public @Nullable <T> T getMetadataSection(@NotNull MetadataSectionSerializer<T> serializer) throws IOException {
        return serializer == PackMetadataSection.TYPE ? (T) metadata : null;
    }

    @Override
    public void close() {
    }
}
