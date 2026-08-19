package dev.shadowsoffire.apothic_enchanting.mixin.client;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import dev.shadowsoffire.apothic_enchanting.table.ApothicTableState;
import net.minecraft.client.renderer.blockentity.state.EnchantTableRenderState;
import net.minecraft.client.resources.model.sprite.SpriteId;

@Mixin(value = EnchantTableRenderState.class, remap = false)
public class EnchantTableRenderStateMixin implements ApothicTableState {

    @Unique
    @Nullable
    private SpriteId apoth_bookTexture;

    @Override
    public void apoth_setBookTexture(@Nullable SpriteId sprite) {
        this.apoth_bookTexture = sprite;
    }

    @Override
    @Nullable
    public SpriteId apoth_getBookTexture() {
        return this.apoth_bookTexture;
    }

}
