package dev.shadowsoffire.apothic_enchanting;

import java.util.function.Supplier;

import com.google.common.collect.ImmutableSet;

import dev.shadowsoffire.apothic_enchanting.enchantments.ChromaticEnchant;
import dev.shadowsoffire.apothic_enchanting.enchantments.IcyThornsEnchant;
import dev.shadowsoffire.apothic_enchanting.enchantments.InertEnchantment;
import dev.shadowsoffire.apothic_enchanting.enchantments.NaturesBlessingEnchant;
import dev.shadowsoffire.apothic_enchanting.enchantments.ReboundingEnchant;
import dev.shadowsoffire.apothic_enchanting.enchantments.ReflectiveEnchant;
import dev.shadowsoffire.apothic_enchanting.enchantments.ShieldBashEnchant;
import dev.shadowsoffire.apothic_enchanting.enchantments.SpearfishingEnchant;
import dev.shadowsoffire.apothic_enchanting.enchantments.StableFootingEnchant;
import dev.shadowsoffire.apothic_enchanting.enchantments.TemptingEnchant;
import dev.shadowsoffire.apothic_enchanting.enchantments.corrupted.BerserkersFuryEnchant;
import dev.shadowsoffire.apothic_enchanting.enchantments.corrupted.LifeMendingEnchant;
import dev.shadowsoffire.apothic_enchanting.enchantments.masterwork.ChainsawEnchant;
import dev.shadowsoffire.apothic_enchanting.enchantments.masterwork.CrescendoEnchant;
import dev.shadowsoffire.apothic_enchanting.enchantments.masterwork.EarthsBoonEnchant;
import dev.shadowsoffire.apothic_enchanting.enchantments.masterwork.EndlessQuiverEnchant;
import dev.shadowsoffire.apothic_enchanting.enchantments.masterwork.GrowthSerumEnchant;
import dev.shadowsoffire.apothic_enchanting.enchantments.masterwork.KnowledgeEnchant;
import dev.shadowsoffire.apothic_enchanting.enchantments.masterwork.ScavengerEnchant;
import dev.shadowsoffire.apothic_enchanting.enchantments.twisted.ExploitationEnchant;
import dev.shadowsoffire.apothic_enchanting.enchantments.twisted.MinersFervorEnchant;
import dev.shadowsoffire.apothic_enchanting.library.EnchLibraryBlock;
import dev.shadowsoffire.apothic_enchanting.library.EnchLibraryContainer;
import dev.shadowsoffire.apothic_enchanting.library.EnchLibraryTile.BasicLibraryTile;
import dev.shadowsoffire.apothic_enchanting.library.EnchLibraryTile.EnderLibraryTile;
import dev.shadowsoffire.apothic_enchanting.objects.ExtractionTomeItem;
import dev.shadowsoffire.apothic_enchanting.objects.FilteringShelfBlock;
import dev.shadowsoffire.apothic_enchanting.objects.FilteringShelfBlock.FilteringShelfTile;
import dev.shadowsoffire.apothic_enchanting.objects.GlowyBlockItem;
import dev.shadowsoffire.apothic_enchanting.objects.ImprovedScrappingTomeItem;
import dev.shadowsoffire.apothic_enchanting.objects.ScrappingTomeItem;
import dev.shadowsoffire.apothic_enchanting.objects.TomeItem;
import dev.shadowsoffire.apothic_enchanting.objects.TreasureShelfBlock;
import dev.shadowsoffire.apothic_enchanting.objects.TypedShelfBlock;
import dev.shadowsoffire.apothic_enchanting.objects.TypedShelfBlock.SculkShelfBlock;
import dev.shadowsoffire.apothic_enchanting.objects.WardenLootModifier;
import dev.shadowsoffire.apothic_enchanting.table.ApothEnchantmentMenu;
import dev.shadowsoffire.apothic_enchanting.table.EnchantingRecipe;
import dev.shadowsoffire.apothic_enchanting.table.KeepNBTEnchantingRecipe;
import dev.shadowsoffire.apothic_enchanting.util.MiscUtil;
import dev.shadowsoffire.apothic_enchanting.util.TooltipUtil;
import dev.shadowsoffire.placebo.color.GradientColor;
import dev.shadowsoffire.placebo.menu.MenuUtil;
import dev.shadowsoffire.placebo.registry.DeferredHelper;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class Ench {

    private static final DeferredHelper R = DeferredHelper.create(ApothicEnchanting.MODID);

    public static final class Blocks {

        public static final Supplier<Block> BEESHELF = woodShelf("beeshelf", MapColor.COLOR_YELLOW, 0.75F, () -> ParticleTypes.ENCHANT);

        public static final Supplier<Block> BLAZING_HELLSHELF = stoneShelf("blazing_hellshelf", MapColor.COLOR_BLACK, 1.5F, Particles.ENCHANT_FIRE);

        public static final Supplier<Block> CRYSTAL_SEASHELF = stoneShelf("crystal_seashelf", MapColor.COLOR_CYAN, 1.5F, Particles.ENCHANT_WATER);

        public static final Supplier<Block> DEEPSHELF = stoneShelf("deepshelf", MapColor.COLOR_BLACK, 2.5F, Particles.ENCHANT_SCULK);

        public static final Supplier<Block> DORMANT_DEEPSHELF = stoneShelf("dormant_deepshelf", MapColor.COLOR_BLACK, 2.5F, Particles.ENCHANT_SCULK);

        public static final Supplier<Block> DRACONIC_ENDSHELF = stoneShelf("draconic_endshelf", MapColor.SAND, 5F, Particles.ENCHANT_END);

        public static final Supplier<Block> ECHOING_DEEPSHELF = stoneShelf("echoing_deepshelf", MapColor.COLOR_BLACK, 2.5F, Particles.ENCHANT_SCULK);

        public static final Supplier<Block> ECHOING_SCULKSHELF = sculkShelf("echoing_sculkshelf");

        public static final Supplier<EnchLibraryBlock> ENDER_LIBRARY = R.block("ender_library", () -> new EnchLibraryBlock(EnderLibraryTile::new, 31));

        public static final Supplier<Block> ENDSHELF = stoneShelf("endshelf", MapColor.SAND, 4.5F, Particles.ENCHANT_END);

        public static final Supplier<Block> GLOWING_HELLSHELF = stoneShelf("glowing_hellshelf", MapColor.COLOR_BLACK, 1.5F, Particles.ENCHANT_FIRE);

        public static final Supplier<Block> HEART_SEASHELF = stoneShelf("heart_seashelf", MapColor.COLOR_CYAN, 1.5F, Particles.ENCHANT_WATER);

        public static final Supplier<Block> HELLSHELF = stoneShelf("hellshelf", MapColor.COLOR_BLACK, 1.5F, Particles.ENCHANT_FIRE);

        public static final Supplier<Block> INFUSED_HELLSHELF = stoneShelf("infused_hellshelf", MapColor.COLOR_BLACK, 1.5F, Particles.ENCHANT_FIRE);

        public static final Supplier<Block> INFUSED_SEASHELF = stoneShelf("infused_seashelf", MapColor.COLOR_CYAN, 1.5F, Particles.ENCHANT_WATER);

        public static final Supplier<EnchLibraryBlock> LIBRARY = R.block("library", () -> new EnchLibraryBlock(BasicLibraryTile::new, 16));

        public static final Supplier<Block> MELONSHELF = woodShelf("melonshelf", MapColor.COLOR_GREEN, 0.75F, () -> ParticleTypes.ENCHANT);

        public static final Supplier<Block> PEARL_ENDSHELF = stoneShelf("pearl_endshelf", MapColor.SAND, 4.5F, Particles.ENCHANT_END);

        public static final Supplier<Block> RECTIFIER = stoneShelf("rectifier", MapColor.COLOR_CYAN, 1.5F, Particles.ENCHANT_WATER);

        public static final Supplier<Block> RECTIFIER_T2 = stoneShelf("rectifier_t2", MapColor.COLOR_BLACK, 1.5F, Particles.ENCHANT_FIRE);

        public static final Supplier<Block> RECTIFIER_T3 = stoneShelf("rectifier_t3", MapColor.SAND, 1.5F, Particles.ENCHANT_END);

        public static final Supplier<Block> SEASHELF = stoneShelf("seashelf", MapColor.COLOR_CYAN, 1.5F, Particles.ENCHANT_WATER);

        public static final Supplier<Block> SIGHTSHELF = stoneShelf("sightshelf", MapColor.COLOR_BLACK, 1.5F, Particles.ENCHANT_FIRE);

        public static final Supplier<Block> SIGHTSHELF_T2 = stoneShelf("sightshelf_t2", MapColor.COLOR_BLACK, 1.5F, Particles.ENCHANT_FIRE);

        public static final Supplier<Block> SOUL_TOUCHED_DEEPSHELF = stoneShelf("soul_touched_deepshelf", MapColor.COLOR_BLACK, 2.5F, Particles.ENCHANT_SCULK);

        public static final Supplier<Block> SOUL_TOUCHED_SCULKSHELF = sculkShelf("soul_touched_sculkshelf");

        public static final Supplier<Block> STONESHELF = stoneShelf("stoneshelf", MapColor.STONE, 1.75F, () -> ParticleTypes.ENCHANT);

        public static final Supplier<Block> FILTERING_SHELF = R.block("filtering_shelf",
            () -> new FilteringShelfBlock(Block.Properties.of().mapColor(MapColor.COLOR_CYAN).sound(SoundType.STONE).strength(1.75F).requiresCorrectToolForDrops()));

        public static final Supplier<Block> TREASURE_SHELF = R.block("treasure_shelf",
            () -> new TreasureShelfBlock(Block.Properties.of().mapColor(MapColor.COLOR_BLACK).sound(SoundType.STONE).strength(1.75F).requiresCorrectToolForDrops()));

        private static void bootstrap() {}

        private static Supplier<Block> sculkShelf(String id) {
            return R.block(id, () -> new SculkShelfBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).sound(SoundType.STONE).randomTicks().requiresCorrectToolForDrops().strength(3.5F), Particles.ENCHANT_SCULK));
        }

        private static Supplier<Block> stoneShelf(String id, MapColor color, float strength, Supplier<? extends ParticleOptions> particle) {
            return R.block(id, () -> new TypedShelfBlock(Block.Properties.of().requiresCorrectToolForDrops().sound(SoundType.STONE).mapColor(color).strength(strength), particle));
        }

        private static Supplier<Block> woodShelf(String id, MapColor color, float strength, Supplier<? extends ParticleOptions> particle) {
            return R.block(id, () -> new TypedShelfBlock(Block.Properties.of().sound(SoundType.WOOD).mapColor(color).strength(strength), particle));
        }

    }

    public static class Items {

        public static final Supplier<BlockItem> BEESHELF = R.item("beeshelf", () -> new BlockItem(Ench.Blocks.BEESHELF.get(), new Item.Properties()));

        public static final Supplier<BlockItem> BLAZING_HELLSHELF = R.item("blazing_hellshelf", () -> new BlockItem(Ench.Blocks.BLAZING_HELLSHELF.get(), new Item.Properties()));

        public static final Supplier<TomeItem> BOOTS_TOME = R.item("boots_tome", () -> new TomeItem(net.minecraft.world.item.Items.DIAMOND_BOOTS, EnchantmentCategory.ARMOR_FEET));

        public static final Supplier<TomeItem> BOW_TOME = R.item("bow_tome", () -> new TomeItem(net.minecraft.world.item.Items.BOW, EnchantmentCategory.BOW));

        public static final Supplier<TomeItem> CHESTPLATE_TOME = R.item("chestplate_tome", () -> new TomeItem(net.minecraft.world.item.Items.DIAMOND_CHESTPLATE, EnchantmentCategory.ARMOR_CHEST));

        public static final Supplier<BlockItem> CRYSTAL_SEASHELF = R.item("crystal_seashelf", () -> new BlockItem(Ench.Blocks.CRYSTAL_SEASHELF.get(), new Item.Properties()));

        public static final Supplier<GlowyBlockItem> DEEPSHELF = R.item("deepshelf", () -> new GlowyBlockItem(Ench.Blocks.DEEPSHELF.get(), new Item.Properties()));

        public static final Supplier<BlockItem> DORMANT_DEEPSHELF = R.item("dormant_deepshelf", () -> new BlockItem(Ench.Blocks.DORMANT_DEEPSHELF.get(), new Item.Properties()));

        public static final Supplier<BlockItem> DRACONIC_ENDSHELF = R.item("draconic_endshelf", () -> new BlockItem(Ench.Blocks.DRACONIC_ENDSHELF.get(), new Item.Properties()));

        public static final Supplier<BlockItem> ECHOING_DEEPSHELF = R.item("echoing_deepshelf", () -> new BlockItem(Ench.Blocks.ECHOING_DEEPSHELF.get(), new Item.Properties()));

        public static final Supplier<BlockItem> ECHOING_SCULKSHELF = R.item("echoing_sculkshelf", () -> new BlockItem(Ench.Blocks.ECHOING_SCULKSHELF.get(), new Item.Properties()));

        public static final Supplier<BlockItem> ENDER_LIBRARY = R.item("ender_library", () -> new BlockItem(Ench.Blocks.ENDER_LIBRARY.get(), new Item.Properties()));

        public static final Supplier<BlockItem> ENDSHELF = R.item("endshelf", () -> new BlockItem(Ench.Blocks.ENDSHELF.get(), new Item.Properties()));

        public static final Supplier<ExtractionTomeItem> EXTRACTION_TOME = R.item("extraction_tome", ExtractionTomeItem::new);

        public static final Supplier<TomeItem> FISHING_TOME = R.item("fishing_tome", () -> new TomeItem(net.minecraft.world.item.Items.FISHING_ROD, EnchantmentCategory.FISHING_ROD));

        public static final Supplier<BlockItem> GLOWING_HELLSHELF = R.item("glowing_hellshelf", () -> new BlockItem(Ench.Blocks.GLOWING_HELLSHELF.get(), new Item.Properties()));

        public static final Supplier<BlockItem> HEART_SEASHELF = R.item("heart_seashelf", () -> new BlockItem(Ench.Blocks.HEART_SEASHELF.get(), new Item.Properties()));

        public static final Supplier<BlockItem> HELLSHELF = R.item("hellshelf", () -> new BlockItem(Ench.Blocks.HELLSHELF.get(), new Item.Properties()));

        public static final Supplier<TomeItem> HELMET_TOME = R.item("helmet_tome", () -> new TomeItem(net.minecraft.world.item.Items.DIAMOND_HELMET, EnchantmentCategory.ARMOR_HEAD));

        public static final Supplier<ImprovedScrappingTomeItem> IMPROVED_SCRAP_TOME = R.item("improved_scrap_tome", ImprovedScrappingTomeItem::new);

        public static final Supplier<Item> INERT_TRIDENT = R.item("inert_trident", () -> new Item(new Item.Properties().stacksTo(1)));

        public static final Supplier<Item> INFUSED_BREATH = R.item("infused_breath", () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)));

        public static final Supplier<GlowyBlockItem> INFUSED_HELLSHELF = R.item("infused_hellshelf", () -> new GlowyBlockItem(Ench.Blocks.INFUSED_HELLSHELF.get(), new Item.Properties()));

        public static final Supplier<GlowyBlockItem> INFUSED_SEASHELF = R.item("infused_seashelf", () -> new GlowyBlockItem(Ench.Blocks.INFUSED_SEASHELF.get(), new Item.Properties()));

        public static final Supplier<TomeItem> LEGGINGS_TOME = R.item("leggings_tome", () -> new TomeItem(net.minecraft.world.item.Items.DIAMOND_LEGGINGS, EnchantmentCategory.ARMOR_LEGS));

        public static final Supplier<BlockItem> LIBRARY = R.item("library", () -> new BlockItem(Ench.Blocks.LIBRARY.get(), new Item.Properties()));

        public static final Supplier<BlockItem> MELONSHELF = R.item("melonshelf", () -> new BlockItem(Ench.Blocks.MELONSHELF.get(), new Item.Properties()));

        public static final Supplier<TomeItem> OTHER_TOME = R.item("other_tome", () -> new TomeItem(net.minecraft.world.item.Items.AIR, null));

        public static final Supplier<BlockItem> PEARL_ENDSHELF = R.item("pearl_endshelf", () -> new BlockItem(Ench.Blocks.PEARL_ENDSHELF.get(), new Item.Properties()));

        public static final Supplier<TomeItem> PICKAXE_TOME = R.item("pickaxe_tome", () -> new TomeItem(net.minecraft.world.item.Items.DIAMOND_PICKAXE, EnchantmentCategory.DIGGER));

        public static final Supplier<Item> PRISMATIC_WEB = R.item("prismatic_web", () -> new Item(new Item.Properties()));

        public static final Supplier<BlockItem> RECTIFIER = R.item("rectifier", () -> new BlockItem(Ench.Blocks.RECTIFIER.get(), new Item.Properties()));

        public static final Supplier<BlockItem> RECTIFIER_T2 = R.item("rectifier_t2", () -> new BlockItem(Ench.Blocks.RECTIFIER_T2.get(), new Item.Properties()));

        public static final Supplier<BlockItem> RECTIFIER_T3 = R.item("rectifier_t3", () -> new BlockItem(Ench.Blocks.RECTIFIER_T3.get(), new Item.Properties()));

        public static final Supplier<ScrappingTomeItem> SCRAP_TOME = R.item("scrap_tome", ScrappingTomeItem::new);

        public static final Supplier<BlockItem> SEASHELF = R.item("seashelf", () -> new BlockItem(Ench.Blocks.SEASHELF.get(), new Item.Properties()));

        public static final Supplier<BlockItem> SIGHTSHELF = R.item("sightshelf", () -> new BlockItem(Ench.Blocks.SIGHTSHELF.get(), new Item.Properties()));

        public static final Supplier<BlockItem> SIGHTSHELF_T2 = R.item("sightshelf_t2", () -> new BlockItem(Ench.Blocks.SIGHTSHELF_T2.get(), new Item.Properties()));

        public static final Supplier<BlockItem> SOUL_TOUCHED_DEEPSHELF = R.item("soul_touched_deepshelf", () -> new BlockItem(Ench.Blocks.SOUL_TOUCHED_DEEPSHELF.get(), new Item.Properties()));

        public static final Supplier<BlockItem> SOUL_TOUCHED_SCULKSHELF = R.item("soul_touched_sculkshelf", () -> new BlockItem(Ench.Blocks.SOUL_TOUCHED_SCULKSHELF.get(), new Item.Properties()));

        public static final Supplier<BlockItem> STONESHELF = R.item("stoneshelf", () -> new BlockItem(Ench.Blocks.STONESHELF.get(), new Item.Properties()));

        public static final Supplier<Item> WARDEN_TENDRIL = R.item("warden_tendril", () -> new Item(new Item.Properties()));

        public static final Supplier<TomeItem> WEAPON_TOME = R.item("weapon_tome", () -> new TomeItem(net.minecraft.world.item.Items.DIAMOND_SWORD, EnchantmentCategory.WEAPON));

        public static final Supplier<BlockItem> FILTERING_SHELF = R.item("filtering_shelf", () -> new BlockItem(Ench.Blocks.FILTERING_SHELF.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));

        public static final Supplier<BlockItem> TREASURE_SHELF = R.item("treasure_shelf", () -> new BlockItem(Ench.Blocks.TREASURE_SHELF.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));

        private static void bootstrap() {}

    }

    public static final class Enchantments {

        public static final Supplier<BerserkersFuryEnchant> BERSERKERS_FURY = R.enchant("berserkers_fury", BerserkersFuryEnchant::new);

        public static final Supplier<ChainsawEnchant> CHAINSAW = R.enchant("chainsaw", ChainsawEnchant::new);

        public static final Supplier<ChromaticEnchant> CHROMATIC = R.enchant("chromatic", ChromaticEnchant::new);

        public static final Supplier<CrescendoEnchant> CRESCENDO = R.enchant("crescendo", CrescendoEnchant::new);

        public static final Supplier<EarthsBoonEnchant> EARTHS_BOON = R.enchant("earths_boon", EarthsBoonEnchant::new);

        public static final Supplier<EndlessQuiverEnchant> ENDLESS_QUIVER = R.enchant("endless_quiver", EndlessQuiverEnchant::new);

        public static final Supplier<ExploitationEnchant> EXPLOITATION = R.enchant("exploitation", ExploitationEnchant::new);

        public static final Supplier<GrowthSerumEnchant> GROWTH_SERUM = R.enchant("growth_serum", GrowthSerumEnchant::new);

        public static final Supplier<IcyThornsEnchant> ICY_THORNS = R.enchant("icy_thorns", IcyThornsEnchant::new);

        public static final Supplier<InertEnchantment> INFUSION = R.enchant("infusion", InertEnchantment::new);

        public static final Supplier<KnowledgeEnchant> KNOWLEDGE = R.enchant("knowledge", KnowledgeEnchant::new);

        public static final Supplier<LifeMendingEnchant> LIFE_MENDING = R.enchant("life_mending", LifeMendingEnchant::new);

        public static final Supplier<MinersFervorEnchant> MINERS_FERVOR = R.enchant("miners_fervor", MinersFervorEnchant::new);

        public static final Supplier<NaturesBlessingEnchant> NATURES_BLESSING = R.enchant("natures_blessing", NaturesBlessingEnchant::new);

        public static final Supplier<ReboundingEnchant> REBOUNDING = R.enchant("rebounding", ReboundingEnchant::new);

        public static final Supplier<ReflectiveEnchant> REFLECTIVE = R.enchant("reflective", ReflectiveEnchant::new);

        public static final Supplier<ScavengerEnchant> SCAVENGER = R.enchant("scavenger", ScavengerEnchant::new);

        public static final Supplier<ShieldBashEnchant> SHIELD_BASH = R.enchant("shield_bash", ShieldBashEnchant::new);

        public static final Supplier<SpearfishingEnchant> SPEARFISHING = R.enchant("spearfishing", SpearfishingEnchant::new);

        public static final Supplier<StableFootingEnchant> STABLE_FOOTING = R.enchant("stable_footing", StableFootingEnchant::new);

        public static final Supplier<TemptingEnchant> TEMPTING = R.enchant("tempting", TemptingEnchant::new);

        private static void bootstrap() {}

    }

    public static class Tabs {

        public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ENCH = R.tab("ench",
            () -> CreativeModeTab.builder().title(TooltipUtil.lang("creative_tab", "all")).icon(() -> Items.HELLSHELF.get().getDefaultInstance()).build());

        private static void bootstrap() {}

    }

    public static class Tiles {

        public static final Supplier<BlockEntityType<FilteringShelfTile>> FILTERING_SHELF = R.blockEntity("filtering_shelf",
            () -> new BlockEntityType<>(FilteringShelfTile::new, ImmutableSet.of(Blocks.FILTERING_SHELF.get()), null));

        public static final Supplier<BlockEntityType<BasicLibraryTile>> LIBRARY = R.blockEntity("library",
            () -> new BlockEntityType<>(BasicLibraryTile::new, ImmutableSet.of(Blocks.LIBRARY.get()), null));

        public static final Supplier<BlockEntityType<EnderLibraryTile>> ENDER_LIBRARY = R.blockEntity("ender_library",
            () -> new BlockEntityType<>(EnderLibraryTile::new, ImmutableSet.of(Blocks.ENDER_LIBRARY.get()), null));

        private static void bootstrap() {}

    }

    public static class Menus {

        public static final Supplier<MenuType<ApothEnchantmentMenu>> ENCHANTING_TABLE = R.menu("enchanting_table", () -> MenuUtil.type(ApothEnchantmentMenu::new));

        public static final Supplier<MenuType<EnchLibraryContainer>> LIBRARY = R.menu("library", () -> MenuUtil.posType(EnchLibraryContainer::new));

        private static void bootstrap() {}

    }

    public static class Colors {
        private static int[] _LIGHT_BLUE_FLASH = { 0x00b3ff, 0x00b3ff, 0x00b3ff, 0x00b3ff, 0x00b3ff, 0x00b3ff, 0x00b3ff, 0x00b3ff, 0x00b3ff,
            0x00b3ff, 0x00b3ff, 0x00b3ff, 0x00b3ff, 0x00b3ff, 0x00b3ff, 0x00b3ff, 0x00b3ff, 0x00b3ff,
            0x00b3ff, 0x00b3ff, 0x00b3ff, 0x00b3ff, 0x00b3ff, 0x00b3ff, 0x00b3ff, 0x00b3ff, 0x00b3ff,
            0x00b3ff, 0x00b3ff, 0x00b3ff, 0x00b3ff, 0x00b3ff, 0x00b3ff, 0x00b3ff, 0x00b3ff, 0x0bb5ff,
            0x17b8ff, 0x22bbff, 0x2dbdff, 0x39c0ff, 0x44c3ff, 0x4fc6ff, 0x5bc9ff, 0x66ccff };

        public static GradientColor LIGHT_BLUE_FLASH = new GradientColor(MiscUtil.doubleUpGradient(_LIGHT_BLUE_FLASH), "light_blue_flash");
    }

    public static class Particles {
        public static final Supplier<SimpleParticleType> ENCHANT_FIRE = R.particle("enchant_fire", () -> new SimpleParticleType(false));
        public static final Supplier<SimpleParticleType> ENCHANT_WATER = R.particle("enchant_water", () -> new SimpleParticleType(false));
        public static final Supplier<SimpleParticleType> ENCHANT_SCULK = R.particle("enchant_sculk", () -> new SimpleParticleType(false));
        public static final Supplier<SimpleParticleType> ENCHANT_END = R.particle("enchant_end", () -> new SimpleParticleType(false));

        private static void bootstrap() {}
    }

    public static class RecipeTypes {
        public static final Supplier<RecipeType<EnchantingRecipe>> INFUSION = R.recipe("infusion", () -> new RecipeType<EnchantingRecipe>(){});

        private static void bootstrap() {}
    }

    public static final class Tags {
        public static final TagKey<Item> BOON_DROPS = ItemTags.create(ApothicEnchanting.loc("boon_drops"));
        public static final TagKey<Item> SPEARFISHING_DROPS = ItemTags.create(ApothicEnchanting.loc("spearfishing_drops"));
    }

    public static final class DamageTypes {
        public static final ResourceKey<DamageType> CORRUPTED = ResourceKey.create(Registries.DAMAGE_TYPE, ApothicEnchanting.loc("corrupted"));
    }

    static {
        R.recipeSerializer("infusion", () -> EnchantingRecipe.SERIALIZER);
        R.recipeSerializer("keep_nbt_infusion", () -> KeepNBTEnchantingRecipe.SERIALIZER);
        R.custom("warden_tendril", NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, () -> WardenLootModifier.CODEC);
    }

    public static void bootstrap(IEventBus bus) {
        Blocks.bootstrap();
        Items.bootstrap();
        Enchantments.bootstrap();
        Tabs.bootstrap();
        Tiles.bootstrap();
        Particles.bootstrap();
        Menus.bootstrap();
        RecipeTypes.bootstrap();
        bus.register(R);
    }

}
