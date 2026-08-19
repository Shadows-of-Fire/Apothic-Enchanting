package dev.shadowsoffire.apothic_enchanting;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.MapMaker;

import dev.shadowsoffire.apothic_attributes.ApothicAttributes;
import dev.shadowsoffire.apothic_enchanting.PowerFunction.DefaultMinPowerFunction;
import dev.shadowsoffire.apothic_enchanting.asm.EnchHooks;
import dev.shadowsoffire.apothic_enchanting.data.ApothEnchDataMapProvider;
import dev.shadowsoffire.apothic_enchanting.data.ApothEnchantmentProvider;
import dev.shadowsoffire.apothic_enchanting.data.EnchDamageTypeProvider;
import dev.shadowsoffire.apothic_enchanting.data.EnchDamageTypeTagsProvider;
import dev.shadowsoffire.apothic_enchanting.data.EnchItemTagsProvider;
import dev.shadowsoffire.apothic_enchanting.data.EnchRecipeProvider;
import dev.shadowsoffire.apothic_enchanting.data.EnchStatsProvider;
import dev.shadowsoffire.apothic_enchanting.data.EnchTagsProvider;
import dev.shadowsoffire.apothic_enchanting.data.LootProvider;
import dev.shadowsoffire.apothic_enchanting.data.SongProvider;
import dev.shadowsoffire.apothic_enchanting.library.EnchLibraryTile;
import dev.shadowsoffire.apothic_enchanting.objects.TomeItem;
import dev.shadowsoffire.apothic_enchanting.payloads.CluePayload;
import dev.shadowsoffire.apothic_enchanting.payloads.SetRavenStatsPayload;
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
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTab.TabVisibility;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantable;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;

@Mod(ApothicEnchanting.MODID)
public class ApothicEnchanting {

    public static final String MODID = "apothic_enchanting";
    public static final Logger LOGGER = LoggerFactory.getLogger("Apotheosis : Enchantment");

    public static final List<TomeItem> TYPED_BOOKS = new ArrayList<>();

    public ApothicEnchanting(IEventBus bus) {
        Ench.bootstrap(bus);
        bus.register(this);
    }

    /**
     * Tombstone written to the old {@code config/apotheosis/enchantments.cfg} location after the per-enchantment
     * config was migrated to the {@code apothic_enchanting:enchantment_info} data map.
     */
    private static final String MIGRATED_CONFIG_NOTICE = """
        # Apotheosis Enchantment Information
        #
        # Per-enchantment configuration (max level, max loot level, level cap, and the min/max power functions)
        # no longer lives in this file. It has migrated to a NeoForge data map:
        #
        #     data/apothic_enchanting/data_maps/enchantment/enchantment_info.json
        #
        # To capture the current effective values for every enchantment as a ready-to-edit data map file,
        # run the following command in-game (requires permission level 4):
        #
        #     /apoth dump_enchantment_info
        #
        # This file is intentionally left empty and is no longer read by the mod.
        """;

