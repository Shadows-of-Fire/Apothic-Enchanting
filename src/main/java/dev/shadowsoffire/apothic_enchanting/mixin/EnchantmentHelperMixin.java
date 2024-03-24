package dev.shadowsoffire.apothic_enchanting.mixin;

import java.util.Collections;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import dev.shadowsoffire.apothic_enchanting.table.RealEnchantmentHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

@Mixin(value = EnchantmentHelper.class, remap = false)
public class EnchantmentHelperMixin {

    /**
     * @author Shadows
     * @reason Enables apotheosis special handling of enchanting rules. More lenient injection is not possible.
     * @param power         The current enchanting power.
     * @param stack         The ItemStack being enchanted.
     * @param allowTreasure If treasure enchantments are allowed.
     * @return All possible enchantments that are eligible to be placed on this item at a specific power level.
     */
    @Overwrite
    public static List<EnchantmentInstance> getAvailableEnchantmentResults(int power, ItemStack stack, boolean allowTreasure) {
        return RealEnchantmentHelper.getAvailableEnchantmentResults(power, stack, allowTreasure, Collections.emptySet());
    }

    /**
     * @author Shadows
     * @reason Enables global consistency with the apotheosis enchanting system, even outside the table.
     * @param pRandom        The random
     * @param pItemStack     The stack being enchanted
     * @param pLevel         The enchanting level
     * @param pAllowTreasure If treasure enchantments are allowed.
     * @return A list of enchantments to apply to this item.
     */
    @Overwrite
    public static List<EnchantmentInstance> selectEnchantment(RandomSource pRandom, ItemStack pItemStack, int pLevel, boolean pAllowTreasure) {
        return RealEnchantmentHelper.selectEnchantment(pRandom, pItemStack, pLevel, 15F, 0, 0, pAllowTreasure, Collections.emptySet());
    }

    /**
     * Reverses the loop iteration order in {@link EnchantmentHelper#getTagEnchantmentLevel} to ensure consistency with duplicate enchantments when compared to
     * {@link EnchantmentHelper#getEnchantments}.
     * 
     * @param tags  The itemstack's enchantment tags from {@link ItemStack#getEnchantmentTags()}.
     * @param index The current list iteration order, between [0, tags.size())
     * @return The compound tag at the reversed index.
     */
    @Redirect(method = "getTagEnchantmentLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/ListTag;getCompound(I)Lnet/minecraft/nbt/CompoundTag;"))
    private static CompoundTag apoth_reverseLoopOrder(ListTag tags, int index) {
        return tags.getCompound(tags.size() - 1 - index);
    }

}
