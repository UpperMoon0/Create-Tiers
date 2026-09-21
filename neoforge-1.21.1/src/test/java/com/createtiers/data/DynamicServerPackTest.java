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
                new Tier("basic", 256, 1024));
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
        ResourceLocation axeTag = ResourceLocation.fromNamespaceAndPath(
                "minecraft", "tags/block/mineable/axe.json");
        ResourceLocation safeNbtTag = ResourceLocation.fromNamespaceAndPath(
                "create", "tags/block/safe_nbt.json");
        ResourceLocation gearboxLoot = ResourceLocation.fromNamespaceAndPath(
                "createtiers", "loot_table/blocks/gearbox_basic.json");
        ResourceLocation defaultUpgrade = ResourceLocation.fromNamespaceAndPath(
                "createtiers", "recipe/tier_upgrade/createtiers/basic/create/large_water_wheel.json");
        ResourceLocation clutchLoot = ResourceLocation.fromNamespaceAndPath(
                "createtiers", "loot_table/blocks/clutch_basic.json");
        ResourceLocation girderLoot = ResourceLocation.fromNamespaceAndPath(
                "createtiers", "loot_table/blocks/metal_girder_encased_shaft_basic.json");

        var tagSupplier = pack.getResource(PackType.SERVER_DATA, pickaxeTag);
        var axeTagSupplier = pack.getResource(PackType.SERVER_DATA, axeTag);
        var safeNbtSupplier = pack.getResource(PackType.SERVER_DATA, safeNbtTag);
        var lootSupplier = pack.getResource(PackType.SERVER_DATA, gearboxLoot);
        var recipeSupplier = pack.getResource(PackType.SERVER_DATA, defaultUpgrade);
        var clutchLootSupplier = pack.getResource(PackType.SERVER_DATA, clutchLoot);
        var girderLootSupplier = pack.getResource(PackType.SERVER_DATA, girderLoot);

        assertNotNull(tagSupplier);
        assertNotNull(axeTagSupplier);
        assertNotNull(safeNbtSupplier);
        assertNotNull(lootSupplier);
        assertNotNull(recipeSupplier);
        assertNotNull(clutchLootSupplier);
        assertNotNull(girderLootSupplier);
        String tagJson = new String(tagSupplier.get().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(tagJson.contains("createtiers:gearbox_basic"));
        assertTrue(tagJson.contains("createtiers:clutch_basic"));
        assertTrue(tagJson.contains("createtiers:rotation_speed_controller_basic"));
        assertTrue(tagJson.contains("createtiers:metal_girder_encased_shaft_basic"));
        String axeTagJson = new String(axeTagSupplier.get().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(axeTagJson.contains("createtiers:cogwheel_basic"));
        assertTrue(axeTagJson.contains("createtiers:gearbox_basic"));
        assertTrue(axeTagJson.contains("createtiers:clutch_basic"));
        assertTrue(axeTagJson.contains("createtiers:gearshift_basic"));
        assertTrue(axeTagJson.contains("createtiers:encased_chain_drive_basic"));
        assertTrue(axeTagJson.contains("createtiers:adjustable_chain_gearshift_basic"));
        assertTrue(axeTagJson.contains("createtiers:rotation_speed_controller_basic"));
        assertFalse(axeTagJson.contains("createtiers:shaft_basic"));
        assertFalse(axeTagJson.contains("createtiers:metal_girder_encased_shaft_basic"));

        String safeNbtJson = new String(safeNbtSupplier.get().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(safeNbtJson.contains("createtiers:rotation_speed_controller_basic"));
        assertTrue(new String(lootSupplier.get().readAllBytes(), StandardCharsets.UTF_8)
                .contains("createtiers:gearbox_basic"));
        String clutchJson = new String(clutchLootSupplier.get().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(clutchJson.contains("createtiers:clutch_basic"));
        String girderJson = new String(girderLootSupplier.get().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(girderJson.contains("create:metal_girder"));
        assertTrue(girderJson.contains("createtiers:shaft_basic"));

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
        assertTrue(listedTags.contains(axeTag));
        Set<ResourceLocation> listedCreateTags = new HashSet<>();
        pack.listResources(PackType.SERVER_DATA, "create", "tags", (location, supplier) -> listedCreateTags.add(location));
        assertTrue(listedCreateTags.contains(safeNbtTag));
        assertTrue(pack.getNamespaces(PackType.SERVER_DATA).contains("create"));
        assertTrue(listedLoot.contains(gearboxLoot));
        assertTrue(listedRecipes.contains(defaultUpgrade));
        assertNull(pack.getResource(PackType.CLIENT_RESOURCES, pickaxeTag));
        assertNull(pack.getResource(PackType.SERVER_DATA,
                ResourceLocation.fromNamespaceAndPath("othermod", "tags/block/test.json")));
    }
}
