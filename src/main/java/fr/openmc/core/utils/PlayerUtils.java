package fr.openmc.core.utils;

import org.bukkit.entity.Player;

public class PlayerUtils {

    /**
     * @param player Player to be tested
     * @return If the player is safe
     */
    private boolean isInSafePosition(Player player) {
        if (player.isFlying()) return false;
        if (player.isInsideVehicle()) return false;
        if (player.isGliding()) return false;
        if (player.isSleeping()) return false;
        if (player.isUnderWater()) return false;
        if (player.isFlying()) return false;
        if (player.isVisualFire()) return false;
        // TODO: Check si le block en pile, sur la tête et en dessous (trapdoor) est plein

        return true;
    }
}
