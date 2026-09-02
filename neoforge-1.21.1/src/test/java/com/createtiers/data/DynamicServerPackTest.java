package com.createtiers.data;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import com.createtiers.Compat;
import com.createtiers.api.Tier;
import com.createtiers.api.TierRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DynamicServerPackTest {

    @BeforeEach
    void setUp() {
        Compat.init(ResourceLocation::fromNamespaceAndPath);
        TierRegistry.clear();
        DynamicServerPack.clear();
        TierRegistry.register(ResourceLocation.fromNamespaceAndPath("createtiers", "basic"),
                new Tier(1, "basic", 256, 1024));
    }

    @AfterEach
    void tearDown() {
        DynamicServerPack.clear();
        TierRegistry.clear();
    }

    @Test
    void doesNotSnapshotPartiallyRegisteredTiers() {
        DynamicServerPack.generateResources();
        assertFalse(DynamicServerPack.isResourcesGenerated());
        assertTrue(DynamicServerPack.getTags().isEmpty());
        assertTrue(DynamicServerPack.getLootTables().isEmpty());
    }

    @Test
    void exposesMinecraft121ResourcesThroughPackApiAfterFreeze() throws Exception {
        TierRegistry.freeze();
        DynamicServerPack pack = new DynamicServerPack();

        ResourceLocation pickaxeTag = ResourceLocation.fromNamespaceAndPath(
                "minecraft", "tags/block/mineable/pickaxe.json");
        ResourceLocation gearboxLoot = ResourceLocation.fromNamespaceAndPath(
                "createtiers", "loot_table/blocks/gearbox_basic.json");

        var tagSupplier = pack.getResource(PackType.SERVER_DATA, pickaxeTag);
        var lootSupplier = pack.getResource(PackType.SERVER_DATA, gearboxLoot);

        assertNotNull(tagSupplier);
        assertNotNull(lootSupplier);
        assertTrue(new String(tagSupplier.get().readAllBytes(), StandardCharsets.UTF_8)
                .contains("createtiers:gearbox_basic"));
        assertTrue(new String(lootSupplier.get().readAllBytes(), StandardCharsets.UTF_8)
                .contains("createtiers:gearbox_basic"));
        assertTrue(DynamicServerPack.isResourcesGenerated());

        Set<ResourceLocation> listedTags = new HashSet<>();
        Set<ResourceLocation> listedLoot = new HashSet<>();
        pack.listResources(PackType.SERVER_DATA, "minecraft", "tags", (location, supplier) -> listedTags.add(location));
        pack.listResources(PackType.SERVER_DATA, "createtiers", "loot_table", (location, supplier) -> listedLoot.add(location));

        assertTrue(listedTags.contains(pickaxeTag));
        assertTrue(listedLoot.contains(gearboxLoot));
        assertNull(pack.getResource(PackType.CLIENT_RESOURCES, pickaxeTag));
        assertNull(pack.getResource(PackType.SERVER_DATA,
                ResourceLocation.fromNamespaceAndPath("othermod", "tags/block/test.json")));
    }
}
