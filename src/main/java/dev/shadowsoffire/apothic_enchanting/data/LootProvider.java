package dev.shadowsoffire.apothic_enchanting.data;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import dev.shadowsoffire.apothic_enchanting.ApothicEnchanting;
import dev.shadowsoffire.apothic_enchanting.Ench;
import dev.shadowsoffire.placebo.loot.StackLootEntry;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds.Doubles;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTable.Builder;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LocationCheck;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

public class LootProvider extends LootTableProvider {

    private LootProvider(PackOutput output, Set<ResourceKey<LootTable>> requiredTables, List<SubProviderEntry> subProviders, CompletableFuture<Provider> registries) {
        super(output, requiredTables, subProviders, registries);
    }

    public static LootProvider create(PackOutput output, CompletableFuture<Provider> registries) {
        return new LootProvider(
            output,
            Set.of(),
            List.of(
                new LootTableProvider.SubProviderEntry(BlockLoot::new, LootContextParamSets.BLOCK),
                new LootTableProvider.SubProviderEntry(BoonLoot::new, LootContextParamSets.HIT_BLOCK)),
            registries);
    }

    public static class BlockLoot extends BlockLootSubProvider {

        public static final Set<Item> EXPLOSION_RESISTANT = Set.of();

        protected BlockLoot(Provider registries) {
            super(EXPLOSION_RESISTANT, FeatureFlags.REGISTRY.allFlags(), registries);
        }

        @Override
        protected void generate() {
            this.dropSelf(Ench.Blocks.HELLSHELF);
            this.dropSelf(Ench.Blocks.INFUSED_HELLSHELF);
            this.dropSelf(Ench.Blocks.BLAZING_HELLSHELF);
            this.dropSelf(Ench.Blocks.GLOWING_HELLSHELF);
            this.dropSelf(Ench.Blocks.SEASHELF);
            this.dropSelf(Ench.Blocks.INFUSED_SEASHELF);
            this.dropSelf(Ench.Blocks.CRYSTAL_SEASHELF);
            this.dropSelf(Ench.Blocks.HEART_SEASHELF);
            this.dropSelf(Ench.Blocks.DORMANT_DEEPSHELF);
            this.dropSelf(Ench.Blocks.DEEPSHELF);
            this.dropSelf(Ench.Blocks.ECHOING_DEEPSHELF);
            this.dropSelf(Ench.Blocks.SOUL_TOUCHED_DEEPSHELF);
            this.dropSelf(Ench.Blocks.ECHOING_SCULKSHELF);
            this.dropSelf(Ench.Blocks.SOUL_TOUCHED_SCULKSHELF);
            this.dropSelf(Ench.Blocks.ENDSHELF);
            this.dropSelf(Ench.Blocks.PEARL_ENDSHELF);
            this.dropSelf(Ench.Blocks.DRACONIC_ENDSHELF);
            this.dropSelf(Ench.Blocks.BEESHELF);
            this.dropSelf(Ench.Blocks.MELONSHELF);
            this.dropSelf(Ench.Blocks.STONESHELF);
            this.dropSelf(Ench.Blocks.LIBRARY);
            this.dropSelf(Ench.Blocks.GEODE_SHELF);
            this.dropSelf(Ench.Blocks.SIGHTSHELF);
            this.dropSelf(Ench.Blocks.SIGHTSHELF_T2);
            this.dropSelf(Ench.Blocks.ENDER_LIBRARY);
            this.dropSelf(Ench.Blocks.FILTERING_SHELF);
            this.dropSelf(Ench.Blocks.TREASURE_SHELF);
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return BuiltInRegistries.BLOCK.holders().filter(h -> h.getKey().location().getNamespace().equals(ApothicEnchanting.MODID)).map(Holder::value).toList();
        }

        protected void dropSelf(Holder<Block> block) {
            this.dropSelf(block.value());
        }

    }

    public static record BoonLoot(HolderLookup.Provider registries) implements LootTableSubProvider {

