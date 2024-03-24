package dev.shadowsoffire.apothic_enchanting.table;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.apothic_enchanting.table.EnchantingStatRegistry.Stats;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class KeepNBTEnchantingRecipe extends EnchantingRecipe {

    public static final Codec<EnchantingRecipe> CODEC = RecordCodecBuilder.create(inst -> inst.group(
        ItemStack.ITEM_WITH_COUNT_CODEC.fieldOf("result").forGetter(EnchantingRecipe::getOutput),
        Ingredient.CODEC_NONEMPTY.fieldOf("input").forGetter(EnchantingRecipe::getInput),
        Stats.CODEC.fieldOf("requirements").forGetter(EnchantingRecipe::getRequirements),
        ExtraCodecs.strictOptionalField(Stats.CODEC, "max_requirements", NO_MAX).forGetter(EnchantingRecipe::getMaxRequirements))
        .apply(inst, KeepNBTEnchantingRecipe::new));

    public static final Serializer SERIALIZER = new Serializer();

    public KeepNBTEnchantingRecipe(ItemStack output, Ingredient input, Stats requirements, Stats maxRequirements) {
        super(output, input, requirements, maxRequirements);
    }

    @Override
    public ItemStack assemble(ItemStack input, float eterna, float quanta, float arcana) {
        ItemStack out = this.getOutput().copy();
        if (input.hasTag()) out.setTag(input.getTag().copy());
        return out;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return KeepNBTEnchantingRecipe.SERIALIZER;
    }

    public static class Serializer extends EnchantingRecipe.Serializer {

        @Override
        public Codec<EnchantingRecipe> codec() {
            return CODEC;
        }

    }

}
