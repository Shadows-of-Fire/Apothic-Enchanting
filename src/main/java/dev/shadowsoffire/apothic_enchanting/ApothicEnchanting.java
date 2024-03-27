package dev.shadowsoffire.apothic_enchanting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.shadowsoffire.apothic_attributes.ApothicAttributes;
import dev.shadowsoffire.apothic_enchanting.Ench.Enchantments;
import dev.shadowsoffire.apothic_enchanting.EnchantmentInfo.PowerFunc;
import dev.shadowsoffire.apothic_enchanting.asm.EnchHooks;
import dev.shadowsoffire.apothic_enchanting.library.EnchLibraryTile;
import dev.shadowsoffire.apothic_enchanting.objects.TomeItem;
import dev.shadowsoffire.apothic_enchanting.table.ApothEnchantTile;
import dev.shadowsoffire.apothic_enchanting.table.ClueMessage;
import dev.shadowsoffire.apothic_enchanting.table.EnchantingStatRegistry;
import dev.shadowsoffire.apothic_enchanting.table.StatsMessage;
import dev.shadowsoffire.apothic_enchanting.util.MiscDatagen;
import dev.shadowsoffire.placebo.config.Configuration;
import dev.shadowsoffire.placebo.events.ResourceReloadEvent;
import dev.shadowsoffire.placebo.loot.LootSystem;
import dev.shadowsoffire.placebo.network.PayloadHelper;
import dev.shadowsoffire.placebo.tabs.ITabFiller;
import dev.shadowsoffire.placebo.tabs.TabFillingRegistry;
import dev.shadowsoffire.placebo.util.PlaceboUtil;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.dispenser.ShearsDispenseItemBehavior;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput.Target;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTab.TabVisibility;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.InterModProcessEvent;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.ToolActions;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@Mod(ApothicEnchanting.MODID)
public class ApothicEnchanting {

    public static final String MODID = "apothic_enchanting";
    public static final Logger LOGGER = LogManager.getLogger("Apotheosis : Enchantment");

    public static final Map<Enchantment, EnchantmentInfo> ENCHANTMENT_INFO = new HashMap<>();
    public static final Object2IntMap<Enchantment> ENCH_HARD_CAPS = new Object2IntOpenHashMap<>();
    public static final String ENCH_HARD_CAP_IMC = "set_ench_hard_cap";
    public static final List<TomeItem> TYPED_BOOKS = new ArrayList<>();
    public static final EquipmentSlot[] ARMOR = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
    public static final EnchantmentCategory HOE = EnchantmentCategory.create("HOE", HoeItem.class::isInstance);
    public static final EnchantmentCategory SHIELD = EnchantmentCategory.create("SHIELD", ShieldItem.class::isInstance);
    public static final EnchantmentCategory ANVIL = EnchantmentCategory.create("ANVIL", i -> i instanceof BlockItem && ((BlockItem) i).getBlock() instanceof AnvilBlock);
    public static final EnchantmentCategory SHEARS = EnchantmentCategory.create("SHEARS", ShearsItem.class::isInstance);
    public static final EnchantmentCategory PICKAXE = EnchantmentCategory.create("PICKAXE", i -> i.canPerformAction(new ItemStack(i), ToolActions.PICKAXE_DIG));
    public static final EnchantmentCategory AXE = EnchantmentCategory.create("AXE", i -> i.canPerformAction(new ItemStack(i), ToolActions.AXE_DIG));
    public static final EnchantmentCategory CORE_ARMOR = EnchantmentCategory.create("CORE_ARMOR", i -> EnchantmentCategory.ARMOR_CHEST.canEnchant(i) || EnchantmentCategory.ARMOR_LEGS.canEnchant(i));
    static Configuration enchInfoConfig;

    public ApothicEnchanting(IEventBus bus) {
        Ench.bootstrap(bus);
        bus.register(this);
    }

