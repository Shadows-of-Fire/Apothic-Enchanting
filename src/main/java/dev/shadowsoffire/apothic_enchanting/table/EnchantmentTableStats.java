package dev.shadowsoffire.apothic_enchanting.table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dev.shadowsoffire.apothic_enchanting.Ench;
import dev.shadowsoffire.apothic_enchanting.api.EnchantmentStatBlock;
import it.unimi.dsi.fastutil.floats.Float2FloatMap;
import it.unimi.dsi.fastutil.floats.Float2FloatOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Holder for the computed stat values of an enchantment table.
 */
public record EnchantmentTableStats(float eterna, float quanta, float arcana, int clues, Set<Holder<Enchantment>> blacklist, boolean treasure, boolean stable) {

    public static final EnchantmentTableStats INVALID = new EnchantmentTableStats(0, 0, 0, 0, Collections.emptySet(), false, false);

    public static final StreamCodec<RegistryFriendlyByteBuf, EnchantmentTableStats> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.FLOAT, EnchantmentTableStats::eterna,
        ByteBufCodecs.FLOAT, EnchantmentTableStats::quanta,
        ByteBufCodecs.FLOAT, EnchantmentTableStats::arcana,
        ByteBufCodecs.INT, EnchantmentTableStats::clues,
        ByteBufCodecs.collection(HashSet::new, ByteBufCodecs.holderRegistry(Registries.ENCHANTMENT)), EnchantmentTableStats::blacklist,
        ByteBufCodecs.BOOL, EnchantmentTableStats::treasure,
        ByteBufCodecs.BOOL, EnchantmentTableStats::stable,
        EnchantmentTableStats::new);

    public EnchantmentTableStats(float eterna, float quanta, float arcana, int clues, Set<Holder<Enchantment>> blacklist, boolean treasure, boolean stable) {
        this.eterna = Mth.clamp(eterna, 0, 100);
        this.quanta = Mth.clamp(quanta, 0, 100);
        this.arcana = Mth.clamp(arcana, 0, 100);
        this.clues = Math.max(clues, 0);
        this.blacklist = Collections.unmodifiableSet(blacklist);
        this.treasure = treasure;
        this.stable = stable;
    }

    /**
     * @deprecated Use {@link #eterna(Player)} or {@link #tableEterna()} depending on context.
     */
    @Deprecated
    public float eterna() {
        return this.eterna;
    }

    /**
     * Returns the player's personal clamped eterna value, which is limited by {@link Ench.Attributes#MAX_ETERNA}.
     */
    public float eterna(Player player) {
        return Math.min(this.eterna, (float) player.getAttributeValue(Ench.Attributes.MAX_ETERNA));
    }

    /**
     * Returns the table's "real" eterna value as determined from the bookshelves. Should only be used in certain display scenarios.
     */
    public float tableEterna() {
        return this.eterna;
    }

    /**
     * Creates a stats object with all default values plus the provided level.
     * Used by systems going through the vanilla interfaces.
     */
    public static EnchantmentTableStats vanilla(int level) {
        return new EnchantmentTableStats(level, 15F, 0, 1, Set.of(), false, false);
    }

    /**
     * Gathers all enchanting stats for an enchantment table located at the specified position.
     *
     * @param level    The level.
     * @param pos      The position of the enchantment table.
     * @param itemEnch The enchantability of the item being enchanted.
     * @return The computed {@link EnchantmentTableStats}.
     */
    public static EnchantmentTableStats gatherStats(LevelReader level, BlockPos pos) {
        EnchantmentTableStats.Builder builder = new EnchantmentTableStats.Builder();
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            if (canReadStatsFrom(level, pos, offset)) {
                gatherStats(builder, level, pos.offset(offset));
            }
        }
        return builder.build();
    }

    /**
     * Checks if stats can be read from a block at a particular offset.
     *
     * @param level    The level.
     * @param tablePos The position of the enchanting table.
     * @param offset   The offset being checked.
     * @return True if the block between the table and the offset is {@link BlockTags#ENCHANTMENT_POWER_TRANSMITTER}, false otherwise.
     */
    public static boolean canReadStatsFrom(LevelReader level, BlockPos tablePos, BlockPos offset) {
        return level.getBlockState(tablePos.offset(offset.getX() / 2, offset.getY(), offset.getZ() / 2)).is(BlockTags.ENCHANTMENT_POWER_TRANSMITTER);
    }

    /**
     * Collects enchanting stats from a particular shelf spot into the stat array.<br>
     * If you are collecting all stats, you should use {@link #gatherStats(Level, BlockPos)} instead.
     *
     * @param eternaMap A map of max eterna contributions to eterna contributions for that max.
     * @param stats     The stat array, with order {eterna, quanta, arcana, stability, clues}.
     * @param level     The world.
     * @param pos       The position of the stat-providing block.
     */
    public static void gatherStats(EnchantmentTableStats.Builder builder, LevelReader level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return;
        float eterna = EnchantingStatRegistry.getEterna(state, level, pos);
        float max = EnchantingStatRegistry.getMaxEterna(state, level, pos);
        builder.addEterna(eterna, max);
        builder.addQuanta(EnchantingStatRegistry.getQuanta(state, level, pos));
        builder.addArcana(EnchantingStatRegistry.getArcana(state, level, pos));
        builder.addClues(EnchantingStatRegistry.getBonusClues(state, level, pos));

        EnchantmentStatBlock enchBlock = ((EnchantmentStatBlock) state.getBlock());

        enchBlock.getBlacklistedEnchantments(state, level, pos).forEach(builder::blacklistEnchant);
        if (enchBlock.allowsTreasure(state, level, pos)) {
            builder.setAllowsTreasure(true);
        }
        if (enchBlock.providesStability(state, level, pos)) {
            builder.setStable(true);
        }
    }

    /**
     * Builder for {@link EnchantmentTableStats}.
     */
    public static class Builder {

        private final Float2FloatMap eternaMap = new Float2FloatOpenHashMap();
        private final Set<Holder<Enchantment>> blacklist = new HashSet<>();

        private float eterna = 0;
        private float quanta = 0;
        private float arcana = 0;
        private int clues = 0;
        private boolean allowsTreasure = false;
        private boolean stable = false;

        public Builder() {
            this.addQuanta(15F);
            this.addClues(1);
        }

        public void addEterna(float eterna, float max) {
            this.eternaMap.put(max, this.eternaMap.getOrDefault(max, 0) + eterna);
        }

        public void addQuanta(float quanta) {
            this.quanta += quanta;
        }

        public void addArcana(float arcana) {
            this.arcana += arcana;
        }

        public void addClues(int clues) {
            this.clues += clues;
        }

        public void blacklistEnchant(Holder<Enchantment> ench) {
            this.blacklist.add(ench);
        }

        public void setAllowsTreasure(boolean allowsTreasure) {
            this.allowsTreasure = allowsTreasure;
        }

        public void setStable(boolean stable) {
            this.stable = stable;
        }

        /**
         * Builds the table stats from the collected values.
         * <p>
         * Performs the work of computing the true eterna value after applying the max eterna caps.
         * Blocks can only provide up to their individual max, but lowest-max blocks are counted first.
         */
        public EnchantmentTableStats build() {
            List<Float2FloatMap.Entry> entries = new ArrayList<>(this.eternaMap.float2FloatEntrySet());
            Collections.sort(entries, Comparator.comparing(Float2FloatMap.Entry::getFloatKey));

            for (Float2FloatMap.Entry e : entries) {
                if (e.getFloatKey() > 0) {
                    this.eterna = Math.min(e.getFloatKey(), this.eterna + e.getFloatValue());
                }
                else {
                    this.eterna += e.getFloatValue();
                }
            }

            return new EnchantmentTableStats(this.eterna, this.quanta, this.arcana, this.clues, this.blacklist, this.allowsTreasure, this.stable);
        }
    }

}
