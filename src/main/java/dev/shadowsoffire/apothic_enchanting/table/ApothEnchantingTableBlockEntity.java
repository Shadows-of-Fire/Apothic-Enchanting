package dev.shadowsoffire.apothic_enchanting.table;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Clearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ApothEnchantingTableBlockEntity extends EnchantingTableBlockEntity implements Clearable {
    public ApothEnchantingTableBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < this.getData(EnchantmentTableItemHandler.TYPE).getSlots(); i++) {
            this.getData(EnchantmentTableItemHandler.TYPE).setStackInSlot(i, ItemStack.EMPTY);
        }
    }
}
