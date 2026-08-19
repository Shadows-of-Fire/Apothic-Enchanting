package dev.shadowsoffire.apothic_enchanting.data;

import dev.shadowsoffire.apothic_enchanting.Ench.Songs;
import dev.shadowsoffire.apothic_enchanting.Ench.Sounds;
import net.minecraft.core.Holder;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Util;
import net.minecraft.world.item.JukeboxSong;

public class SongProvider {

    private static void register(
        BootstrapContext<JukeboxSong> context, ResourceKey<JukeboxSong> key, SoundEvent soundEvent, int lengthInSeconds, int comparatorOutput) {
        context.register(
            key,
            new JukeboxSong(Holder.direct(soundEvent), Component.translatable(Util.makeDescriptionId("jukebox_song", key.identifier())), (float) lengthInSeconds, comparatorOutput));
    }

    public static void bootstrap(BootstrapContext<JukeboxSong> context) {
        register(context, Songs.ETERNA, Sounds.MUSIC_DISC_ETERNA, 3 * 60 + 51, 1);
        register(context, Songs.QUANTA, Sounds.MUSIC_DISC_QUANTA, 2 * 60 + 56, 2);
        register(context, Songs.ARCANA, Sounds.MUSIC_DISC_ARCANA, 4 * 60 + 51, 3);
    }

}
