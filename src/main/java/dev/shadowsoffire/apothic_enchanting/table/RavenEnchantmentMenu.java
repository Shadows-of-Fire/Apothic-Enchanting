package dev.shadowsoffire.apothic_enchanting.table;

import dev.shadowsoffire.apothic_enchanting.Ench;
import dev.shadowsoffire.apothic_enchanting.payloads.StatsPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.network.PacketDistributor;

public class RavenEnchantmentMenu extends ApothEnchantmentMenu {

    private final RavenTableStats ravenStats;

    public static RavenEnchantmentMenu fromBuf(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int e = buf.readInt();
        int q = buf.readInt();
        int a = buf.readInt();
        return new RavenEnchantmentMenu(id, inv, pos, e, q, a);
    }

    public RavenEnchantmentMenu(int id, Inventory inv, BlockPos pos, int eterna, int quanta, int arcana) {
        super(id, inv, pos);
        this.ravenStats = new RavenTableStats(eterna, quanta, arcana);

    }

    public RavenEnchantmentMenu(int id, Inventory inv, ContainerLevelAccess access, EnchantmentTableItemHandler teInv, BlockPos pos, RavenTableStats stats) {
        super(id, inv, access, teInv, pos);
        this.ravenStats = stats;
    }

    public RavenTableStats getRavenStats() {
        return ravenStats;
    }

    @Override
    public MenuType<?> getType() {
        return Ench.Menus.RAVEN_ENCHANTING_TABLE;
    }

    @Override
    public void gatherStats() {
        this.access.execute((world, pos) -> {
            EnchantmentTableStats blockStats = EnchantmentTableStats.gatherStats(world, pos);
            this.stats = new EnchantmentTableStats(
                this.ravenStats.eterna(),
                this.ravenStats.quanta(),
                this.ravenStats.arcana(),
                blockStats.clues(),
                blockStats.blacklist(),
                blockStats.treasure(),
                blockStats.stable());
            PacketDistributor.sendToPlayer((ServerPlayer) this.player, new StatsPayload(this.stats));
        });
    }

    /**
     * Server-side commit of player-set slider values. Validates against the {@link Ench.Attributes#MAX_ETERNA}
     * attribute (for eterna) and the hardcoded [0,100] range (for quanta and arcana), writes them
     * into the persistent attachment, then triggers a re-gather so the new stats propagate back to
     * the client.
     */
    public void setPlayerStats(int eterna, int quanta, int arcana) {
        int maxEterna = (int) ((Player) this.player).getAttributeValue(Ench.Attributes.MAX_ETERNA);
        this.ravenStats.set(
            Mth.clamp(eterna, 0, maxEterna),
            Mth.clamp(quanta, 0, 100),
            Mth.clamp(arcana, 0, 100));
        this.slotsChanged(this.enchantSlots);
    }

}
