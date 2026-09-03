package dev.shadowsoffire.apothic_enchanting.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.shadowsoffire.apothic_enchanting.ApothicEnchanting;
import dev.shadowsoffire.apothic_enchanting.util.TooltipUtil;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.enchantment.Enchantment;

@Mixin(value = Enchantment.class, priority = 1500, remap = false)
public class EnchantmentMixin {

    /**
     * Adjusts the color of the enchantment text if above the vanilla max.
     */
    @Inject(method = "getFullname", at = @At("RETURN"), cancellable = true, require = 1)
    private static void apoth_modifyEnchColorForAboveMaxLevel(Holder<Enchantment> ench, int level, CallbackInfoReturnable<Component> cir) {
        TooltipUtil.applyOverMaxLevelColor(ench, level, cir.getReturnValue());
        // A star prefix is shown to indicate that an Enchantment is above the Apoth configured max value.
        if (level > ApothicEnchanting.getEnchInfo(ench).getMaxLevel()) {
            MutableComponent star = ApothicEnchanting.lang("text", "star_prefix").withStyle(cir.getReturnValue().getStyle());
            star.append(cir.getReturnValue());
            // We need to flatten the original components' siblings into the star prefix so that later logic can remove the level component if needed.
            for (Component c : cir.getReturnValue().getSiblings()) {
                star.append(c);
            }
            cir.getReturnValue().getSiblings().clear();
            cir.setReturnValue(star);
        }
    }

}
