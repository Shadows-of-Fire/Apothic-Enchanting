package dev.shadowsoffire.apothic_enchanting.table;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dev.shadowsoffire.apothic_enchanting.ApothicEnchanting;
import dev.shadowsoffire.placebo.network.PayloadHelper;
import dev.shadowsoffire.placebo.network.PayloadProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;

public class ClueMessage implements CustomPacketPayload {

    public static final ResourceLocation ID = ApothicEnchanting.loc("clue");

    protected final int slot;
    protected final List<EnchantmentInstance> clues;
    protected final boolean all;

    /**
     * Sends a clue message to the client.
     *
     * @param slot
     * @param clues The clues.
     * @param all   If this is all of the enchantments being received.
     */
    public ClueMessage(int slot, List<EnchantmentInstance> clues, boolean all) {
        this.slot = slot;
        this.clues = clues;
        this.all = all;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeByte(this.clues.size());
        for (EnchantmentInstance e : this.clues) {
            buf.writeShort(BuiltInRegistries.ENCHANTMENT.getId(e.enchantment));
            buf.writeByte(e.level);
        }
        buf.writeByte(this.slot);
        buf.writeBoolean(this.all);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    public static class Provider implements PayloadProvider<ClueMessage, PlayPayloadContext> {

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public ClueMessage read(FriendlyByteBuf buf) {
            int size = buf.readByte();
            List<EnchantmentInstance> clues = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                Enchantment ench = BuiltInRegistries.ENCHANTMENT.byIdOrThrow(buf.readShort());
                clues.add(new EnchantmentInstance(ench, buf.readByte()));
            }
            return new ClueMessage(buf.readByte(), clues, buf.readBoolean());
        }

        @Override
        public void handle(ClueMessage msg, PlayPayloadContext ctx) {
            PayloadHelper.handle(() -> {
                if (Minecraft.getInstance().screen instanceof ApothEnchantScreen es) {
                    es.acceptClues(msg.slot, msg.clues, msg.all);
                }
            }, ctx);
        }

        @Override
        public List<ConnectionProtocol> getSupportedProtocols() {
            return List.of(ConnectionProtocol.PLAY);
        }

        @Override
        public Optional<PacketFlow> getFlow() {
            return Optional.of(PacketFlow.CLIENTBOUND);
        }
    }

}
