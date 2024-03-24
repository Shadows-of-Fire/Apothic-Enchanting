package dev.shadowsoffire.apothic_enchanting.compat;

import java.util.List;
import java.util.function.Predicate;

import dev.shadowsoffire.apothic_enchanting.Ench;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

public class CuriosCompat {

    private static final Predicate<ItemStack> HAS_LIFE_MEND = stack -> stack.getEnchantmentLevel(Ench.Enchantments.LIFE_MENDING.get()) > 0;

    public static List<ItemStack> getLifeMendingCurios(LivingEntity entity) {
        List<SlotResult> slots = CuriosApi.getCuriosInventory(entity).map(handler -> handler.findCurios(HAS_LIFE_MEND)).orElse(List.of());
        return slots.stream().map(SlotResult::stack).toList();
    }

}
