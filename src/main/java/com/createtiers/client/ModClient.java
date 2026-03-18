package com.createtiers.client;

import com.createtiers.CreateTiers;
import com.createtiers.api.Tier;
import com.createtiers.content.kinetics.TieredCogwheelBlock;
import com.createtiers.content.kinetics.TieredCogwheelBlockEntity;
import com.createtiers.content.kinetics.TieredShaftBlock;
import com.createtiers.content.kinetics.TieredShaftBlockEntity;
import com.createtiers.registry.ModBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import com.createtiers.foundation.item.TieredKineticStats;
import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockModel;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.BlockEntityVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Mod.EventBusSubscriber(modid = CreateTiers.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModClient {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlocks.TIERED_SHAFT.get(), TieredKineticBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlocks.TIERED_COGWHEEL.get(), TieredKineticBlockEntityRenderer::new);
    }

    @SubscribeEvent
    public static void onModelBake(ModelEvent.ModifyBakingResult event) {
        Map<ResourceLocation, BakedModel> modelRegistry = event.getModels();

        List<Block> blocksToWrap = new ArrayList<>();
        blocksToWrap.addAll(ModBlocks.SHAFTS);
        blocksToWrap.addAll(ModBlocks.COGWHEELS);
        blocksToWrap.addAll(ModBlocks.LARGE_COGWHEELS);

        for (Block block : blocksToWrap) {
            ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block);
            if (blockId == null) continue;

            block.getStateDefinition().getPossibleStates().forEach(state -> {
                ModelResourceLocation mrl = BlockModelShaper.stateToModelLocation(blockId, state);
                BakedModel original = modelRegistry.get(mrl);
                if (original != null && !(original instanceof BracketedKineticBlockModel)) {
                    modelRegistry.put(mrl, new BracketedKineticBlockModel(original));
                }
            });
        }
    }

    @SubscribeEvent
    public static void registerVisualizers(FMLClientSetupEvent event) {
        // Initialize tiered partial models
        AllTieredPartialModels.init();

        SimpleBlockEntityVisualizer.builder(ModBlocks.TIERED_SHAFT.get())
                .factory(TieredShaftVisual::new)
                .skipVanillaRender(be -> VisualizationManager.supportsVisualization(be.getLevel()))
                .apply();

        SimpleBlockEntityVisualizer.builder(ModBlocks.TIERED_COGWHEEL.get())
                .factory(TieredCogwheelVisual::create)
                .skipVanillaRender(be -> VisualizationManager.supportsVisualization(be.getLevel()))
                .apply();

        TieredKineticStats kineticStats = new TieredKineticStats();
        ModBlocks.SHAFT_ITEMS.forEach(item -> TooltipModifier.REGISTRY.register(item, kineticStats));
        ModBlocks.COGWHEEL_ITEMS.forEach(item -> TooltipModifier.REGISTRY.register(item, kineticStats));
        ModBlocks.LARGE_COGWHEEL_ITEMS.forEach(item -> TooltipModifier.REGISTRY.register(item, kineticStats));
    }

    public static class TieredShaftVisual extends SingleAxisRotatingVisual<TieredShaftBlockEntity> {
        private final Tier tier;

        public TieredShaftVisual(VisualizationContext context, TieredShaftBlockEntity blockEntity, float partialTick) {
            super(context, blockEntity, partialTick, Models.partial(AllTieredPartialModels.forTier(
                    ((TieredShaftBlock) blockEntity.getBlockState().getBlock()).getTier()).SHAFT));
            this.tier = ((TieredShaftBlock) blockEntity.getBlockState().getBlock()).getTier();
            applyTierColor();
        }

        private void applyTierColor() {
            rotatingModel.setColor(new Color(tier.getShaftColor()));
            rotatingModel.setChanged();
        }

        @Override
        public void update(float pt) {
            super.update(pt);
            applyTierColor();
        }

        @Override
        public void tick(Context context) {
            // super.tick() calls applyOverstressEffect() which resets color to WHITE.
            // We must re-apply the tier color afterwards.
            super.tick(context);
            applyTierColor();
        }
    }

    public static class TieredCogwheelVisual {
        public static BlockEntityVisual<TieredCogwheelBlockEntity> create(VisualizationContext context, TieredCogwheelBlockEntity blockEntity, float partialTick) {
            if (ICogWheel.isLargeCog(blockEntity.getBlockState())) {
                return new LargeTieredCogVisual(context, blockEntity, partialTick);
            } else {
                return new SmallTieredCogwheelVisual(context, blockEntity, partialTick);
            }
        }

        public static class SmallTieredCogwheelVisual extends SingleAxisRotatingVisual<TieredCogwheelBlockEntity> {
            protected final RotatingInstance additionalShaft;
            private final Tier tier;

            public SmallTieredCogwheelVisual(VisualizationContext context, TieredCogwheelBlockEntity blockEntity, float partialTick) {
                super(context, blockEntity, partialTick, Models.partial(AllTieredPartialModels.forTier(
                        ((TieredCogwheelBlock) blockEntity.getBlockState().getBlock()).getTier()).COGWHEEL_SHAFTLESS));
                
                this.tier = ((TieredCogwheelBlock) blockEntity.getBlockState().getBlock()).getTier();
                Direction.Axis axis = KineticBlockEntityVisual.rotationAxis(blockEntity.getBlockState());
                additionalShaft = instancerProvider().instancer(AllInstanceTypes.ROTATING, 
                        Models.partial(AllTieredPartialModels.forTier(tier).COGWHEEL_SHAFT))
                        .createInstance();

                additionalShaft.rotateToFace(axis)
                        .setup(blockEntity)
                        .setRotationOffset(BracketedKineticBlockEntityRenderer.getShaftAngleOffset(axis, pos))
                        .setPosition(getVisualPosition())
                        .setChanged();

                applyTierColor();
            }

            private void applyTierColor() {
                rotatingModel.setColor(new Color(tier.getCogwheelColor()));
                rotatingModel.setChanged();
                additionalShaft.setColor(new Color(tier.getShaftColor()));
                additionalShaft.setChanged();
            }

            @Override
            public void update(float pt) {
                super.update(pt);
                additionalShaft.setup(blockEntity)
                        .setRotationOffset(BracketedKineticBlockEntityRenderer.getShaftAngleOffset(rotationAxis(), pos))
                        .setChanged();
                applyTierColor();
            }

            @Override
            public void tick(Context context) {
                super.tick(context);
                applyTierColor();
            }

            @Override
            public void updateLight(float partialTick) {
                super.updateLight(partialTick);
                relight(additionalShaft);
            }

            @Override
            protected void _delete() {
                super._delete();
                additionalShaft.delete();
            }

            @Override
            public void collectCrumblingInstances(Consumer<Instance> consumer) {
                super.collectCrumblingInstances(consumer);
                consumer.accept(additionalShaft);
            }
        }

        public static class LargeTieredCogVisual extends SingleAxisRotatingVisual<TieredCogwheelBlockEntity> {
            protected final RotatingInstance additionalShaft;
            private final Tier tier;

            public LargeTieredCogVisual(VisualizationContext context, TieredCogwheelBlockEntity blockEntity, float partialTick) {
                super(context, blockEntity, partialTick, Models.partial(AllTieredPartialModels.forTier(
                        ((TieredCogwheelBlock) blockEntity.getBlockState().getBlock()).getTier()).LARGE_COGWHEEL_SHAFTLESS));

                this.tier = ((TieredCogwheelBlock) blockEntity.getBlockState().getBlock()).getTier();
                Direction.Axis axis = KineticBlockEntityVisual.rotationAxis(blockEntity.getBlockState());
                additionalShaft = instancerProvider().instancer(AllInstanceTypes.ROTATING, 
                        Models.partial(AllTieredPartialModels.forTier(tier).COGWHEEL_SHAFT))
                        .createInstance();

                additionalShaft.rotateToFace(axis)
                        .setup(blockEntity)
                        .setRotationOffset(BracketedKineticBlockEntityRenderer.getShaftAngleOffset(axis, pos))
                        .setPosition(getVisualPosition())
                        .setChanged();

                applyTierColor();
            }

            private void applyTierColor() {
                rotatingModel.setColor(new Color(tier.getCogwheelColor()));
                rotatingModel.setChanged();
                additionalShaft.setColor(new Color(tier.getShaftColor()));
                additionalShaft.setChanged();
            }

            @Override
            public void update(float pt) {
                super.update(pt);
                additionalShaft.setup(blockEntity)
                        .setRotationOffset(BracketedKineticBlockEntityRenderer.getShaftAngleOffset(rotationAxis(), pos))
                        .setChanged();
                applyTierColor();
            }

            @Override
            public void tick(Context context) {
                // super.tick() calls applyOverstressEffect() which resets color to WHITE.
                // We must re-apply the tier color afterwards.
                super.tick(context);
                applyTierColor();
            }

            @Override
            public void updateLight(float partialTick) {
                super.updateLight(partialTick);
                relight(additionalShaft);
            }

            @Override
            protected void _delete() {
                super._delete();
                additionalShaft.delete();
            }

            @Override
            public void collectCrumblingInstances(Consumer<Instance> consumer) {
                super.collectCrumblingInstances(consumer);
                consumer.accept(additionalShaft);
            }
        }
    }
}
