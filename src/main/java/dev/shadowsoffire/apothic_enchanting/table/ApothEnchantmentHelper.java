package dev.shadowsoffire.apothic_enchanting.table;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.lang3.mutable.MutableObject;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;

import dev.shadowsoffire.apothic_enchanting.ApothicEnchanting;
import dev.shadowsoffire.apothic_enchanting.EnchantmentInfo;
import dev.shadowsoffire.apothic_enchanting.api.EnchantableItem;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedEntry.IntrusiveBase;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ConditionalEffect;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.effects.EnchantmentValueEffect;
import net.minecraft.world.level.storage.loot.LootContext;

public class ApothEnchantmentHelper {

    /**
     * Determines the level of the given enchantment table slot.
     * An item with 0 enchantability cannot be enchanted, so this method returns zero.
     * Slot 2 (the highest level slot) always receives a level equal to power * 2.
     * Slot 1 recieves between 60% and 80% of Slot 2.
     * Slot 0 receives between 20% and 40% of Slot 2.
     *
     * @param rand   Pre-seeded random.
     * @param num    Enchantment Slot Number [0-2]
     * @param eterna Enchantment Power (Eterna Level)
     * @param stack  Itemstack to be enchanted.
     * @return The level that the table will use for this specific slot.
     */
    public static int getEnchantmentCost(RandomSource rand, int num, float eterna, ItemStack stack) {
        int level = Math.round(eterna);
        if (num == 2) return level;
        float lowBound = 0.6F - 0.4F * (1 - num);
        float highBound = 0.8F - 0.4F * (1 - num);
        return Math.max(1, Math.round(level * Mth.nextFloat(rand, lowBound, highBound)));
    }

    public static Stream<Holder<Enchantment>> getPossibleEnchantments(RegistryLookup<Enchantment> reg, ItemStack stack, EnchantmentTableStats stats) {
        ItemEnchantments enchants = EnchantmentHelper.getEnchantmentsForCrafting(stack);

        return reg.listElements()
            .filter(h -> h.is(EnchantmentTags.IN_ENCHANTING_TABLE) || (stats.treasure() && h.is(EnchantmentTags.TREASURE)))
            .filter(h -> !stats.blacklist().contains(h))
            .filter(h -> !enchants.keySet().contains(h))
            .map(Function.<Holder<Enchantment>>identity());
    }

    /**
     * Creates a list of enchantments for a specific slot given various variables.
     *
     * @param rand  Pre-seeded random.
     * @param stack Itemstack to be enchanted.
     * @param level The level of the selected enchantment slot.
     * @param stats The stats of the enchantment table.
     * @return A list of enchantments based on the seed, item, and table stats.
     */
    public static List<EnchantmentInstance> selectEnchantment(RandomSource rand, ItemStack stack, int level, EnchantmentTableStats stats, RegistryLookup<Enchantment> reg) {
        List<EnchantmentInstance> chosenEnchants = new ArrayList<>();
        int enchantability = stack.getEnchantmentValue();

        if (enchantability > 0) {
            float quantaFactor = getQuantaFactor(rand, stats.quanta(), stats.stable());
            int power = Mth.clamp(Math.round(level * quantaFactor), 1, 200);
            Arcana arcanaVals = Arcana.getForThreshold(stats.arcana());

            Stream<Holder<Enchantment>> possible = getPossibleEnchantments(reg, stack, stats);
            List<EnchantmentInstance> allEnchants = ApothEnchantmentHelper.getAvailableEnchantmentResults(power, stack, possible);
            List<ArcanaEnchantmentData> possibleEnchants = allEnchants.stream().map(d -> new ArcanaEnchantmentData(arcanaVals, d)).collect(Collectors.toList());

            // At least one enchantment is guaranteed, with an extra one per 33 Arcana.
            for (int i = 0; i < 100; i += 33) {
                if (stats.arcana() >= i && possibleEnchants.size() > 0) {
                    pickEnchantment(rand, chosenEnchants, possibleEnchants);
                }
            }

            // A random number of extra enchantments are added, with the chance reducing per enchantment added.
            int randomBound = Math.max(50, (int) (level * 1.15F)); // Vanilla threshold is 50 for all levels.
            while (rand.nextInt(randomBound) <= level && !possibleEnchants.isEmpty()) {
                pickEnchantment(rand, chosenEnchants, possibleEnchants);
                level /= 2;
            }
        }

        // Item enchantability is treated as an element-wise % chance to increase enchantment levels by one.
        // Diminishing returns start at 80% via some natural logic magic formula. Guaranteed at ~385.
        float chance = Math.min(0.80F, enchantability / 100F);
        chance += Math.clamp(3.5 * Math.log(enchantability - 80) / 100F, 0, 0.20F);
        for (int i = 0; i < chosenEnchants.size(); i++) {
            EnchantmentInstance inst = chosenEnchants.get(i);
            if (rand.nextFloat() >= chance) {
                chosenEnchants.set(i, new EnchantmentInstance(inst.enchantment, inst.level + 1));
            }
        }

        return ((EnchantableItem) stack.getItem()).selectEnchantments(chosenEnchants, rand, stack, level, stats);
    }

