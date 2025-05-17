package fr.openmc.core.features.corporation.shops;

import fr.openmc.core.features.corporation.company.Company;
import lombok.Getter;

import java.util.UUID;

@Getter
public class ShopOwner {

    private final Company company;
    private final UUID player;

    /**
     * ShopOwner for a company or a player
     * @param company to set a company as the owner
     */

    public ShopOwner(Company company) {
        this.company = company;
        this.player = null;
    }

    /**
     * ShopOwner for a company or a player
     * @param owner to set a player as the owner
     */
    public ShopOwner(UUID owner) {
        this.company = null;
        this.player = owner;
    }

    /**
     * know if it's a company
     */
    public boolean isCompany() {
        return company != null;
    }

    /**
     * know if it's a player
     */
    public boolean isPlayer() {
        return player != null;
    }

}
