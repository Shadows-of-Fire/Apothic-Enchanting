package dev.shadowsoffire.apothic_enchanting.util;

import java.util.Set;
import java.util.function.Consumer;

import dev.shadowsoffire.apothic_enchanting.ApothicEnchanting;
import dev.shadowsoffire.apothic_enchanting.api.IEnchantingBlock;
import dev.shadowsoffire.apothic_enchanting.table.ApothEnchantmentMenu;
import dev.shadowsoffire.apothic_enchanting.table.ApothEnchantmentMenu.TableStats;
import dev.shadowsoffire.apothic_enchanting.table.EnchantingStatRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class TooltipUtil {

    public static void appendBlockStats(Level world, BlockState state, BlockPos pos, Consumer<Component> tooltip) {
        float maxEterna = EnchantingStatRegistry.getMaxEterna(state, world, pos);
        float eterna = EnchantingStatRegistry.getEterna(state, world, pos);
        float quanta = EnchantingStatRegistry.getQuanta(state, world, pos);
        float arcana = EnchantingStatRegistry.getArcana(state, world, pos);
        float rectification = EnchantingStatRegistry.getQuantaRectification(state, world, pos);
        int clues = EnchantingStatRegistry.getBonusClues(state, world, pos);
        boolean treasure = ((IEnchantingBlock) state.getBlock()).allowsTreasure(state, world, pos);
        if (eterna != 0 || quanta != 0 || arcana != 0 || rectification != 0 || clues != 0) {
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
        if (rectification != 0) {
            tooltip.accept(TooltipUtil.lang("info", "rectification" + (rectification > 0 ? ".p" : ""), String.format("%.2f", rectification)).withStyle(ChatFormatting.YELLOW));
        }
        if (clues != 0) {
            tooltip.accept(TooltipUtil.lang("info", "clues" + (clues > 0 ? ".p" : ""), String.format("%d", clues)).withStyle(ChatFormatting.DARK_AQUA));
        }
        if (treasure) {
            tooltip.accept(TooltipUtil.lang("info", "allows_treasure").withStyle(ChatFormatting.GOLD));
        }
        Set<Enchantment> blacklist = ((IEnchantingBlock) state.getBlock()).getBlacklistedEnchantments(state, world, pos);
        if (blacklist.size() > 0) {
            tooltip.accept(TooltipUtil.lang("info", "filter").withStyle(s -> s.withColor(0x58B0CC)));
            for (Enchantment e : blacklist) {
                MutableComponent name = (MutableComponent) e.getFullname(1);
                name.getSiblings().clear();
                name.withStyle(s -> s.withColor(0x5878AA));
                tooltip.accept(Component.literal(" - ").append(name).withStyle(s -> s.withColor(0x5878AA)));
            }
        }
    }

    public static void appendTableStats(Level world, BlockPos pos, Consumer<Component> tooltip) {
        TableStats stats = ApothEnchantmentMenu.gatherStats(world, pos, 0);
        tooltip.accept(TooltipUtil.lang("info", "eterna.t", String.format("%.2f", stats.eterna()), String.format("%.2f", EnchantingStatRegistry.getAbsoluteMaxEterna())).withStyle(ChatFormatting.GREEN));
        tooltip.accept(TooltipUtil.lang("info", "quanta.t", String.format("%.2f", Math.min(100, stats.quanta()))).withStyle(ChatFormatting.RED));
        tooltip.accept(TooltipUtil.lang("info", "arcana.t", String.format("%.2f", Math.min(100, stats.arcana()))).withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.accept(TooltipUtil.lang("info", "rectification.t", String.format("%.2f", Mth.clamp(stats.rectification(), -100, 100))).withStyle(ChatFormatting.YELLOW));
        tooltip.accept(TooltipUtil.lang("info", "clues.t", String.format("%d", stats.clues())).withStyle(ChatFormatting.DARK_AQUA));
    }

    public static MutableComponent lang(String type, String path, Object... args) {
        return Component.translatable(type + "." + ApothicEnchanting.MODID + "." + path, args);
    }
}
