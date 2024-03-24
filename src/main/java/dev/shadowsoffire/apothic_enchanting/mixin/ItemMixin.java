package dev.shadowsoffire.apothic_enchanting.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import dev.shadowsoffire.apothic_enchanting.table.IEnchantableItem;
import net.minecraft.world.item.Item;

@Mixin(value = Item.class, remap = false)
public class ItemMixin implements IEnchantableItem {

    /**
     * @author Shadows
     * @reason Enables all items to be enchantable by default.
     */
    @Overwrite
    public int getEnchantmentValue() {
        return 1;
    }

}
