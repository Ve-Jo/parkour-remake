package net.crumb.lobbyParkour.listeners;

import net.crumb.lobbyParkour.systems.ParkourSessionManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class PlayerDamageListener implements Listener {

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (ParkourSessionManager.isInSession(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }
}
