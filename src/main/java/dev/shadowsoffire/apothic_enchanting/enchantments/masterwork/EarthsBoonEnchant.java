package dev.shadowsoffire.apothic_enchanting.enchantments.masterwork;

import dev.shadowsoffire.apothic_enchanting.ApothicEnchanting;
import dev.shadowsoffire.apothic_enchanting.Ench;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;

public class EarthsBoonEnchant extends Enchantment {

    public EarthsBoonEnchant() {
        super(Rarity.VERY_RARE, ApothicEnchanting.PICKAXE, new EquipmentSlot[] { EquipmentSlot.MAINHAND });
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }

    @Override
    public int getMinCost(int level) {
        return 60 + (level - 1) * 20;
    }

    @Override
    public int getMaxCost(int enchantmentLevel) {
        return 200;
    }

    @Override
    public Component getFullname(int level) {
        return ((MutableComponent) super.getFullname(level)).withStyle(ChatFormatting.DARK_GREEN);
    }

    public void provideBenefits(BreakEvent e) {
        Player player = e.getPlayer();
        ItemStack stack = player.getMainHandItem();
        int level = stack.getEnchantmentLevel(this);
        if (player.level().isClientSide) return;
        if (e.getState().is(Tags.Blocks.STONE) && level > 0 && player.getRandom().nextFloat() <= 0.01F * level) {
            ItemStack newDrop = new ItemStack(BuiltInRegistries.ITEM.getTag(Ench.Tags.BOON_DROPS).flatMap(set -> set.getRandomElement(player.getRandom())).map(Holder::value).orElse(Items.AIR));
            Block.popResource(player.level(), e.getPos(), newDrop);
        }
    }
}