    @SubscribeEvent
    public void init(FMLCommonSetupEvent e) {
        this.reload(null);

        NeoForge.EVENT_BUS.register(new EnchModuleEvents());
        NeoForge.EVENT_BUS.addListener(this::reload);
        e.enqueueWork(() -> {
            LootSystem.defaultBlockTable(Ench.Blocks.HELLSHELF.get());
            LootSystem.defaultBlockTable(Ench.Blocks.INFUSED_HELLSHELF.get());
            LootSystem.defaultBlockTable(Ench.Blocks.BLAZING_HELLSHELF.get());
            LootSystem.defaultBlockTable(Ench.Blocks.GLOWING_HELLSHELF.get());
            LootSystem.defaultBlockTable(Ench.Blocks.SEASHELF.get());
            LootSystem.defaultBlockTable(Ench.Blocks.INFUSED_SEASHELF.get());
            LootSystem.defaultBlockTable(Ench.Blocks.CRYSTAL_SEASHELF.get());
            LootSystem.defaultBlockTable(Ench.Blocks.HEART_SEASHELF.get());
            LootSystem.defaultBlockTable(Ench.Blocks.DORMANT_DEEPSHELF.get());
            LootSystem.defaultBlockTable(Ench.Blocks.DEEPSHELF.get());
            LootSystem.defaultBlockTable(Ench.Blocks.ECHOING_DEEPSHELF.get());
            LootSystem.defaultBlockTable(Ench.Blocks.SOUL_TOUCHED_DEEPSHELF.get());
            LootSystem.defaultBlockTable(Ench.Blocks.ECHOING_SCULKSHELF.get());
            LootSystem.defaultBlockTable(Ench.Blocks.SOUL_TOUCHED_SCULKSHELF.get());
            LootSystem.defaultBlockTable(Ench.Blocks.ENDSHELF.get());
            LootSystem.defaultBlockTable(Ench.Blocks.PEARL_ENDSHELF.get());
            LootSystem.defaultBlockTable(Ench.Blocks.DRACONIC_ENDSHELF.get());
            LootSystem.defaultBlockTable(Ench.Blocks.BEESHELF.get());
            LootSystem.defaultBlockTable(Ench.Blocks.MELONSHELF.get());
            LootSystem.defaultBlockTable(Ench.Blocks.STONESHELF.get());
            LootSystem.defaultBlockTable(Ench.Blocks.LIBRARY.get());
            LootSystem.defaultBlockTable(Ench.Blocks.RECTIFIER.get());
            LootSystem.defaultBlockTable(Ench.Blocks.RECTIFIER_T2.get());
            LootSystem.defaultBlockTable(Ench.Blocks.RECTIFIER_T3.get());
            LootSystem.defaultBlockTable(Ench.Blocks.SIGHTSHELF.get());
            LootSystem.defaultBlockTable(Ench.Blocks.SIGHTSHELF_T2.get());
            LootSystem.defaultBlockTable(Ench.Blocks.ENDER_LIBRARY.get());
            LootSystem.defaultBlockTable(Ench.Blocks.FILTERING_SHELF.get());
            LootSystem.defaultBlockTable(Ench.Blocks.TREASURE_SHELF.get());
            DispenserBlock.registerBehavior(Items.SHEARS, new ShearsDispenseItemBehavior());

            TabFillingRegistry.register(Ench.Tabs.ENCH.getKey(), Ench.Items.HELLSHELF, Ench.Items.INFUSED_HELLSHELF, Ench.Items.BLAZING_HELLSHELF, Ench.Items.GLOWING_HELLSHELF, Ench.Items.SEASHELF, Ench.Items.INFUSED_SEASHELF,
                Ench.Items.CRYSTAL_SEASHELF, Ench.Items.HEART_SEASHELF, Ench.Items.DORMANT_DEEPSHELF, Ench.Items.DEEPSHELF, Ench.Items.ECHOING_DEEPSHELF, Ench.Items.SOUL_TOUCHED_DEEPSHELF, Ench.Items.ECHOING_SCULKSHELF,
                Ench.Items.SOUL_TOUCHED_SCULKSHELF, Ench.Items.ENDSHELF, Ench.Items.PEARL_ENDSHELF, Ench.Items.DRACONIC_ENDSHELF, Ench.Items.BEESHELF, Ench.Items.MELONSHELF, Ench.Items.STONESHELF, Ench.Items.RECTIFIER,
                Ench.Items.RECTIFIER_T2, Ench.Items.RECTIFIER_T3, Ench.Items.SIGHTSHELF, Ench.Items.SIGHTSHELF_T2, Ench.Items.FILTERING_SHELF, Ench.Items.TREASURE_SHELF, Ench.Items.LIBRARY, Ench.Items.ENDER_LIBRARY);

            TabFillingRegistry.register(Ench.Tabs.ENCH.getKey(), Ench.Items.HELMET_TOME, Ench.Items.CHESTPLATE_TOME, Ench.Items.LEGGINGS_TOME, Ench.Items.BOOTS_TOME, Ench.Items.WEAPON_TOME, Ench.Items.BOW_TOME, Ench.Items.PICKAXE_TOME,
                Ench.Items.FISHING_TOME, Ench.Items.OTHER_TOME, Ench.Items.SCRAP_TOME, Ench.Items.IMPROVED_SCRAP_TOME, Ench.Items.EXTRACTION_TOME);

            TabFillingRegistry.register(Ench.Tabs.ENCH.getKey(), Ench.Items.PRISMATIC_WEB, Ench.Items.INERT_TRIDENT, Ench.Items.WARDEN_TENDRIL, Ench.Items.INFUSED_BREATH);

            fill(Ench.Tabs.ENCH.getKey(), Enchantments.BERSERKERS_FURY, Enchantments.CHAINSAW, Enchantments.CHROMATIC, Enchantments.CRESCENDO, Enchantments.EARTHS_BOON, Enchantments.ENDLESS_QUIVER, Enchantments.EXPLOITATION,
                Enchantments.GROWTH_SERUM, Enchantments.ICY_THORNS, Enchantments.KNOWLEDGE, Enchantments.LIFE_MENDING, Enchantments.MINERS_FERVOR, Enchantments.NATURES_BLESSING, Enchantments.REBOUNDING,
                Enchantments.REFLECTIVE, Enchantments.SCAVENGER, Enchantments.SHIELD_BASH, Enchantments.SPEARFISHING, Enchantments.STABLE_FOOTING, Enchantments.TEMPTING);

            PlaceboUtil.registerCustomColor(Ench.Colors.LIGHT_BLUE_FLASH);

            ObfuscationReflectionHelper.<BlockEntityType<?>, BlockEntityType.BlockEntitySupplier<?>>setPrivateValue(BlockEntityType.class, BlockEntityType.ENCHANTING_TABLE, ApothEnchantTile::new, "factory");
        });

        EnchantingStatRegistry.INSTANCE.registerToBus();

        PayloadHelper.registerPayload(new ClueMessage.Provider());
        PayloadHelper.registerPayload(new StatsMessage.Provider());
    }

