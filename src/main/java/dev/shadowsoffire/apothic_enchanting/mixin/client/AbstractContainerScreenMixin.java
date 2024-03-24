package dev.shadowsoffire.apothic_enchanting.mixin.client;

import org.spongepowered.asm.mixin.Mixin;

import dev.shadowsoffire.apothic_enchanting.client.DrawsOnLeft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

@Mixin(value = AbstractContainerScreen.class, remap = false)
public class AbstractContainerScreenMixin implements DrawsOnLeft {

}
