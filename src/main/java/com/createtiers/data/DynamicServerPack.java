package com.createtiers.data;

import com.google.gson.JsonObject;
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

/**
 * A dynamic server-side resource pack that generates tags and loot tables at runtime.
 * This ensures dynamically registered tiered blocks have proper mining tags and loot tables.
 */
public class DynamicServerPack implements PackResources {

    private static final String NAME = "createtiers:dynamic_server";
    
    // Storage for dynamically generated server resources
    private static final Map<ResourceLocation, JsonObject> TAGS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, JsonObject> LOOT_TABLES = new ConcurrentHashMap<>();
    
    // Track if resources have been generated
    private static volatile boolean resourcesGenerated = false;
    
    private final PackMetadataSection metadata;
    
    public DynamicServerPack() {
        this.metadata = new PackMetadataSection(
            net.minecraft.network.chat.Component.literal("Dynamic server data for Create Tiers"), 
            10 // 1.20.1 pack format for SERVER_DATA is 10
        );
    }
    
    /**
     * Generate all server-side resources for registered tiers
     */
    public static void generateResources() {
        if (resourcesGenerated) {
            return;
        }
        
        // Don't generate if no tiers are registered yet (KubeJS may not have run)
        if (TierRegistry.size() == 0) {
            CreateTiers.LOGGER.debug("TierRegistry is empty - skipping server data generation for now");
            return;
        }
        
        CreateTiers.LOGGER.info("Generating Create-Tiers dynamic server data...");
        
        generateMiningTags();
        generateLootTables();
        
        resourcesGenerated = true;
        CreateTiers.LOGGER.info("Dynamic server data generated. Tags: {}, Loot Tables: {}", 
            TAGS.size(), LOOT_TABLES.size());
    }
    
    /**
     * Ensure resources are generated before attempting to retrieve them.
     * This is called lazily when resources are requested.
     */
    private static void ensureResourcesGenerated() {
        if (!resourcesGenerated && TierRegistry.size() > 0) {
            synchronized (DynamicServerPack.class) {
                if (!resourcesGenerated && TierRegistry.size() > 0) {
                    generateResources();
                }
            }
        }
    }
    
    /**
     * Generate block tags for mining with pickaxe
     */
    private static void generateMiningTags() {
        JsonObject mineablePickaxe = new JsonObject();
        mineablePickaxe.addProperty("replace", false);
        var blocksArray = new com.google.gson.JsonArray();
        
        for (Tier tier : TierRegistry.getAllTiers()) {
            // Add shaft
            blocksArray.add("createtiers:shaft_" + tier.getName());
            // Add cogwheels
            blocksArray.add("createtiers:cogwheel_" + tier.getName());
            blocksArray.add("createtiers:large_cogwheel_" + tier.getName());
        }
        
        mineablePickaxe.add("values", blocksArray);
        // Put in minecraft namespace for vanilla mining tag
        TAGS.put(
            ResourceLocation.fromNamespaceAndPath("minecraft", "tags/blocks/mineable/pickaxe"),
            mineablePickaxe
        );
        
        // Also generate individual tier tags for shafts (useful for recipes)
        for (Tier tier : TierRegistry.getAllTiers()) {
            JsonObject tierShaftTag = new JsonObject();
            tierShaftTag.addProperty("replace", false);
            var tierArray = new com.google.gson.JsonArray();
            tierArray.add("createtiers:shaft_" + tier.getName());
            tierShaftTag.add("values", tierArray);
            TAGS.put(
                ResourceLocation.fromNamespaceAndPath(CreateTiers.MOD_ID, "tags/blocks/" + tier.getName() + "_shafts"),
                tierShaftTag
            );
        }
        
        CreateTiers.LOGGER.debug("Generated mining tags for {} tiers", TierRegistry.size());
    }
    
    /**
     * Generate loot tables for all tiered blocks
     */
    private static void generateLootTables() {
        for (Tier tier : TierRegistry.getAllTiers()) {
            // Shaft loot table
            generateBlockLootTable("shaft_" + tier.getName());
            // Cogwheel loot table
            generateBlockLootTable("cogwheel_" + tier.getName());
            // Large cogwheel loot table
            generateBlockLootTable("large_cogwheel_" + tier.getName());
        }
        
        CreateTiers.LOGGER.debug("Generated loot tables for {} tiers", TierRegistry.size());
    }
    
    /**
     * Generate a simple block loot table that drops the item
     */
    private static void generateBlockLootTable(String blockName) {
        JsonObject lootTable = new JsonObject();
        lootTable.addProperty("type", "minecraft:block");
        
        var pools = new com.google.gson.JsonArray();
        var pool = new JsonObject();
        pool.addProperty("rolls", 1);
        pool.addProperty("bonus_rolls", 0);
        
        var entries = new com.google.gson.JsonArray();
        var entry = new JsonObject();
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", CreateTiers.MOD_ID + ":" + blockName);
        entries.add(entry);
        pool.add("entries", entries);
        
        var conditions = new com.google.gson.JsonArray();
        var condition = new JsonObject();
        condition.addProperty("condition", "minecraft:survives_explosion");
        conditions.add(condition);
        pool.add("conditions", conditions);
        
        pools.add(pool);
        lootTable.add("pools", pools);
        
        LOOT_TABLES.put(
            ResourceLocation.fromNamespaceAndPath(CreateTiers.MOD_ID, "loot_tables/blocks/" + blockName),
            lootTable
        );
    }
    
