package dev.shadowsoffire.apothic_enchanting.util;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.mojang.serialization.Codec;

import dev.shadowsoffire.apothic_enchanting.ApothicEnchanting;
import dev.shadowsoffire.apothic_enchanting.Ench;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Ingredient.ItemValue;
import net.minecraft.world.item.crafting.Ingredient.TagValue;
import net.minecraft.world.item.crafting.Ingredient.Value;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.crafting.NBTIngredient;

public class MiscDatagen implements DataProvider {

    private final Path outputDir;
    private CachedOutput cachedOutput;
    private List<CompletableFuture<?>> futures = new ArrayList<>();

    public MiscDatagen(Path outputDir) {
        this.outputDir = outputDir;
    }

    private void genRecipes() {
        Ingredient pot = potionIngredient(Potions.REGENERATION);
        addShaped(Ench.Blocks.HELLSHELF.get(), 3, 3, Blocks.NETHER_BRICKS, Blocks.NETHER_BRICKS, Blocks.NETHER_BRICKS, Items.BLAZE_ROD, "forge:bookshelves", pot, Blocks.NETHER_BRICKS, Blocks.NETHER_BRICKS,
            Blocks.NETHER_BRICKS);
        addShaped(Ench.Items.PRISMATIC_WEB, 3, 3, null, Items.PRISMARINE_SHARD, null, Items.PRISMARINE_SHARD, Blocks.COBWEB, Items.PRISMARINE_SHARD, null, Items.PRISMARINE_SHARD, null);
        ItemStack book = new ItemStack(Items.BOOK);
        ItemStack stick = new ItemStack(Items.STICK);
        ItemStack blaze = new ItemStack(Items.BLAZE_ROD);
        addShaped(new ItemStack(Ench.Items.HELMET_TOME.get(), 5), 3, 2, book, book, book, book, blaze, book);
        addShaped(new ItemStack(Ench.Items.CHESTPLATE_TOME.get(), 8), 3, 3, book, blaze, book, book, book, book, book, book, book);
        addShaped(new ItemStack(Ench.Items.LEGGINGS_TOME.get(), 7), 3, 3, book, null, book, book, blaze, book, book, book, book);
        addShaped(new ItemStack(Ench.Items.BOOTS_TOME.get(), 4), 3, 2, book, null, book, book, blaze, book);
        addShaped(new ItemStack(Ench.Items.WEAPON_TOME.get(), 2), 1, 3, book, book, new ItemStack(Items.BLAZE_POWDER));
        addShaped(new ItemStack(Ench.Items.PICKAXE_TOME.get(), 3), 3, 3, book, book, book, null, blaze, null, null, stick, null);
        addShaped(new ItemStack(Ench.Items.FISHING_TOME.get(), 2), 3, 3, null, null, blaze, null, stick, book, stick, null, book);
        addShaped(new ItemStack(Ench.Items.BOW_TOME.get(), 3), 3, 3, null, stick, book, blaze, null, book, null, stick, book);
        // addShapeless(new ItemStack(Ench.Items.OTHER_TOME.get(), 6), book, book, book, book, book, book, blaze);
        addShaped(new ItemStack(Ench.Items.SCRAP_TOME.get(), 8), 3, 3, book, book, book, book, Blocks.ANVIL, book, book, book, book);
        Ingredient maxHellshelf = Ingredient.of(Ench.Blocks.INFUSED_HELLSHELF.get());
        addShaped(Ench.Blocks.BLAZING_HELLSHELF.get(), 3, 3, null, Items.FIRE_CHARGE, null, Items.FIRE_CHARGE, maxHellshelf, Items.FIRE_CHARGE, Items.BLAZE_POWDER, Items.BLAZE_POWDER, Items.BLAZE_POWDER);
        addShaped(Ench.Blocks.GLOWING_HELLSHELF.get(), 3, 3, null, Blocks.GLOWSTONE, null, null, maxHellshelf, null, Blocks.GLOWSTONE, null, Blocks.GLOWSTONE);
        addShaped(Ench.Blocks.SEASHELF.get(), 3, 3, Blocks.PRISMARINE_BRICKS, Blocks.PRISMARINE_BRICKS, Blocks.PRISMARINE_BRICKS, potionIngredient(Potions.WATER), "forge:bookshelves", Items.PUFFERFISH,
            Blocks.PRISMARINE_BRICKS, Blocks.PRISMARINE_BRICKS, Blocks.PRISMARINE_BRICKS);
        Ingredient maxSeashelf = Ingredient.of(Ench.Blocks.INFUSED_SEASHELF.get());
        addShaped(Ench.Blocks.CRYSTAL_SEASHELF.get(), 3, 3, null, Items.PRISMARINE_CRYSTALS, null, null, maxSeashelf, null, Items.PRISMARINE_CRYSTALS, null, Items.PRISMARINE_CRYSTALS);
        addShaped(Ench.Blocks.HEART_SEASHELF.get(), 3, 3, null, Items.HEART_OF_THE_SEA, null, Items.PRISMARINE_SHARD, maxSeashelf, Items.PRISMARINE_SHARD, Items.PRISMARINE_SHARD, Items.PRISMARINE_SHARD,
            Items.PRISMARINE_SHARD);
        addShaped(Ench.Blocks.PEARL_ENDSHELF.get(), 3, 3, Items.END_ROD, null, Items.END_ROD, Items.ENDER_PEARL, Ench.Blocks.ENDSHELF.get(), Items.ENDER_PEARL, Items.END_ROD, null, Items.END_ROD);
        addShaped(Ench.Blocks.DRACONIC_ENDSHELF.get(), 3, 3, null, Items.DRAGON_HEAD, null, Items.ENDER_PEARL, Ench.Blocks.ENDSHELF.get(), Items.ENDER_PEARL, Items.ENDER_PEARL, Items.ENDER_PEARL, Items.ENDER_PEARL);
        addShaped(Ench.Blocks.BEESHELF.get(), 3, 3, Items.HONEYCOMB, Items.BEEHIVE, Items.HONEYCOMB, Items.HONEY_BLOCK, "forge:bookshelves", Items.HONEY_BLOCK, Items.HONEYCOMB, Items.BEEHIVE, Items.HONEYCOMB);
        addShaped(Ench.Blocks.MELONSHELF.get(), 3, 3, Items.MELON, Items.MELON, Items.MELON, Items.GLISTERING_MELON_SLICE, "forge:bookshelves", Items.GLISTERING_MELON_SLICE, Items.MELON, Items.MELON, Items.MELON);
    }

