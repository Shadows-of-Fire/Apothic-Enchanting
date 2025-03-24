package dev.shadowsoffire.apothic_enchanting;

import java.util.List;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;

import dev.shadowsoffire.apothic_enchanting.advancements.EnchantedTrigger;
import dev.shadowsoffire.apothic_enchanting.enchantments.components.BerserkingComponent;
import dev.shadowsoffire.apothic_enchanting.enchantments.components.BoonComponent;
import dev.shadowsoffire.apothic_enchanting.enchantments.components.ReflectiveComponent;
import dev.shadowsoffire.apothic_enchanting.enchantments.entity_effects.ReboundingEffect;
import dev.shadowsoffire.apothic_enchanting.enchantments.values.ExponentialLevelBasedValue;
import dev.shadowsoffire.apothic_enchanting.library.EnchLibraryBlock;
import dev.shadowsoffire.apothic_enchanting.library.EnchLibraryContainer;
import dev.shadowsoffire.apothic_enchanting.library.EnchLibraryTile.BasicLibraryTile;
import dev.shadowsoffire.apothic_enchanting.library.EnchLibraryTile.EnderLibraryTile;
import dev.shadowsoffire.apothic_enchanting.objects.ExtractionTomeItem;
import dev.shadowsoffire.apothic_enchanting.objects.FilteringShelfBlock;
import dev.shadowsoffire.apothic_enchanting.objects.FilteringShelfBlock.FilteringShelfTile;
import dev.shadowsoffire.apothic_enchanting.objects.GeodeShelfBlock;
import dev.shadowsoffire.apothic_enchanting.objects.GlowyBlockItem;
import dev.shadowsoffire.apothic_enchanting.objects.ImprovedScrappingTomeItem;
import dev.shadowsoffire.apothic_enchanting.objects.ScrappingTomeItem;
import dev.shadowsoffire.apothic_enchanting.objects.TomeItem;
import dev.shadowsoffire.apothic_enchanting.objects.TreasureShelfBlock;
import dev.shadowsoffire.apothic_enchanting.objects.TypedShelfBlock;
import dev.shadowsoffire.apothic_enchanting.objects.TypedShelfBlock.SculkShelfBlock;
import dev.shadowsoffire.apothic_enchanting.objects.WardenLootModifier;
import dev.shadowsoffire.apothic_enchanting.table.ApothEnchantmentMenu;
import dev.shadowsoffire.apothic_enchanting.table.EnchantmentTableItemHandler;
import dev.shadowsoffire.apothic_enchanting.table.infusion.InfusionRecipe;
import dev.shadowsoffire.apothic_enchanting.table.infusion.KeepNBTInfusionRecipe;
import dev.shadowsoffire.apothic_enchanting.util.MiscUtil;
import dev.shadowsoffire.apothic_enchanting.util.TooltipUtil;
import dev.shadowsoffire.placebo.color.GradientColor;
import dev.shadowsoffire.placebo.registry.DeferredHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.ConditionalEffect;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.EnchantmentValueEffect;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Registration and object holders. Each type of object has its own subclass.
 */
public class Ench {

    private static final DeferredHelper R = DeferredHelper.create(ApothicEnchanting.MODID);

    static {
        R.recipeSerializer("infusion", () -> InfusionRecipe.SERIALIZER);
        R.recipeSerializer("keep_nbt_infusion", () -> KeepNBTInfusionRecipe.SERIALIZER);
        R.custom("warden_tendril", NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, () -> WardenLootModifier.CODEC);
        R.custom("enchantment_table_item_handler", NeoForgeRegistries.Keys.ATTACHMENT_TYPES, () -> EnchantmentTableItemHandler.TYPE);
        R.custom("rebounding", Registries.ENCHANTMENT_ENTITY_EFFECT_TYPE, () -> ReboundingEffect.CODEC);
        R.custom("exponential", Registries.ENCHANTMENT_LEVEL_BASED_VALUE_TYPE, () -> ExponentialLevelBasedValue.CODEC);
    }

    public static final class Blocks {

        public static final Holder<Block> BEESHELF = woodShelf("beeshelf", MapColor.COLOR_YELLOW, 0.75F, () -> ParticleTypes.ENCHANT);

