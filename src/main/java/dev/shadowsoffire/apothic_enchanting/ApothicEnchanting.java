package dev.shadowsoffire.apothic_enchanting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.datafixers.util.Pair;

import dev.shadowsoffire.apothic_attributes.ApothicAttributes;
import dev.shadowsoffire.apothic_enchanting.PowerFunction.DefaultMinPowerFunction;
import dev.shadowsoffire.apothic_enchanting.asm.EnchHooks;
import dev.shadowsoffire.apothic_enchanting.data.ApothEnchantmentProvider;
import dev.shadowsoffire.apothic_enchanting.data.EnchRecipeProvider;
import dev.shadowsoffire.apothic_enchanting.data.EnchTagsProvider;
import dev.shadowsoffire.apothic_enchanting.data.LootProvider;
import dev.shadowsoffire.apothic_enchanting.library.EnchLibraryTile;
import dev.shadowsoffire.apothic_enchanting.objects.TomeItem;
import dev.shadowsoffire.apothic_enchanting.payloads.CluePayload;
import dev.shadowsoffire.apothic_enchanting.payloads.EnchantmentInfoPayload;
import dev.shadowsoffire.apothic_enchanting.payloads.StatsPayload;
import dev.shadowsoffire.apothic_enchanting.table.ApothEnchantingTableBlock;
import dev.shadowsoffire.apothic_enchanting.table.EnchantingStatRegistry;
import dev.shadowsoffire.placebo.config.Configuration;
import dev.shadowsoffire.placebo.datagen.DataGenBuilder;
import dev.shadowsoffire.placebo.events.ResourceReloadEvent;
import dev.shadowsoffire.placebo.network.PayloadHelper;
import dev.shadowsoffire.placebo.tabs.ITabFiller;
import dev.shadowsoffire.placebo.tabs.TabFillingRegistry;
import dev.shadowsoffire.placebo.util.PlaceboUtil;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.dispenser.ShearsDispenseItemBehavior;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTab.TabVisibility;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.InterModProcessEvent;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@Mod(ApothicEnchanting.MODID)
public class ApothicEnchanting {

    public static final String MODID = "apothic_enchanting";
    public static final Logger LOGGER = LogManager.getLogger("Apotheosis : Enchantment");

    public static final Map<Holder<Enchantment>, EnchantmentInfo> ENCHANTMENT_INFO = new HashMap<>();
    public static final Object2IntMap<ResourceKey<Enchantment>> ENCH_HARD_CAPS = new Object2IntOpenHashMap<>();
    public static final String ENCH_HARD_CAP_IMC = "set_ench_hard_cap";
    public static final List<TomeItem> TYPED_BOOKS = new ArrayList<>();

    public ApothicEnchanting(IEventBus bus) {
        Ench.bootstrap(bus);
        bus.register(this);
    }

