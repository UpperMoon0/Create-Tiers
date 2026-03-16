package com.createtiers.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.createtiers.CreateTiers;
import com.createtiers.api.TierRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A dynamic resource pack that generates models and blockstates at runtime.
 * All tiers use Create's original textures - only models/blockstates are generated.
 */
public class DynamicResourcePack implements PackResources {
    
    private static final String NAME = "createtiers:dynamic";
    
    // Storage for dynamically generated resources using ResourceLocation for reliable lookup
    private static final Map<ResourceLocation, JsonElement> MODELS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, JsonObject> BLOCKSTATES = new ConcurrentHashMap<>();
    private static final Map<String, JsonObject> LANGUAGES = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, byte[]> GRAYSCALE_CACHE = new ConcurrentHashMap<>();
    private static final Set<ResourceLocation> GRAYSCALE_TEXTURES = ConcurrentHashMap.newKeySet();
    
    // Track if resources have been generated
    private static volatile boolean resourcesGenerated = false;
    private static final Object GENERATION_LOCK = new Object();
    
    // Pack metadata
    private final PackMetadataSection metadata;
    
    public DynamicResourcePack() {
        // Create metadata: (description, pack_format)
        // 1.20.1 pack format for CLIENT_RESOURCES is 15
        this.metadata = new PackMetadataSection(Component.literal("Dynamic resources for Create Tiers"), 15);
    }
    
    /**
     * Add a dynamically generated model
     */
    public static void addModel(ResourceLocation location, JsonElement modelJson) {
        MODELS.put(location, modelJson);
        CreateTiers.LOGGER.debug("Added model: {}", location);
        
        // Collect textures from model
        if (modelJson.isJsonObject()) {
            JsonObject model = modelJson.getAsJsonObject();
            if (model.has("textures")) {
                JsonObject textures = model.getAsJsonObject("textures");
                for (String key : textures.keySet()) {
                    String texPath = textures.get(key).getAsString();
                    if (texPath.contains("block/grayscale/")) {
                        ResourceLocation texLoc = ResourceLocation.parse(texPath);
                        // Convert to full asset path if it's just a shorthand
                        if (!texLoc.getPath().startsWith("textures/")) {
                            texLoc = ResourceLocation.fromNamespaceAndPath(texLoc.getNamespace(), "textures/" + texLoc.getPath() + ".png");
                        }
                        GRAYSCALE_TEXTURES.add(texLoc);
                    }
                }
            }
        }
    }
    
    /**
     * Add a dynamically generated blockstate
     */
    public static void addBlockState(ResourceLocation location, JsonObject blockstateJson) {
        BLOCKSTATES.put(location, blockstateJson);
        CreateTiers.LOGGER.debug("Added blockstate: {}", location);
    }
    
    /**
     * Add a translation to a specific language file
     */
    public static void addTranslation(String lang, String key, String value) {
        LANGUAGES.computeIfAbsent(lang, k -> new JsonObject()).addProperty(key, value);
        CreateTiers.LOGGER.debug("Added translation [{}]: {} = {}", lang, key, value);
    }
    
    /**
     * Mark resources as generated (called after all models are added)
     */
    public static void markGenerated() {
        resourcesGenerated = true;
        CreateTiers.LOGGER.info("Dynamic resources marked as generated. Models: {}, Blockstates: {}", 
                MODELS.size(), BLOCKSTATES.size());
    }
    