    /**
     * Randomly selects an enchantment from the possible enchantments and adds it to the list of chosen enchantments.
     */
    public static void pickEnchantment(RandomSource rand, List<EnchantmentInstance> chosenEnchants, List<ArcanaEnchantmentData> possibleEnchants) {
        chosenEnchants.add(WeightedRandom.getRandomItem(rand, possibleEnchants).get().data);
        removeIncompatible(possibleEnchants, Util.lastOf(chosenEnchants));
    }

    /**
     * Removes all enchantments from the list that are incompatible with the passed enchantment.
     */
    public static void removeIncompatible(List<ArcanaEnchantmentData> possibleEnchants, EnchantmentInstance data) {
        Iterator<ArcanaEnchantmentData> iterator = possibleEnchants.iterator();
        while (iterator.hasNext()) {
            if (!Enchantment.areCompatible(data.enchantment, iterator.next().data.enchantment)) {
                iterator.remove();
            }
        }
    }

    /**
     * @param power         The current enchanting power.
     * @param stack         The ItemStack being enchanted.
     * @param allowTreasure If treasure enchantments are allowed.
     * @param blacklist     A list of all enchantments that may not be selected.
     * @return All possible enchantments that are eligible to be placed on this item at a specific power level.
     */
    public static List<EnchantmentInstance> getAvailableEnchantmentResults(int level, ItemStack stack, Stream<Holder<Enchantment>> possibleEnchantments) {
        List<EnchantmentInstance> selected = Lists.newArrayList();
        possibleEnchantments.filter(stack::isPrimaryItemFor).forEach(ench -> {
            EnchantmentInfo info = ApothicEnchanting.getEnchInfo(ench);

            for (int i = info.getMaxLevel(); i >= ench.value().getMinLevel(); i--) {
                if (level >= info.getMinPower(i) && level <= info.getMaxPower(i)) {
                    selected.add(new EnchantmentInstance(ench, i));
                    break;
                }
            }
        });
        return selected;
    }

    /**
     * Generates a quanta factor, which is one plus the quanta value times a random number within the range [-1, 1].
     * <p>
     * For unstable enchanting tables, the random number is normally distributed between -1 and 1.
     * <p>
     * For stable enchanting tables, the random number is uniformly distributed between 0 and 1.
     *
     * @param rand     The pre-seeded enchanting random.
     * @param quanta   The quanta value, in [0, 100].
     * @param isStable If the enchanting table has been stabilized by a bookshelf.
     * @return A quanta factor that should be multiplied with the level to retrieve the power.
     */
    public static float getQuantaFactor(RandomSource rand, float quanta, boolean isStable) {
        if (isStable) {
            return 1 + quanta * rand.nextFloat() / 100F;
        }
        else {
            // Division by three yields a "good enough" normal distribution over [-1, 1].
            float gaussian = (float) rand.nextGaussian();
            float factor = Mth.clamp(gaussian / 3F, -1F, 1F);
            return 1 + quanta * factor / 100F;
        }
    }

    @Nullable
    public static <T> Pair<T, Integer> getHighestEquippedLevel(DataComponentType<T> effectComp, LivingEntity entity) {
        MutableObject<Pair<T, Integer>> result = new MutableObject<>();

        EnchantmentHelper.runIterationOnEquipment(entity, (ench, level, slot) -> {
            T data = ench.value().effects().get(effectComp);
            if (data != null && (result.getValue() == null || result.getValue().getSecond() < level)) {
                result.setValue(Pair.of(data, level));
            }
        });

        return result.getValue();
    }

    public static float processValue(List<ConditionalEffect<EnchantmentValueEffect>> effects, LootContext ctx, int level, float initial) {
        MutableFloat f = new MutableFloat(initial);
        Enchantment.applyEffects(effects, ctx, valueEffect -> {
            f.setValue(valueEffect.process(level, ctx.getRandom(), f.getValue()));
        });
        return f.getValue();
    }

    public static float processValue(List<EnchantmentValueEffect> effects, RandomSource rand, int level, float initial) {
        MutableFloat f = new MutableFloat(initial);
        effects.forEach(valueEffect -> {
            f.setValue(valueEffect.process(level, rand, f.getValue()));
        });
        return f.getValue();
    }

    public static class ArcanaEnchantmentData extends IntrusiveBase {
        EnchantmentInstance data;

        public ArcanaEnchantmentData(Arcana arcana, EnchantmentInstance data) {
            super(arcana.adjustWeight(data.enchantment.value().getWeight()));
            this.data = data;
        }
    }
}