    /**
     * This handles IMC events for the enchantment module. <br>
     * Currently only one type is supported. A mod may pass a single {@link EnchantmentInstance} indicating the hard capped max level for an enchantment. <br>
     * That pair must use the method {@link ENCH_HARD_CAP_IMC}.
     */
    @SubscribeEvent
    public void handleIMC(InterModProcessEvent e) {
        e.getIMCStream(ENCH_HARD_CAP_IMC::equals).forEach(msg -> {
            try {
                EnchantmentInstance data = (EnchantmentInstance) msg.messageSupplier().get();
                if (data != null && data.enchantment != null && data.level > 0) {
                    ENCH_HARD_CAPS.put(data.enchantment, data.level);
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
        e.registerBlockEntity(ItemHandler.BLOCK, BlockEntityType.ENCHANTING_TABLE, (be, ctx) -> ((ApothEnchantTile) be).getItemHandler(ctx));
    }

    @SubscribeEvent
    public void data(GatherDataEvent e) {
        MiscDatagen gen = new MiscDatagen(e.getGenerator().getPackOutput().getOutputFolder(Target.DATA_PACK).resolve(MODID));
        e.getGenerator().addProvider(true, gen);
    }

    /*
     * @SubscribeEvent
     * public void enchants(Register<Enchantment> e) {
     * e.getRegistry().registerAll(
     * new BaneEnchant(Rarity.UNCOMMON, MobType.ARTHROPOD, EquipmentSlot.MAINHAND), new ResourceLocation("minecraft", "bane_of_arthropods"),
     * new BaneEnchant(Rarity.UNCOMMON, MobType.UNDEAD, EquipmentSlot.MAINHAND), new ResourceLocation("minecraft", "smite"),
     * new BaneEnchant(Rarity.COMMON, MobType.UNDEFINED, EquipmentSlot.MAINHAND), new ResourceLocation("minecraft", "sharpness"),
     * new BaneEnchant(Rarity.UNCOMMON, MobType.ILLAGER, EquipmentSlot.MAINHAND), "bane_of_illagers",
     * new DefenseEnchant(Rarity.COMMON, ProtectionEnchantment.Type.ALL, ARMOR), new ResourceLocation("minecraft", "protection"),
     * new DefenseEnchant(Rarity.UNCOMMON, ProtectionEnchantment.Type.FIRE, ARMOR), new ResourceLocation("minecraft", "fire_protection"),
     * new DefenseEnchant(Rarity.RARE, ProtectionEnchantment.Type.EXPLOSION, ARMOR), new ResourceLocation("minecraft", "blast_protection"),
     * new DefenseEnchant(Rarity.UNCOMMON, ProtectionEnchantment.Type.PROJECTILE, ARMOR), new ResourceLocation("minecraft", "projectile_protection"),
     * new DefenseEnchant(Rarity.UNCOMMON, ProtectionEnchantment.Type.FALL, EquipmentSlot.FEET), new ResourceLocation("minecraft", "feather_falling"));
     * }
     */

    @SuppressWarnings("deprecation")
    public static EnchantmentInfo getEnchInfo(Enchantment ench) {
        EnchantmentInfo info = ENCHANTMENT_INFO.get(ench);

        if (enchInfoConfig == null) { // Legitimate occurances can now happen, such as when vanilla calls fillItemGroup
            // LOGGER.error("A mod has attempted to access enchantment information before Apotheosis init, this should not happen.");
            // Thread.dumpStack();
            return new EnchantmentInfo(ench);
        }

        if (info == null) { // Should be impossible now.
            info = EnchantmentInfo.load(ench, enchInfoConfig);
            ENCHANTMENT_INFO.put(ench, info);
            if (enchInfoConfig.hasChanged()) enchInfoConfig.save();
            LOGGER.error("Had to late load enchantment info for {}, this is a bug in the mod {} as they are registering late!", BuiltInRegistries.ENCHANTMENT.getKey(ench), BuiltInRegistries.ENCHANTMENT.getKey(ench).getNamespace());
        }

        return info;
    }

    /**
     * Tries to find a max level for this enchantment. This is used to scale up default levels to the Apoth cap.
     * Single-Level enchantments are not scaled.
     * Barring that, enchantments are scaled using the {@link EnchantmentInfo#defaultMin(Enchantment)} until outside the default level space.
     */
    public static int getDefaultMax(Enchantment ench) {
        int level = ench.getMaxLevel();
        if (level == 1) return 1;
        PowerFunc minFunc = EnchantmentInfo.defaultMin(ench);
        int max = (int) (EnchantingStatRegistry.getAbsoluteMaxPower());
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
    public static void fill(ResourceKey<CreativeModeTab> tab, Supplier<? extends Enchantment>... enchants) {
        Arrays.stream(enchants).map(ApothicEnchanting::enchFiller).forEach(filler -> TabFillingRegistry.register(filler, tab));
    }

    public static ITabFiller enchFiller(Supplier<? extends Enchantment> e) {
        return (tab, output) -> {
            Enchantment ench = e.get();
            int maxLevel = EnchHooks.getMaxLevel(ench);
            output.accept(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(ench, maxLevel)), TabVisibility.PARENT_TAB_ONLY);
            for (int level = 1; level <= maxLevel; level++) {
                output.accept(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(ench, level)), TabVisibility.SEARCH_TAB_ONLY);
            }
        };
    }

    public void reload(ResourceReloadEvent e) {
        enchInfoConfig = new Configuration(ApothicAttributes.getConfigFile("enchantments"));
        enchInfoConfig.setTitle("Apotheosis Enchantment Information");
        enchInfoConfig.setComment("This file contains configurable data for each enchantment.\nThe names of each category correspond to the registry names of every loaded enchantment.");
        ENCHANTMENT_INFO.clear();

        for (Enchantment ench : BuiltInRegistries.ENCHANTMENT) {
            ENCHANTMENT_INFO.put(ench, EnchantmentInfo.load(ench, enchInfoConfig));
        }

        for (Enchantment ench : BuiltInRegistries.ENCHANTMENT) {
            EnchantmentInfo info = ENCHANTMENT_INFO.get(ench);
            for (int i = 1; i <= info.getMaxLevel(); i++)
                if (info.getMinPower(i) > info.getMaxPower(i))
                    LOGGER.warn("Enchantment {} has min/max power {}/{} at level {}, making this level unobtainable except by combination.", BuiltInRegistries.ENCHANTMENT.getKey(ench), info.getMinPower(i), info.getMaxPower(i), i);
        }

        if (e == null && enchInfoConfig.hasChanged()) enchInfoConfig.save();
        EnchConfig.load(new Configuration(ApothicAttributes.getConfigFile(MODID)));
    }

    public static ResourceLocation loc(String path) {
        return new ResourceLocation(MODID, path);
    }
}
