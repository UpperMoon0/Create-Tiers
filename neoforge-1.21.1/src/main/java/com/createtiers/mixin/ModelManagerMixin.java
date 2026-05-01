package com.createtiers.mixin;

import com.createtiers.CreateTiers;
import com.createtiers.client.DynamicResourcePack;
import com.createtiers.client.TieredModelGenerator;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ModelManager.class)
public class ModelManagerMixin {

    @Inject(method = "reload", at = @At("HEAD"))
    private void onCreateReload(PreparableReloadListener.PreparationBarrier preparationBarrier,
                                ResourceManager resourceManager,
                                ProfilerFiller profiler,
                                ProfilerFiller profiler2,
                                Executor executor,
                                Executor executor2,
                                CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        CreateTiers.LOGGER.info("Reinitializing Create-Tiers dynamic resources...");

        DynamicResourcePack.clear();

        TieredModelGenerator.generateAllModels(resourceManager);

        CreateTiers.LOGGER.info("Create-Tiers dynamic resources reinitialized!");
    }
}