    @SubscribeEvent
    public void init(FMLCommonSetupEvent e) {
        this.reload(null);

        NeoForge.EVENT_BUS.register(new ApothEnchEvents());
        NeoForge.EVENT_BUS.addListener(this::reload);
        e.enqueueWork(() -> {
            DispenserBlock.registerBehavior(Items.SHEARS, new ShearsDispenseItemBehavior());

            TabFillingRegistry.register(Ench.Tabs.ENCH.getKey(), Ench.Items.HELLSHELF, Ench.Items.INFUSED_HELLSHELF, Ench.Items.BLAZING_HELLSHELF, Ench.Items.GLOWING_HELLSHELF, Ench.Items.SEASHELF, Ench.Items.INFUSED_SEASHELF,
                Ench.Items.CRYSTAL_SEASHELF, Ench.Items.HEART_SEASHELF, Ench.Items.DORMANT_DEEPSHELF, Ench.Items.DEEPSHELF, Ench.Items.ECHOING_DEEPSHELF, Ench.Items.SOUL_TOUCHED_DEEPSHELF, Ench.Items.ECHOING_SCULKSHELF,
                Ench.Items.SOUL_TOUCHED_SCULKSHELF, Ench.Items.ENDSHELF, Ench.Items.PEARL_ENDSHELF, Ench.Items.DRACONIC_ENDSHELF, Ench.Items.BEESHELF, Ench.Items.MELONSHELF, Ench.Items.STONESHELF, Ench.Items.SIGHTSHELF,
                Ench.Items.SIGHTSHELF_T2, Ench.Items.FILTERING_SHELF, Ench.Items.TREASURE_SHELF, Ench.Items.GEODE_SHELF, Ench.Items.LIBRARY, Ench.Items.ENDER_LIBRARY);

            TabFillingRegistry.register(Ench.Tabs.ENCH.getKey(), Ench.Items.HELMET_TOME, Ench.Items.CHESTPLATE_TOME, Ench.Items.LEGGINGS_TOME, Ench.Items.BOOTS_TOME, Ench.Items.WEAPON_TOME, Ench.Items.BOW_TOME, Ench.Items.PICKAXE_TOME,
                Ench.Items.FISHING_TOME, Ench.Items.OTHER_TOME, Ench.Items.SCRAP_TOME, Ench.Items.IMPROVED_SCRAP_TOME, Ench.Items.EXTRACTION_TOME);

            TabFillingRegistry.register(Ench.Tabs.ENCH.getKey(), Ench.Items.PRISMATIC_WEB, Ench.Items.INERT_TRIDENT, Ench.Items.WARDEN_TENDRIL, Ench.Items.INFUSED_BREATH);

            fill(Ench.Tabs.ENCH.getKey(), Ench.Enchantments.BERSERKERS_FURY, Ench.Enchantments.CHAINSAW, Ench.Enchantments.CHROMATIC, Ench.Enchantments.CRESCENDO_OF_BOLTS, Ench.Enchantments.BOON_OF_THE_EARTH,
                Ench.Enchantments.ENDLESS_QUIVER, Ench.Enchantments.WORKER_EXPLOITATION, Ench.Enchantments.GROWTH_SERUM, Ench.Enchantments.ICY_THORNS, Ench.Enchantments.KNOWLEDGE_OF_THE_AGES, Ench.Enchantments.LIFE_MENDING,
                Ench.Enchantments.MINERS_FERVOR, Ench.Enchantments.NATURES_BLESSING, Ench.Enchantments.REBOUNDING, Ench.Enchantments.REFLECTIVE_DEFENSES, Ench.Enchantments.SCAVENGER, Ench.Enchantments.SHIELD_BASH,
                Ench.Enchantments.STABLE_FOOTING, Ench.Enchantments.TEMPTING);

            PlaceboUtil.registerCustomColor(Ench.Colors.LIGHT_BLUE_FLASH);
        });

        EnchantingStatRegistry.INSTANCE.registerToBus();

        PayloadHelper.registerPayload(new CluePayload.Provider());
        PayloadHelper.registerPayload(new StatsPayload.Provider());
        PayloadHelper.registerPayload(new EnchantmentInfoPayload.Provider());
    }

    /**
     * This handles IMC events for the enchantment module.
     * Currently only one type is supported.
     * A mod may pass a single {@link Pair} holding a {@link ResourceKey} naming the enchantment and an {@link Integer} indicating the hard capped max level for an
     * enchantment.
     * That pair must use the method {@link ENCH_HARD_CAP_IMC}.
     */
    @SubscribeEvent
    @SuppressWarnings("unchecked")
    public void handleIMC(InterModProcessEvent e) {
        e.getIMCStream(ENCH_HARD_CAP_IMC::equals).forEach(msg -> {
            try {
                Pair<ResourceKey<Enchantment>, Integer> data = (Pair<ResourceKey<Enchantment>, Integer>) msg.messageSupplier().get();
                if (data.getFirst() != null && data.getSecond() > 0) {
                    ENCH_HARD_CAPS.put(data.getFirst(), data.getSecond().intValue());
                }
                else LOGGER.error("Failed to process IMC message with method {} from {} (invalid values passed).", msg.method(), msg.senderModId());
            }
            catch (Exception ex) {
                LOGGER.error("Exception thrown during IMC message with method {} from {}.", msg.method(), msg.senderModId());
                ex.printStackTrace();
            }
        });
    }

