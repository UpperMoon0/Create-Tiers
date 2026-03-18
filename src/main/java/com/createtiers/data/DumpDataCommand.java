package com.createtiers.data;

import com.createtiers.CreateTiers;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import com.mojang.brigadier.context.CommandContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Utility to dump generated data to files for debugging/inspection.
 * Use via command: /createtiers dumpdata
 */
public class DumpDataCommand {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    public static int dumpData(CommandContext<CommandSourceStack> context) throws IOException {
        CommandSourceStack source = context.getSource();
        
        // Ensure resources are generated
        DynamicServerPack.generateResources();
        
        Path dumpPath = Paths.get("createtiers_dump");
        Files.createDirectories(dumpPath);
        
        int tagCount = 0;
        int lootCount = 0;
        
        // Dump tags (can be in both minecraft and createtiers namespaces)
        for (Map.Entry<ResourceLocation, com.google.gson.JsonObject> entry : DynamicServerPack.getTags().entrySet()) {
            ResourceLocation loc = entry.getKey();
            String namespace = loc.getNamespace();
            String path = loc.getPath();
            
            Path tagFile = dumpPath.resolve("data/" + namespace + "/" + path + ".json");
            Files.createDirectories(tagFile.getParent());
            Files.writeString(tagFile, GSON.toJson(entry.getValue()));
            tagCount++;
        }
        
        // Dump loot tables (only in createtiers namespace)
        for (Map.Entry<ResourceLocation, com.google.gson.JsonObject> entry : DynamicServerPack.getLootTables().entrySet()) {
            ResourceLocation loc = entry.getKey();
            String namespace = loc.getNamespace();
            String path = loc.getPath();
            
            Path lootFile = dumpPath.resolve("data/" + namespace + "/" + path + ".json");
            Files.createDirectories(lootFile.getParent());
            Files.writeString(lootFile, GSON.toJson(entry.getValue()));
            lootCount++;
        }
        
        final int tags = tagCount;
        final int loots = lootCount;
        source.sendSuccess(() -> Component.literal("Dumped CreateTiers data to " + dumpPath.toAbsolutePath() 
            + " (" + tags + " tags, " + loots + " loot tables)"), true);
        CreateTiers.LOGGER.info("Dumped CreateTiers data to {} ({} tags, {} loot tables)", 
            dumpPath.toAbsolutePath(), tagCount, lootCount);
        
        return 1;
    }
}
