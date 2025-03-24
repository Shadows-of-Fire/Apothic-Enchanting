package dev.shadowsoffire.apothic_enchanting;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import dev.shadowsoffire.apothic_enchanting.Ench.Particles;
import dev.shadowsoffire.apothic_enchanting.library.EnchLibraryScreen;
import dev.shadowsoffire.apothic_enchanting.payloads.CluePayload;
import dev.shadowsoffire.apothic_enchanting.payloads.StatsPayload;
import dev.shadowsoffire.apothic_enchanting.table.ApothEnchantmentScreen;
import dev.shadowsoffire.apothic_enchanting.util.FakeLevelReader;
import dev.shadowsoffire.apothic_enchanting.util.TooltipUtil;
import dev.shadowsoffire.placebo.util.DrawsOnLeft;
import dev.shadowsoffire.placebo.util.EnchantmentUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.particle.FlyTowardsPositionParticle;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = ApothicEnchanting.MODID, bus = Bus.MOD, value = Dist.CLIENT)
public class ApothEnchClient {

    private static final BlockHitResult MISS = BlockHitResult.miss(Vec3.ZERO, Direction.NORTH, BlockPos.ZERO);

    @SubscribeEvent
    public static void client(FMLClientSetupEvent e) {
        NeoForge.EVENT_BUS.register(new ForgeBusEvents());
    }

    @SubscribeEvent
    public static void screens(RegisterMenuScreensEvent e) {
        e.register(Ench.Menus.ENCHANTING_TABLE, ApothEnchantmentScreen::new);
        e.register(Ench.Menus.LIBRARY, EnchLibraryScreen::new);
    }

    @SubscribeEvent
    public static void particleFactories(RegisterParticleProvidersEvent e) {
        e.registerSpriteSet(Particles.ENCHANT_FIRE.get(), FlyTowardsPositionParticle.EnchantProvider::new);
        e.registerSpriteSet(Particles.ENCHANT_WATER.get(), FlyTowardsPositionParticle.EnchantProvider::new);
        e.registerSpriteSet(Particles.ENCHANT_SCULK.get(), FlyTowardsPositionParticle.EnchantProvider::new);
        e.registerSpriteSet(Particles.ENCHANT_END.get(), FlyTowardsPositionParticle.EnchantProvider::new);
    }

    public static void handleCluePayload(CluePayload msg) {
        if (Minecraft.getInstance().screen instanceof ApothEnchantmentScreen es) {
            es.acceptClues(msg.slot(), msg.clues(), msg.all());
        }
    }

    public static void handleStatsPayload(StatsPayload msg) {
        if (Minecraft.getInstance().screen instanceof ApothEnchantmentScreen es) {
            es.getMenu().setStats(msg.stats());
        }
    }

    @Nullable
    public static <T> Registry<T> findClientRegistry(ResourceKey<? extends Registry<T>> registryKey) {
        ClientPacketListener listener = Minecraft.getInstance().getConnection();
        if (listener == null) {
            return null;
        }
        return listener.registryAccess().registry(registryKey).orElse(null);
    }

    public static class ForgeBusEvents {

