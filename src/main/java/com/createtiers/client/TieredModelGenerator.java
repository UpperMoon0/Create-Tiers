package com.createtiers.client;

import com.createtiers.CreateTiers;
import com.createtiers.api.Tier;
import com.createtiers.api.TierRegistry;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Collections;

@OnlyIn(Dist.CLIENT)
public class TieredModelGenerator {

    public static void generateAllModels(net.minecraft.server.packs.resources.ResourceManager resourceManager) {
        CreateTiers.LOGGER.info("Generating tiered models with grayscale textures and tinting...");

        DynamicResourcePack.clear();

        Map<ResourceLocation, JsonElement> models = new HashMap<>();
        Map<ResourceLocation, JsonObject> blockstates = new HashMap<>();

        for (Tier tier : TierRegistry.getAllTiers()) {
            generateTierModels(tier, models, blockstates, resourceManager);
        }

        models.forEach(DynamicResourcePack::addModel);
        blockstates.forEach(DynamicResourcePack::addBlockState);

        DynamicResourcePack.addTranslation("en_us", "itemGroup.createtiers", "Create Tiers");

        DynamicResourcePack.addTranslation("en_us", "createtiers.tooltip.tiered_max_rpm", "Maximum Speed");
        DynamicResourcePack.addTranslation("en_us", "createtiers.tooltip.tiered_max_su", "Maximum Stress Capacity");

        DynamicResourcePack.markGenerated();
    }

    public static void generateTierModels(Tier tier, Map<ResourceLocation, JsonElement> models,
            Map<ResourceLocation, JsonObject> blockstates, net.minecraft.server.packs.resources.ResourceManager resourceManager) {
        String tierName = tier.getName();
        generateShaftModels(tierName, models, resourceManager);
        generateCogwheelShaftModel(tierName, models, resourceManager);
        generateCogwheelModels(tierName, false, models, resourceManager);
        generateCogwheelModels(tierName, true, models, resourceManager);
        generateItemModels(tierName, models);
        generateShaftBlockstate(tierName, blockstates);
        generateCogwheelBlockstate(tierName, false, blockstates);
        generateCogwheelBlockstate(tierName, true, blockstates);

        generateEncasedShaftModels(tierName, "andesite", models, resourceManager);
        generateEncasedShaftModels(tierName, "brass", models, resourceManager);
        generateEncasedCogwheelModels(tierName, "andesite", false, models, resourceManager);
        generateEncasedCogwheelModels(tierName, "brass", false, models, resourceManager);
        generateEncasedCogwheelModels(tierName, "andesite", true, models, resourceManager);
        generateEncasedCogwheelModels(tierName, "brass", true, models, resourceManager);

        generateEncasedShaftBlockstate(tierName, "andesite", blockstates);
        generateEncasedShaftBlockstate(tierName, "brass", blockstates);
        generateEncasedCogwheelBlockstate(tierName, "andesite", false, blockstates);
        generateEncasedCogwheelBlockstate(tierName, "brass", false, blockstates);
        generateEncasedCogwheelBlockstate(tierName, "andesite", true, blockstates);
        generateEncasedCogwheelBlockstate(tierName, "brass", true, blockstates);

        generateEncasedItemModels(tierName, models);
        generateTierLanguages(tier);
    }

