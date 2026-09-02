package com.createtiers.mixin;

import com.createtiers.api.IAttachedTierBlockEntity;
import com.createtiers.api.Tier;
import com.createtiers.api.TierRegistry;
import com.createtiers.foundation.utility.AdjustableKineticTierPolicy;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds optional persisted tier state to every Create kinetic block entity. */
@Mixin(value = KineticBlockEntity.class, remap = false)
public abstract class KineticBlockEntityTierMixin implements IAttachedTierBlockEntity {

    @Unique
    private static final String CREATETIERS$TIER_KEY = "CreateTiersTier";

    @Unique
    private ResourceLocation createtiers$attachedTierId;

    @Unique
    private Tier createtiers$attachedTier;

    @Override
    public Tier getTier() {
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

    @Inject(method = "write", at = @At("TAIL"))
    private void createtiers$writeAttachedTier(CompoundTag tag, HolderLookup.Provider registries,
            boolean clientPacket, CallbackInfo ci) {
        if (createtiers$attachedTierId != null) {
            tag.putString(CREATETIERS$TIER_KEY, createtiers$attachedTierId.toString());
        }
    }

    @Inject(method = "read", at = @At("TAIL"))
    private void createtiers$readAttachedTier(CompoundTag tag, HolderLookup.Provider registries,
            boolean clientPacket, CallbackInfo ci) {
        createtiers$loadTier(tag);
        AdjustableKineticTierPolicy.refresh((KineticBlockEntity) (Object) this, createtiers$attachedTier);
    }

    @Unique
    private void createtiers$loadTier(CompoundTag tag) {
        createtiers$attachedTierId = null;
        createtiers$attachedTier = null;
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
