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
import com.createtiers.api.TierUpgradeRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DynamicServerPackTest {

    @BeforeEach
    void setUp() {
        Compat.init(ResourceLocation::fromNamespaceAndPath);
        TierUpgradeRegistry.clear();
        TierRegistry.clear();
        DynamicServerPack.clear();
        TierRegistry.register(ResourceLocation.fromNamespaceAndPath("createtiers", "basic"),
                new Tier(1, "basic", 256, 1024));
        TierUpgradeRegistry.register(
                ResourceLocation.fromNamespaceAndPath("create", "large_water_wheel"),
                ResourceLocation.fromNamespaceAndPath("createtiers", "basic"),
                true);
    }

    @AfterEach
    void tearDown() {
        DynamicServerPack.clear();
        TierUpgradeRegistry.clear();
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
        TierUpgradeRegistry.freeze();
        DynamicServerPack pack = new DynamicServerPack();

        ResourceLocation pickaxeTag = ResourceLocation.fromNamespaceAndPath(
                "minecraft", "tags/block/mineable/pickaxe.json");
        ResourceLocation gearboxLoot = ResourceLocation.fromNamespaceAndPath(
                "createtiers", "loot_table/blocks/gearbox_basic.json");
        ResourceLocation defaultUpgrade = ResourceLocation.fromNamespaceAndPath(
                "createtiers", "recipe/tier_upgrade/createtiers/basic/create/large_water_wheel.json");

        var tagSupplier = pack.getResource(PackType.SERVER_DATA, pickaxeTag);
        var lootSupplier = pack.getResource(PackType.SERVER_DATA, gearboxLoot);
        var recipeSupplier = pack.getResource(PackType.SERVER_DATA, defaultUpgrade);

        assertNotNull(tagSupplier);
        assertNotNull(lootSupplier);
        assertNotNull(recipeSupplier);
        assertTrue(new String(tagSupplier.get().readAllBytes(), StandardCharsets.UTF_8)
                .contains("createtiers:gearbox_basic"));
        assertTrue(new String(lootSupplier.get().readAllBytes(), StandardCharsets.UTF_8)
                .contains("createtiers:gearbox_basic"));
        String recipeJson = new String(recipeSupplier.get().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(recipeJson.contains("createtiers:tier_upgrade"));
        assertTrue(recipeJson.contains("createtiers:shaft_basic"));
        assertTrue(DynamicServerPack.isResourcesGenerated());

        Set<ResourceLocation> listedTags = new HashSet<>();
        Set<ResourceLocation> listedLoot = new HashSet<>();
        Set<ResourceLocation> listedRecipes = new HashSet<>();
        pack.listResources(PackType.SERVER_DATA, "minecraft", "tags", (location, supplier) -> listedTags.add(location));
        pack.listResources(PackType.SERVER_DATA, "createtiers", "loot_table", (location, supplier) -> listedLoot.add(location));
        pack.listResources(PackType.SERVER_DATA, "createtiers", "recipe", (location, supplier) -> listedRecipes.add(location));

        assertTrue(listedTags.contains(pickaxeTag));
        assertTrue(listedLoot.contains(gearboxLoot));
        assertTrue(listedRecipes.contains(defaultUpgrade));
        assertNull(pack.getResource(PackType.CLIENT_RESOURCES, pickaxeTag));
        assertNull(pack.getResource(PackType.SERVER_DATA,
                ResourceLocation.fromNamespaceAndPath("othermod", "tags/block/test.json")));
    }
}