    private static void generateTierLanguages(Tier tier) {
        String tierName = tier.getName();
        String displayName = tier.getDisplayName();

        DynamicResourcePack.addTranslation("en_us", "block.createtiers.shaft_" + tierName, displayName + " Shaft");
        DynamicResourcePack.addTranslation("en_us", "block.createtiers.cogwheel_" + tierName, displayName + " Cogwheel");
        DynamicResourcePack.addTranslation("en_us", "block.createtiers.large_cogwheel_" + tierName, "Large " + displayName + " Cogwheel");

        DynamicResourcePack.addTranslation("en_us", "block.createtiers.andesite_encased_shaft_" + tierName, "Andesite Encased " + displayName + " Shaft");
        DynamicResourcePack.addTranslation("en_us", "block.createtiers.brass_encased_shaft_" + tierName, "Brass Encased " + displayName + " Shaft");
        DynamicResourcePack.addTranslation("en_us", "block.createtiers.andesite_encased_cogwheel_" + tierName, "Andesite Encased " + displayName + " Cogwheel");
        DynamicResourcePack.addTranslation("en_us", "block.createtiers.brass_encased_cogwheel_" + tierName, "Brass Encased " + displayName + " Cogwheel");
        DynamicResourcePack.addTranslation("en_us", "block.createtiers.andesite_encased_large_cogwheel_" + tierName, "Andesite Encased Large " + displayName + " Cogwheel");
        DynamicResourcePack.addTranslation("en_us", "block.createtiers.brass_encased_large_cogwheel_" + tierName, "Brass Encased Large " + displayName + " Cogwheel");
    }

    private static void generateItemModels(String tierName, Map<ResourceLocation, JsonElement> models) {
        models.put(ResourceLocation.fromNamespaceAndPath(CreateTiers.MOD_ID, "models/item/shaft_" + tierName),
                createParentModel(CreateTiers.MOD_ID + ":block/" + tierName + "/shaft"));
        models.put(ResourceLocation.fromNamespaceAndPath(CreateTiers.MOD_ID, "models/item/cogwheel_" + tierName),
                createParentModel(CreateTiers.MOD_ID + ":block/" + tierName + "/cogwheel"));
        models.put(ResourceLocation.fromNamespaceAndPath(CreateTiers.MOD_ID, "models/item/large_cogwheel_" + tierName),
                createParentModel(CreateTiers.MOD_ID + ":block/" + tierName + "/large_cogwheel"));
    }

    private static void generateEncasedItemModels(String tierName, Map<ResourceLocation, JsonElement> models) {
        models.put(ResourceLocation.fromNamespaceAndPath(CreateTiers.MOD_ID, "models/item/andesite_encased_shaft_" + tierName),
                createParentModel(CreateTiers.MOD_ID + ":block/" + tierName + "/andesite_encased_shaft"));
        models.put(ResourceLocation.fromNamespaceAndPath(CreateTiers.MOD_ID, "models/item/brass_encased_shaft_" + tierName),
                createParentModel(CreateTiers.MOD_ID + ":block/" + tierName + "/brass_encased_shaft"));
        models.put(ResourceLocation.fromNamespaceAndPath(CreateTiers.MOD_ID, "models/item/andesite_encased_cogwheel_" + tierName),
                createParentModel(CreateTiers.MOD_ID + ":block/" + tierName + "/andesite_encased_cogwheel"));
        models.put(ResourceLocation.fromNamespaceAndPath(CreateTiers.MOD_ID, "models/item/brass_encased_cogwheel_" + tierName),
                createParentModel(CreateTiers.MOD_ID + ":block/" + tierName + "/brass_encased_cogwheel"));
        models.put(ResourceLocation.fromNamespaceAndPath(CreateTiers.MOD_ID, "models/item/andesite_encased_large_cogwheel_" + tierName),
                createParentModel(CreateTiers.MOD_ID + ":block/" + tierName + "/andesite_encased_large_cogwheel"));
        models.put(ResourceLocation.fromNamespaceAndPath(CreateTiers.MOD_ID, "models/item/brass_encased_large_cogwheel_" + tierName),
                createParentModel(CreateTiers.MOD_ID + ":block/" + tierName + "/brass_encased_large_cogwheel"));
    }

    private static JsonObject createParentModel(String parent) {
        JsonObject model = new JsonObject();
        model.addProperty("parent", parent);
        return model;
    }

