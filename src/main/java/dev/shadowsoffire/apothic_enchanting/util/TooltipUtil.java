package dev.shadowsoffire.apothic_enchanting.util;

import java.util.Set;
import java.util.function.Consumer;

import dev.shadowsoffire.apothic_enchanting.ApothEnchConfig;
import dev.shadowsoffire.apothic_enchanting.ApothicEnchanting;
import dev.shadowsoffire.apothic_enchanting.Ench;
import dev.shadowsoffire.apothic_enchanting.api.EnchantmentStatBlock;
import dev.shadowsoffire.apothic_enchanting.asm.EnchHooks;
import dev.shadowsoffire.apothic_enchanting.mixin.ItemStackMixin;
import dev.shadowsoffire.apothic_enchanting.table.EnchantingStatRegistry;
import dev.shadowsoffire.apothic_enchanting.table.EnchantmentTableStats;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.enchanting.GetEnchantmentLevelEvent;

public class TooltipUtil {

    public static void appendBlockStats(LevelReader level, BlockState state, BlockPos pos, Consumer<Component> tooltip) {
        EnchantmentStatBlock enchBlock = ((EnchantmentStatBlock) state.getBlock());

        float maxEterna = EnchantingStatRegistry.getMaxEterna(state, level, pos);
        float eterna = EnchantingStatRegistry.getEterna(state, level, pos);
        float quanta = EnchantingStatRegistry.getQuanta(state, level, pos);
        float arcana = EnchantingStatRegistry.getArcana(state, level, pos);
        int clues = EnchantingStatRegistry.getBonusClues(state, level, pos);
        boolean treasure = enchBlock.allowsTreasure(state, level, pos);
        boolean stable = enchBlock.providesStability(state, level, pos);

        if (eterna != 0 || quanta != 0 || arcana != 0 || clues != 0 || treasure || stable) {
            tooltip.accept(TooltipUtil.lang("info", "ench_stats").withStyle(ChatFormatting.GOLD));
        }

        if (eterna != 0) {
            if (eterna > 0) {
                tooltip.accept(TooltipUtil.lang("info", "eterna.p", String.format("%.2f", eterna), String.format("%.2f", maxEterna)).withStyle(ChatFormatting.GREEN));
            }
            else tooltip.accept(TooltipUtil.lang("info", "eterna", String.format("%.2f", eterna)).withStyle(ChatFormatting.GREEN));
        }

        if (quanta != 0) {
            tooltip.accept(TooltipUtil.lang("info", "quanta" + (quanta > 0 ? ".p" : ""), String.format("%.2f", quanta)).withStyle(ChatFormatting.RED));
        }

        if (arcana != 0) {
            tooltip.accept(TooltipUtil.lang("info", "arcana" + (arcana > 0 ? ".p" : ""), String.format("%.2f", arcana)).withStyle(ChatFormatting.DARK_PURPLE));
        }

        if (clues != 0) {
            tooltip.accept(TooltipUtil.lang("info", "clues" + (clues > 0 ? ".p" : ""), String.format("%d", clues)).withStyle(ChatFormatting.DARK_AQUA));
        }

        if (treasure) {
            tooltip.accept(TooltipUtil.lang("info", "allows_treasure").withStyle(ChatFormatting.GOLD));
        }

        if (stable) {
            tooltip.accept(TooltipUtil.lang("info", "provides_stability").withStyle(ChatFormatting.GOLD));
        }

        Set<Holder<Enchantment>> blacklist = enchBlock.getBlacklistedEnchantments(state, level, pos);
        if (blacklist.size() > 0) {
            tooltip.accept(TooltipUtil.lang("info", "filter").withStyle(s -> s.withColor(0x58B0CC)));
            for (Holder<Enchantment> e : blacklist) {
                MutableComponent name = (MutableComponent) Enchantment.getFullname(e, 1);
                name.getSiblings().clear();
                name.withStyle(s -> s.withColor(0x5878AA));
                tooltip.accept(Component.literal(" - ").append(name).withStyle(s -> s.withColor(0x5878AA)));
            }
        }
    }

    public static void appendTableStats(LevelReader level, BlockPos pos, Consumer<Component> tooltip) {
        appendTableStats(EnchantmentTableStats.gatherStats(level, pos), tooltip);
    }