        @SubscribeEvent
        public void tooltips(ItemTooltipEvent e) {
            Item i = e.getItemStack().getItem();
            List<Component> tooltip = e.getToolTip();
            if (i == Ench.Items.PRISMATIC_WEB.value()) {
                tooltip.add(TooltipUtil.lang("info", "prismatic_cobweb").withStyle(ChatFormatting.GRAY));
            }
            else if (i instanceof BlockItem) {
                Block block = ((BlockItem) i).getBlock();
                BlockState state = block.defaultBlockState();

                Level level = e.getContext().level();
                if (level != null && Minecraft.getInstance().player != null) {
                    BlockPlaceContext ctx = new BlockPlaceContext(level, Minecraft.getInstance().player, InteractionHand.MAIN_HAND, e.getItemStack(), MISS);
                    try {
                        state = block.getStateForPlacement(ctx);
                    }
                    catch (Exception ex) {
                        // Ignore, we're calling this with an invalid context. We just want to try to get the placed state if possible.
                    }
                }

                if (state == null) {
                    state = block.defaultBlockState();
                }

                try {
                    LevelReader reader = level == null ? new FakeLevelReader(state) : level;
                    TooltipUtil.appendBlockStats(reader, state, BlockPos.ZERO, tooltip::add);
                }
                catch (NullPointerException ex) {
                    // Ignore, we're trying to eagerly resolve this with a null level.
                }
            }
            else if (i == Items.ENCHANTED_BOOK) {
                ItemStack stack = e.getItemStack();
                ItemEnchantments enchMap = EnchantmentHelper.getEnchantmentsForCrafting(stack);
                if (enchMap.size() == 1) {
                    Object2IntMap.Entry<Holder<Enchantment>> entry = enchMap.entrySet().iterator().next(); // pull the first enchantment via iterator
                    Holder<Enchantment> ench = entry.getKey();
                    int level = entry.getIntValue();

                    if (!ModList.get().isLoaded("enchdesc")) {
                        String key = ench.getKey().location().toLanguageKey("enchantment") + ".desc";
                        if (I18n.exists(key)) {
                            tooltip.add(Component.translatable(key).withStyle(ChatFormatting.DARK_GRAY));
                        }
                    }

                    if (ApothEnchConfig.showEnchantedBookMetadata) {
                        EnchantmentInfo info = ApothicEnchanting.getEnchInfo(ench);
                        Object[] args = new Object[4];
                        args[0] = boolComp("info.apothic_enchanting.discoverable", ench.is(EnchantmentTags.IN_ENCHANTING_TABLE));
                        args[1] = boolComp("info.apothic_enchanting.lootable", ench.is(EnchantmentTags.ON_RANDOM_LOOT));
                        args[2] = boolComp("info.apothic_enchanting.tradeable", ench.is(EnchantmentTags.TRADEABLE));
                        args[3] = boolComp("info.apothic_enchanting.treasure", ench.is(EnchantmentTags.TREASURE));
                        if (e.getFlags().isAdvanced()) {
                            tooltip.add(Component.translatable("%s \u2507 %s \u2507 %s \u2507 %s", args[0], args[1], args[2], args[3]).withStyle(ChatFormatting.DARK_GRAY));
                            tooltip.add(Component.translatable("info.apothic_enchanting.book_range", info.getMinPower(level), info.getMaxPower(level)).withStyle(ChatFormatting.GREEN));
                        }
                        else {
                            tooltip.add(Component.translatable("%s \u2507 %s", args[2], args[3]).withStyle(ChatFormatting.DARK_GRAY));
                        }
                    }

                    EnchantmentInfo info = ApothicEnchanting.getEnchInfo(ench);
                    if (info.levelCap() != -1) {
                        Iterator<Component> it = tooltip.iterator();
                        Component toReplace = null;
                        while (it.hasNext()) {
                            Component comp = it.next();
                            if (comp.contains(ench.value().description())) {
                                toReplace = comp;
                                break;
                            }
                        }
                        if (toReplace != null) {
                            MutableComponent newComp = toReplace.copy();
                            Component limit = Component.translatable("info.apothic_enchanting.limit", Component.translatable("enchantment.level." + info.levelCap())).withStyle(ChatFormatting.DARK_GRAY);
                            newComp.append(CommonComponents.SPACE).append(limit);
                            tooltip.set(tooltip.indexOf(toReplace), newComp);
                        }
                    }
                }
            }
        }

        @SubscribeEvent
        public void drawAnvilCostBlob(ScreenEvent.Render.Post e) {
            if (e.getScreen() instanceof AnvilScreen anv) {
                int level = anv.getMenu().getCost();
                if (level <= 0 || !anv.getMenu().getSlot(anv.getMenu().getResultSlot()).hasItem() || level == Integer.MAX_VALUE) return;
                List<Component> list = new ArrayList<>();
                list.add(TooltipUtil.lang("info", "anvil_at", level).withStyle(ChatFormatting.UNDERLINE, ChatFormatting.GREEN));
                int expCost = EnchantmentUtils.getTotalExperienceForLevel(level);
                list.add(TooltipUtil.lang("info", "anvil_xp_cost", Component.literal("" + expCost).withStyle(ChatFormatting.GREEN),
                    Component.literal("" + level).withStyle(ChatFormatting.GREEN)));
                DrawsOnLeft.draw(anv, e.getGuiGraphics(), list, anv.getGuiTop() + 28);
            }
        }

    }

    private static Component boolComp(String key, boolean flag) {
        return Component.translatable(key + (flag ? "" : ".not")).withStyle(Style.EMPTY.withColor(flag ? 0x108810 : 0xAA1616));
    }

}
