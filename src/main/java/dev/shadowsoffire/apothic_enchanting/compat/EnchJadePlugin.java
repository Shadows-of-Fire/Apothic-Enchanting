package dev.shadowsoffire.apothic_enchanting.compat;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import org.jspecify.annotations.Nullable;

import dev.shadowsoffire.apothic_enchanting.ApothicEnchanting;
import dev.shadowsoffire.apothic_enchanting.Ench;
import dev.shadowsoffire.apothic_enchanting.objects.FilteringShelfBlock;
import dev.shadowsoffire.apothic_enchanting.objects.FilteringShelfBlock.FilteringShelfTile;
import dev.shadowsoffire.apothic_enchanting.table.EnchantmentTableStats;
import dev.shadowsoffire.apothic_enchanting.table.RavenEnchantingTableBlock;
import dev.shadowsoffire.apothic_enchanting.table.RavenTableStats;
import dev.shadowsoffire.apothic_enchanting.util.TooltipUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.JadeIds;
import snownee.jade.api.StreamServerDataProvider;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin
public class EnchJadePlugin implements IWailaPlugin, IBlockComponentProvider {

    private static final Identifier UID = ApothicEnchanting.loc("ench");
    private static final Identifier RAVEN_STATS_UID = ApothicEnchanting.loc("raven_stats");
    private static final StreamCodec<RegistryFriendlyByteBuf, List<Identifier>> BLACKLIST_STREAM_CODEC = Identifier.STREAM_CODEC.<RegistryFriendlyByteBuf>cast().apply(ByteBufCodecs.list());
    private static final StreamCodec<RegistryFriendlyByteBuf, RavenTableStats> RAVEN_STATS_STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT, RavenTableStats::eterna,
        ByteBufCodecs.INT, RavenTableStats::quanta,
        ByteBufCodecs.INT, RavenTableStats::arcana,
        RavenTableStats::new);

    @Override
    public void register(IWailaCommonRegistration reg) {
        reg.registerBlockDataProvider(FilterDataProvider.INSTANCE, ChiseledBookShelfBlock.class);
        reg.registerBlockDataProvider(RavenStatsDataProvider.INSTANCE, RavenEnchantingTableBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration reg) {
        reg.registerBlockComponent(this, Block.class);
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        // Filtering shelves have to bypass the normal appendBlockStats path: every stat getter on
        // FilteringShelfBlock reads the block entity's item list, which isn't synced to the client
        // (ChiseledBookShelfBlockEntity has no update packet), so client-side stat lookups always
        // return zero and the blacklist comes up empty. We stream the blacklist from the server
        // separately (via FilterDataProvider) and derive the stat lines from its size, since
        // canInsert already restricts the shelf to single-enchantment books → blacklist count
        // equals getEnchantedBooks().size().
        if (accessor.getBlock() == Ench.Blocks.FILTERING_SHELF.value()) {
            tooltip.remove(JadeIds.MC_ENCHANTMENT_POWER);
            tooltip.remove(JadeIds.UNIVERSAL_ITEM_STORAGE);

            List<Identifier> filtered = FilterDataProvider.INSTANCE.decodeFromData(accessor).orElse(List.of());
            appendFilteringShelfStats(tooltip, filtered.size());

            // Suppress the filter list when the player is looking directly at a populated slot.
            // In that case, vanilla Jade's ShelfProvider renders the book + its stored enchantment,
            // which is more relevant than the broader filter overview.
            if (isHoveringPopulatedSlot(accessor)) {
                return;
            }

            if (!filtered.isEmpty()) {
                HolderLookup.RegistryLookup<Enchantment> lookup = accessor.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                tooltip.add(TooltipUtil.lang("info", "filter").withStyle(s -> s.withColor(0x58B0CC)));
                for (Identifier id : filtered) {
                    Holder<Enchantment> ench = lookup.get(ResourceKey.create(Registries.ENCHANTMENT, id)).orElse(null);
                    if (ench == null) continue;
                    Component line = Enchantment.getFullname(ench, 1).copy().withStyle(ChatFormatting.RESET).withStyle(s -> s.withColor(0x5878AA));
                    tooltip.add(Component.literal(" - ").append(line).withStyle(s -> s.withColor(0x5878AA)));
                }
            }
            return;
        }

        TooltipUtil.appendBlockStats(accessor.getLevel(), accessor.getBlockState(), accessor.getPosition(), tooltip::add);
        if (accessor.getBlock() instanceof EnchantingTableBlock) {
            if (accessor.getBlock() == Ench.Blocks.RAVEN_ENCHANTING_TABLE.value()) {
                RavenTableStats raven = RavenStatsDataProvider.INSTANCE.decodeFromData(accessor).orElseGet(RavenTableStats::new);
                EnchantmentTableStats shelfStats = EnchantmentTableStats.gatherStats(accessor.getLevel(), accessor.getPosition());
                EnchantmentTableStats merged = new EnchantmentTableStats(
                    raven.eterna(), raven.quanta(), raven.arcana(),
                    shelfStats.clues(), shelfStats.blacklist(), shelfStats.treasure(), shelfStats.stable());
                TooltipUtil.appendTableStats(merged, tooltip::add);
            }
            else {
                TooltipUtil.appendTableStats(accessor.getLevel(), accessor.getPosition(), tooltip::add);
            }
            tooltip.remove(JadeIds.MC_TOTAL_ENCHANTMENT_POWER);
        }
    }

    /**
     * Mirrors the format that {@link TooltipUtil#appendBlockStats} produces, but sources the numbers
     * from the streamed blacklist size rather than from {@link FilteringShelfBlock#getEnchantPowerBonus}
     * et al. (which return zero client-side). Keeps the values in sync with {@code FilteringShelfBlock}:
     * eterna = count, maxEterna = 30, arcana = count, everything else defaulted.
     */
    private static void appendFilteringShelfStats(ITooltip tooltip, int bookCount) {
        float eterna = bookCount * 1.0F;      // FilteringShelfBlock#getEnchantPowerBonus returns count * 0.5F, × 2 in EnchantingStatRegistry#getEterna
        float maxEterna = 30F;
        float arcana = bookCount;
        if (eterna == 0 && arcana == 0) {
            return;
        }
        tooltip.add(TooltipUtil.lang("info", "ench_stats").withStyle(ChatFormatting.GOLD));
        if (eterna != 0) {
            tooltip.add(TooltipUtil.lang("info", "eterna.p", String.format("%.2f", eterna), String.format("%.2f", maxEterna)).withStyle(ChatFormatting.GREEN));
        }
        if (arcana != 0) {
            tooltip.add(TooltipUtil.lang("info", "arcana.p", String.format("%.2f", arcana)).withStyle(ChatFormatting.DARK_PURPLE));
        }
    }

    /**
     * Mirrors the check vanilla Jade's {@code ShelfProvider.shouldRequestData} uses: if the player is
     * looking at the front face of a chiseled-bookshelf-like block AND the targeted slot's occupied
     * flag is set in the block state, vanilla will stream the book there and render it. We want to
     * stay out of the way in that case.
     */
    private static boolean isHoveringPopulatedSlot(BlockAccessor accessor) {
        Direction facing = accessor.getBlockState().getValue(ChiseledBookShelfBlock.FACING);
        OptionalInt slot = ((ChiseledBookShelfBlock) accessor.getBlock()).getHitSlot(accessor.getHitResult(), facing);
        if (slot.isEmpty()) {
            return false;
        }
        int i = slot.getAsInt();
        if (i >= ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.size()) {
            return false;
        }
        return accessor.getBlockState().getValue(ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.get(i));
    }

    @Override
    public Identifier getUid() {
        return UID;
    }

    @Override
    public int getDefaultPriority() {
        return 1150; // Magic number which puts us after item display.
    }

    /**
     * Server-side stream provider for the filtering shelf's current enchantment blacklist. Must be a
     * separate class from the client component provider — since MC 1.21.6 Jade refuses to register
     * a provider that implements both {@link IComponentProvider} and {@link IServerDataProvider}.
     * <p>
     * Computes the blacklist on the server because {@link net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity}
     * doesn't sync its book contents over the normal update packet, so the client cannot compute it
     * directly from the block entity.
     */
    public static final class FilterDataProvider implements StreamServerDataProvider<BlockAccessor, List<Identifier>> {

        public static final FilterDataProvider INSTANCE = new FilterDataProvider();

        @Override
        public @Nullable List<Identifier> streamData(BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof FilteringShelfTile shelf)) {
                return null;
            }
            List<Identifier> ids = new java.util.ArrayList<>();
            for (ItemStack book : shelf.getEnchantedBooks()) {
                ItemEnchantments enchants = EnchantmentHelper.getEnchantmentsForCrafting(book);
                if (enchants.size() != 1) continue; // Only single-enchantment books contribute to the filter.
                Optional<ResourceKey<Enchantment>> key = enchants.keySet().stream().findFirst().flatMap(Holder::unwrapKey);
                key.ifPresent(k -> ids.add(k.identifier()));
            }
            return ids.isEmpty() ? null : ids;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, List<Identifier>> streamCodec() {
            return BLACKLIST_STREAM_CODEC;
        }

        @Override
        public boolean shouldRequestData(BlockAccessor accessor) {
            return accessor.getBlock() == Ench.Blocks.FILTERING_SHELF.value();
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }

    public static final class RavenStatsDataProvider implements StreamServerDataProvider<BlockAccessor, RavenTableStats> {

        public static final RavenStatsDataProvider INSTANCE = new RavenStatsDataProvider();

        @Override
        public @Nullable RavenTableStats streamData(BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof EnchantingTableBlockEntity tile)) {
                return null;
            }
            return tile.getData(RavenTableStats.TYPE);
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, RavenTableStats> streamCodec() {
            return RAVEN_STATS_STREAM_CODEC;
        }

        @Override
        public boolean shouldRequestData(BlockAccessor accessor) {
            return accessor.getBlock() == Ench.Blocks.RAVEN_ENCHANTING_TABLE.value();
        }

        @Override
        public Identifier getUid() {
            return RAVEN_STATS_UID;
        }
    }

}
