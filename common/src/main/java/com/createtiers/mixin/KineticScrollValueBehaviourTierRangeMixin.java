package com.createtiers.mixin;

import com.createtiers.api.ITieredBlockEntity;
import com.createtiers.api.Tier;
import com.simibubi.create.content.kinetics.motor.KineticScrollValueBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Create's kinetic value board uses a hard-coded maximum of 256 even when the
 * backing ScrollValueBehaviour has a different range. Mirror the effective tier
 * RPM limit in the UI while leaving ordinary untiered Create controls unchanged.
 */
@Mixin(value = KineticScrollValueBehaviour.class, remap = false)
public abstract class KineticScrollValueBehaviourTierRangeMixin {

    @Inject(method = "createBoard", at = @At("RETURN"), cancellable = true)
    private void createtiers$useEffectiveTierRange(Player player, BlockHitResult hitResult,
            CallbackInfoReturnable<ValueSettingsBoard> cir) {
        KineticScrollValueBehaviour self = (KineticScrollValueBehaviour) (Object) this;
        if (!(self.blockEntity instanceof ITieredBlockEntity tiered)) {
            return;
        }

        Tier tier = tiered.getTier();
        if (tier == null) {
            return;
        }

        ValueSettingsBoard board = cir.getReturnValue();
        int max = Math.max(1, tier.getMaxRPM());
        int milestone = Math.max(1, Math.min(board.milestoneInterval(), max));
        cir.setReturnValue(new ValueSettingsBoard(
                board.title(), max, milestone, board.rows(), board.formatter()));
    }
}
