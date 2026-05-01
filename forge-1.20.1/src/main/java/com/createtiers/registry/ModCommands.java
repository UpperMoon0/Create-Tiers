package com.createtiers.registry;

import com.createtiers.CreateTiers;
import com.createtiers.data.DumpDataCommand;
import net.minecraft.commands.Commands;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Registers commands for Create Tiers.
 */
@Mod.EventBusSubscriber(modid = CreateTiers.MOD_ID)
public class ModCommands {
    
    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("createtiers")
                .requires(source -> source.hasPermission(2)) // Requires OP level 2
                .then(Commands.literal("dumpdata")
                    .executes(context -> {
                        try {
                            return DumpDataCommand.dumpData(context);
                        } catch (Exception e) {
                            CreateTiers.LOGGER.error("Error dumping CreateTiers data", e);
                            context.getSource().sendFailure(
                                net.minecraft.network.chat.Component.literal("Error dumping data: " + e.getMessage())
                            );
                            return 0;
                        }
                    })
                )
        );
        
        CreateTiers.LOGGER.info("Registered CreateTiers commands");
    }
}