    private static void generateShaftModels(String tierName, Map<ResourceLocation, JsonElement> models, net.minecraft.server.packs.resources.ResourceManager resourceManager) {
        Map<String, String> textures = Map.of(
                "0", CreateTiers.MOD_ID + ":block/grayscale/axis",
                "1", CreateTiers.MOD_ID + ":block/grayscale/axis_top",
                "particle", CreateTiers.MOD_ID + ":block/grayscale/axis");
        Map<String, Integer> tintMap = Map.of("Axis", 0);

        models.put(ResourceLocation.fromNamespaceAndPath(CreateTiers.MOD_ID, "models/block/" + tierName + "/shaft"),
                mutateModel(ResourceLocation.fromNamespaceAndPath("create", "block/shaft"), textures, tintMap, 0, Collections.emptySet(), resourceManager));
        models.put(
                ResourceLocation.fromNamespaceAndPath(CreateTiers.MOD_ID, "models/block/" + tierName + "/shaft_half"),
                mutateModel(ResourceLocation.fromNamespaceAndPath("create", "block/shaft_half"), textures, tintMap, 0, Collections.emptySet(), resourceManager));
    }

    private static void generateCogwheelShaftModel(String tierName, Map<ResourceLocation, JsonElement> models, net.minecraft.server.packs.resources.ResourceManager resourceManager) {
        Map<String, String> textures = Map.of(
                "0", CreateTiers.MOD_ID + ":block/grayscale/axis_top",
                "1", CreateTiers.MOD_ID + ":block/grayscale/cogwheel_axis",
                "particle", CreateTiers.MOD_ID + ":block/grayscale/axis_top");
        Map<String, Integer> tintMap = Collections.emptyMap();

        models.put(ResourceLocation.fromNamespaceAndPath(CreateTiers.MOD_ID, "models/block/" + tierName + "/cogwheel_shaft"),
                mutateModel(ResourceLocation.fromNamespaceAndPath("create", "block/cogwheel_shaft"), textures, tintMap, 0, Collections.emptySet(), resourceManager));
    }

    private static void generateCogwheelModels(String tierName, boolean isLarge,
            Map<ResourceLocation, JsonElement> models, net.minecraft.server.packs.resources.ResourceManager resourceManager) {
        String suffix = isLarge ? "large_cogwheel" : "cogwheel";
        String gearTextureKey = isLarge ? "4" : "1_2";

        Map<String, String> textures = new HashMap<>();
        textures.put("0", CreateTiers.MOD_ID + ":block/grayscale/cogwheel_axis");
        textures.put("3", CreateTiers.MOD_ID + ":block/grayscale/axis_top");
        textures.put(gearTextureKey, CreateTiers.MOD_ID + ":block/grayscale/" + suffix);
        textures.put("particle", CreateTiers.MOD_ID + ":block/grayscale/" + suffix);

        Map<String, Integer> tintMap = new HashMap<>();
        tintMap.put("Axis", 0);
        tintMap.put("Gear", 1);
        tintMap.put("Gear2", 1);
        tintMap.put("Gear3", 1);
        tintMap.put("Gear4", 1);
        tintMap.put("Gear5", 1);
        tintMap.put("Gear6", 1);
        tintMap.put("Gear7", 1);
        tintMap.put("Gear8", 1);
        tintMap.put("GearCaseInner", 1);
        tintMap.put("GearCaseInnerRotated", 1);
        tintMap.put("GearCaseOuter", 1);

        models.put(ResourceLocation.fromNamespaceAndPath(CreateTiers.MOD_ID, "models/block/" + tierName + "/" + suffix),
                mutateModel(ResourceLocation.fromNamespaceAndPath("create", "block/" + suffix), textures, tintMap, 1, Collections.emptySet(), resourceManager));

        ResourceLocation shaftlessModelLoc = ResourceLocation.fromNamespaceAndPath(CreateTiers.MOD_ID, "models/block/" + tierName + "/" + suffix + "_shaftless");
        models.put(shaftlessModelLoc,
                mutateModel(ResourceLocation.fromNamespaceAndPath("create", "block/" + suffix + "_shaftless"), textures,
                        tintMap, 1, Collections.emptySet(), resourceManager));

        dev.engine_room.flywheel.lib.model.baked.PartialModel.of(ResourceLocation.fromNamespaceAndPath(CreateTiers.MOD_ID, "block/" + tierName + "/" + suffix + "_shaftless"));
    }