    public static void appendTableStats(EnchantmentTableStats stats, Consumer<Component> tooltip) {
        tooltip.accept(TooltipUtil.lang("info", "eterna.t", String.format("%.2f", stats.tableEterna()), 100).withStyle(ChatFormatting.GREEN));
        tooltip.accept(TooltipUtil.lang("info", "quanta.t", String.format("%.2f", Math.min(100, stats.quanta()))).withStyle(ChatFormatting.RED));
        tooltip.accept(TooltipUtil.lang("info", "arcana.t", String.format("%.2f", Math.min(100, stats.arcana()))).withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.accept(TooltipUtil.lang("info", "clues.t", String.format("%d", stats.clues())).withStyle(ChatFormatting.DARK_AQUA));
    }

    public static MutableComponent lang(String type, String path, Object... args) {
        return Component.translatable(type + "." + ApothicEnchanting.MODID + "." + path, args);
    }

    public static void applyOverMaxLevelColor(Holder<Enchantment> ench, int level, Component name) {
        if (!ench.is(EnchantmentTags.CURSE) && level > ench.value().definition().maxLevel() && name instanceof MutableComponent mc) {
            mc.setStyle(mc.getStyle().withColor(Ench.Colors.LIGHT_BLUE_FLASH));
        }
    }

    /**
     * Applies the enchantment tooltip line(s) for a single enchantment.
     * <p>
     * The generated tooltip will be different if the enchantment's effective level has been changed by the {@link GetEnchantmentLevelEvent}.
     * 
     * @param ench     The enchantment.
     * @param nbt      The enchantment data component from the item
     * @param gameplay The effective levels from {@link ItemStack#getAllEnchantments()}.
     * @param tooltip  The tooltip consumer.
     * @implNote This method is called from {@link ItemStackMixin#apoth_enchTooltipRewrite} and replaces vanilla handling of enchantment tooltips.
     */
    public static void applyEnchTooltip(Holder<Enchantment> ench, ItemEnchantments nbt, ItemEnchantments gameplay, Consumer<Component> tooltip) {
        int nbtLevel = nbt.getLevel(ench);
        int realLevel = gameplay.getLevel(ench);

        if (nbtLevel == realLevel) {
            // Default logic when levels are the same
            if (realLevel > 0) {
                tooltip.accept(Enchantment.getFullname(ench, realLevel));
            }
        }
        else {
            // Show the change vs nbt level
            appendModifiedEnchTooltip(tooltip, ench, realLevel, nbtLevel);
        }

        if ((realLevel > 0 || nbtLevel != realLevel) && FMLEnvironment.dist.isClient() && ApothEnchConfig.enableInlineEnchDescs) {
            String key = ench.getKey().location().toLanguageKey("enchantment") + ".desc";
            if (I18n.exists(key)) {
                tooltip.accept(Component.translatable(key).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }

    /**
     * Appends a modified enchantment tooltip. Used when the enchantment level in the NBT is different from the actual level.
     * <p>
     * The generated tooltip will show the tooltip line regardless of the real level, and will append the difference between the real and NBT levels.
     * 
     * @param tooltip   The tooltip consumer to append to.
     * @param ench      The enchantment
     * @param realLevel The effective level for gameplay purposes.
     * @param nbtLevel  The NBT level.
     */
    private static void appendModifiedEnchTooltip(Consumer<Component> tooltip, Holder<Enchantment> ench, int realLevel, int nbtLevel) {
        MutableComponent mc = Enchantment.getFullname(ench, realLevel).copy();
        mc.getSiblings().clear();
        Component nbtLevelComp = Component.translatable("enchantment.level." + nbtLevel);
        Component realLevelComp = Component.translatable("enchantment.level." + realLevel);
        if (realLevel != 1 || EnchHooks.getMaxLevel(ench.value()) != 1) {
            // Enchantments with a max level of 1 (and an effective level of 1) don't show the level in the tooltip.
            mc.append(CommonComponents.SPACE).append(realLevelComp);
        }

        int diff = realLevel - nbtLevel;
        char sign = diff > 0 ? '+' : '-';
        Component diffComp = Component.translatable("(%s " + sign + " %s)", nbtLevelComp, Component.translatable("enchantment.level." + Math.abs(diff))).withStyle(ChatFormatting.DARK_GRAY);
        mc.append(CommonComponents.SPACE).append(diffComp);
        if (realLevel == 0) {
            mc.withStyle(ChatFormatting.DARK_GRAY); // TODO: Slightly less boring gradient color for this, maybe?
        }
        tooltip.accept(mc);
    }
}