    @SubscribeEvent
    public void init(FMLCommonSetupEvent e) {
        this.reload(null);
        writeMigratedConfigNotice();

        NeoForge.EVENT_BUS.register(new ApothEnchEvents());
        NeoForge.EVENT_BUS.addListener(this::reload);
        e.enqueueWork(() -> {
            TabFillingRegistry.register(Ench.Tabs.ENCH.getKey(), Ench.Items.APOTHIC_ENCHANTING_TABLE, Ench.Items.RAVEN_ENCHANTING_TABLE);

            TabFillingRegistry.register(Ench.Tabs.ENCH.getKey(), Ench.Items.BASIC_BOOKSHELF, Ench.Items.HELLSHELF, Ench.Items.INFUSED_HELLSHELF, Ench.Items.BLAZING_HELLSHELF, Ench.Items.GLOWING_HELLSHELF, Ench.Items.SEASHELF,
                Ench.Items.INFUSED_SEASHELF, Ench.Items.CRYSTAL_SEASHELF, Ench.Items.HEART_SEASHELF, Ench.Items.DORMANT_DEEPSHELF, Ench.Items.DEEPSHELF, Ench.Items.ECHOING_DEEPSHELF, Ench.Items.SOUL_TOUCHED_DEEPSHELF,
                Ench.Items.ECHOING_SCULKSHELF, Ench.Items.SOUL_TOUCHED_SCULKSHELF, Ench.Items.ENDSHELF, Ench.Items.PEARL_ENDSHELF, Ench.Items.DRACONIC_ENDSHELF, Ench.Items.BEESHELF, Ench.Items.MELONSHELF, Ench.Items.STONESHELF,
                Ench.Items.SIGHTSHELF, Ench.Items.SIGHTSHELF_T2, Ench.Items.FILTERING_SHELF, Ench.Items.TREASURE_SHELF, Ench.Items.GEODE_SHELF, Ench.Items.LIBRARY, Ench.Items.ENDER_LIBRARY);

            TabFillingRegistry.register(Ench.Tabs.ENCH.getKey(), Ench.Items.HELMET_TOME, Ench.Items.CHESTPLATE_TOME, Ench.Items.LEGGINGS_TOME, Ench.Items.BOOTS_TOME, Ench.Items.WEAPON_TOME, Ench.Items.BOW_TOME, Ench.Items.PICKAXE_TOME,
                Ench.Items.FISHING_TOME, Ench.Items.OTHER_TOME, Ench.Items.SCRAP_TOME, Ench.Items.IMPROVED_SCRAP_TOME, Ench.Items.EXTRACTION_TOME);

            TabFillingRegistry.register(Ench.Tabs.ENCH.getKey(), Ench.Items.PRISMATIC_WEB, Ench.Items.INERT_TRIDENT, Ench.Items.WARDEN_TENDRIL, Ench.Items.INFUSED_BREATH,
                Ench.Items.FLIMSY_ENDER_LEAD, Ench.Items.ENDER_LEAD, Ench.Items.OCCULT_ENDER_LEAD, Ench.Items.MUSIC_DISC_ETERNA, Ench.Items.MUSIC_DISC_QUANTA, Ench.Items.MUSIC_DISC_ARCANA);

            fill(Ench.Tabs.ENCH.getKey(), Ench.Enchantments.BERSERKERS_FURY, Ench.Enchantments.CHAINSAW, Ench.Enchantments.CHROMATIC, Ench.Enchantments.CRESCENDO_OF_BOLTS, Ench.Enchantments.BOON_OF_THE_EARTH,
                Ench.Enchantments.ENDLESS_QUIVER, Ench.Enchantments.WORKER_EXPLOITATION, Ench.Enchantments.GROWTH_SERUM, Ench.Enchantments.ICY_THORNS, Ench.Enchantments.KNOWLEDGE_OF_THE_AGES, Ench.Enchantments.LIFE_MENDING,
                Ench.Enchantments.MINERS_FERVOR, Ench.Enchantments.NATURES_BLESSING, Ench.Enchantments.REBOUNDING, Ench.Enchantments.REFLECTIVE_DEFENSES, Ench.Enchantments.SCAVENGER, Ench.Enchantments.SHIELD_BASH,
                Ench.Enchantments.STABLE_FOOTING, Ench.Enchantments.TEMPTING);

            PlaceboUtil.registerCustomColor(Ench.Colors.LIGHT_BLUE_FLASH);
        });

        EnchantingStatRegistry.INSTANCE.registerToBus();

        PayloadHelper.registerPayload(new CluePayload.Provider());
        PayloadHelper.registerPayload(new StatsPayload.Provider());
        PayloadHelper.registerPayload(new SetRavenStatsPayload.Provider());
    }

    // Make shears "reasonably" enchantable.
    @SubscribeEvent
    public void modifyComponents(ModifyDefaultComponentsEvent e) {
        e.modify(Items.SHEARS, (builder, regs, item) -> builder.set(DataComponents.ENCHANTABLE, new Enchantable(15)));
    }