    public static ItemStack makeStack(Object thing) {
        if (thing instanceof ItemStack stack) return stack;
        if (thing instanceof ItemLike il) return new ItemStack(il);
        if (thing instanceof Holder<?> h) return new ItemStack((ItemLike) h.value());
        throw new IllegalArgumentException("Attempted to create an ItemStack from something that cannot be converted: " + thing);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static NonNullList<Ingredient> createInput(String modid, boolean allowEmpty, Object... inputArr) {
        NonNullList<Ingredient> inputL = NonNullList.create();
        for (int i = 0; i < inputArr.length; i++) {
            Object input = inputArr[i];
            if (input instanceof TagKey tag) inputL.add(i, Ingredient.of(tag));
            else if (input instanceof String str) inputL.add(i, Ingredient.of(ItemTags.create(new ResourceLocation(str))));
            else if (input instanceof ItemStack stack && !stack.isEmpty()) inputL.add(i, Ingredient.of(stack));
            else if (input instanceof ItemLike || input instanceof Holder) inputL.add(i, Ingredient.of(makeStack(input)));
            else if (input instanceof Ingredient ing) inputL.add(i, ing);
            else if (allowEmpty) inputL.add(i, Ingredient.EMPTY);
            else throw new UnsupportedOperationException("Attempted to add invalid recipe.  Complain to the author of " + modid + ". (Input " + input + " not allowed.)");
        }
        return inputL;
    }

    public static ShapedRecipePattern toPattern(int width, int height, NonNullList<Ingredient> input) {
        Map<Character, Ingredient> key = new HashMap<>();
        Map<IngredientKey, Character> chars = new HashMap<>();
        List<String> rows = new ArrayList<>(height);
        for (int h = 0; h < height; h++) {
            String row = "";
            for (int w = 0; w < width; w++) {
                Ingredient ing = input.get(h * width + w);
                IngredientKey iKey = new IngredientKey(ing);
                if (chars.containsKey(iKey)) {
                    row += chars.get(iKey);
                    continue;
                }
                else {
                    Character c = getFirstChar(chars.values(), ing);
                    key.put(c, ing);
                    chars.put(iKey, c);
                    row += c;
                    continue;
                }
            }
            rows.add(row);
        }
        key.remove(' ');
        return ShapedRecipePattern.of(key, rows);
    }

    private static Character getFirstChar(Collection<Character> inUse, Ingredient ing) {
        String path;
        if (ing == Ingredient.EMPTY) {
            return ' ';
        }
        else if (ing instanceof NBTIngredient nbt) {
            path = BuiltInRegistries.ITEM.getKey(nbt.getItems()[0].getItem()).getPath();
        }
        else {
            Value v = ing.values[0];
            if (v instanceof TagValue t) {
                path = t.tag().location().getPath();
            }
            else if (v instanceof ItemValue i) {
                path = BuiltInRegistries.ITEM.getKey(i.item().getItem()).getPath();
            }
            else {
                throw new UnsupportedOperationException();
            }
        }
        path = path.toUpperCase(Locale.ROOT);
        for (char c : path.toCharArray()) {
            if (!inUse.contains(c)) return c;
        }
        throw new UnsupportedOperationException();
    }

    private ShapedRecipe genShaped(ItemStack output, int width, int height, Object... input) {
        if (width * height != input.length) throw new UnsupportedOperationException("Attempted to add invalid shaped recipe.");
        return new ShapedRecipe(ApothicEnchanting.MODID, CraftingBookCategory.MISC, toPattern(width, height, createInput(ApothicEnchanting.MODID, true, input)), output);
    }

    public void addShaped(Object output, int width, int height, Object... input) {
        ItemStack out = makeStack(output);
        ShapedRecipe recipe = this.genShaped(out, width, height, input);
        write(recipe, ShapedRecipe.CODEC, "recipes", BuiltInRegistries.ITEM.getKey(out.getItem()).getPath());
    }

    private <T> void write(T object, Codec<T> codec, String type, String path) {
        this.futures.add(DataProvider.saveStable(this.cachedOutput, codec, object, outputDir.resolve(type + "/" + path + ".json")));
    }

    @SuppressWarnings("removal")
    public static Ingredient potionIngredient(Potion type) {
        return new NBTIngredient(Set.of(Items.POTION), PotionUtils.setPotion(new ItemStack(Items.POTION), type).getTag(), false){};
    }

    @Override
    public CompletableFuture<?> run(CachedOutput pOutput) {
        this.cachedOutput = pOutput;
        genRecipes();
        return CompletableFuture.allOf(this.futures.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return ApothicEnchanting.MODID;
    }

    private static class IngredientKey {
        private final Ingredient ing;

        private IngredientKey(Ingredient ing) {
            this.ing = ing;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof IngredientKey key) {
                Value[] ours = this.ing.values;
                Value[] theirs = key.ing.values;
                if (ours.length != theirs.length) return false;
                for (int i = 0; i < ours.length; i++) {
                    if (!ours[i].equals(theirs[i])) return false;
                }
                return true;
            }
            return false;
        }

        @Override
        public int hashCode() {
            int hash = 31;
            for (Value v : this.ing.values) {
                if (v instanceof TagValue t) {
                    hash ^= t.hashCode();
                }
                else if (v instanceof ItemValue i) {
                    hash ^= BuiltInRegistries.ITEM.getKey(i.item().getItem()).hashCode();
                }
                else {
                    throw new UnsupportedOperationException();
                }
            }
            return hash;
        }
    }
}