    /**
     * Clear all dynamic resources (called on reload)
     */
    public static void clear() {
        TAGS.clear();
        LOOT_TABLES.clear();
        resourcesGenerated = false;
    }
    
    public static boolean isResourcesGenerated() {
        return resourcesGenerated;
    }
    
    /**
     * Get all generated tags (for debugging/dumping)
     */
    public static Map<ResourceLocation, JsonObject> getTags() {
        return TAGS;
    }
    
    /**
     * Get all generated loot tables (for debugging/dumping)
     */
    public static Map<ResourceLocation, JsonObject> getLootTables() {
        return LOOT_TABLES;
    }
    
    @Override
    public @NotNull String packId() {
        return NAME;
    }
    
    @Nullable
    @Override
    public IoSupplier<InputStream> getRootResource(String... pElements) {
        return null;
    }
    
    @Override
    public @Nullable IoSupplier<InputStream> getResource(@NotNull PackType type, @NotNull ResourceLocation location) {
        if (type != PackType.SERVER_DATA) {
            return null;
        }
        
        // Ensure resources are generated (lazy generation)
        ensureResourcesGenerated();
        
        String namespace = location.getNamespace();
        String path = location.getPath();
        
        // Handle pack.mcmeta
        if (path.equals(PackResources.PACK_META)) {
            JsonObject packJson = new JsonObject();
            JsonObject packMeta = new JsonObject();
            packMeta.addProperty("description", "Dynamic server data for Create Tiers");
            packMeta.addProperty("pack_format", 10);
            packJson.add("pack", packMeta);
            return () -> new ByteArrayInputStream(packJson.toString().getBytes(StandardCharsets.UTF_8));
        }
        
        // Only handle createtiers and minecraft namespaces
        if (!namespace.equals(CreateTiers.MOD_ID) && !namespace.equals("minecraft")) {
            return null;
        }
        
        // Handle tags
        if (path.startsWith("tags/") && path.endsWith(".json")) {
            String tagPath = path.substring(0, path.length() - 5);
            ResourceLocation tagLoc = ResourceLocation.fromNamespaceAndPath(namespace, tagPath);
            JsonObject tagJson = TAGS.get(tagLoc);
            if (tagJson != null) {
                return () -> new ByteArrayInputStream(tagJson.toString().getBytes(StandardCharsets.UTF_8));
            }
        }
        
        // Handle loot tables (only for createtiers namespace)
        if (namespace.equals(CreateTiers.MOD_ID) && path.startsWith("loot_tables/") && path.endsWith(".json")) {
            String lootPath = path.substring(0, path.length() - 5);
            ResourceLocation lootLoc = ResourceLocation.fromNamespaceAndPath(namespace, lootPath);
            JsonObject lootJson = LOOT_TABLES.get(lootLoc);
            if (lootJson != null) {
                return () -> new ByteArrayInputStream(lootJson.toString().getBytes(StandardCharsets.UTF_8));
            }
        }
        
        return null;
    }
    
    @Override
    public void listResources(@NotNull PackType type, @NotNull String namespace, @NotNull String path,
            @NotNull ResourceOutput output) {
        if (type != PackType.SERVER_DATA) {
            return;
        }
        
        // Ensure resources are generated (lazy generation)
        ensureResourcesGenerated();
        
        // Handle both createtiers and minecraft namespaces
        if (!namespace.equals(CreateTiers.MOD_ID) && !namespace.equals("minecraft")) {
            return;
        }
        
        // List tags (for both namespaces)
        if (path.startsWith("tags/") || path.equals("tags")) {
            TAGS.forEach((loc, json) -> {
                if (loc.getNamespace().equals(namespace) && loc.getPath().startsWith(path)) {
                    ResourceLocation outputLoc = loc.withSuffix(".json");
                    output.accept(outputLoc, 
                        () -> new ByteArrayInputStream(json.toString().getBytes(StandardCharsets.UTF_8)));
                }
            });
        }
        
        // List loot tables (only for createtiers namespace)
        if (namespace.equals(CreateTiers.MOD_ID) && (path.startsWith("loot_tables/") || path.equals("loot_tables"))) {
            LOOT_TABLES.forEach((loc, json) -> {
                if (loc.getPath().startsWith(path)) {
                    ResourceLocation outputLoc = loc.withSuffix(".json");
                    output.accept(outputLoc, 
                        () -> new ByteArrayInputStream(json.toString().getBytes(StandardCharsets.UTF_8)));
                }
            });
        }
    }
    
    @Override
    public @NotNull Set<String> getNamespaces(@NotNull PackType type) {
        return type == PackType.SERVER_DATA ? Set.of(CreateTiers.MOD_ID, "minecraft") : Set.of();
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public @Nullable <T> T getMetadataSection(@NotNull MetadataSectionSerializer<T> serializer) throws IOException {
        if (serializer == PackMetadataSection.TYPE) {
            return (T) this.metadata;
        }
        return null;
    }
    
    @Override
    public void close() {
    }
}
