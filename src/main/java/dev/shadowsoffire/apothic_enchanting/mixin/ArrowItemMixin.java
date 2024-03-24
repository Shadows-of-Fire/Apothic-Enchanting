package dev.shadowsoffire.apothic_enchanting.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.shadowsoffire.apothic_enchanting.Ench;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;

@Mixin(value = ArrowItem.class, remap = false)
public class ArrowItemMixin {

    @Inject(method = "isInfinite", at = @At(value = "RETURN"), remap = false, cancellable = true)
    public void apoth_isInfinite(ItemStack stack, ItemStack bow, Player player, CallbackInfoReturnable<Boolean> ci) {
        if (!ci.getReturnValueZ()) {
            ci.setReturnValue(Ench.Enchantments.ENDLESS_QUIVER.get().isTrulyInfinite(stack, bow, player));
        }
    }

}
