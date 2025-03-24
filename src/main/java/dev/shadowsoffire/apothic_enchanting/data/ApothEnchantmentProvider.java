package dev.shadowsoffire.apothic_enchanting.data;

import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

import dev.shadowsoffire.apothic_enchanting.Ench;
import dev.shadowsoffire.apothic_enchanting.enchantments.components.BerserkingComponent;
import dev.shadowsoffire.apothic_enchanting.enchantments.components.BerserkingComponent.VariableMobEffect;
import dev.shadowsoffire.apothic_enchanting.enchantments.components.BoonComponent;
import dev.shadowsoffire.apothic_enchanting.enchantments.components.ReflectiveComponent;
import dev.shadowsoffire.apothic_enchanting.enchantments.entity_effects.ReboundingEffect;
import dev.shadowsoffire.apothic_enchanting.enchantments.values.ExponentialLevelBasedValue;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.advancements.critereon.DamageSourcePredicate;
import net.minecraft.advancements.critereon.TagPredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.ConditionalEffect;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantment.EnchantmentDefinition;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentTarget;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.AddValue;
import net.minecraft.world.item.enchantment.effects.ApplyMobEffect;
import net.minecraft.world.item.enchantment.effects.DamageItem;
import net.minecraft.world.item.enchantment.effects.SetValue;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.predicates.DamageSourceCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.EnchantmentLevelProvider;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.registries.holdersets.OrHolderSet;

public class ApothEnchantmentProvider {