        public static final Holder<Block> BLAZING_HELLSHELF = stoneShelf("blazing_hellshelf", MapColor.COLOR_BLACK, 1.5F, Particles.ENCHANT_FIRE);

        public static final Holder<Block> CRYSTAL_SEASHELF = stoneShelf("crystal_seashelf", MapColor.COLOR_CYAN, 1.5F, Particles.ENCHANT_WATER);

        public static final Holder<Block> DEEPSHELF = stoneShelf("deepshelf", MapColor.COLOR_BLACK, 2.5F, Particles.ENCHANT_SCULK);

        public static final Holder<Block> DORMANT_DEEPSHELF = stoneShelf("dormant_deepshelf", MapColor.COLOR_BLACK, 2.5F, Particles.ENCHANT_SCULK);

        public static final Holder<Block> DRACONIC_ENDSHELF = stoneShelf("draconic_endshelf", MapColor.SAND, 5F, Particles.ENCHANT_END);

        public static final Holder<Block> ECHOING_DEEPSHELF = stoneShelf("echoing_deepshelf", MapColor.COLOR_BLACK, 2.5F, Particles.ENCHANT_SCULK);

        public static final Holder<Block> ECHOING_SCULKSHELF = sculkShelf("echoing_sculkshelf");

        public static final Holder<Block> ENDER_LIBRARY = R.block("ender_library", () -> new EnchLibraryBlock(EnderLibraryTile::new, 31));

        public static final Holder<Block> ENDSHELF = stoneShelf("endshelf", MapColor.SAND, 4.5F, Particles.ENCHANT_END);

        public static final Holder<Block> FILTERING_SHELF = R.block("filtering_shelf", FilteringShelfBlock::new,
            p -> p.mapColor(MapColor.COLOR_CYAN).sound(SoundType.STONE).strength(1.75F).requiresCorrectToolForDrops());

        public static final Holder<Block> GEODE_SHELF = R.block("geode_shelf", GeodeShelfBlock::new,
            p -> p.mapColor(MapColor.TERRACOTTA_WHITE).sound(SoundType.STONE).strength(1.75F).requiresCorrectToolForDrops());

        public static final Holder<Block> GLOWING_HELLSHELF = stoneShelf("glowing_hellshelf", MapColor.COLOR_BLACK, 1.5F, Particles.ENCHANT_FIRE);

        public static final Holder<Block> HEART_SEASHELF = stoneShelf("heart_seashelf", MapColor.COLOR_CYAN, 1.5F, Particles.ENCHANT_WATER);

        public static final Holder<Block> HELLSHELF = stoneShelf("hellshelf", MapColor.COLOR_BLACK, 1.5F, Particles.ENCHANT_FIRE);

        public static final Holder<Block> INFUSED_HELLSHELF = stoneShelf("infused_hellshelf", MapColor.COLOR_BLACK, 1.5F, Particles.ENCHANT_FIRE);

        public static final Holder<Block> INFUSED_SEASHELF = stoneShelf("infused_seashelf", MapColor.COLOR_CYAN, 1.5F, Particles.ENCHANT_WATER);

        public static final Holder<Block> LIBRARY = R.block("library", () -> new EnchLibraryBlock(BasicLibraryTile::new, 16));

        public static final Holder<Block> MELONSHELF = woodShelf("melonshelf", MapColor.COLOR_GREEN, 0.75F, () -> ParticleTypes.ENCHANT);

        public static final Holder<Block> PEARL_ENDSHELF = stoneShelf("pearl_endshelf", MapColor.SAND, 4.5F, Particles.ENCHANT_END);

        public static final Holder<Block> SEASHELF = stoneShelf("seashelf", MapColor.COLOR_CYAN, 1.5F, Particles.ENCHANT_WATER);

        public static final Holder<Block> SIGHTSHELF = stoneShelf("sightshelf", MapColor.COLOR_BLACK, 1.5F, Particles.ENCHANT_FIRE);

        public static final Holder<Block> SIGHTSHELF_T2 = stoneShelf("sightshelf_t2", MapColor.COLOR_BLACK, 1.5F, Particles.ENCHANT_FIRE);

        public static final Holder<Block> SOUL_TOUCHED_DEEPSHELF = stoneShelf("soul_touched_deepshelf", MapColor.COLOR_BLACK, 2.5F, Particles.ENCHANT_SCULK);

