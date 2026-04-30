package com.createtiers.client;

import com.createtiers.CreateTiers;
import com.createtiers.api.Tier;
import com.createtiers.api.TierRegistry;
import com.createtiers.content.kinetics.TieredCogwheelBlock;
import com.createtiers.content.kinetics.TieredCogwheelBlockEntity;
import com.createtiers.content.kinetics.TieredEncasedCogwheelBlock;
import com.createtiers.content.kinetics.TieredEncasedShaftBlock;
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
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = CreateTiers.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModClient {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlocks.TIERED_SHAFT.get(), TieredKineticBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlocks.TIERED_COGWHEEL.get(), TieredKineticBlockEntityRenderer::new);
    }

    @SubscribeEvent
    public static void onModelBake(ModelEvent.ModifyBakingResult event) {
        Map<net.minecraft.client.resources.model.ModelResourceLocation, BakedModel> modelRegistry = event.getModels();

        List<Block> blocksToWrap = new ArrayList<>();
        blocksToWrap.addAll(ModBlocks.SHAFTS);
        blocksToWrap.addAll(ModBlocks.COGWHEELS);
        blocksToWrap.addAll(ModBlocks.LARGE_COGWHEELS);

        for (Block block : blocksToWrap) {
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
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
    public static void registerVisualizers(EntityRenderersEvent.RegisterRenderers event) {
        AllTieredPartialModels.init();

        SimpleBlockEntityVisualizer.builder(ModBlocks.TIERED_SHAFT.get())
                .factory(TieredShaftVisual::create)
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
        ModBlocks.ENCASED_SHAFT_ITEMS.forEach(item -> TooltipModifier.REGISTRY.register(item, kineticStats));
        ModBlocks.ENCASED_COGWHEEL_ITEMS.forEach(item -> TooltipModifier.REGISTRY.register(item, kineticStats));
        ModBlocks.ENCASED_LARGE_COGWHEEL_ITEMS.forEach(item -> TooltipModifier.REGISTRY.register(item, kineticStats));
    }

    static Tier resolveTier(net.minecraft.world.level.block.state.BlockState state) {
        Block block = state.getBlock();
        if (block instanceof TieredShaftBlock shaft) return shaft.getTier();
        if (block instanceof TieredEncasedShaftBlock encased) return encased.getTier();
        if (block instanceof TieredCogwheelBlock cog) return cog.getTier();
        if (block instanceof TieredEncasedCogwheelBlock encasedCog) return encasedCog.getTier();
        return null;
    }

    public static class TieredShaftVisual {
        public static BlockEntityVisual<TieredShaftBlockEntity> create(VisualizationContext context, TieredShaftBlockEntity blockEntity, float partialTick) {
            Block block = blockEntity.getBlockState().getBlock();
            if (block instanceof TieredEncasedShaftBlock) {
                return new EncasedShaftVisual(context, blockEntity, partialTick);
            }
            return new PlainShaftVisual(context, blockEntity, partialTick);
        }

        public static class PlainShaftVisual extends SingleAxisRotatingVisual<TieredShaftBlockEntity> {
            private final Tier tier;

            public PlainShaftVisual(VisualizationContext context, TieredShaftBlockEntity blockEntity, float partialTick) {
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
                super.tick(context);
                applyTierColor();
            }
        }

        public static class EncasedShaftVisual extends SingleAxisRotatingVisual<TieredShaftBlockEntity> {
            private final Tier tier;

            public EncasedShaftVisual(VisualizationContext context, TieredShaftBlockEntity blockEntity, float partialTick) {
                super(context, blockEntity, partialTick, Models.partial(getEncasedShaftPartial(blockEntity)));
                this.tier = resolveTier(blockEntity.getBlockState());
                applyTierColor();
            }

            private static PartialModel getEncasedShaftPartial(TieredShaftBlockEntity be) {
                Block block = be.getBlockState().getBlock();
                if (!(block instanceof TieredEncasedShaftBlock encased)) return AllPartialModels.SHAFT;
                AllTieredPartialModels.TieredPartials partials = AllTieredPartialModels.forTier(encased.getTier());
                if (encased.getCasing() == AllBlocks.ANDESITE_CASING.get()) {
                    return partials.ANDESITE_ENCASED_SHAFT;
                } else if (encased.getCasing() == AllBlocks.BRASS_CASING.get()) {
                    return partials.BRASS_ENCASED_SHAFT;
                }
                return AllPartialModels.SHAFT;
            }

            private void applyTierColor() {
                if (tier != null) {
                    rotatingModel.setColor(new Color(tier.getShaftColor()));
                    rotatingModel.setChanged();
                }
            }

            @Override
            public void update(float pt) {
                super.update(pt);
                applyTierColor();
            }

            @Override
            public void tick(Context context) {
                super.tick(context);
                applyTierColor();
            }
        }
    }

    public static class TieredCogwheelVisual {
        public static BlockEntityVisual<TieredCogwheelBlockEntity> create(VisualizationContext context, TieredCogwheelBlockEntity blockEntity, float partialTick) {
            Block block = blockEntity.getBlockState().getBlock();
            if (block instanceof TieredEncasedCogwheelBlock) {
                return TieredEncasedCogVisual.create(context, blockEntity, partialTick);
            }
            return TieredPlainCogVisual.create(context, blockEntity, partialTick);
        }
    }

    public static class TieredPlainCogVisual {
        public static BlockEntityVisual<TieredCogwheelBlockEntity> create(VisualizationContext context, TieredCogwheelBlockEntity blockEntity, float partialTick) {
            if (ICogWheel.isLargeCog(blockEntity.getBlockState())) {
                return new LargeCogVisual(context, blockEntity, partialTick);
            } else {
                return new SmallCogVisual(context, blockEntity, partialTick);
            }
        }

        public static class SmallCogVisual extends SingleAxisRotatingVisual<TieredCogwheelBlockEntity> {
            protected final RotatingInstance additionalShaft;
            private final Tier tier;

            public SmallCogVisual(VisualizationContext context, TieredCogwheelBlockEntity blockEntity, float partialTick) {
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

        public static class LargeCogVisual extends SingleAxisRotatingVisual<TieredCogwheelBlockEntity> {
            protected final RotatingInstance additionalShaft;
            private final Tier tier;

            public LargeCogVisual(VisualizationContext context, TieredCogwheelBlockEntity blockEntity, float partialTick) {
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

    public static class TieredEncasedCogVisual {
        public static BlockEntityVisual<TieredCogwheelBlockEntity> create(VisualizationContext context, TieredCogwheelBlockEntity blockEntity, float partialTick) {
            if (ICogWheel.isLargeCog(blockEntity.getBlockState())) {
                return new LargeEncasedCogVisual(context, blockEntity, partialTick);
            } else {
                return new SmallEncasedCogVisual(context, blockEntity, partialTick);
            }
        }

        public static class SmallEncasedCogVisual extends SingleAxisRotatingVisual<TieredCogwheelBlockEntity> {
            private final Tier tier;
            @org.jetbrains.annotations.Nullable
            protected final RotatingInstance rotatingTopShaft;
            @org.jetbrains.annotations.Nullable
            protected final RotatingInstance rotatingBottomShaft;

            public SmallEncasedCogVisual(VisualizationContext context, TieredCogwheelBlockEntity blockEntity, float partialTick) {
                super(context, blockEntity, partialTick, Models.partial(getEncasedCogwheelShaftlessPartial(blockEntity)));

                Block block = blockEntity.getBlockState().getBlock();
                this.tier = resolveTier(blockEntity.getBlockState());

                RotatingInstance topShaft = null;
                RotatingInstance bottomShaft = null;

                if (block instanceof TieredEncasedCogwheelBlock encased) {
                    for (Direction d : Iterate.directionsInAxis(rotationAxis())) {
                        if (!encased.hasShaftTowards(blockEntity.getLevel(), blockEntity.getBlockPos(), blockEntity.getBlockState(), d))
                            continue;
                        RotatingInstance instance = instancerProvider().instancer(AllInstanceTypes.ROTATING,
                                        Models.partial(AllTieredPartialModels.forTier(tier).SHAFT_HALF))
                                .createInstance();
                        instance.setup(blockEntity)
                                .setPosition(getVisualPosition())
                                .rotateToFace(Direction.SOUTH, d)
                                .setChanged();

                        if (d.getAxisDirection() == AxisDirection.POSITIVE) {
                            topShaft = instance;
                        } else {
                            bottomShaft = instance;
                        }
                    }
                }

                this.rotatingTopShaft = topShaft;
                this.rotatingBottomShaft = bottomShaft;
                applyTierColor();
            }

            private static PartialModel getEncasedCogwheelShaftlessPartial(TieredCogwheelBlockEntity be) {
                Block block = be.getBlockState().getBlock();
                if (!(block instanceof TieredEncasedCogwheelBlock encased)) return AllPartialModels.SHAFTLESS_COGWHEEL;
                AllTieredPartialModels.TieredPartials partials = AllTieredPartialModels.forTier(encased.getTier());
                return partials.COGWHEEL_SHAFTLESS;
            }

            private void applyTierColor() {
                if (tier != null) {
                    rotatingModel.setColor(new Color(tier.getCogwheelColor()));
                    rotatingModel.setChanged();
                    if (rotatingTopShaft != null) {
                        rotatingTopShaft.setColor(new Color(tier.getShaftColor()));
                        rotatingTopShaft.setChanged();
                    }
                    if (rotatingBottomShaft != null) {
                        rotatingBottomShaft.setColor(new Color(tier.getShaftColor()));
                        rotatingBottomShaft.setChanged();
                    }
                }
            }

            @Override
            public void update(float pt) {
                super.update(pt);
                if (rotatingTopShaft != null) rotatingTopShaft.setup(blockEntity).setChanged();
                if (rotatingBottomShaft != null) rotatingBottomShaft.setup(blockEntity).setChanged();
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
                relight(rotatingTopShaft, rotatingBottomShaft);
            }

            @Override
            protected void _delete() {
                super._delete();
                if (rotatingTopShaft != null) rotatingTopShaft.delete();
                if (rotatingBottomShaft != null) rotatingBottomShaft.delete();
            }

            @Override
            public void collectCrumblingInstances(Consumer<Instance> consumer) {
                super.collectCrumblingInstances(consumer);
                consumer.accept(rotatingTopShaft);
                consumer.accept(rotatingBottomShaft);
            }
        }

        public static class LargeEncasedCogVisual extends SingleAxisRotatingVisual<TieredCogwheelBlockEntity> {
            private final Tier tier;
            @org.jetbrains.annotations.Nullable
            protected final RotatingInstance rotatingTopShaft;
            @org.jetbrains.annotations.Nullable
            protected final RotatingInstance rotatingBottomShaft;

            public LargeEncasedCogVisual(VisualizationContext context, TieredCogwheelBlockEntity blockEntity, float partialTick) {
                super(context, blockEntity, partialTick, Models.partial(getEncasedLargeCogwheelShaftlessPartial(blockEntity)));

                Block block = blockEntity.getBlockState().getBlock();
                this.tier = resolveTier(blockEntity.getBlockState());

                RotatingInstance topShaft = null;
                RotatingInstance bottomShaft = null;

                if (block instanceof TieredEncasedCogwheelBlock encased) {
                    for (Direction d : Iterate.directionsInAxis(rotationAxis())) {
                        if (!encased.hasShaftTowards(blockEntity.getLevel(), blockEntity.getBlockPos(), blockEntity.getBlockState(), d))
                            continue;
                        RotatingInstance instance = instancerProvider().instancer(AllInstanceTypes.ROTATING,
                                        Models.partial(AllTieredPartialModels.forTier(tier).SHAFT_HALF))
                                .createInstance();
                        instance.setup(blockEntity)
                                .setPosition(getVisualPosition())
                                .rotateToFace(Direction.SOUTH, d)
                                .setChanged();

                        if (encased.isLargeCog()) {
                            instance.setRotationOffset(BracketedKineticBlockEntityRenderer.getShaftAngleOffset(rotationAxis(), pos));
                        }

                        if (d.getAxisDirection() == AxisDirection.POSITIVE) {
                            topShaft = instance;
                        } else {
                            bottomShaft = instance;
                        }
                    }
                }

                this.rotatingTopShaft = topShaft;
                this.rotatingBottomShaft = bottomShaft;
                applyTierColor();
            }

            private static PartialModel getEncasedLargeCogwheelShaftlessPartial(TieredCogwheelBlockEntity be) {
                Block block = be.getBlockState().getBlock();
                if (!(block instanceof TieredEncasedCogwheelBlock encased)) return AllPartialModels.SHAFTLESS_LARGE_COGWHEEL;
                AllTieredPartialModels.TieredPartials partials = AllTieredPartialModels.forTier(encased.getTier());
                return partials.LARGE_COGWHEEL_SHAFTLESS;
            }

            private void applyTierColor() {
                if (tier != null) {
                    rotatingModel.setColor(new Color(tier.getCogwheelColor()));
                    rotatingModel.setChanged();
                    if (rotatingTopShaft != null) {
                        rotatingTopShaft.setColor(new Color(tier.getShaftColor()));
                        rotatingTopShaft.setChanged();
                    }
                    if (rotatingBottomShaft != null) {
                        rotatingBottomShaft.setColor(new Color(tier.getShaftColor()));
                        rotatingBottomShaft.setChanged();
                    }
                }
            }

            @Override
            public void update(float pt) {
                super.update(pt);
                if (rotatingTopShaft != null) rotatingTopShaft.setup(blockEntity).setChanged();
                if (rotatingBottomShaft != null) rotatingBottomShaft.setup(blockEntity).setChanged();
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
                relight(rotatingTopShaft, rotatingBottomShaft);
            }

            @Override
            protected void _delete() {
                super._delete();
                if (rotatingTopShaft != null) rotatingTopShaft.delete();
                if (rotatingBottomShaft != null) rotatingBottomShaft.delete();
            }

            @Override
            public void collectCrumblingInstances(Consumer<Instance> consumer) {
                super.collectCrumblingInstances(consumer);
                consumer.accept(rotatingTopShaft);
                consumer.accept(rotatingBottomShaft);
            }
        }
    }
}
