package dev.shadowsoffire.apothic_enchanting.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.shadowsoffire.apothic_enchanting.Ench;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.TemptGoal;

@Mixin(value = TemptGoal.class, remap = false)
public class TemptGoalMixin {

    @Inject(method = "shouldFollow", at = @At(value = "RETURN"), cancellable = true)
    public void apoth_tempting(LivingEntity entity, CallbackInfoReturnable<Boolean> ci) {
        if (!ci.getReturnValueZ() && Ench.Enchantments.TEMPTING.get().shouldFollow(entity)) ci.setReturnValue(true);
    }

}