    public static void bootstrap(BootstrapContext<Enchantment> context) {
        HolderGetter<Enchantment> enchantments = context.lookup(Registries.ENCHANTMENT);
        HolderGetter<Item> items = context.lookup(Registries.ITEM);
        HolderGetter<Block> blocks = context.lookup(Registries.BLOCK);

        register(context, Ench.Enchantments.BERSERKERS_FURY,
            enchantment(
                Enchantment.definition(
                    items.getOrThrow(ItemTags.CHEST_ARMOR_ENCHANTABLE),
                    1, // weight
                    3, // max level
                    Enchantment.dynamicCost(50, 40),
                    Enchantment.constantCost(200),
                    10, // anvil cost
                    EquipmentSlotGroup.CHEST))
                .withCustomName(c -> c.withStyle(ChatFormatting.DARK_RED))
                .withSpecialEffect(Ench.EnchantEffects.BERSERKING,
                    new BerserkingComponent(
                        List.of(noCondition(new AddValue(new ExponentialLevelBasedValue(2.5F)))),
                        List.of(
                            noCondition(simpleMobEffect(MobEffects.DAMAGE_RESISTANCE, 500)),
                            noCondition(simpleMobEffect(MobEffects.DAMAGE_BOOST, 500)),
                            noCondition(simpleMobEffect(MobEffects.MOVEMENT_SPEED, 500))),
                        List.of(noCondition(new AddValue(LevelBasedValue.constant(900)))))));

        register(context, Ench.Enchantments.CHAINSAW,
            enchantment(
                Enchantment.definition(
                    items.getOrThrow(ItemTags.AXES),
                    1, // weight
                    1, // max level
                    Enchantment.constantCost(55),
                    Enchantment.constantCost(200),
                    10, // anvil cost
                    EquipmentSlotGroup.MAINHAND))
                .withCustomName(c -> c.withStyle(ChatFormatting.DARK_GREEN))
                .withEffect(Ench.EnchantEffects.CHAINSAW));

        register(context, Ench.Enchantments.CHROMATIC,
            Enchantment.enchantment(
                Enchantment.definition(
                    items.getOrThrow(Tags.Items.TOOLS_SHEAR),
                    5, // weight
                    1, // max level
                    Enchantment.constantCost(25),
                    Enchantment.constantCost(200),
                    2, // anvil cost
                    EquipmentSlotGroup.HAND))
                .withEffect(Ench.EnchantEffects.CHROMATIC));

        register(context, Ench.Enchantments.WORKER_EXPLOITATION,
            enchantment(
                Enchantment.definition(
                    items.getOrThrow(Tags.Items.TOOLS_SHEAR),
                    2, // weight
                    1, // max level
                    Enchantment.constantCost(30),
                    Enchantment.constantCost(200),
                    5, // anvil cost
                    EquipmentSlotGroup.HAND))
                .withCustomName(c -> c.withStyle(ChatFormatting.DARK_PURPLE))
                .withEffect(Ench.EnchantEffects.EXPLOITATION));

        register(context, Ench.Enchantments.GROWTH_SERUM,
            enchantment(
                Enchantment.definition(
                    items.getOrThrow(Tags.Items.TOOLS_SHEAR),
                    1, // weight
                    1, // max level
                    Enchantment.constantCost(40),
                    Enchantment.constantCost(200),
                    10, // anvil cost
                    EquipmentSlotGroup.HAND))
                .withCustomName(c -> c.withStyle(ChatFormatting.DARK_GREEN))
                .withSpecialEffect(Ench.EnchantEffects.GROWTH_SERUM, 0.5F));

        register(context, Ench.Enchantments.TEMPTING,
            Enchantment.enchantment(
                Enchantment.definition(
                    items.getOrThrow(ItemTags.HOES),
                    5, // weight
                    1, // max level
                    Enchantment.constantCost(0),
                    Enchantment.constantCost(200),
                    1, // anvil cost
                    EquipmentSlotGroup.HAND))
                .withEffect(Ench.EnchantEffects.TEMPTING));

        register(context, Ench.Enchantments.STABLE_FOOTING,
            Enchantment.enchantment(
                Enchantment.definition(
                    items.getOrThrow(ItemTags.FOOT_ARMOR_ENCHANTABLE),
                    2, // weight
                    1, // max level
                    Enchantment.constantCost(40),
                    Enchantment.constantCost(200),
                    1, // anvil cost
                    EquipmentSlotGroup.FEET))
                .withEffect(Ench.EnchantEffects.STABLE_FOOTING));

        register(context, Ench.Enchantments.SHIELD_BASH,
            Enchantment.enchantment(
                Enchantment.definition(
                    items.getOrThrow(Tags.Items.TOOLS_SHIELD),
                    2, // weight
                    4, // max level
                    Enchantment.dynamicCost(1, 17),
                    Enchantment.constantCost(200),
                    1, // anvil cost
                    EquipmentSlotGroup.MAINHAND))
                .exclusiveWith(enchantments.getOrThrow(EnchantmentTags.DAMAGE_EXCLUSIVE))
                .withEffect(EnchantmentEffectComponents.DAMAGE, new AddValue(LevelBasedValue.perLevel(3.5F)))
                .withEffect(EnchantmentEffectComponents.POST_ATTACK,
                    EnchantmentTarget.ATTACKER,
                    EnchantmentTarget.ATTACKER,
                    new DamageItem(new LevelBasedValue.Clamped(LevelBasedValue.perLevel(20, -2), 1, 1024))));

        register(context, Ench.Enchantments.BOON_OF_THE_EARTH,
            enchantment(
                Enchantment.definition(
                    items.getOrThrow(ItemTags.PICKAXES),
                    1, // weight
                    5, // max level
                    Enchantment.dynamicCost(55, 25),
                    Enchantment.constantCost(200),
                    10, // anvil cost
                    EquipmentSlotGroup.MAINHAND))
                .withCustomName(c -> c.withStyle(ChatFormatting.DARK_GREEN))
                .withSpecialEffect(Ench.EnchantEffects.EARTHS_BOON,
                    new BoonComponent(
                        List.of(
                            new BoonComponent.BoonData(
                                blocks.getOrThrow(BlockTags.DEEPSLATE_ORE_REPLACEABLES),
                                Ench.LootTables.BOON_DEEPSLATE_DROPS,
                                List.of(noCondition(new AddValue(LevelBasedValue.perLevel(0.015F))))),
                            new BoonComponent.BoonData(
                                blocks.getOrThrow(BlockTags.STONE_ORE_REPLACEABLES),
                                Ench.LootTables.BOON_STONE_DROPS,
                                List.of(noCondition(new AddValue(LevelBasedValue.perLevel(0.01F))))),
                            new BoonComponent.BoonData(
                                blocks.getOrThrow(BlockTags.BASE_STONE_NETHER),
                                Ench.LootTables.BOON_NETHER_DROPS,
                                List.of(noCondition(new AddValue(LevelBasedValue.perLevel(0.01F)))))))));

        register(context, Ench.Enchantments.CRESCENDO_OF_BOLTS,
            enchantment(
                Enchantment.definition(
                    items.getOrThrow(ItemTags.CROSSBOW_ENCHANTABLE),
                    1, // weight
                    5, // max level
                    Enchantment.dynamicCost(55, 30),
                    Enchantment.constantCost(200),
                    10, // anvil cost
                    EquipmentSlotGroup.HAND))
                .withCustomName(c -> c.withStyle(ChatFormatting.DARK_GREEN))
                .withEffect(Ench.EnchantEffects.CRESCENDO, new AddValue(LevelBasedValue.perLevel(1))));

        register(context, Ench.Enchantments.ENDLESS_QUIVER,
            enchantment(
                Enchantment.definition(
                    items.getOrThrow(ItemTags.BOW_ENCHANTABLE),
                    1, // weight
                    1, // max level
                    Enchantment.constantCost(60),
                    Enchantment.constantCost(200),
                    10, // anvil cost
                    EquipmentSlotGroup.HAND))
                .withCustomName(c -> c.withStyle(ChatFormatting.DARK_GREEN))
                .exclusiveWith(HolderSet.direct(enchantments.getOrThrow(Enchantments.INFINITY)))
                .withEffect(EnchantmentEffectComponents.AMMO_USE, new SetValue(LevelBasedValue.constant(0))));

        register(context, Ench.Enchantments.LIFE_MENDING,
            enchantment(
                Enchantment.definition(
                    items.getOrThrow(ItemTags.DURABILITY_ENCHANTABLE),
                    1, // weight
                    3, // max level
                    Enchantment.dynamicCost(65, 35),
                    Enchantment.constantCost(200),
                    10, // anvil cost
                    EquipmentSlotGroup.ANY))
                .withCustomName(c -> c.withStyle(ChatFormatting.DARK_RED))
                .exclusiveWith(HolderSet.direct(enchantments.getOrThrow(Enchantments.MENDING)))
                .withEffect(Ench.EnchantEffects.REPAIR_WITH_HP, new AddValue(new ExponentialLevelBasedValue(2, LevelBasedValue.perLevel(0, 1)))));

        register(context, Ench.Enchantments.MINERS_FERVOR,
            enchantment(
                Enchantment.definition(
                    items.getOrThrow(ItemTags.MINING_ENCHANTABLE),
                    2, // weight
                    5, // max level
                    Enchantment.dynamicCost(45, 30),
                    Enchantment.constantCost(200),
                    10, // anvil cost
                    EquipmentSlotGroup.MAINHAND))
                .withCustomName(c -> c.withStyle(ChatFormatting.DARK_PURPLE))
                .exclusiveWith(HolderSet.direct(enchantments.getOrThrow(Enchantments.EFFICIENCY)))
                .withSpecialEffect(Ench.EnchantEffects.MINERS_FERVOR, LevelBasedValue.perLevel(12, 4.5F)));

        register(context, Ench.Enchantments.KNOWLEDGE_OF_THE_AGES,
            enchantment(
                Enchantment.definition(
                    items.getOrThrow(ItemTags.SHARP_WEAPON_ENCHANTABLE),
                    items.getOrThrow(ItemTags.SWORD_ENCHANTABLE),
                    2, // weight
                    3, // max level
                    Enchantment.dynamicCost(55, 45),
                    Enchantment.constantCost(200),
                    10, // anvil cost
                    EquipmentSlotGroup.MAINHAND))
                .withCustomName(c -> c.withStyle(ChatFormatting.DARK_GREEN))
                .withEffect(Ench.EnchantEffects.DROPS_TO_XP, new AddValue(LevelBasedValue.perLevel(25))));

        register(context, Ench.Enchantments.SCAVENGER,
            enchantment(
                Enchantment.definition(
                    items.getOrThrow(ItemTags.SHARP_WEAPON_ENCHANTABLE),
                    items.getOrThrow(ItemTags.SWORD_ENCHANTABLE),
                    1, // weight
                    3, // max level
                    Enchantment.dynamicCost(55, 50),
                    Enchantment.constantCost(200),
                    10, // anvil cost
                    EquipmentSlotGroup.MAINHAND))
                .withCustomName(c -> c.withStyle(ChatFormatting.DARK_GREEN))
                .withEffect(Ench.EnchantEffects.EXTRA_LOOT_ROLL, new AddValue(LevelBasedValue.perLevel(0.025F))));

        register(context, Ench.Enchantments.REFLECTIVE_DEFENSES,
            Enchantment.enchantment(
                Enchantment.definition(
                    items.getOrThrow(Tags.Items.TOOLS_SHIELD),
                    2, // weight
                    5, // max level
                    Enchantment.dynamicCost(0, 18),
                    Enchantment.constantCost(200),
                    5, // anvil cost
                    EquipmentSlotGroup.HAND))
                .withSpecialEffect(Ench.EnchantEffects.REFLECTIVE, new ReflectiveComponent(LevelBasedValue.perLevel(0.15F, 0.10F), LevelBasedValue.perLevel(0.15F))));

        register(context, Ench.Enchantments.ICY_THORNS,
            Enchantment.enchantment(
                Enchantment.definition(
                    items.getOrThrow(ItemTags.CHEST_ARMOR_ENCHANTABLE),
                    2, // weight
                    3, // max level
                    Enchantment.dynamicCost(35, 20),
                    Enchantment.constantCost(200),
                    5, // anvil cost
                    EquipmentSlotGroup.CHEST))
                .exclusiveWith(HolderSet.direct(enchantments.getOrThrow(Enchantments.THORNS)))
                .withEffect(
                    EnchantmentEffectComponents.POST_ATTACK,
                    EnchantmentTarget.VICTIM,
                    EnchantmentTarget.ATTACKER,
                    new ApplyMobEffect(
                        HolderSet.direct(MobEffects.MOVEMENT_SLOWDOWN),
                        LevelBasedValue.constant(10), LevelBasedValue.constant(20),
                        LevelBasedValue.constant(0), new LevelBasedValue.Clamped(LevelBasedValue.perLevel(2), 1, 4)),
                    LootItemRandomChanceCondition.randomChance(EnchantmentLevelProvider.forEnchantmentLevel(LevelBasedValue.perLevel(0.5F)))));

        register(context, Ench.Enchantments.INFUSION,
            Enchantment.enchantment(
                Enchantment.definition(
                    HolderSet.empty(),
                    1, // weight
                    1, // max level
                    Enchantment.constantCost(0),
                    Enchantment.constantCost(0),
                    0, // anvil cost
                    EquipmentSlotGroup.ANY)));

        register(context, Ench.Enchantments.REBOUNDING,
            Enchantment.enchantment(
                Enchantment.definition(
                    new OrHolderSet<>(List.of(items.getOrThrow(ItemTags.CHEST_ARMOR_ENCHANTABLE), items.getOrThrow(ItemTags.LEG_ARMOR_ENCHANTABLE))),
                    2, // weight
                    3, // max level
                    Enchantment.dynamicCost(22, 18),
                    Enchantment.constantCost(200),
                    5, // anvil cost
                    EquipmentSlotGroup.ARMOR))
                .withEffect(
                    EnchantmentEffectComponents.POST_ATTACK,
                    EnchantmentTarget.VICTIM,
                    EnchantmentTarget.ATTACKER,
                    new ReboundingEffect(LevelBasedValue.constant(4F), LevelBasedValue.perLevel(2), LevelBasedValue.perLevel(3))));

        register(context, Ench.Enchantments.NATURES_BLESSING,
            Enchantment.enchantment(
                Enchantment.definition(
                    items.getOrThrow(ItemTags.HOES),
                    2, // weight
                    3, // max level
                    Enchantment.dynamicCost(15, 10),
                    Enchantment.constantCost(200),
                    3, // anvil cost
                    EquipmentSlotGroup.HAND))
                .withSpecialEffect(Ench.EnchantEffects.BONEMEAL_CROPS, LevelBasedValue.perLevel(5, -1)));

        // Vanilla Overrides

        register(context, Enchantments.SHARPNESS,
            Enchantment.enchantment(
                Enchantment.definition(
                    items.getOrThrow(ItemTags.SHARP_WEAPON_ENCHANTABLE),
                    items.getOrThrow(ItemTags.SWORD_ENCHANTABLE),
                    10,
                    5,
                    Enchantment.dynamicCost(1, 11),
                    Enchantment.dynamicCost(21, 11),
                    1,
                    EquipmentSlotGroup.MAINHAND))
                .withEffect(EnchantmentEffectComponents.DAMAGE, new AddValue(LevelBasedValue.perLevel(1.0F, 0.5F))));

        register(context, Enchantments.PROTECTION,
            Enchantment.enchantment(
                Enchantment.definition(
                    items.getOrThrow(ItemTags.ARMOR_ENCHANTABLE),
                    10,
                    4,
                    Enchantment.dynamicCost(1, 11),
                    Enchantment.dynamicCost(12, 11),
                    1,
                    EquipmentSlotGroup.ARMOR))
                .withEffect(
                    EnchantmentEffectComponents.DAMAGE_PROTECTION,
                    new AddValue(LevelBasedValue.perLevel(1.0F)),
                    DamageSourceCondition.hasDamageSource(
                        DamageSourcePredicate.Builder.damageType().tag(TagPredicate.isNot(DamageTypeTags.BYPASSES_INVULNERABILITY)))));
    }

