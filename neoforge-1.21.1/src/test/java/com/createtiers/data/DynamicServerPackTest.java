package com.createtiers.data;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.createtiers.Compat;
import com.createtiers.api.Tier;
import com.createtiers.api.TierRegistry;
import net.minecraft.resources.ResourceLocation;
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
    void usesMinecraft121SingularDataPathsAndIncludesGearbox() {
        DynamicServerPack.generateResources();

        ResourceLocation pickaxeTag = ResourceLocation.fromNamespaceAndPath(
                "minecraft", "tags/block/mineable/pickaxe");
        ResourceLocation gearboxLoot = ResourceLocation.fromNamespaceAndPath(
                "createtiers", "loot_table/blocks/gearbox_basic");

        assertTrue(DynamicServerPack.getTags().containsKey(pickaxeTag));
        assertTrue(DynamicServerPack.getTags().get(pickaxeTag).toString().contains("createtiers:gearbox_basic"));
        assertTrue(DynamicServerPack.getLootTables().containsKey(gearboxLoot));
    }
}
