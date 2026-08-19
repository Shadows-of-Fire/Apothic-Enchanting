package dev.shadowsoffire.apothic_enchanting.enchantments;

import org.apache.commons.lang3.mutable.MutableFloat;

import dev.shadowsoffire.apothic_enchanting.Ench;
import dev.shadowsoffire.apothic_enchanting.Ench.EnchantEffects;
import dev.shadowsoffire.apothic_enchanting.mixin.CrossbowItemMixin;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public class CrescendoHooks {

    /**
     * Called when projectiles are loaded into a crossbow from {@link CrossbowItem#tryLoadProjectiles}
     * <p>
     * If {@link EnchantEffects#CRESCENDO} is present, this hook will compute the number of bonus shots, and store the number and the original projectiles in the
     * crossbow for later usage.
     */
    public static void prepareCrescendoShots(LivingEntity shooter, ItemStack crossbow) {
        if (EnchantmentHelper.has(crossbow, Ench.EnchantEffects.CRESCENDO)) {
            MutableFloat f = new MutableFloat();
            EnchantmentHelper.runIterationOnItem(crossbow, (ench, level) -> {
                ench.value().modifyItemFilteredCount(Ench.EnchantEffects.CRESCENDO, (ServerLevel) shooter.level(), level, crossbow, f);
            });

            if (f.intValue() > 0) {
                crossbow.set(Ench.Components.CRESCENDO_SHOTS, f.intValue());
                crossbow.set(Ench.Components.CRESCENDO_PROJECTILES, crossbow.get(DataComponents.CHARGED_PROJECTILES));
            }
        }
    }

    /**
     * Handles the usage of the Crescendo of Bolts enchantment.
     * The enchantment gives the crossbow extra shots per charge, one per enchantment level.
     * Called from {@link CrossbowItem#use}, before the first return.
     * Injected by {@link CrossbowItemMixin}
     */
    public static void reloadFromCrescendoCharge(ServerLevel serverLevel, ItemStack crossbow) {
        int shots = crossbow.getOrDefault(Ench.Components.CRESCENDO_SHOTS, 0);
        if (shots > 0) {
            ChargedProjectiles projectiles = crossbow.get(Ench.Components.CRESCENDO_PROJECTILES);
            crossbow.set(DataComponents.CHARGED_PROJECTILES, projectiles);

            if (shots == 1) {
                crossbow.remove(Ench.Components.CRESCENDO_SHOTS);
                crossbow.remove(Ench.Components.CRESCENDO_PROJECTILES);
            }
            else {
                crossbow.set(Ench.Components.CRESCENDO_SHOTS, shots - 1);
            }
        }
    }

    /**
     * Arrow fired hook for the Crescendo of Bolts enchantment.
     * This is required to mark generated arrows as creative-only so arrows are not duplicated.
     * Injected by {@link CrossbowItemMixin}
     */
    public static void markGeneratedArrows(Projectile arrow, ItemStack crossbow) {
        if (crossbow.has(Ench.Components.CRESCENDO_SHOTS) && arrow instanceof AbstractArrow arr) {
            arr.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
        }
    }

}
