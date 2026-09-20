package com.createtiers.mixin;

import com.createtiers.api.IAttachedTierBlockEntity;
import com.createtiers.api.IReplacementSourceBlockEntity;
import com.createtiers.api.Tier;
import com.createtiers.api.TierRegistry;
import com.createtiers.api.TieredNativeKineticBlock;
import com.createtiers.foundation.utility.AdjustableKineticTierPolicy;
import com.createtiers.foundation.utility.AttachedTierTransfer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds optional persisted tier state to every Create kinetic block entity. */
@Mixin(value = KineticBlockEntity.class, remap = false)
public abstract class KineticBlockEntityTierMixin implements IAttachedTierBlockEntity, IReplacementSourceBlockEntity {

    @Unique
    private static final String CREATETIERS$TIER_KEY = "CreateTiersTier";

    @Unique
    private static final String CREATETIERS$SOURCE_BLOCK_KEY = "CreateTiersReplacementSourceBlock";

    @Unique
    private ResourceLocation createtiers$attachedTierId;

    @Unique
    private Tier createtiers$attachedTier;

    @Unique
    private ResourceLocation createtiers$replacementSourceBlockId;

    @Override
    public ResourceLocation getCreateTiersReplacementSourceBlockId() {
        return createtiers$replacementSourceBlockId;
    }

    @Override
    public void setCreateTiersReplacementSourceBlockId(ResourceLocation id) {
        createtiers$replacementSourceBlockId = id;
        ((KineticBlockEntity) (Object) this).setChanged();
    }

    @Override
    public void clearCreateTiersReplacementSourceBlockId() {
        if (createtiers$replacementSourceBlockId == null) {
            return;
        }
        createtiers$replacementSourceBlockId = null;
        ((KineticBlockEntity) (Object) this).setChanged();
    }

    @Override
    public Tier getTier() {
        KineticBlockEntity self = (KineticBlockEntity) (Object) this;
        if (self.getBlockState().getBlock() instanceof TieredNativeKineticBlock nativeBlock) {
            return nativeBlock.getTier();
        }
        return createtiers$attachedTier;
    }

    @Override
    public ResourceLocation getAttachedTierId() {
        return createtiers$attachedTierId;
    }

    @Override
    public Tier getAttachedTier() {
        return createtiers$attachedTier;
    }

    @Override
    public void setAttachedTier(Tier tier) {
        KineticBlockEntity self = (KineticBlockEntity) (Object) this;
        if (self.getBlockState().getBlock() instanceof TieredNativeKineticBlock) {
            throw new IllegalStateException("Native Create Tiers relay blocks already have an intrinsic tier");
        }

        ResourceLocation id = TierRegistry.getId(tier);
        if (id == null) {
            throw new IllegalArgumentException("Cannot attach an unregistered Create Tiers tier");
        }
        if (id.equals(createtiers$attachedTierId)) {
            return;
        }

        createtiers$attachedTierId = id;
        createtiers$attachedTier = tier;
        AdjustableKineticTierPolicy.refresh((KineticBlockEntity) (Object) this, tier);
        createtiers$rebuildKinetics();
    }

    @Override
    public void clearAttachedTier() {
        if (createtiers$attachedTierId == null && createtiers$attachedTier == null) {
            return;
        }

        createtiers$attachedTierId = null;
        createtiers$attachedTier = null;
        AdjustableKineticTierPolicy.refresh((KineticBlockEntity) (Object) this, null);
        createtiers$rebuildKinetics();
    }

    @Unique
    private void createtiers$rebuildKinetics() {
        KineticBlockEntity self = (KineticBlockEntity) (Object) this;
        self.setChanged();
        if (self.getLevel() == null || self.getLevel().isClientSide) {
            return;
        }

        self.detachKinetics();
        self.removeSource();
        self.updateSpeed = true;
        self.networkDirty = true;
        self.sendData();
    }

    @Inject(method = "switchToBlockState", at = @At("HEAD"))
    private static void createtiers$captureAttachedTierBeforeStateReplacement(
            Level level, BlockPos pos, BlockState state, CallbackInfo ci) {
        AttachedTierTransfer.begin(level, pos);
    }

    @Inject(method = "switchToBlockState", at = @At("RETURN"))
    private static void createtiers$restoreAttachedTierAfterStateReplacement(
            Level level, BlockPos pos, BlockState state, CallbackInfo ci) {
        AttachedTierTransfer.end(level, pos);
    }

    @Inject(method = "initialize", at = @At("TAIL"))
    private void createtiers$refreshIntrinsicTierOnInitialize(CallbackInfo ci) {
        AdjustableKineticTierPolicy.refresh((KineticBlockEntity) (Object) this, getTier());
    }

    @Inject(method = "write", at = @At("TAIL"))
    private void createtiers$writeAttachedTier(CompoundTag tag, HolderLookup.Provider registries,
            boolean clientPacket, CallbackInfo ci) {
        if (createtiers$attachedTierId != null) {
            tag.putString(CREATETIERS$TIER_KEY, createtiers$attachedTierId.toString());
        }
        if (createtiers$replacementSourceBlockId != null) {
            tag.putString(CREATETIERS$SOURCE_BLOCK_KEY, createtiers$replacementSourceBlockId.toString());
        }
    }

    @Inject(method = "read", at = @At("RETURN"))
    private void createtiers$readAttachedTier(CompoundTag tag, HolderLookup.Provider registries,
            boolean clientPacket, CallbackInfo ci) {
        createtiers$loadTier(tag);
        createtiers$loadReplacementSource(tag);
        AdjustableKineticTierPolicy.refresh((KineticBlockEntity) (Object) this, getTier());
    }

    @Unique
    private void createtiers$loadReplacementSource(CompoundTag tag) {
        createtiers$replacementSourceBlockId = null;
        if (!tag.contains(CREATETIERS$SOURCE_BLOCK_KEY)) {
            return;
        }

        ResourceLocation id = ResourceLocation.tryParse(tag.getString(CREATETIERS$SOURCE_BLOCK_KEY));
        if (id != null) {
            createtiers$replacementSourceBlockId = id;
        }
    }

    @Unique
    private void createtiers$loadTier(CompoundTag tag) {
        createtiers$attachedTierId = null;
        createtiers$attachedTier = null;

        KineticBlockEntity self = (KineticBlockEntity) (Object) this;
        if (self.getBlockState().getBlock() instanceof TieredNativeKineticBlock) {
            return;
        }

        if (!tag.contains(CREATETIERS$TIER_KEY)) {
            return;
        }

        ResourceLocation id = ResourceLocation.tryParse(tag.getString(CREATETIERS$TIER_KEY));
        if (id == null) {
            return;
        }
        Tier tier = TierRegistry.get(id);
        if (tier != null) {
            createtiers$attachedTierId = id;
            createtiers$attachedTier = tier;
        }
    }
}