        @Override
        public void generate(BiConsumer<ResourceKey<LootTable>, Builder> output) {

            HolderGetter<Biome> biomes = registries.lookupOrThrow(Registries.BIOME);

            output.accept(Ench.LootTables.BOON_STONE_DROPS,
                LootTable.lootTable()
                    .withPool(LootPool.lootPool()
                        .setBonusRolls(UniformGenerator.between(0, 0.02F))
                        .add(item(Items.RAW_COPPER, 3, 5).setWeight(100))
                        .add(item(Items.RAW_IRON, 3, 5).setWeight(100).setQuality(1))
                        .add(item(Items.COAL, 2, 10).setWeight(100))
                        .add(item(Items.RAW_GOLD, 2, 5).setWeight(20).setQuality(2))
                        .add(item(Items.LAPIS_LAZULI, 2, 7).setWeight(20).setQuality(2))
                        .add(item(Items.DIAMOND, 1, 1).setWeight(5).setQuality(3))

                    )
                    // When in a location where emeralds would spawn, add a 15% drop chance.
                    .withPool(LootPool.lootPool()
                        .when(
                            LocationCheck.checkLocation(
                                LocationPredicate.Builder.location()
                                    .setY(Doubles.atLeast(32))
                                    .setBiomes(biomes.getOrThrow(BiomeTags.IS_MOUNTAIN))))
                        .add(item(Items.EMERALD, 1, 1).setWeight(15))
                        .add(EmptyLootItem.emptyItem().setWeight(85))));

            output.accept(Ench.LootTables.BOON_DEEPSLATE_DROPS,
                LootTable.lootTable()
                    .withPool(LootPool.lootPool()
                        .setBonusRolls(UniformGenerator.between(0, 0.02F))
                        .add(item(Items.RAW_IRON, 3, 5).setWeight(80))
                        .add(item(Items.RAW_GOLD, 1, 5).setWeight(100).setQuality(1))
                        .add(item(Items.REDSTONE, 2, 10).setWeight(60).setQuality(1))
                        .add(item(Items.LAPIS_LAZULI, 5, 15).setWeight(60).setQuality(2))
                        .add(item(Items.DIAMOND, 1, 3).setWeight(60).setQuality(3))

                    )
                    // At the bottom of the earth, add extra redstone and diamonds.
                    .withPool(LootPool.lootPool()
                        .when(
                            LocationCheck.checkLocation(
                                LocationPredicate.Builder.location()
                                    .setY(Doubles.atMost(-48))))
                        .add(item(Items.REDSTONE, 2, 10).setWeight(15))
                        .add(item(Items.DIAMOND, 1, 3).setWeight(15))
                        .add(EmptyLootItem.emptyItem().setWeight(70))));

            output.accept(Ench.LootTables.BOON_NETHER_DROPS,
                LootTable.lootTable()
                    .withPool(LootPool.lootPool()
                        .setBonusRolls(UniformGenerator.between(0, 0.02F))
                        .add(item(Items.QUARTZ, 3, 5).setWeight(150))
                        .add(item(Items.RAW_GOLD, 1, 5).setWeight(100))
                        .add(item(Items.GLOWSTONE_DUST, 2, 10).setWeight(60))
                        .add(item(Items.ANCIENT_DEBRIS, 0, 1).setWeight(1)))
                    // At the netherite peak location, generate more ancient debris than usual.
                    .withPool(LootPool.lootPool()
                        .when(
                            LocationCheck.checkLocation(
                                LocationPredicate.Builder.location()
                                    .setY(Doubles.between(12, 18))))
                        .add(item(Items.ANCIENT_DEBRIS, 1, 1).setWeight(3))
                        .add(EmptyLootItem.emptyItem().setWeight(97))));

        }
    }

    private static StackEntryBuilder item(Item item, int min, int max) {
        return new StackEntryBuilder(item.getDefaultInstance()).count(min, max);
    }

    public static class StackEntryBuilder extends LootPoolSingletonContainer.Builder<StackEntryBuilder> {

        protected final ItemStack stack;
        protected int min = -1, max = -1;

        public StackEntryBuilder(ItemStack stack) {
            this.stack = stack;
        }

        public StackEntryBuilder count(int min, int max) {
            this.min = min;
            this.max = max;
            return this;
        }

        @Override
        protected StackEntryBuilder getThis() {
            return this;
        }

        @Override
        public LootPoolEntryContainer build() {
            if (this.min == -1) {
                this.min = stack.getCount();
            }
            if (this.max == -1) {
                this.max = stack.getCount();
            }
            return new StackLootEntry(this.stack, this.min, this.max, this.weight, this.quality, this.getConditions(), this.getFunctions());

        }
    }
}