    @SubscribeEvent
    public void caps(RegisterCapabilitiesEvent e) {
        e.registerBlockEntity(ItemHandler.BLOCK, Ench.Tiles.LIBRARY.get(), EnchLibraryTile::getItemHandler);
        e.registerBlockEntity(ItemHandler.BLOCK, Ench.Tiles.ENDER_LIBRARY.get(), EnchLibraryTile::getItemHandler);
        e.registerBlockEntity(ItemHandler.BLOCK, BlockEntityType.ENCHANTING_TABLE, ApothEnchantingTableBlock::getItemHandler);
    }

    @SubscribeEvent
    public void data(GatherDataEvent event) {
        DataProvider.INDENT_WIDTH.set(4);
        DataGenBuilder.create(MODID, "minecraft")
            .registry(Registries.ENCHANTMENT, ApothEnchantmentProvider::bootstrap)
            .provider(LootProvider::create)
            .provider(EnchTagsProvider::new)
            .provider(EnchRecipeProvider::new)
            .build(event);
    }

    public void reload(ResourceReloadEvent e) {
        ApothEnchConfig.load(new Configuration(ApothicAttributes.getConfigFile(MODID)));
    }

    /**
     * Retrieves the {@link EnchantmentInfo} for a given enchantment.
     * 
     * @throws UnsupportedOperationException if the enchantment config has not been loaded.
     */
    public static EnchantmentInfo getEnchInfo(Holder<Enchantment> ench) {
        if (ENCHANTMENT_INFO.isEmpty()) {
            throw new UnsupportedOperationException("Cannot access enchantment information before it has been loaded!");
        }

        EnchantmentInfo info = ENCHANTMENT_INFO.get(ench);
        return info != null ? info : EnchantmentInfo.fallback(ench);
    }

    /**
     * Tries to find a max level for this enchantment. This is used to scale up default levels to the Apoth cap.
     * Single-Level enchantments are not scaled.
     * Barring that, enchantments are scaled using the {@link EnchantmentInfo#defaultMin(Enchantment)} until outside the default level space.
     */
    public static int getDefaultMaxLevel(Holder<Enchantment> ench) {
        int level = ench.value().getMaxLevel();
        if (level == 1) return 1;
        PowerFunction minFunc = new DefaultMinPowerFunction(ench);
        int max = 200;
        int minPower = minFunc.getPower(level);
        if (minPower >= max) return level;
        int lastPower = minPower;
        while (minPower < max) {
            minPower = minFunc.getPower(++level);
            if (lastPower == minPower) return level;
            if (minPower > max) return level - 1;
            lastPower = minPower;
        }
        return level;
    }

    @SafeVarargs
    public static void fill(ResourceKey<CreativeModeTab> tab, ResourceKey<Enchantment>... enchants) {
        Arrays.stream(enchants).map(ApothicEnchanting::enchFiller).forEach(filler -> TabFillingRegistry.register(filler, tab));
    }

    public static ITabFiller enchFiller(ResourceKey<Enchantment> e) {
        return (tab, event) -> {
            Holder<Enchantment> ench = event.getParameters().holders().lookupOrThrow(Registries.ENCHANTMENT).get(e).orElse(null);
            if (ench == null) {
                return;
            }

            int maxLevel = EnchHooks.getMaxLevel(ench.value());
            event.accept(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(ench, maxLevel)), TabVisibility.PARENT_TAB_ONLY);
            for (int level = 1; level <= maxLevel; level++) {
                event.accept(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(ench, level)), TabVisibility.SEARCH_TAB_ONLY);
            }
        };
    }

    public static ResourceLocation loc(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