    /**
     * Clear all dynamic resources and reset generation flag (called on resource reload)
     */
    public static void clear() {
        MODELS.clear();
        BLOCKSTATES.clear();
        LANGUAGES.clear();
        resourcesGenerated = false;
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
    
    /**
     * Ensure resources are generated before attempting to retrieve them.
     */
    private static void ensureResourcesGenerated() {
        if (!resourcesGenerated) {
            synchronized (GENERATION_LOCK) {
                if (!resourcesGenerated) {
                    // Check if tiers are registered yet
                    if (TierRegistry.size() == 0) {
                        CreateTiers.LOGGER.warn("TierRegistry is empty - cannot generate resources yet.");
                        return;
                    }
                    
                    CreateTiers.LOGGER.info("Generating Create-Tiers dynamic resources...");
                    TieredModelGenerator.generateAllModels();
                }
            }
        }
    }
    
    @Override
    public @Nullable IoSupplier<InputStream> getResource(@NotNull PackType type, @NotNull ResourceLocation location) {
        if (type != PackType.CLIENT_RESOURCES) return null;
        
        String namespace = location.getNamespace();
        String path = location.getPath();
        
        // Handle pack.mcmeta request
        if (path.equals(PackResources.PACK_META)) {
            JsonObject packJson = new JsonObject();
            JsonObject packMeta = new JsonObject();
            packMeta.addProperty("description", "Dynamic resources for Create Tiers");
            packMeta.addProperty("pack_format", 15);
            packJson.add("pack", packMeta);
            return () -> new ByteArrayInputStream(packJson.toString().getBytes(StandardCharsets.UTF_8));
        }

        if (!namespace.equals(CreateTiers.MOD_ID)) return null;
        
        ensureResourcesGenerated();
        
        // Handle models - Minecraft requests "models/block/shaft.json"
        if (path.startsWith("models/") && path.endsWith(".json")) {
            // Remove .json extension for key lookup
            String modelPath = path.substring(0, path.length() - 5);
            ResourceLocation modelLoc = ResourceLocation.fromNamespaceAndPath(namespace, modelPath);
            JsonElement modelJson = MODELS.get(modelLoc);
            if (modelJson != null) {
                return () -> new ByteArrayInputStream(modelJson.toString().getBytes(StandardCharsets.UTF_8));
            }
        }
        
        // Handle blockstates - Minecraft requests "blockstates/shaft.json"
        if (path.startsWith("blockstates/") && path.endsWith(".json")) {
            // Remove .json extension for key lookup
            String statePath = path.substring(0, path.length() - 5);
            ResourceLocation stateLoc = ResourceLocation.fromNamespaceAndPath(namespace, statePath);
            JsonElement stateJson = BLOCKSTATES.get(stateLoc);
            if (stateJson != null) {
                return () -> new ByteArrayInputStream(stateJson.toString().getBytes(StandardCharsets.UTF_8));
            }
        }
        
        // Handle languages - Minecraft requests "lang/en_us.json"
        if (path.startsWith("lang/") && path.endsWith(".json")) {
            String lang = path.substring(5, path.length() - 5);
            JsonObject langJson = LANGUAGES.get(lang);
            if (langJson != null) {
                return () -> new ByteArrayInputStream(langJson.toString().getBytes(StandardCharsets.UTF_8));
            }
        }
        
        // Handle textures - Minecraft requests "textures/block/grayscale/shaft.png"
        if (path.startsWith("textures/block/grayscale/") && path.endsWith(".png")) {
            return getGrayscaleTextureStream(location);
        }
        
        return null;
    }

    /**
     * Virtually grayscales a texture from the Create mod.
     */
    private static IoSupplier<InputStream> getGrayscaleTextureStream(ResourceLocation location) {
        return () -> {
            if (GRAYSCALE_CACHE.containsKey(location)) {
                return new ByteArrayInputStream(GRAYSCALE_CACHE.get(location));
            }

            String path = location.getPath(); // textures/block/grayscale/shaft.png
            String fileName = path.substring("textures/block/grayscale/".length());
            try {
                // Try different possible paths for the original texture in Create
                String[] commonPaths = {
                    "assets/create/textures/block/",
                    "assets/create/textures/block/kinetic/"
                };
                
                InputStream is = null;
                String foundPath = null;
                
                for (String basePath : commonPaths) {
                    String fullPath = basePath + fileName;
                    is = CreateTiers.class.getClassLoader().getResourceAsStream(fullPath);
                    if (is != null) {
                        foundPath = fullPath;
                        break;
                    }
                }

                if (is == null) {
                    CreateTiers.LOGGER.error("Could not find original texture for grayscaling in any common path: {}", fileName);
                    throw new IOException("Original texture not found: " + fileName);
                }

                CreateTiers.LOGGER.debug("Grayscaling texture: {} from {}", location, foundPath);

                BufferedImage image = ImageIO.read(is);
                if (image == null) {
                    throw new IOException("Failed to load image for grayscaling from " + foundPath);
                }

                int width = image.getWidth();
                int height = image.getHeight();

                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        int pixel = image.getRGB(x, y);
                        int a = (pixel >> 24) & 0xFF;
                        int r = (pixel >> 16) & 0xFF;
                        int g = (pixel >> 8) & 0xFF;
                        int b = pixel & 0xFF;

                        // Standard luminance grayscale
                        int gray = (int) (0.2126 * r + 0.7152 * g + 0.0722 * b);
                        int newPixel = (a << 24) | (gray << 16) | (gray << 8) | gray;
                        image.setRGB(x, y, newPixel);
                    }
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "png", baos);
                byte[] bytes = baos.toByteArray();
                GRAYSCALE_CACHE.put(location, bytes);

                return new ByteArrayInputStream(bytes);
            } catch (IOException e) {
                CreateTiers.LOGGER.error("Failed to grayscale texture: " + location, e);
                throw e;
            }
        };
    }
    
    @Override
    public void listResources(@NotNull PackType type, @NotNull String namespace, @NotNull String path, @NotNull ResourceOutput output) {
        if (type != PackType.CLIENT_RESOURCES || !namespace.equals(CreateTiers.MOD_ID)) return;
        
        ensureResourcesGenerated();
        
        // ModelBakery usually lists "models" and "blockstates"
        if (path.equals("models") || path.startsWith("models/")) {
            MODELS.forEach((loc, json) -> {
                if (loc.getPath().startsWith(path)) {
                    // Output location must include .json
                    ResourceLocation outputLoc = loc.withSuffix(".json");
                    output.accept(outputLoc, () -> new ByteArrayInputStream(json.toString().getBytes(StandardCharsets.UTF_8)));
                }
            });
        }
        
        if (path.equals("blockstates") || path.startsWith("blockstates/")) {
            BLOCKSTATES.forEach((loc, json) -> {
                if (loc.getPath().startsWith(path)) {
                    // Output location must include .json
                    ResourceLocation outputLoc = loc.withSuffix(".json");
                    output.accept(outputLoc, () -> new ByteArrayInputStream(json.toString().getBytes(StandardCharsets.UTF_8)));
                }
            });
        }

        if (path.equals("lang") || path.startsWith("lang/")) {
            LANGUAGES.forEach((lang, json) -> {
                String langPath = "lang/" + lang + ".json";
                if (langPath.startsWith(path)) {
                    ResourceLocation outputLoc = ResourceLocation.fromNamespaceAndPath(namespace, langPath);
                    output.accept(outputLoc, () -> new ByteArrayInputStream(json.toString().getBytes(StandardCharsets.UTF_8)));
                }
            });
        }

        // List grayscale textures
        if (path.equals("textures") || path.startsWith("textures/")) {
            GRAYSCALE_TEXTURES.forEach(loc -> {
                if (loc.getPath().startsWith(path)) {
                    output.accept(loc, getGrayscaleTextureStream(loc));
                }
            });
        }
    }
    
    @Override
    public @NotNull Set<String> getNamespaces(@NotNull PackType type) {
        return type == PackType.CLIENT_RESOURCES ? Set.of(CreateTiers.MOD_ID) : Set.of();
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
    
    public static boolean hasModel(ResourceLocation location) {
        return MODELS.containsKey(location);
    }
    
    public static boolean hasBlockState(ResourceLocation location) {
        return BLOCKSTATES.containsKey(location);
    }
}