    // Make everything that isn't explicitly not enchantable enchantable and let enchantments manage this concept.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void defaultEnchantability(ModifyDefaultComponentsEvent e) {
        e.modifyMatching((item, components) -> !components.has(DataComponents.ENCHANTABLE),
            (builder, regs, item) -> builder.set(DataComponents.ENCHANTABLE, new Enchantable(1)));
    }

    @SubscribeEvent
    public void caps(RegisterCapabilitiesEvent e) {
        e.registerBlockEntity(Capabilities.Item.BLOCK, Ench.Tiles.LIBRARY, EnchLibraryTile::getItemHandler);
        e.registerBlockEntity(Capabilities.Item.BLOCK, Ench.Tiles.ENDER_LIBRARY, EnchLibraryTile::getItemHandler);
        e.registerBlockEntity(Capabilities.Item.BLOCK, BlockEntityType.ENCHANTING_TABLE, ApothEnchantingTableBlock::getItemHandler);
        e.registerBlockEntity(Capabilities.Item.BLOCK, Ench.Tiles.FILTERING_SHELF,
            (tile, side) -> VanillaContainerWrapper.of(tile));
    }

    @SubscribeEvent
    public void addBlockEntityValidBlocks(BlockEntityTypeAddBlocksEvent e) {
        e.modify(BlockEntityType.ENCHANTING_TABLE,
            Ench.Blocks.APOTHIC_ENCHANTING_TABLE.value(),
            Ench.Blocks.RAVEN_ENCHANTING_TABLE.value());
    }

    @SubscribeEvent
    public void data(GatherDataEvent.Client event) {
        DataProvider.INDENT_WIDTH.set(4);
        DataGenBuilder.create(MODID, "minecraft")
            .registry(Registries.ENCHANTMENT, ApothEnchantmentProvider::bootstrap)
            .registry(Registries.DAMAGE_TYPE, EnchDamageTypeProvider::bootstrap)
            .registry(Registries.JUKEBOX_SONG, SongProvider::bootstrap)
            .provider(LootProvider::create)
            .provider(EnchTagsProvider::new)
            .provider(EnchItemTagsProvider::new)
            .provider(EnchDamageTypeTagsProvider::new)
            .provider(EnchRecipeProvider::new)
            .provider(EnchStatsProvider::new)
            .provider(ApothEnchDataMapProvider::new)
            .build(event);
    }

    @SubscribeEvent
    public void applyAttribs(EntityAttributeModificationEvent e) {
        e.getTypes().forEach(type -> {
            e.add(type, Ench.Attributes.MAX_ETERNA);
        });
    }

    public void reload(ResourceReloadEvent e) {
        ApothEnchConfig.load(new Configuration(ApothicAttributes.getConfigFile(MODID)));
    }

    /**
     * Replaces the legacy {@code config/apotheosis/enchantments.cfg} with an empty tombstone that points users at
     * the {@code apothic_enchanting:enchantment_info} data map and the {@code /apoth dump_enchantment_info} command.
     * The legacy file is no longer read; leaving the old populated config in place would mislead users into thinking
     * it still applies. Skips the write when the tombstone is already present so launches don't churn the file.
     */
    private static void writeMigratedConfigNotice() {
        Path file = ApothicAttributes.getConfigFile("enchantments").toPath();
        try {
            if (Files.exists(file) && MIGRATED_CONFIG_NOTICE.equals(Files.readString(file))) {
                return;
            }
            Files.createDirectories(file.getParent());
            Files.writeString(file, MIGRATED_CONFIG_NOTICE);
        }
        catch (IOException ex) {
            LOGGER.error("Failed to write the migrated enchantments.cfg notice", ex);
        }
    }

    /**
     * Retrieves the {@link EnchantmentInfo} for a given enchantment. Reads from the
     * {@code apothic_enchanting:enchantment_info} datamap; if the enchantment has no datamap entry (or the
     * supplied holder is not a {@link Holder.Reference}), returns {@link EnchantmentInfo#fallback(Holder)}.
     */
    public static EnchantmentInfo getEnchInfo(Holder<Enchantment> ench) {
        if (ench instanceof Holder.Reference<Enchantment> ref) {
            EnchantmentInfo info = ref.getData(Ench.DataMaps.ENCHANTMENT_INFO);
            if (info != null) return info;
        }
        return EnchantmentInfo.fallback(ench);
    }

    /**
     * Weakly-keyed cache for {@link #getDefaultMaxLevel} results. The computation loops the min-power function until
     * it crosses 200, which is fairly expensive but called from hot tooltip and lookup paths. Holders dropped from the
     * enchantment registry on datapack reload become unreferenced and are GC'd along with their cached entry.
     */
    private static final ConcurrentMap<Holder<Enchantment>, Integer> DEFAULT_MAX_LEVEL_CACHE = new MapMaker().weakKeys().makeMap();

    /**
     * Tries to find a max level for this enchantment. This is used to scale up default levels to the Apoth cap.
     * Single-Level enchantments are not scaled.
     * Barring that, enchantments are scaled using the {@link DefaultMinPowerFunction} until outside the default level space.
     * <p>
     * Results are memoized per-holder via {@link #DEFAULT_MAX_LEVEL_CACHE}.
     */
    public static int getDefaultMaxLevel(Holder<Enchantment> ench) {
        return DEFAULT_MAX_LEVEL_CACHE.computeIfAbsent(ench, ApothicEnchanting::computeDefaultMaxLevel);
    }

    private static int computeDefaultMaxLevel(Holder<Enchantment> ench) {
        int level = ench.value().getMaxLevel();
        if (level == 1) return 1;
        DefaultMinPowerFunction minFunc = DefaultMinPowerFunction.INSTANCE;
        int max = 200;
        int minPower = minFunc.getPower(level, ench);
        if (minPower >= max) return level;
        int lastPower = minPower;
        while (minPower < max) {
            minPower = minFunc.getPower(++level, ench);
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
            event.accept(createEnchantedBook(new EnchantmentInstance(ench, maxLevel)), TabVisibility.PARENT_TAB_ONLY);
            for (int level = 1; level <= maxLevel; level++) {
                event.accept(createEnchantedBook(new EnchantmentInstance(ench, level)), TabVisibility.SEARCH_TAB_ONLY);
            }
        };
    }

    private static ItemStack createEnchantedBook(EnchantmentInstance inst) {
        ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK);
        stack.enchant(inst.enchantment(), inst.level());
        return stack;
    }

    public static Identifier loc(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    /**
     * Constructs a mutable component with a lang key of the form "type.modid.path", using {@link Apotheosis#MODID}.
     *
     * @param type The type of language key, "misc", "info", "title", etc...
     * @param path The path of the language key.
     * @param args Translation arguments passed to the created translatable component.
     */
    public static MutableComponent lang(String type, String path, Object... args) {
        return Component.translatable(langKey(type, path), args);
    }

    public static String langKey(String type, String path) {
        return type + "." + MODID + "." + path;
    }
}
