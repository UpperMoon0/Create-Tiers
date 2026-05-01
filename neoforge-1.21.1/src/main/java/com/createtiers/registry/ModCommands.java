package com.createtiers.registry;

import com.createtiers.CreateTiers;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = CreateTiers.MOD_ID)
public class ModCommands {

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("createtiers")
                .requires(source -> source.hasPermission(2))
        );

        CreateTiers.LOGGER.info("Registered CreateTiers commands");
    }
}