    private static void register(BootstrapContext<Enchantment> context, ResourceKey<Enchantment> key, Enchantment.Builder builder) {
        context.register(key, builder.build(key.location()));
    }

    private static <T> ConditionalEffect<T> noCondition(T obj) {
        return new ConditionalEffect<>(obj, Optional.empty());
    }

    private static VariableMobEffect simpleMobEffect(Holder<MobEffect> effect, int duration) {
        return new VariableMobEffect(effect,
            List.of(new AddValue(LevelBasedValue.constant(duration))),
            List.of(new AddValue(LevelBasedValue.perLevel(0, 1))),
            false, true, Optional.empty());
    }

    private static NamedBuilder enchantment(Enchantment.EnchantmentDefinition definition) {
        return new NamedBuilder(definition);
    }

    private static class NamedBuilder extends Enchantment.Builder {

        private UnaryOperator<MutableComponent> op = UnaryOperator.identity();

        public NamedBuilder(EnchantmentDefinition definition) {
            super(definition);
        }

        public Enchantment.Builder withCustomName(UnaryOperator<MutableComponent> op) {
            this.op = op;
            return this;
        }

        @Override
        public Enchantment build(ResourceLocation location) {
            return new Enchantment(
                this.op.apply(Component.translatable(Util.makeDescriptionId("enchantment", location))), this.definition, this.exclusiveSet, this.effectMapBuilder.build());
        }

    }
}