    private static void generateEncasedShaftModels(String tierName, String casingType,
            Map<ResourceLocation, JsonElement> models, net.minecraft.server.packs.resources.ResourceManager resourceManager) {
        String createBaseModel = "block/encased_shaft/block_" + casingType;

        models.put(ResourceLocation.fromNamespaceAndPath(CreateTiers.MOD_ID, "models/block/" + tierName + "/" + casingType + "_encased_shaft"),
                mutateEncasedShaftModel(ResourceLocation.fromNamespaceAndPath("create", createBaseModel), tierName, resourceManager));
    }

    private static JsonObject mutateEncasedShaftModel(ResourceLocation baseModel, String tierName,
            net.minecraft.server.packs.resources.ResourceManager resourceManager) {
        try {
            ResourceLocation modelLoc = ResourceLocation.fromNamespaceAndPath(baseModel.getNamespace(), "models/" + baseModel.getPath() + ".json");
            java.util.Optional<net.minecraft.server.packs.resources.Resource> resource = resourceManager.getResource(modelLoc);
            if (resource.isEmpty()) {
                CreateTiers.LOGGER.error("Could not find base encased shaft model: {}", modelLoc);
                return new JsonObject();
            }

            JsonObject model = JsonParser.parseReader(new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8))
                    .getAsJsonObject();

            JsonObject textures = new JsonObject();
            textures.addProperty("casing", "create:block/" + baseModel.getPath().split("/")[2] + "_casing");
            textures.addProperty("opening", CreateTiers.MOD_ID + ":block/grayscale/gearbox");
            textures.addProperty("1", CreateTiers.MOD_ID + ":block/grayscale/gearbox");
            textures.addProperty("particle", "create:block/" + baseModel.getPath().split("/")[2] + "_casing");
            model.add("textures", textures);

            if (model.has("elements")) {
                JsonArray elements = model.getAsJsonArray("elements");
                for (JsonElement el : elements) {
                    JsonObject element = el.getAsJsonObject();
                    String name = element.has("name") ? element.get("name").getAsString() : "";
                    if (name.equals("Core")) {
                        if (element.has("faces")) {
                            JsonObject faces = element.getAsJsonObject("faces");
                            for (String faceName : faces.keySet()) {
                                faces.getAsJsonObject(faceName).addProperty("tintindex", 0);
                            }
                        }
                    }
                }
            }

            return model;
        } catch (Exception e) {
            CreateTiers.LOGGER.error("Failed to mutate encased shaft model: " + baseModel, e);
            return new JsonObject();
        }
    }

    private static void generateEncasedCogwheelModels(String tierName, String casingType, boolean isLarge,
            Map<ResourceLocation, JsonElement> models, net.minecraft.server.packs.resources.ResourceManager resourceManager) {
        String suffix = isLarge ? "large_cogwheel" : "cogwheel";
        String blockFolder = isLarge ? "encased_large_cogwheel" : "encased_cogwheel";
        String sideTexture = isLarge
                ? "create:block/" + casingType + "_encased_cogwheel_side_connected"
                : "create:block/" + casingType + "_encased_cogwheel_side";
        String woodTexture = casingType.equals("brass") ? "block/stripped_dark_oak_log_top" : "block/stripped_spruce_log_top";
        String gearboxTexture = casingType.equals("brass") ? "create:block/brass_gearbox" : "create:block/gearbox";

        for (String variant : new String[]{"", "_top", "_bottom", "_top_bottom"}) {
            String createBaseModel = "block/" + blockFolder + "/block" + variant;
            String outputName = casingType + "_encased_" + suffix + variant;

            models.put(ResourceLocation.fromNamespaceAndPath(CreateTiers.MOD_ID, "models/block/" + tierName + "/" + outputName),
                    mutateEncasedCogwheelModel(
                            ResourceLocation.fromNamespaceAndPath("create", createBaseModel),
                            casingType, sideTexture, woodTexture, gearboxTexture, resourceManager));
        }
    }

    private static JsonObject mutateEncasedCogwheelModel(ResourceLocation baseModel, String casingType,
            String sideTexture, String woodTexture, String gearboxTexture,
            net.minecraft.server.packs.resources.ResourceManager resourceManager) {
        try {
            ResourceLocation modelLoc = ResourceLocation.fromNamespaceAndPath(baseModel.getNamespace(), "models/" + baseModel.getPath() + ".json");
            java.util.Optional<net.minecraft.server.packs.resources.Resource> resource = resourceManager.getResource(modelLoc);
            if (resource.isEmpty()) {
                CreateTiers.LOGGER.error("Could not find base encased cogwheel model: {}", modelLoc);
                return new JsonObject();
            }

            JsonObject model = JsonParser.parseReader(new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8))
                    .getAsJsonObject();

            if (!model.has("textures")) {
                return model;
            }

            JsonObject textures = model.getAsJsonObject("textures");
            if (textures.has("casing")) {
                textures.addProperty("casing", "create:block/" + casingType + "_casing");
            }
            if (textures.has("side")) {
                textures.addProperty("side", sideTexture);
            }
            if (textures.has("1")) {
                textures.addProperty("1", woodTexture);
            }
            if (textures.has("4")) {
                textures.addProperty("4", gearboxTexture);
            }
            if (textures.has("particle")) {
                textures.addProperty("particle", "create:block/" + casingType + "_casing");
            }

            model.addProperty("render_type", "cutout_mipped");

            return model;
        } catch (Exception e) {
            CreateTiers.LOGGER.error("Failed to mutate encased cogwheel model: " + baseModel, e);
            return new JsonObject();
        }
    }

    private static JsonObject mutateModel(ResourceLocation baseModel, Map<String, String> textures,
            Map<String, Integer> tintMap, int defaultTint, Set<String> excludeElements, net.minecraft.server.packs.resources.ResourceManager resourceManager) {
        try {
            String[] parts = baseModel.toString().split(":");
            String namespace = parts[0];
            String path = parts[1];
            ResourceLocation modelLoc = ResourceLocation.fromNamespaceAndPath(namespace, "models/" + path + ".json");
            java.util.Optional<net.minecraft.server.packs.resources.Resource> resource = resourceManager.getResource(modelLoc);
            if (resource.isEmpty()) {
                CreateTiers.LOGGER.error("Could not find base model to mutate: {}", modelLoc);
                return new JsonObject();
            }

            JsonObject model = JsonParser.parseReader(new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8))
                    .getAsJsonObject();

            JsonObject texturesObj = new JsonObject();
            textures.forEach(texturesObj::addProperty);
            model.add("textures", texturesObj);

            if (model.has("elements")) {
                JsonArray elements = model.getAsJsonArray("elements");
                JsonArray newElements = new JsonArray();
                for (JsonElement el : elements) {
                    JsonObject element = el.getAsJsonObject();
                    String name = element.has("name") ? element.get("name").getAsString() : "";

                    if (excludeElements.contains(name)) continue;

                    int tintIndex = tintMap.getOrDefault(name, defaultTint);

                    if (element.has("faces")) {
                        JsonObject faces = element.getAsJsonObject("faces");
                        for (String faceName : faces.keySet()) {
                            faces.getAsJsonObject(faceName).addProperty("tintindex", tintIndex);
                        }
                    }
                    newElements.add(element);
                }
                model.add("elements", newElements);
            }

            return model;
        } catch (Exception e) {
            CreateTiers.LOGGER.error("Failed to mutate model: " + baseModel, e);
            return new JsonObject();
        }
    }

    private static void generateShaftBlockstate(String tierName, Map<ResourceLocation, JsonObject> blockstates) {
        ResourceLocation modelLocation = ResourceLocation.fromNamespaceAndPath(CreateTiers.MOD_ID,
                "block/" + tierName + "/shaft");
        blockstates.put(ResourceLocation.fromNamespaceAndPath(CreateTiers.MOD_ID, "blockstates/shaft_" + tierName),
                createAxisBlockstate(modelLocation));
    }

    private static void generateCogwheelBlockstate(String tierName, boolean isLarge,
            Map<ResourceLocation, JsonObject> blockstates) {
        String suffix = isLarge ? "large_cogwheel" : "cogwheel";
        ResourceLocation modelLocation = ResourceLocation.fromNamespaceAndPath(CreateTiers.MOD_ID,
                "block/" + tierName + "/" + suffix);
        blockstates.put(
                ResourceLocation.fromNamespaceAndPath(CreateTiers.MOD_ID, "blockstates/" + suffix + "_" + tierName),
                createAxisBlockstate(modelLocation));
    }

    private static void generateEncasedShaftBlockstate(String tierName, String casingType,
            Map<ResourceLocation, JsonObject> blockstates) {
        String blockName = casingType + "_encased_shaft_" + tierName;
        ResourceLocation modelLocation = ResourceLocation.fromNamespaceAndPath(CreateTiers.MOD_ID,
                "block/" + tierName + "/" + casingType + "_encased_shaft");
        blockstates.put(ResourceLocation.fromNamespaceAndPath(CreateTiers.MOD_ID, "blockstates/" + blockName),
                createAxisBlockstate(modelLocation));
    }

    private static void generateEncasedCogwheelBlockstate(String tierName, String casingType, boolean isLarge,
            Map<ResourceLocation, JsonObject> blockstates) {
        String suffix = isLarge ? "large_cogwheel" : "cogwheel";
        String blockName = casingType + "_encased_" + suffix + "_" + tierName;
        String modelPrefix = CreateTiers.MOD_ID + ":block/" + tierName + "/" + casingType + "_encased_" + suffix;
        blockstates.put(
                ResourceLocation.fromNamespaceAndPath(CreateTiers.MOD_ID, "blockstates/" + blockName),
                createEncasedCogwheelBlockstate(modelPrefix));
    }

    private static JsonObject createAxisBlockstate(ResourceLocation model) {
        JsonObject blockstate = new JsonObject();
        JsonObject variants = new JsonObject();
        for (Direction.Axis axis : Direction.Axis.values()) {
            for (boolean waterlogged : new boolean[]{false, true}) {
                JsonObject variant = new JsonObject();
                variant.addProperty("model", model.toString());
                if (axis == Direction.Axis.X) {
                    variant.addProperty("x", 90);
                    variant.addProperty("y", 90);
                } else if (axis == Direction.Axis.Z) {
                    variant.addProperty("x", 90);
                }
                variants.add("axis=" + axis.getName() + ",waterlogged=" + waterlogged, variant);
            }
        }
        blockstate.add("variants", variants);
        return blockstate;
    }

    private static JsonObject createEncasedCogwheelBlockstate(String modelPrefix) {
        JsonObject blockstate = new JsonObject();
        JsonObject variants = new JsonObject();
        for (Direction.Axis axis : Direction.Axis.values()) {
            for (boolean topShaft : new boolean[]{false, true}) {
                for (boolean bottomShaft : new boolean[]{false, true}) {
                    String suffix = (topShaft ? "_top" : "") + (bottomShaft ? "_bottom" : "");
                    JsonObject variant = new JsonObject();
                    variant.addProperty("model", modelPrefix + suffix);
                    if (axis == Direction.Axis.X) {
                        variant.addProperty("x", 90);
                        variant.addProperty("y", 90);
                    } else if (axis == Direction.Axis.Z) {
                        variant.addProperty("x", 90);
                    }
                    variants.add("axis=" + axis.getName() + ",top_shaft=" + topShaft + ",bottom_shaft=" + bottomShaft, variant);
                }
            }
        }
        blockstate.add("variants", variants);
        return blockstate;
    }
}
