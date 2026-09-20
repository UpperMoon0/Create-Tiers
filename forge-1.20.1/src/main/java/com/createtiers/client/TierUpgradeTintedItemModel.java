package com.createtiers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds tier tint channels to ordinary Create item models without replacing their
 * geometry. Untiered stacks remain visually identical because their item-color
 * handler returns the vanilla no-tint sentinel.
 */
public final class TierUpgradeTintedItemModel extends BakedModelWrapper<BakedModel> {

    private final TierUpgradeItemTintPolicy.Mode mode;

    public TierUpgradeTintedItemModel(BakedModel originalModel, TierUpgradeItemTintPolicy.Mode mode) {
        super(originalModel);
        this.mode = mode;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random) {
        return tint(originalModel.getQuads(state, side, random));
    }

    @NotNull
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
            @NotNull RandomSource random, @NotNull ModelData data, @Nullable RenderType renderType) {
        return tint(originalModel.getQuads(state, side, random, data, renderType));
    }

    @Override
    public BakedModel applyTransform(ItemDisplayContext context, PoseStack poseStack, boolean leftHand) {
        BakedModel transformed = originalModel.applyTransform(context, poseStack, leftHand);
        return transformed == originalModel ? this : wrap(transformed);
    }

    @Override
    public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
        List<BakedModel> passes = originalModel.getRenderPasses(stack, fabulous);
        if (passes.size() == 1 && passes.get(0) == originalModel) {
            return List.of(this);
        }
        List<BakedModel> wrapped = new ArrayList<>(passes.size());
        for (BakedModel pass : passes) {
            wrapped.add(pass == originalModel ? this : wrap(pass));
        }
        return wrapped;
    }

    private BakedModel wrap(BakedModel model) {
        if (model instanceof TierUpgradeTintedItemModel) {
            return model;
        }
        return new TierUpgradeTintedItemModel(model, mode);
    }

    private List<BakedQuad> tint(List<BakedQuad> quads) {
        List<BakedQuad> result = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            if (quad.isTinted()) {
                result.add(quad);
                continue;
            }
            int tintIndex = TierUpgradeItemTintPolicy.tintIndex(mode, quad.getSprite().contents().name());
            if (tintIndex < 0) {
                result.add(quad);
                continue;
            }
            result.add(new BakedQuad(
                    quad.getVertices(),
                    tintIndex,
                    quad.getDirection(),
                    quad.getSprite(),
                    quad.isShade(),
                    quad.hasAmbientOcclusion()));
        }
        return result;
    }
}