        public static final Holder<Block> SOUL_TOUCHED_SCULKSHELF = sculkShelf("soul_touched_sculkshelf");

        public static final Holder<Block> STONESHELF = stoneShelf("stoneshelf", MapColor.STONE, 1.75F, () -> ParticleTypes.ENCHANT);

        public static final Holder<Block> TREASURE_SHELF = R.block("treasure_shelf", TreasureShelfBlock::new,
            p -> p.mapColor(MapColor.COLOR_BLACK).sound(SoundType.STONE).strength(1.75F).requiresCorrectToolForDrops());

        private static void bootstrap() {}

        private static Holder<Block> sculkShelf(String id) {
            return R.block(id, () -> new SculkShelfBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).sound(SoundType.STONE).randomTicks().requiresCorrectToolForDrops().strength(3.5F), Particles.ENCHANT_SCULK));
        }

        private static Holder<Block> stoneShelf(String id, MapColor color, float strength, Supplier<? extends ParticleOptions> particle) {
            return R.block(id, () -> new TypedShelfBlock(Block.Properties.of().requiresCorrectToolForDrops().sound(SoundType.STONE).mapColor(color).strength(strength), particle));
        }

        private static Holder<Block> woodShelf(String id, MapColor color, float strength, Supplier<? extends ParticleOptions> particle) {
            return R.block(id, () -> new TypedShelfBlock(Block.Properties.of().sound(SoundType.WOOD).mapColor(color).strength(strength), particle));
        }

    }

    public static class Colors {
        private static int[] _LIGHT_BLUE_FLASH = { 0x00b3ff, 0x00b3ff, 0x00b3ff, 0x00b3ff, 0x00b3ff, 0x00b3ff, 0x00b3ff, 0x00b3ff, 0x00b3ff,
            0x00b3ff, 0x00b3ff, 0x00b3ff, 0x00b3ff, 0x00b3ff, 0x00b3ff, 0x00b3ff, 0x00b3ff, 0x00b3ff,
            0x00b3ff, 0x00b3ff, 0x00b3ff, 0x00b3ff, 0x00b3ff, 0x00b3ff, 0x00b3ff, 0x00b3ff, 0x00b3ff,
            0x00b3ff, 0x00b3ff, 0x00b3ff, 0x00b3ff, 0x00b3ff, 0x00b3ff, 0x00b3ff, 0x00b3ff, 0x0bb5ff,
            0x17b8ff, 0x22bbff, 0x2dbdff, 0x39c0ff, 0x44c3ff, 0x4fc6ff, 0x5bc9ff, 0x66ccff };

        public static GradientColor LIGHT_BLUE_FLASH = new GradientColor(MiscUtil.doubleUpGradient(_LIGHT_BLUE_FLASH), "light_blue_flash");
    }

    public static class Components {

        /**
         * Keeps a copy of the original {@link ChargedProjectiles} when crescendo is present, so they can be re-charged after firing.
         */
        public static final DataComponentType<ChargedProjectiles> CRESCENDO_PROJECTILES = R.component("crescendo_projectiles",
            b -> b.persistent(ChargedProjectiles.CODEC).networkSynchronized(ChargedProjectiles.STREAM_CODEC).cacheEncoding());

        /**
         * Used when Crescendo of Bolts is active to track the number of remaining bonus shots.
         */
        public static final DataComponentType<Integer> CRESCENDO_SHOTS = R.component("crescendo_shots", b -> b.persistent(Codec.intRange(1, 1024)).networkSynchronized(ByteBufCodecs.VAR_INT));

        private static void bootstrap() {}
    }

    public static final class DamageTypes {
        public static final ResourceKey<DamageType> CORRUPTED = ResourceKey.create(Registries.DAMAGE_TYPE, ApothicEnchanting.loc("corrupted"));
    }

    public static class EnchantEffects {

        /**
         * Component used by Berserker's Fury. Allows configuring the mob effects, health cost, and cooldown.
         */
        public static final DataComponentType<BerserkingComponent> BERSERKING = R.enchantmentEffect("berserking", b -> b.persistent(BerserkingComponent.CODEC));

        /**
         * The bonemeal crops effect, when present on an item, will bonemeal any right-clicked crops at a durability cost equal to the level-based value.
         */
        public static final DataComponentType<LevelBasedValue> BONEMEAL_CROPS = R.enchantmentEffect("bonemeal_crops", b -> b.persistent(LevelBasedValue.CODEC));

        /**
         * The chainsaw effect causes whole trees to break when a log is broken.
         */
        public static final DataComponentType<Unit> CHAINSAW = R.enchantmentEffect("chainsaw", b -> b.persistent(Unit.CODEC));

        /**
         * The chromatic effect causes shears to randomize the color of all sheared wool.
         */
        public static final DataComponentType<Unit> CHROMATIC = R.enchantmentEffect("chromatic", b -> b.persistent(Unit.CODEC));

        /**
         * The crescendo effect causes the crossbow to have an additional number of shots per consumed ammunition, without having to reload between them.
         */
        public static final DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> CRESCENDO = R.enchantmentEffect("crescendo",
            b -> b.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_ITEM).listOf()));

        /**
         * The drops to xp effect, if present on a weapon, causes all items dropped by slain mobs to be converted to experience.
         * The amount of experience, per item, is equal to the value of the component.
         */
        public static final DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> DROPS_TO_XP = R.enchantmentEffect("drops_to_xp",
            b -> b.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_ITEM).listOf()));

        /**
         * The boon component allows a chance at dropping a random item from a tag when any block from a target tag is broken.
         */
        public static final DataComponentType<BoonComponent> EARTHS_BOON = R.enchantmentEffect("earths_boon", b -> b.persistent(BoonComponent.CODEC));

        /**
         * The exploitation effect doubles all dropped wool, but deals two damage to sheared sheep.
         */
        public static final DataComponentType<Unit> EXPLOITATION = R.enchantmentEffect("exploitation", b -> b.persistent(Unit.CODEC));

        /**
         * The extra loot roll effect, if present on a weapon, gives a chance to roll and drop an additional copy of the slain mob's loot.
         */
        public static final DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> EXTRA_LOOT_ROLL = R.enchantmentEffect("extra_loot_roll",
            b -> b.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_DAMAGE).listOf()));

        /**
         * The growth serum effect has a chance (equal to the value) to make a sheared sheep immediately regrow its wool.
         */
        public static final DataComponentType<Float> GROWTH_SERUM = R.enchantmentEffect("growth_serum", b -> b.persistent(Codec.floatRange(0.001F, 1)));

        /**
         * The miner's fervor effect is a version of efficiency that scales faster but has a cap on the max bonus.
         * The value of the component is the scaling, the cap is hardcoded.
         * <p>
         * Since this has to be evaluated on the client, we can't use {@link ConditionalEffect}.
         */
        public static final DataComponentType<LevelBasedValue> MINERS_FERVOR = R.enchantmentEffect("miners_fervor", b -> b.persistent(LevelBasedValue.CODEC));

        /**
         * The reflective effect, if present on a blocking shield, gives a chance to inflict part of the blocked damage to the attacker.
         */
        public static final DataComponentType<ReflectiveComponent> REFLECTIVE = R.enchantmentEffect("reflective", b -> b.persistent(ReflectiveComponent.CODEC));

        /**
         * The repair with hp effect causes incoming healing to be converted into durability. The final value of the effect is the amount of durability restored per
         * full point of hp.
         * <p>
         * If the amount of durability per hp is more than one, fractional units of hp may be
         * consumed to restore integer durability values (i.e. at 4 / hp, 0.25 hp can repair 1 durability).
         */
        public static final DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> REPAIR_WITH_HP = R.enchantmentEffect("repair_with_hp",
            b -> b.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_ITEM).listOf()));

        /**
         * The stable footing effect causes the break speed penalty for flying to be ignored.
         */
        public static final DataComponentType<Unit> STABLE_FOOTING = R.enchantmentEffect("stable_footing", b -> b.persistent(Unit.CODEC));

        /**
         * The tempting effect causes animals to follow the item that has the effect.
         */
        public static final DataComponentType<Unit> TEMPTING = R.enchantmentEffect("tempting", b -> b.persistent(Unit.CODEC));

        private static void bootstrap() {}
    }

    public static final class Enchantments {

        public static final ResourceKey<Enchantment> BERSERKERS_FURY = key("berserkers_fury");
        public static final ResourceKey<Enchantment> BOON_OF_THE_EARTH = key("boon_of_the_earth");
        public static final ResourceKey<Enchantment> CHAINSAW = key("chainsaw");
        public static final ResourceKey<Enchantment> CHROMATIC = key("chromatic");
        public static final ResourceKey<Enchantment> CRESCENDO_OF_BOLTS = key("crescendo_of_bolts");
        public static final ResourceKey<Enchantment> ENDLESS_QUIVER = key("endless_quiver");
        public static final ResourceKey<Enchantment> GROWTH_SERUM = key("growth_serum");
        public static final ResourceKey<Enchantment> ICY_THORNS = key("icy_thorns");
        public static final ResourceKey<Enchantment> INFUSION = key("infusion");
        public static final ResourceKey<Enchantment> KNOWLEDGE_OF_THE_AGES = key("knowledge_of_the_ages");
        public static final ResourceKey<Enchantment> LIFE_MENDING = key("life_mending");
        public static final ResourceKey<Enchantment> MINERS_FERVOR = key("miners_fervor");
        public static final ResourceKey<Enchantment> NATURES_BLESSING = key("natures_blessing");
        public static final ResourceKey<Enchantment> REBOUNDING = key("rebounding");
        public static final ResourceKey<Enchantment> REFLECTIVE_DEFENSES = key("reflective_defenses");
        public static final ResourceKey<Enchantment> SCAVENGER = key("scavenger");
        public static final ResourceKey<Enchantment> SHIELD_BASH = key("shield_bash");
        public static final ResourceKey<Enchantment> STABLE_FOOTING = key("stable_footing");
        public static final ResourceKey<Enchantment> TEMPTING = key("tempting");
        public static final ResourceKey<Enchantment> WORKER_EXPLOITATION = key("worker_exploitation");

        private static ResourceKey<Enchantment> key(String name) {
            return ResourceKey.create(Registries.ENCHANTMENT, ApothicEnchanting.loc(name));
        }
    }

    public static class Items extends net.minecraft.world.item.Items {

        public static final Holder<Item> BEESHELF = R.item("beeshelf", () -> new BlockItem(Ench.Blocks.BEESHELF.value(), new Item.Properties()));

        public static final Holder<Item> BLAZING_HELLSHELF = R.item("blazing_hellshelf", () -> new BlockItem(Ench.Blocks.BLAZING_HELLSHELF.value(), new Item.Properties()));

        public static final Holder<Item> BOOTS_TOME = R.item("boots_tome", () -> new TomeItem(DIAMOND_BOOTS));

        public static final Holder<Item> BOW_TOME = R.item("bow_tome", () -> new TomeItem(BOW));

        public static final Holder<Item> CHESTPLATE_TOME = R.item("chestplate_tome", () -> new TomeItem(Items.DIAMOND_CHESTPLATE));

        public static final Holder<Item> CRYSTAL_SEASHELF = R.blockItem("crystal_seashelf", Ench.Blocks.CRYSTAL_SEASHELF);

        public static final Holder<Item> DEEPSHELF = R.blockItem("deepshelf", Ench.Blocks.DEEPSHELF, GlowyBlockItem::new, UnaryOperator.identity());

        public static final Holder<Item> DORMANT_DEEPSHELF = R.blockItem("dormant_deepshelf", Ench.Blocks.DORMANT_DEEPSHELF);

        public static final Holder<Item> DRACONIC_ENDSHELF = R.blockItem("draconic_endshelf", Ench.Blocks.DRACONIC_ENDSHELF);

        public static final Holder<Item> ECHOING_DEEPSHELF = R.blockItem("echoing_deepshelf", Ench.Blocks.ECHOING_DEEPSHELF);

        public static final Holder<Item> ECHOING_SCULKSHELF = R.blockItem("echoing_sculkshelf", Ench.Blocks.ECHOING_SCULKSHELF);

        public static final Holder<Item> ENDER_LIBRARY = R.blockItem("ender_library", Ench.Blocks.ENDER_LIBRARY);

        public static final Holder<Item> ENDSHELF = R.blockItem("endshelf", Ench.Blocks.ENDSHELF);

        public static final Holder<Item> EXTRACTION_TOME = R.item("extraction_tome", ExtractionTomeItem::new, p -> p.rarity(Rarity.EPIC));

        public static final Holder<Item> FILTERING_SHELF = R.blockItem("filtering_shelf", Ench.Blocks.FILTERING_SHELF, p -> p.rarity(Rarity.UNCOMMON));

        public static final Holder<Item> FISHING_TOME = R.item("fishing_tome", () -> new TomeItem(Items.FISHING_ROD));

        public static final Holder<Item> GEODE_SHELF = R.blockItem("geode_shelf", Ench.Blocks.GEODE_SHELF, p -> p.rarity(Rarity.UNCOMMON));

        public static final Holder<Item> GLOWING_HELLSHELF = R.blockItem("glowing_hellshelf", Ench.Blocks.GLOWING_HELLSHELF);

        public static final Holder<Item> HEART_SEASHELF = R.blockItem("heart_seashelf", Ench.Blocks.HEART_SEASHELF);

        public static final Holder<Item> HELLSHELF = R.blockItem("hellshelf", Ench.Blocks.HELLSHELF);

        public static final Holder<Item> HELMET_TOME = R.item("helmet_tome", () -> new TomeItem(Items.DIAMOND_HELMET));

        public static final Holder<Item> IMPROVED_SCRAP_TOME = R.item("improved_scrap_tome", ImprovedScrappingTomeItem::new, p -> p.rarity(Rarity.RARE));

        public static final Holder<Item> INERT_TRIDENT = R.item("inert_trident", () -> new Item(new Item.Properties().stacksTo(1)));

        public static final Holder<Item> INFUSED_BREATH = R.item("infused_breath", () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)));

        public static final Holder<Item> INFUSED_HELLSHELF = R.blockItem("infused_hellshelf", Ench.Blocks.INFUSED_HELLSHELF, GlowyBlockItem::new, UnaryOperator.identity());

        public static final Holder<Item> INFUSED_SEASHELF = R.blockItem("infused_seashelf", Ench.Blocks.INFUSED_SEASHELF, GlowyBlockItem::new, UnaryOperator.identity());

        public static final Holder<Item> LEGGINGS_TOME = R.item("leggings_tome", () -> new TomeItem(net.minecraft.world.item.Items.DIAMOND_LEGGINGS));

        public static final Holder<Item> LIBRARY = R.blockItem("library", Ench.Blocks.LIBRARY);

        public static final Holder<Item> MELONSHELF = R.blockItem("melonshelf", Ench.Blocks.MELONSHELF);

        public static final Holder<Item> OTHER_TOME = R.item("other_tome", () -> new TomeItem(net.minecraft.world.item.Items.AIR));

        public static final Holder<Item> PEARL_ENDSHELF = R.blockItem("pearl_endshelf", Ench.Blocks.PEARL_ENDSHELF);

        public static final Holder<Item> PICKAXE_TOME = R.item("pickaxe_tome", () -> new TomeItem(net.minecraft.world.item.Items.DIAMOND_PICKAXE));

        public static final Holder<Item> PRISMATIC_WEB = R.item("prismatic_web", () -> new Item(new Item.Properties()));

        public static final Holder<Item> SCRAP_TOME = R.item("scrap_tome", () -> new ScrappingTomeItem(new Item.Properties().rarity(Rarity.UNCOMMON)));

        public static final Holder<Item> SEASHELF = R.blockItem("seashelf", Ench.Blocks.SEASHELF);

        public static final Holder<Item> SIGHTSHELF = R.blockItem("sightshelf", Ench.Blocks.SIGHTSHELF, p -> p.rarity(Rarity.UNCOMMON));

        public static final Holder<Item> SIGHTSHELF_T2 = R.blockItem("sightshelf_t2", Ench.Blocks.SIGHTSHELF_T2, p -> p.rarity(Rarity.UNCOMMON));

        public static final Holder<Item> SOUL_TOUCHED_DEEPSHELF = R.blockItem("soul_touched_deepshelf", Ench.Blocks.SOUL_TOUCHED_DEEPSHELF);

        public static final Holder<Item> SOUL_TOUCHED_SCULKSHELF = R.blockItem("soul_touched_sculkshelf", Ench.Blocks.SOUL_TOUCHED_SCULKSHELF);

        public static final Holder<Item> STONESHELF = R.blockItem("stoneshelf", Ench.Blocks.STONESHELF);

        public static final Holder<Item> TREASURE_SHELF = R.blockItem("treasure_shelf", Ench.Blocks.TREASURE_SHELF, p -> p.rarity(Rarity.UNCOMMON));

        public static final Holder<Item> WARDEN_TENDRIL = R.item("warden_tendril", () -> new Item(new Item.Properties()));

        public static final Holder<Item> WEAPON_TOME = R.item("weapon_tome", () -> new TomeItem(net.minecraft.world.item.Items.DIAMOND_SWORD));

        private static void bootstrap() {}

    }

    public static class Menus {

        public static final MenuType<ApothEnchantmentMenu> ENCHANTING_TABLE = R.menu("enchanting_table", ApothEnchantmentMenu::new);

        public static final MenuType<EnchLibraryContainer> LIBRARY = R.menuWithPos("library", EnchLibraryContainer::new);

        private static void bootstrap() {}
    }

    public static class Particles {
        public static final Supplier<SimpleParticleType> ENCHANT_END = R.simpleParticle("enchant_end", false);
        public static final Supplier<SimpleParticleType> ENCHANT_FIRE = R.simpleParticle("enchant_fire", false);
        public static final Supplier<SimpleParticleType> ENCHANT_SCULK = R.simpleParticle("enchant_sculk", false);
        public static final Supplier<SimpleParticleType> ENCHANT_WATER = R.simpleParticle("enchant_water", false);

        private static void bootstrap() {}
    }

    public static class RecipeTypes {
        public static final RecipeType<InfusionRecipe> INFUSION = R.recipe("infusion");

        private static void bootstrap() {}
    }

    public static class Tabs {

        public static final Holder<CreativeModeTab> ENCH = R.creativeTab("ench", b -> b.title(TooltipUtil.lang("creative_tab", "all")).icon(() -> Items.HELLSHELF.value().getDefaultInstance()));

        private static void bootstrap() {}
    }

    public static class Triggers {

        public static final EnchantedTrigger ENCHANTED = R.criteriaTrigger("enchanted", new EnchantedTrigger());

        private static void bootstrap() {}
    }

    public static final class Tags {
        // TODO: Replace this with a loot table.
        public static final TagKey<Item> SPEARFISHING_DROPS = ItemTags.create(ApothicEnchanting.loc("spearfishing_drops"));

        /**
         * Any items in this tag will not be converted to experience when the Drops to XP effect (KoTA) is active.
         */
        public static final TagKey<Item> CANNOT_BE_CONVERTED_TO_XP = ItemTags.create(ApothicEnchanting.loc("cannot_be_converted_to_xp"));
    }

    public static class Tiles {

        public static final Supplier<BlockEntityType<EnderLibraryTile>> ENDER_LIBRARY = R.blockEntity("ender_library", EnderLibraryTile::new, () -> ImmutableSet.of(Blocks.ENDER_LIBRARY.value()));

        public static final Supplier<BlockEntityType<FilteringShelfTile>> FILTERING_SHELF = R.blockEntity("filtering_shelf", FilteringShelfTile::new, () -> ImmutableSet.of(Blocks.FILTERING_SHELF.value()));

        public static final Supplier<BlockEntityType<BasicLibraryTile>> LIBRARY = R.blockEntity("library", BasicLibraryTile::new, () -> ImmutableSet.of(Blocks.LIBRARY.value()));

        private static void bootstrap() {}
    }

    public static class LootTables {
        public static final ResourceKey<LootTable> BOON_STONE_DROPS = ResourceKey.create(Registries.LOOT_TABLE, ApothicEnchanting.loc("boon_stone_drops"));
        public static final ResourceKey<LootTable> BOON_DEEPSLATE_DROPS = ResourceKey.create(Registries.LOOT_TABLE, ApothicEnchanting.loc("boon_deepslate_drops"));
        public static final ResourceKey<LootTable> BOON_NETHER_DROPS = ResourceKey.create(Registries.LOOT_TABLE, ApothicEnchanting.loc("boon_netherrack_drops"));
    }

    public static void bootstrap(IEventBus bus) {
        Blocks.bootstrap();
        Items.bootstrap();
        EnchantEffects.bootstrap();
        Tabs.bootstrap();
        Tiles.bootstrap();
        Particles.bootstrap();
        Menus.bootstrap();
        RecipeTypes.bootstrap();
        Components.bootstrap();
        Triggers.bootstrap();
        bus.register(R);
    }

}
