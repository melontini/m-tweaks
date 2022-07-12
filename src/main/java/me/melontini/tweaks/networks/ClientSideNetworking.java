package me.melontini.tweaks.networks;

import me.melontini.tweaks.Tweaks;
import me.melontini.tweaks.client.sound.PersistentMovingSoundInstance;
import me.melontini.tweaks.util.LogUtil;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MusicDiscItem;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.MutableText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

import static me.melontini.tweaks.Tweaks.MODID;

public class ClientSideNetworking {

    public static Map<Integer, SoundInstance> soundInstanceMap = new HashMap<>();

    public static void register() {
        if (Tweaks.CONFIG.newMinecarts.isJukeboxMinecartOn) {
            ClientPlayNetworking.registerGlobalReceiver(new Identifier(MODID, "jukebox_minecart_audio"), (client, handler, buf, responseSender) -> {
                int id = buf.readInt();
                ItemStack stack = buf.readItemStack();
                client.execute(() -> {
                    assert client.world != null;
                    Entity entity = client.world.getEntityById(id);
                    if (stack.getItem() instanceof MusicDiscItem) {
                        MusicDiscItem disc = (MusicDiscItem) stack.getItem();

                        MutableText discName = disc.getDescription();
                        SoundInstance instance = new PersistentMovingSoundInstance(disc.getSound(), SoundCategory.RECORDS, id, client.world);
                        soundInstanceMap.put(id, instance);
                        client.getSoundManager().play(instance);

                        if (discName != null)
                            if (client.player != null) if (entity != null) if (entity.distanceTo(client.player) < 76) {
                                client.player.sendMessage(new TranslatableText("record.nowPlaying", discName), true);
                            }
                    }
                });
            });
        }
        if (Tweaks.CONFIG.newMinecarts.isJukeboxMinecartOn) {
            ClientPlayNetworking.registerGlobalReceiver(new Identifier(MODID, "jukebox_minecart_audio_stop"), (client, handler, buf, responseSender) -> {
                int id = buf.readInt();
                client.execute(() -> {
                    SoundInstance instance = soundInstanceMap.get(id);
                    if (client.getSoundManager().isPlaying(instance)) {
                        client.getSoundManager().stop(instance);
                        soundInstanceMap.remove(id);
                        LogUtil.info("removed jbmc sound instance");
                    }
                });
            });
        }

        LogUtil.info("ClientSideNetworking init complete!");
    }
}
