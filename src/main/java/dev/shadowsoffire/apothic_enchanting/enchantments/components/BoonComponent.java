package dev.shadowsoffire.apothic_enchanting.enchantments.components;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.apothic_enchanting.ApothicEnchanting;
import dev.shadowsoffire.apothic_enchanting.Ench;
import dev.shadowsoffire.apothic_enchanting.table.ApothEnchantmentHelper;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ConditionalEffect;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.effects.EnchantmentValueEffect;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/**
 * @param entries A list of {@link BoonData} entries, determining the pairs of blocks and loot tables to drop items from.
 */
public record BoonComponent(List<BoonData> entries) {

    public static Codec<BoonComponent> CODEC = RecordCodecBuilder.create(inst -> inst.group(
        BoonData.CODEC.listOf().fieldOf("entries").forGetter(BoonComponent::entries))
        .apply(inst, BoonComponent::new));

    public static void provideBenefits(BlockDropsEvent e) {
        if (e.getBreaker() == null || e.getTool().isEmpty()) return;

        ItemStack stack = e.getTool();

        EnchantmentHelper.runIterationOnItem(stack, (ench, level) -> {
            BoonComponent comp = ench.value().effects().get(Ench.EnchantEffects.EARTHS_BOON);
            if (comp != null) {
                for (BoonData entry : comp.entries) {
                    if (entry.matches(e.getState())) {
                        LootTable table = e.getLevel().getServer().reloadableRegistries().getLootTable(entry.lootTable);
                        if (table == LootTable.EMPTY) {
                            ApothicEnchanting.LOGGER.error("[Boon] Loot table '{}' not found!", entry.lootTable.location());
                            break;
                        }

                        // TODO: Custom context which has the tool available so fortune can be evaluated.
                        LootContext ctx = Enchantment.blockHitContext(e.getLevel(), level, e.getBreaker(), Vec3.atCenterOf(e.getPos()), e.getState());

                        float chance = ApothEnchantmentHelper.processValue(entry.dropChance, ctx, level, 0);
                        RandomSource rand = e.getLevel().random;

                        if (rand.nextFloat() <= chance) {
                            Vec3 pos = e.getDrops().stream().findAny().map(ItemEntity::position).orElse(Vec3.atCenterOf(e.getPos()));
                            table.getRandomItems(ctx, item -> e.getDrops().add(new ItemEntity(e.getLevel(), pos.x, pos.y, pos.z, item)));
                        }

                        break;
                    }
                }
            }
        });
    }

    /**
     * @param targets    A set of blocks to match against.
     * @param lootTable  The loot table to drop items from.
     * @param dropChance A list of {@link ConditionalEffect}s to determine the drop chance.
     */
    public static record BoonData(HolderSet<Block> targets, ResourceKey<LootTable> lootTable, List<ConditionalEffect<EnchantmentValueEffect>> dropChance) {

        public static Codec<BoonData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("target").forGetter(BoonData::targets),
            ResourceKey.codec(Registries.LOOT_TABLE).fieldOf("loot_table").forGetter(BoonData::lootTable),
            ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.HIT_BLOCK).listOf().fieldOf("drop_chance").forGetter(BoonData::dropChance))
            .apply(inst, BoonData::new));

        public boolean matches(BlockState state) {
            return targets.contains(state.getBlockHolder());
        }
    }

}
