package fr.openmc.core.features.city.mayor.perks;

import fr.openmc.core.utils.customitems.CustomItemRegistry;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

@Getter
public enum Perks {
    FOU_DE_RAGE(
            1,
            "§e§lFou de Rage",
            List.of(
                    Component.text("§7Donne §3un effet de force I §7dans une ville adverse"),
                    Component.text("§7Et donne §3un effet de protection I§7 dans sa ville")
            ),
            ItemStack.of(Material.BLAZE_POWDER),
            PerkType.BASIC,
            0
    ),
    IMPOT(
            2,
            "§e§lPrévélement d'Impot",
            List.of(
                    Component.text("§7Possibilité de lancer un §3événement §7pour préléver les Impots"),
                    Component.text("§7Limite de perte d'argent :§3 5k §8(Cooldown : 3j)")
            ),
            ItemStack.of(Material.GOLD_BLOCK),
            PerkType.EVENT,
            3 * 24 * 60 * 60 * 1000L // 3 jours
    ),
    MINER(
            3,
            "§e§lMineur Dévoué",
            List.of(
                    Component.text("§7Donne §3Haste I §7aux membres de la ville")
            ),
            ItemStack.of(Material.GOLDEN_PICKAXE),
            PerkType.BASIC,
            0
    ),
    FRUIT_DEMON(
            4,
            "§e§lFruit du Démon",
            List.of(
                    Component.text("§7Augmente §3la portée §7de tous les membres de la ville"),
                    Component.text("§7de §31 §7bloc")
            ),
            ItemStack.of(Material.CHORUS_FRUIT),
            PerkType.BASIC,
            0
    ),
    BUISNESS_MAN(
            5,
            "§e§lBuisness Man",
            List.of(
                    Component.text("§7Mets l'intérêt à §33%"),
                    Component.text("§7pour la ville et les membres de la ville !")
            ),
            ItemStack.of(Material.DIAMOND),
            PerkType.BASIC,
            0
    ),
    IRON_BLOOD(
            6,
            "§e§lFer dans le Sang",
            List.of(
                    Component.text("§7Fait apparaître un §3Golem de Fer §7lorsque la mascotte"),
                    Component.text("§7se fait taper par l'ennemi §8(Cooldown : 3 min)")
            ),
            ItemStack.of(Material.IRON_BLOCK),
            PerkType.BASIC,
            0
    ),
    CITY_HUNTER(
            7,
            "§e§lChasseur Urbain",
            List.of(
                    Component.text("§7Augmente de §320 % §7les dégâts infligés aux"),
                    Component.text("§3monstres §7et §3joueurs §7dans sa propre ville.")
            ),
            ItemStack.of(Material.BOW),
            PerkType.BASIC,
            0
    ),
    AYWENITER(
            8,
            "§e§lAyweniteur",
            List.of(
                    Component.text("§7Casser une pierre donne 1% de chance d'avoir 2 d'Aywenites")
            ),
            CustomItemRegistry.getByName("omc_items:aywenite").getBest(),
            PerkType.BASIC,
            0
    ),
    GPS_TRACKER(
            9,
            "§e§lTraceur GPS",
            List.of(
                    Component.text("§7Lorsqu'un §3ennemi §7rentre dans votre ville,"),
                    Component.text("§7un §3effet de glowing §7lui est donné.")
            ),
            ItemStack.of(Material.COMPASS),
            PerkType.BASIC,
            0
    ),
    SYMBIOSIS(
            10,
            "§e§lSymbiose",
            List.of(
                    Component.text("§7Réduit les dégâts subis de §315%"),
                    Component.text("§7lorsque vous êtes autour de votre §3Mascotte")
            ),
            ItemStack.of(Material.SCULK_CATALYST),
            PerkType.BASIC,
            0
    ),
    AGRICULTURAL_ESSOR(
            11,
            "§e§lEssor Agricole",
            List.of(
                    Component.text("§7La récolte est doublée pendant§3 30 min §7dans la ville! §8(Cooldown : 1j)")
            ),
            ItemStack.of(Material.NETHERITE_HOE),
            PerkType.EVENT,
            24 * 60 * 60 * 1000L // 1 jour
    ),
    MINERAL_RUSH(
            12,
            "§e§lRuée Minière",
            List.of(
                    Component.text("§7Tous les minerais extraits pendant§3 5 §7minutes"),
                    Component.text("§7donnent le double de ressources. §8(Cooldown : 1j)")
            ),
            ItemStack.of(Material.DIAMOND_PICKAXE),
            PerkType.EVENT,
            24 * 60 * 60 * 1000L // 1 jour
    ),
    MILITARY_DISSUASION(
            13,
            "§e§lDissuasion Militaire",
            List.of(
                    Component.text("§7Fait apparaître §310 Golem de Fer §7partout"),
                    Component.text("§7dans votre ville qui disparaissent dans §310 min §8(Cooldown : 25 min)")
            ),
            ItemStack.of(Material.IRON_GOLEM_SPAWN_EGG),
            PerkType.EVENT,
            25 * 60 * 1000L // 25 minutes
    ),
    IDYLLIC_RAIN(
            14,
            "§e§lPluie idyllique",
            List.of(
                    Component.text("§7Fait apparaître de l'§3Aywenite §7dans votre ville pendant§3 1 §7min. §8(Cooldown : 1j)")
            ),
            ItemStack.of(Material.GHAST_TEAR),
            PerkType.EVENT,
            24 * 60 * 60 * 1000L // 1 jour
    ),
    MASCOTS_FRIENDLY(
            15,
            "§e§lMascotte de Compagnie",
            List.of(
                    Component.text("§7A Partir du §cLevel 4 §7de la Mascotte, vous"),
                    Component.text("§7aurez des §3effets bonus §7si la mascotte est en vie !")
            ),
            ItemStack.of(Material.SADDLE),
            PerkType.BASIC,
            0
    )
    ;

    private final int id;
    private final String name;
    private final List<Component> lore;
    private final ItemStack itemStack;
    private final PerkType type;
    private final long cooldown;

    Perks(int id, String name, List<Component> lore, ItemStack itemStack, PerkType type, long cooldown) {
        this.id = id;
        this.name = name;
        this.lore = lore;
        this.itemStack = itemStack;
        this.type = type;
        this.cooldown = cooldown;
    }


}
