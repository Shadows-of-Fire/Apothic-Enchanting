package dev.shadowsoffire.apothic_enchanting.compat;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import dev.shadowsoffire.apothic_attributes.ApothicAttributes;
import dev.shadowsoffire.apothic_enchanting.ApothicEnchanting;
import dev.shadowsoffire.apothic_enchanting.Ench;
import dev.shadowsoffire.apothic_enchanting.objects.FilteringShelfBlock.FilteringShelfTile;
import dev.shadowsoffire.apothic_enchanting.table.EnchantmentTableStats;
import dev.shadowsoffire.apothic_enchanting.table.RavenEnchantingTableBlock;
import dev.shadowsoffire.apothic_enchanting.table.RavenTableStats;
import dev.shadowsoffire.apothic_enchanting.util.TooltipUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.JadeIds;
import snownee.jade.api.StreamServerDataProvider;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IDisplayHelper;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;

@WailaPlugin
public class EnchJadePlugin implements IWailaPlugin, IBlockComponentProvider {

    private static final ResourceLocation RAVEN_STATS_UID = ApothicEnchanting.loc("raven_stats");
    private static final StreamCodec<RegistryFriendlyByteBuf, RavenTableStats> RAVEN_STATS_STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT, RavenTableStats::eterna,
        ByteBufCodecs.INT, RavenTableStats::quanta,
        ByteBufCodecs.INT, RavenTableStats::arcana,
        RavenTableStats::new);

    @Override
    public void register(IWailaCommonRegistration reg) {
        reg.registerBlockDataProvider(RavenStatsDataProvider.INSTANCE, RavenEnchantingTableBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration reg) {
        reg.registerBlockComponent(this, Block.class);
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
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

        if (accessor.getBlock() == Ench.Blocks.FILTERING_SHELF.value()) {
            this.handleFilteringShelf(tooltip, accessor);
        }
    }

    @Override
    public IElement getIcon(BlockAccessor accessor, IPluginConfig config, IElement currentIcon) {
        if (accessor.getBlock() == Ench.Blocks.FILTERING_SHELF.value()) {
            return IElementHelper.get().item(accessor.getPickedResult()); // Need to override the book icon back to the shelf when Jade triggers vanilla integration.
        }
        return currentIcon;
    }

    @Override
    public ResourceLocation getUid() {
        return ApothicEnchanting.loc("ench");
    }

    @Override
    public int getDefaultPriority() {
        return 1150; // Magic number which puts us after item display.
    }

    public void handleFilteringShelf(ITooltip tooltip, BlockAccessor accessor) {
        tooltip.remove(JadeIds.MC_ENCHANTMENT_POWER);
        tooltip.remove(JadeIds.MC_CHISELED_BOOKSHELF);
        tooltip.remove(JadeIds.UNIVERSAL_ITEM_STORAGE);

        if (accessor.showDetails()) {
            return;
        }

        if (accessor.getBlockEntity() instanceof FilteringShelfTile tile) {
            int slot = ((ChiseledBookShelfBlock) accessor.getBlock()).getHitSlot(accessor.getHitResult(), accessor.getBlockState()).orElse(-1);
            if (slot == -1) return;
            ItemStack stack = tile.getItem(slot);
            if (stack.isEmpty()) return;
            tooltip.add(CommonComponents.EMPTY);
            IElementHelper helper = IElementHelper.get();
            List<IElement> elements = new ArrayList<>();
            elements.add(helper.smallItem(stack).clearCachedMessage());
            elements.add(helper
                .text(
                    Component.literal(" ").append(Component.literal(IDisplayHelper.get().humanReadableNumber(stack.getCount(), "", false)).append("× ").append(stack.getHoverName())))
                .message(null));
            tooltip.add(elements);

            ItemEnchantments enchants = EnchantmentHelper.getEnchantmentsForCrafting(stack);
            if (!enchants.isEmpty()) {
                List<Component> list = new ArrayList<>();
                enchants.addToTooltip(TooltipContext.of(accessor.getLevel()), list::add, ApothicAttributes.getTooltipFlag());
                for (Component c : list) {
                    tooltip.add(Component.literal(" - ").append(c));
                }
            }
        }
    }

    public static final class RavenStatsDataProvider implements StreamServerDataProvider<BlockAccessor, RavenTableStats> {

        public static final RavenStatsDataProvider INSTANCE = new RavenStatsDataProvider();

        @Override
        @Nullable
        public RavenTableStats streamData(BlockAccessor accessor) {
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
        public ResourceLocation getUid() {
            return RAVEN_STATS_UID;
        }
    }

}
