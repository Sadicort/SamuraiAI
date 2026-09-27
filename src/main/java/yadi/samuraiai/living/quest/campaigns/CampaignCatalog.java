package yadi.samuraiai.living.quest.campaigns;

import java.util.List;
import java.util.Optional;
import yadi.samuraiai.living.quest.conditions.ConditionKind;

/**
 * Which conditions open a campaign and which quests it is made of. A war is reconnaissance, defending the village and taking
 * the pass; a trade campaign reopens the route and then escorts the first caravan; a temple campaign gathers for the rite and
 * performs it; a family campaign finds the heirloom and restores the family's honour.
 */
public final class CampaignCatalog {
    public record Definition(Campaign.Type type, ConditionKind trigger, String title, List<String> stages) { }

    private static final List<Definition> DEFINITIONS = List.of(
            new Definition(Campaign.Type.WAR, ConditionKind.WAR, "La guerra de {regionName}", List.of("war_reconnaissance", "defend_village", "capture_bridge")),
            new Definition(Campaign.Type.TRADE, ConditionKind.ROUTE_BLOCKED, "La ruta de {destinationName}", List.of("open_trade_route", "escort_caravan")),
            new Definition(Campaign.Type.TEMPLE, ConditionKind.TEMPLE_RITUAL, "Los ritos de la luna", List.of("temple_ritual", "temple_relic")),
            new Definition(Campaign.Type.FAMILY, ConditionKind.HEIRLOOM_LOST, "El legado de los {familyName}", List.of("recover_heirloom", "restore_honor")),
            new Definition(Campaign.Type.CLAN, ConditionKind.GRUDGE, "Viejas rencillas", List.of("resolve_dispute", "restore_honor")));

    private CampaignCatalog() { }

    public static Optional<Definition> forCondition(ConditionKind kind) { return DEFINITIONS.stream().filter(d -> d.trigger() == kind).findFirst(); }
    public static List<Definition> all() { return DEFINITIONS; }
}
