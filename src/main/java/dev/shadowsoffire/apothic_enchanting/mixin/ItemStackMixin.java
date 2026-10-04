package dev.shadowsoffire.apothic_enchanting.mixin;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.shadowsoffire.apothic_enchanting.util.TooltipUtil;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

@Mixin(value = ItemStack.class, priority = 500, remap = false)
public class ItemStackMixin {

    /**
     * Rewrites the enchantment tooltip lines to include the effective level, as well as the (NBT + bonus) calculation.
     *
     * <p>In 26.1 {@code ItemStack#addToTooltip} is called once per {@link DataComponentType}, so this injection filters for
     * the enchantment component types up front and cancels the vanilla path only for those. Display filtering uses
     * {@link TooltipDisplay#shows(DataComponentType)} since the old {@code ItemEnchantments.showInTooltip} field was removed.
     */
    @SuppressWarnings("deprecation")
    @Inject(method = "addToTooltip", at = @At(value = "HEAD"), cancellable = true)
    public <T extends TooltipProvider> void apoth_enchTooltipRewrite(DataComponentType<T> component, Item.TooltipContext ctx, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag tooltipFlag, CallbackInfo ci) {
        if (component != DataComponents.ENCHANTMENTS && component != DataComponents.STORED_ENCHANTMENTS) return;
        if (!display.shows(component)) return;

        ItemStack ths = (ItemStack) (Object) this;
        T value = ths.get(component);
        if (!(value instanceof ItemEnchantments enchants)) return;

        HolderLookup.Provider regs = ctx.registries();
        HolderSet<Enchantment> iterationOrder = getTagOrEmpty(regs, Registries.ENCHANTMENT, EnchantmentTags.TOOLTIP_ORDER);
        // STORED_ENCHANTMENTS (books) have no "effective" level — getAllEnchantments only reads the active
        // ENCHANTMENTS component, so comparing stored vs that emits the modified-level rewrite for every line.
        // Use the NBT levels directly so the normal getFullname path runs (which still picks up the over-max color flash).
        ItemEnchantments realLevels;
        if (component == DataComponents.STORED_ENCHANTMENTS || regs == null) {
            realLevels = enchants;
        }
        else {
            realLevels = ths.getAllEnchantments(regs.lookupOrThrow(Registries.ENCHANTMENT));
        }

        Consumer<Holder<Enchantment>> applyTooltip = ench -> TooltipUtil.applyEnchTooltip(ench, enchants, realLevels, tooltip, tooltipFlag);

        iterationOrder.forEach(applyTooltip);

        Set<Holder<Enchantment>> seen = new HashSet<>();

        enchants.entrySet().stream()
            .map(Entry::getKey)
            .filter(ench -> !iterationOrder.contains(ench))
            .forEach(ench -> {
                applyTooltip.accept(ench);
                seen.add(ench);
            });

        realLevels.entrySet().stream()
            .map(Entry::getKey)
            .filter(ench -> !iterationOrder.contains(ench))
            .filter(ench -> !seen.contains(ench))
            .forEach(applyTooltip);

        ci.cancel();
    }

    @Unique
    private static <T> HolderSet<T> getTagOrEmpty(@Nullable HolderLookup.Provider registries, ResourceKey<Registry<T>> registryKey, TagKey<T> key) {
        if (registries != null) {
            Optional<HolderSet.Named<T>> optional = registries.lookupOrThrow(registryKey).get(key);
            if (optional.isPresent()) {
                return optional.get();
            }
        }

        return HolderSet.direct();
    }
}
