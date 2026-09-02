package com.createtiers.integration.jade;

import com.createtiers.CreateTiers;
import com.createtiers.api.IAttachedTierBlockEntity;
import com.createtiers.api.ITieredBlockEntity;
import com.createtiers.api.Tier;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/** Optional Jade integration for intrinsic and calibrated Create Tiers kinetics. */
@WailaPlugin
public final class CreateTiersJadePlugin implements IWailaPlugin {

    private static final ResourceLocation UID = CreateTiers.asResource("tier_info");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(TierInfoProvider.INSTANCE, KineticBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        // Register broadly on the client so unusual/future Create kinetic block classes are
        // not missed. The provider is inert unless the authoritative server payload contains
        // Create Tiers data from a KineticBlockEntity.
        registration.registerBlockComponent(TierInfoProvider.INSTANCE, Block.class);
    }

    private enum TierInfoProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        INSTANCE;

        private static final String TIER_NAME = "CreateTiersTierName";
        private static final String MAX_RPM = "CreateTiersMaxRPM";
        private static final String MAX_SU = "CreateTiersMaxSU";
        private static final String CALIBRATED = "CreateTiersCalibrated";

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof ITieredBlockEntity tiered)) {
                return;
            }

            Tier tier = tiered.getTier();
            if (tier == null) {
                return;
            }

            data.putString(TIER_NAME, tier.getDisplayName());
            data.putInt(MAX_RPM, tier.getMaxRPM());
            data.putInt(MAX_SU, tier.getMaxSU());

            // Native Create Tiers block entities expose an intrinsic getTier() while the
            // generic attachment remains null. Ordinary calibrated Create machines expose
            // the attached tier as their effective tier.
            boolean calibrated = accessor.getBlockEntity() instanceof IAttachedTierBlockEntity attached
                    && attached.getAttachedTier() != null
                    && tier.equals(attached.getAttachedTier());
            data.putBoolean(CALIBRATED, calibrated);
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (!data.contains(TIER_NAME)) {
                return;
            }

            tooltip.add(Component.translatable("createtiers.jade.tier", data.getString(TIER_NAME)));
            tooltip.add(Component.translatable(
                    "createtiers.jade.source",
                    Component.translatable(data.getBoolean(CALIBRATED)
                            ? "createtiers.jade.source.calibrated"
                            : "createtiers.jade.source.intrinsic")));
            tooltip.add(Component.translatable("createtiers.jade.max_rpm", data.getInt(MAX_RPM)));
            tooltip.add(Component.translatable("createtiers.jade.max_su", data.getInt(MAX_SU)));
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }
}
