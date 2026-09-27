package yadi.samuraiai.living.quest.templates;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import yadi.samuraiai.ai.scheduler.util.Segments;
import yadi.samuraiai.living.core.Season;
import yadi.samuraiai.living.quest.branching.BranchSpec;
import yadi.samuraiai.living.quest.branching.Path;
import yadi.samuraiai.living.quest.conditions.ConditionKind;
import yadi.samuraiai.living.quest.consequences.ConsequenceSpec;
import yadi.samuraiai.living.quest.objectives.ObjectiveSpec;
import yadi.samuraiai.living.quest.objectives.ObjectiveType;
import yadi.samuraiai.living.quest.rewards.RewardSpec;
import yadi.samuraiai.living.quest.templates.QuestTemplate.Category;
import yadi.samuraiai.living.quest.templates.QuestTemplate.StoryStage;

/**
 * The quest template catalogue: the built-in stories (village needs, temple rites, trade, war, relationships, memory,
 * families, exploration) and templates added by configuration lines such as
 * {@code honey;category=ECONOMY;when=SCARCITY;title=Miel para {settlement};text=...;objective=DELIVER:{resource}:{quantity}:settlement;reward=COINS:{value};reputation=VILLAGE:0.05}.
 * A line with an existing id replaces it; {@code disabled=true} removes one.
 */
public final class QuestTemplates {
    private final Map<String, QuestTemplate> templates = new LinkedHashMap<>();
    private final List<String> problems = new ArrayList<>();

    public QuestTemplates(List<String> lines) {
        builtIns().forEach(t -> templates.put(t.id(), t));
        if (lines != null) for (String line : lines) parse(line);
    }

    public static QuestTemplates defaults() { return new QuestTemplates(List.of()); }

    public Optional<QuestTemplate> get(String id) { return Optional.ofNullable(templates.get(id == null ? "" : id.toLowerCase(Locale.ROOT))); }
    public List<QuestTemplate> all() { return List.copyOf(templates.values()); }
    public List<QuestTemplate> triggeredBy(ConditionKind kind) { return templates.values().stream().filter(t -> t.triggers().contains(kind)).toList(); }
    public List<String> problems() { return List.copyOf(problems); }

    // ------------------------------------------------------------------ builder

    private static final class B {
        final String id; final Category category;
        final Set<ConditionKind> when = EnumSet.noneOf(ConditionKind.class);
        String title = "";
        final Map<StoryStage, String> story = new EnumMap<>(StoryStage.class);
        final List<ObjectiveSpec> objectives = new ArrayList<>();
        final List<BranchSpec> branches = new ArrayList<>();
        final List<RewardSpec> rewards = new ArrayList<>();
        final List<ConsequenceSpec> consequences = new ArrayList<>();
        int offer = 3, duration = 5;
        final Set<Season> seasons = EnumSet.noneOf(Season.class);
        final List<String> givers = new ArrayList<>();
        double weight = 1, minTrust;
        String follow = "";
        boolean mergeable;

        B(String id, Category category) { this.id = id; this.category = category; }
        B when(ConditionKind... k) { when.addAll(List.of(k)); return this; }
        B title(String t) { title = t; return this; }
        B story(String prologue, String development, String twist, String ending, String after) {
            story.put(StoryStage.PROLOGUE, prologue); story.put(StoryStage.DEVELOPMENT, development); story.put(StoryStage.TWIST, twist);
            story.put(StoryStage.ENDING, ending); story.put(StoryStage.CONSEQUENCES, after); return this;
        }
        B obj(ObjectiveSpec o) { objectives.add(o); return this; }
        B branch(Path p, String label, double trust, double standing, double factor) { branches.add(new BranchSpec(p, label, trust, standing, factor)); return this; }
        B reward(RewardSpec r) { rewards.add(r); return this; }
        B then(ConsequenceSpec c) { consequences.add(c); return this; }
        B days(int offerDays, int durationDays) { offer = offerDays; duration = durationDays; return this; }
        B givers(String... p) { givers.addAll(List.of(p)); return this; }
        B weight(double w) { weight = w; return this; }
        B trust(double t) { minTrust = t; return this; }
        B follow(String f) { follow = f; return this; }
        B mergeable() { mergeable = true; return this; }
        B seasons(Season... s) { seasons.addAll(List.of(s)); return this; }
        QuestTemplate build() {
            return new QuestTemplate(id, category, when, title, story, objectives, branches, rewards, consequences, offer, duration, seasons, Set.of(), givers, weight, follow, mergeable, minTrust);
        }
    }

    private static ObjectiveSpec o(ObjectiveType t, String d, String target, String qty, String place) { return ObjectiveSpec.of(t, d, target, qty, place); }
    private static ConsequenceSpec ok(ConsequenceSpec.Kind k, String target, String amount, String text) { return ConsequenceSpec.success(k, target, amount, text); }
    private static ConsequenceSpec ko(ConsequenceSpec.Kind k, String target, String amount, String text) { return ConsequenceSpec.failure(k, target, amount, text); }

    static List<QuestTemplate> builtIns() {
        List<QuestTemplate> t = new ArrayList<>();
        // ---- villages
        t.add(new B("village_food_shortage", Category.ECONOMY).when(ConditionKind.SCARCITY).title("Hambre en {settlement}")
                .story("Los graneros de {settlement} están casi vacíos. {giver} teme que no lleguen al final de la semana.",
                        "Hace falta comida: {quantity} raciones de lo que sea que alimente a la gente.",
                        "La escasez empeora: ahora hacen falta más raciones.",
                        "La gente de {settlement} vuelve a comer tranquila.",
                        "{settlement} recordará quién la ayudó cuando tuvo hambre.")
                .obj(o(ObjectiveType.DELIVER, "Entregar {quantity} de comida en {settlement}", "food", "{quantity}", "settlement"))
                .obj(o(ObjectiveType.TALK, "Convencer a un mercader de {neighbour} de enviar una caravana", "merchant", "1", "neighbour").on(Path.DIPLOMATIC))
                .branch(Path.PEACEFUL, "llevar la comida uno mismo", 0, -1, 1.0).branch(Path.DIPLOMATIC, "negociar un envío con la aldea vecina", 40, 0, 0.8)
                .reward(RewardSpec.coins("{value}*0.6")).reward(RewardSpec.reputation("VILLAGE", "0.08")).reward(RewardSpec.relationship("1"))
                .then(ok(ConsequenceSpec.Kind.RENOWN, "settlement", "1", "el hambre pasó")).then(ok(ConsequenceSpec.Kind.HISTORY, "ECONOMY", "0.45", "Un forastero salvó a {settlement} del hambre"))
                .then(ko(ConsequenceSpec.Kind.UNREST, "settlement", "0.15", "el hambre sigue")).then(ko(ConsequenceSpec.Kind.RENOWN, "settlement", "-2", "hambruna"))
                .days(3, 6).givers("farmer", "cook", "merchant").weight(2).mergeable().build());
        t.add(new B("workshop_needs_material", Category.ECONOMY).when(ConditionKind.WORKSHOP_STARVED).title("{giverProfessionName} sin {resourceName}")
                .story("{giver} tiene la fragua fría: le falta {resourceName}.", "Hacen falta {quantity} de {resourceName} para volver al trabajo.",
                        "Otro encargo se suma: ahora necesita más.", "El taller vuelve a sonar.", "Con el material, {settlement} vuelve a tener herramientas.")
                .obj(o(ObjectiveType.TALK, "Hablar con {giver}", "giver", "1", "settlement"))
                .obj(o(ObjectiveType.DELIVER, "Entregar {quantity} de {resourceName}", "{resource}", "{quantity}", "settlement"))
                .reward(RewardSpec.items("{product}", "{productQuantity}")).reward(RewardSpec.coins("{value}*0.3")).reward(RewardSpec.relationship("1"))
                .then(ok(ConsequenceSpec.Kind.RENOWN, "settlement", "0.5", "el taller trabaja")).then(ko(ConsequenceSpec.Kind.UNREST, "settlement", "0.05", "sin herramientas"))
                .days(3, 5).givers("blacksmith", "carpenter", "cook").weight(1.5).follow("forged_gift").mergeable().build());
        t.add(new B("forged_gift", Category.STORY).when(ConditionKind.CUSTOM).title("El regalo de {giver}")
                .story("{giver} quiere agradecer la ayuda con algo hecho con sus manos.", "Ve a verle.", "", "{giver} te entrega su trabajo.", "")
                .obj(o(ObjectiveType.TALK, "Visitar a {giver}", "giver", "1", "settlement"))
                .reward(RewardSpec.items("tools", "1")).reward(RewardSpec.relationship("1")).days(5, 5).weight(0).build());
        t.add(new B("guards_need_help", Category.COMBAT).when(ConditionKind.GUARD_SHORTAGE, ConditionKind.BANDITS).title("La guardia de {settlement} pide ayuda")
                .story("{giver} no tiene hombres suficientes para vigilar {settlement}.", "Patrulla los alrededores y ocúpate de lo que encuentres.",
                        "Hay más peligro del que parecía.", "Los alrededores vuelven a ser seguros.", "La guardia no olvida a quien la apoya.")
                .obj(o(ObjectiveType.INVESTIGATE, "Patrullar el camino del norte", "", "1", "settlement_north").within(20))
                .obj(o(ObjectiveType.INVESTIGATE, "Patrullar el camino del sur", "", "1", "settlement_south").within(20))
                .obj(o(ObjectiveType.COMBAT, "Abatir {threats} amenazas cerca de {settlement}", "hostile", "{threats}", "settlement").within(96))
                .reward(RewardSpec.coins("25")).reward(RewardSpec.reputation("GUARDS", "0.1"))
                .then(ok(ConsequenceSpec.Kind.RESOLVE_EVENT, "eventId", "1", "patrulla"))
                .days(2, 3).givers("guard", "samurai").weight(1.5).build());
        t.add(new B("defend_village", Category.COMBAT).when(ConditionKind.ATTACK).title("¡Defender {settlement}!")
                .story("{settlement} está siendo atacada. {giver} pide a cualquiera que sepa luchar que se una.", "Resiste en la aldea hasta que el ataque termine.",
                        "Llegan más atacantes.", "El ataque ha sido rechazado.", "Las familias de {settlement} contarán esta defensa durante años.")
                .obj(o(ObjectiveType.DEFEND, "Estar en {settlement} hasta que termine el ataque", "{eventId}", "1", "settlement").within(64))
                .obj(o(ObjectiveType.COMBAT, "Abatir atacantes", "hostile", "3", "settlement").within(80).asOptional())
                .obj(o(ObjectiveType.TALK, "Llevar a los ancianos al templo", "citizen", "3", "settlement").on(Path.STEALTH))
                .branch(Path.HONOR, "luchar en primera línea", 0, -1, 1.2).branch(Path.STEALTH, "poner a salvo a los vecinos", 0, -1, 1.0)
                .reward(RewardSpec.coins("40")).reward(RewardSpec.reputation("VILLAGE", "0.15")).reward(RewardSpec.reputation("GUARDS", "0.1"))
                .then(ok(ConsequenceSpec.Kind.RESOLVE_EVENT, "eventId", "1", "defensa")).then(ok(ConsequenceSpec.Kind.MEMORY, "HONOR_OBSERVED", "1", "defendió la aldea"))
                .then(ok(ConsequenceSpec.Kind.HISTORY, "BATTLE", "0.7", "{player} defendió {settlement}")).then(ok(ConsequenceSpec.Kind.FAMILY_HONOR, "giver", "1", "defendió el hogar"))
                .then(ko(ConsequenceSpec.Kind.LOOT, "settlement", "0.3", "saqueo")).then(ko(ConsequenceSpec.Kind.RENOWN, "settlement", "-3", "la aldea cayó"))
                .days(1, 1).givers("guard", "samurai").weight(3).build());
        t.add(new B("fire_aid", Category.STORY).when(ConditionKind.FIRE).title("Reconstruir tras el fuego")
                .story("El fuego arrasó parte de {settlement}. {giver} mira las cenizas.", "Hace falta madera para reconstruir: {quantity}.",
                        "", "Las casas vuelven a levantarse.", "{settlement} no olvida quién trajo madera tras el incendio.")
                .obj(o(ObjectiveType.DELIVER, "Entregar {quantity} de madera", "wood", "{quantity}", "settlement"))
                .obj(o(ObjectiveType.TALK, "Consolar a las familias afectadas", "citizen", "2", "settlement").asOptional())
                .reward(RewardSpec.coins("{value}*0.4")).reward(RewardSpec.reputation("VILLAGE", "0.08"))
                .then(ok(ConsequenceSpec.Kind.RENOWN, "settlement", "1", "reconstrucción")).then(ko(ConsequenceSpec.Kind.UNREST, "settlement", "0.1", "sin reconstruir"))
                .days(3, 7).givers("carpenter", "farmer").weight(1.5).build());
        t.add(new B("housing", Category.ECONOMY).when(ConditionKind.HOUSING_SHORTAGE).title("Una casa más para {settlement}")
                .story("En {settlement} hay gente durmiendo donde puede. {giver} quiere levantar otra casa.", "Hacen falta {quantity} tablones y piedra.",
                        "", "La nueva casa tiene tejado.", "Una familia duerme bajo techo gracias a ti.")
                .obj(o(ObjectiveType.DELIVER, "Entregar {quantity} tablones", "lumber", "{quantity}", "settlement"))
                .obj(o(ObjectiveType.DELIVER, "Entregar {stone} piedras", "stone", "{stone}", "settlement"))
                .reward(RewardSpec.coins("30")).reward(RewardSpec.reputation("VILLAGE", "0.1"))
                .then(ok(ConsequenceSpec.Kind.BUILD_HOUSE, "settlement", "1", "nueva casa")).then(ok(ConsequenceSpec.Kind.HISTORY, "CONSTRUCTION", "0.35", "Se construye una casa nueva en {settlement}"))
                .days(4, 10).givers("carpenter").weight(1).build());
        t.add(new B("festival_preparation", Category.STORY).when(ConditionKind.FESTIVAL_SOON).title("Preparativos para {festival}")
                .story("{festival} se acerca y {giver} quiere que {settlement} lo celebre como merece.", "Faltan {quantity} de {resourceName} para la fiesta.",
                        "", "Todo está listo para {festival}.", "La fiesta de este año se recordará.")
                .obj(o(ObjectiveType.DELIVER, "Entregar {quantity} de {resourceName}", "{resource}", "{quantity}", "settlement"))
                .obj(o(ObjectiveType.TALK, "Avisar al templo", "priest", "1", "temple").asOptional())
                .reward(RewardSpec.coins("{value}*0.5")).reward(RewardSpec.reputation("VILLAGE", "0.06"))
                .then(ok(ConsequenceSpec.Kind.RENOWN, "settlement", "1.5", "gran fiesta"))
                .days(4, 6).givers("merchant", "monk", "cook").weight(1).build());
        // ---- temples
        t.add(new B("temple_ritual", Category.RELIGION).when(ConditionKind.TEMPLE_RITUAL).title("El rito de la luna llena")
                .story("{giver} prepara el rito de la luna llena y necesita manos puras.", "Trae {quantity} hierbas y medita en el templo cuando la luna esté llena.",
                        "", "El rito se ha cumplido.", "Los monjes hablarán de ti con respeto.")
                .obj(o(ObjectiveType.DELIVER, "Entregar {quantity} hierbas", "herbs", "{quantity}", "temple"))
                .obj(o(ObjectiveType.MEDITATE, "Meditar en el templo", "", "300", "temple").within(12))
                .reward(RewardSpec.knowledge("{giver} te enseña la historia del templo de {settlement}")).reward(RewardSpec.reputation("TEMPLE", "0.12")).reward(RewardSpec.reputation("MONKS", "0.08"))
                .then(ok(ConsequenceSpec.Kind.MEMORY, "ATTENDED_RITUAL", "1", "participó en el rito"))
                .days(5, 8).givers("monk").weight(1.2).build());
        t.add(new B("temple_relic", Category.MYSTERY).when(ConditionKind.HEIRLOOM_LOST, ConditionKind.EXPLORATION).title("La reliquia perdida")
                .story("{giver} cuenta que una reliquia desapareció hace tiempo, quizá en {placeName}.", "Investiga el lugar.", "Alguien más la busca.", "La reliquia vuelve a su sitio.", "")
                .obj(o(ObjectiveType.INVESTIGATE, "Investigar {placeName}", "", "1", "place").within(24))
                .obj(o(ObjectiveType.TALK, "Devolver lo encontrado a {giver}", "giver", "1", "settlement"))
                .reward(RewardSpec.reputation("TEMPLE", "0.1")).reward(RewardSpec.knowledge("la historia de la reliquia"))
                .then(ok(ConsequenceSpec.Kind.FAMILY_HONOR, "family", "1", "reliquia recuperada")).then(ok(ConsequenceSpec.Kind.FAMILY_MEMORY, "family", "1", "recuperó la reliquia familiar"))
                .days(5, 10).givers("monk").weight(1).build());
        // ---- trade
        t.add(new B("escort_caravan", Category.ESCORT).when(ConditionKind.LOST_CARAVAN, ConditionKind.BANDITS).title("Escoltar la caravana hacia {destinationName}")
                .story("La última caravana de {settlement} no llegó. {giver} no se arriesga otra vez sin escolta.", "Acompaña la próxima caravana hasta {destinationName}.",
                        "La caravana ha sido emboscada: recupera la carga.", "La caravana llegó entera.", "Los mercaderes pagan bien a quien protege sus rutas.")
                .obj(o(ObjectiveType.ESCORT, "Acompañar la caravana hasta {destinationName}", "caravan", "1", "destination").within(48))
                .reward(RewardSpec.coins("35")).reward(RewardSpec.reputation("MERCHANTS", "0.12"))
                .then(ok(ConsequenceSpec.Kind.RENOWN, "settlement", "0.5", "comercio seguro")).then(ok(ConsequenceSpec.Kind.HISTORY, "TRADE", "0.35", "{player} escoltó una caravana de {settlement}"))
                .days(3, 6).givers("merchant").weight(1.5).build());
        t.add(new B("open_trade_route", Category.COMBAT).when(ConditionKind.ROUTE_BLOCKED, ConditionKind.BANDITS).title("Reabrir el camino a {destinationName}")
                .story("El camino entre {settlement} y {destinationName} está cortado. {giver} pierde dinero cada día.", "Ve a ver qué pasa en el camino.",
                        "Hay más bandidos de los que se creía.", "El camino vuelve a estar abierto.", "El comercio vuelve a fluir.")
                .obj(o(ObjectiveType.INVESTIGATE, "Inspeccionar el tramo cortado", "", "1", "route").within(40))
                .obj(o(ObjectiveType.COMBAT, "Expulsar a los bandidos", "hostile", "4", "route").within(96).on(Path.VIOLENT))
                .obj(o(ObjectiveType.TALK, "Negociar el paso con el capitán de la guardia", "guard", "1", "settlement").on(Path.DIPLOMATIC))
                .obj(o(ObjectiveType.INVESTIGATE, "Encontrar un atajo por el monte", "", "1", "detour").within(30).on(Path.STEALTH))
                .branch(Path.VIOLENT, "limpiar el camino por la fuerza", 0, -1, 1.1).branch(Path.DIPLOMATIC, "pagar y negociar el paso", 30, 0, 0.9)
                .branch(Path.STEALTH, "abrir una senda alternativa", 0, -1, 1.0)
                .reward(RewardSpec.coins("50")).reward(RewardSpec.reputation("MERCHANTS", "0.15"))
                .then(ok(ConsequenceSpec.Kind.RESOLVE_EVENT, "eventId", "1", "camino despejado").on(Path.VIOLENT))
                .then(ok(ConsequenceSpec.Kind.RESOLVE_EVENT, "eventId", "1", "paso negociado").on(Path.DIPLOMATIC))
                .then(ok(ConsequenceSpec.Kind.BUILD_ROAD, "destination", "1", "senda alternativa").on(Path.STEALTH))
                .then(ok(ConsequenceSpec.Kind.HISTORY, "TRADE", "0.5", "Se reabre la ruta entre {settlement} y {destinationName}"))
                .days(4, 8).givers("merchant", "guard").weight(2).build());
        // ---- war
        t.add(new B("war_reconnaissance", Category.INVESTIGATION).when(ConditionKind.WAR).title("Reconocimiento: {regionName}")
                .story("La guerra ha llegado a {regionName}. {giver} necesita saber dónde está el enemigo.", "Recorre la región y vuelve con noticias.",
                        "", "Tus noticias llegan a tiempo.", "Gracias a tu informe, la aldea se prepara.")
                .obj(o(ObjectiveType.INVESTIGATE, "Observar el centro de {regionName}", "", "1", "region").within(64))
                .obj(o(ObjectiveType.TALK, "Informar a {giver}", "giver", "1", "settlement"))
                .reward(RewardSpec.coins("30")).reward(RewardSpec.reputation("GUARDS", "0.1")).days(2, 4).givers("samurai", "guard").weight(2).build());
        t.add(new B("capture_bridge", Category.COMBAT).when(ConditionKind.WAR).title("Tomar el paso de {regionName}")
                .story("El enemigo controla el paso. {giver} planea recuperarlo.", "Despeja el paso de enemigos.", "El enemigo recibe refuerzos.", "El paso es nuestro.", "La guerra da un giro.")
                .obj(o(ObjectiveType.COMBAT, "Abatir a los defensores del paso", "hostile", "6", "route").within(96))
                .reward(RewardSpec.coins("60")).reward(RewardSpec.reputation("CLAN", "0.2"))
                .then(ok(ConsequenceSpec.Kind.RESOLVE_EVENT, "eventId", "1", "paso tomado")).then(ok(ConsequenceSpec.Kind.HISTORY, "WAR", "0.8", "{player} tomó el paso de {regionName}"))
                .days(2, 5).givers("samurai").weight(0.5).build());
        // ---- people: relationships, memory, families
        t.add(new B("gratitude_gift", Category.STORY).when(ConditionKind.GRATITUDE).title("{giver} quiere darte las gracias")
                .story("{giver} no ha olvidado lo que hiciste.", "Ve a verle cuando puedas.", "", "{giver} te cuenta algo que pocos saben.", "")
                .obj(o(ObjectiveType.TALK, "Visitar a {giver}", "giver", "1", "settlement"))
                .reward(RewardSpec.knowledge("{secret}")).reward(RewardSpec.relationship("1")).reward(RewardSpec.coins("10")).days(10, 10).weight(1).trust(55).build());
        t.add(new B("resolve_dispute", Category.DIPLOMACY).when(ConditionKind.DISPUTE, ConditionKind.GRUDGE).title("Una disputa en {settlement}")
                .story("{giver} y {otherName} no se hablan desde hace tiempo.", "Habla con los dos y decide cómo resolverlo.", "", "La disputa se resolvió.", "")
                .obj(o(ObjectiveType.TALK, "Escuchar a {giver}", "giver", "1", "settlement"))
                .obj(o(ObjectiveType.TALK, "Escuchar a {otherName}", "{other}", "1", "settlement"))
                .branch(Path.DIPLOMATIC, "reconciliarlos", 0, -1, 1.0).branch(Path.HONOR, "dar la razón a quien la tiene", 0, -1, 1.0)
                .reward(RewardSpec.reputation("VILLAGE", "0.06")).then(ok(ConsequenceSpec.Kind.RENOWN, "settlement", "0.5", "paz entre vecinos")).days(5, 7).weight(1).build());
        t.add(new B("rescue_relative", Category.STORY).when(ConditionKind.MISSING_PERSON).title("¿Dónde está {personName}?")
                .story("{giver} no sabe nada de {personName} desde hace días.", "Busca en {placeName}, donde se le vio por última vez.",
                        "Hay rastros de lucha.", "{personName} está a salvo.", "La familia {familyName} no lo olvidará.")
                .obj(o(ObjectiveType.INVESTIGATE, "Buscar en {placeName}", "", "1", "place").within(32))
                .obj(o(ObjectiveType.TALK, "Dar noticias a {giver}", "giver", "1", "settlement"))
                .reward(RewardSpec.coins("20")).reward(RewardSpec.relationship("2"))
                .then(ok(ConsequenceSpec.Kind.FAMILY_MEMORY, "family", "1", "un forastero buscó a {personName}")).days(3, 5).weight(1.5).build());
        t.add(new B("recover_heirloom", Category.MYSTERY).when(ConditionKind.HEIRLOOM_LOST).title("{heirloomName} de la familia {familyName}")
                .story("{heirloomName}, que pasó de generación en generación en la familia {familyName}, ha desaparecido.", "Sigue la pista en {placeName}.",
                        "Alguien intentó venderla.", "{heirloomName} vuelve a la familia.", "La familia {familyName} recupera su honor.")
                .obj(o(ObjectiveType.INVESTIGATE, "Buscar pistas en {placeName}", "", "1", "place").within(24))
                .obj(o(ObjectiveType.TALK, "Devolver {heirloomName} a {giver}", "giver", "1", "settlement"))
                .reward(RewardSpec.coins("25")).reward(RewardSpec.relationship("2")).reward(RewardSpec.reputation("CLAN", "0.1"))
                .then(ok(ConsequenceSpec.Kind.FAMILY_HONOR, "family", "2", "herencia recuperada")).then(ok(ConsequenceSpec.Kind.FAMILY_MEMORY, "family", "1", "{player} recuperó {heirloomName}"))
                .days(5, 10).weight(1.5).build());
        t.add(new B("restore_honor", Category.STORY).when(ConditionKind.FAMILY_DISHONOR).title("El honor de la familia {familyName}")
                .story("Desde lo ocurrido, la familia {familyName} no levanta la cabeza en {settlement}.", "Ayuda a {giver} a reparar la falta ante la comunidad.",
                        "", "La comunidad vuelve a saludar a los {familyName}.", "")
                .obj(o(ObjectiveType.TALK, "Hablar con el anciano del templo", "priest", "1", "temple"))
                .obj(o(ObjectiveType.DELIVER, "Llevar una ofrenda de {quantity} de arroz", "rice", "{quantity}", "temple"))
                .branch(Path.SPIRITUAL, "ofrenda y oración", 0, -1, 1.0).branch(Path.HONOR, "reparación pública", 0, -1, 1.1)
                .reward(RewardSpec.relationship("2")).then(ok(ConsequenceSpec.Kind.FAMILY_HONOR, "family", "3", "honor restaurado")).days(7, 14).weight(1).build());
        t.add(new B("train_disciple", Category.TRAINING).when(ConditionKind.MENTOR_WANTED).title("{master} busca un discípulo")
                .story("{master} envejece y nadie ha aprendido su técnica.", "Presenta a {master} un joven prometedor de {settlement}.", "", "La técnica tendrá quien la herede.", "")
                .obj(o(ObjectiveType.TALK, "Hablar con {master}", "master", "1", "settlement"))
                .obj(o(ObjectiveType.TALK, "Convencer a {disciple}", "disciple", "1", "settlement"))
                .reward(RewardSpec.reputation("CLAN", "0.08")).reward(RewardSpec.relationship("1"))
                .then(ok(ConsequenceSpec.Kind.MENTORSHIP, "master", "0.2", "empieza el aprendizaje")).days(7, 14).givers("samurai", "blacksmith", "monk").weight(1).build());
        // ---- exploration
        t.add(new B("explore_region", Category.EXPLORATION).when(ConditionKind.EXPLORATION).title("Tierras desconocidas: {regionName}")
                .story("Nadie en {settlement} sabe qué hay en {regionName}.", "Explora la región y cuenta lo que veas.", "", "Ahora {settlement} sabe qué hay allí.", "")
                .obj(o(ObjectiveType.INVESTIGATE, "Llegar al corazón de {regionName}", "", "1", "region").within(96))
                .obj(o(ObjectiveType.TALK, "Contarlo en {settlement}", "giver", "1", "settlement"))
                .reward(RewardSpec.coins("15")).reward(RewardSpec.knowledge("los caminos de {regionName}")).days(7, 10).weight(0.7).build());
        return t;
    }

    // ------------------------------------------------------------------ configured templates

    private void parse(String line) {
        var parsed = Segments.parse(line);
        if (parsed.isEmpty()) { problems.add("template line '" + line + "' has no id"); return; }
        Segments s = parsed.get();
        String id = s.id().toLowerCase(Locale.ROOT);
        if (s.text("disabled", "false").equalsIgnoreCase("true")) { templates.remove(id); return; }
        Category category;
        try { category = Category.valueOf(s.text("category", "STORY").toUpperCase(Locale.ROOT)); } catch (IllegalArgumentException e) { problems.add(id + ": unknown category"); return; }
        B b = new B(id, category).title(s.text("title", id)).days(s.integer("offer", 3), s.integer("duration", 5)).weight(s.number("weight", 1));
        for (String k : s.list("when")) {
            try { b.when(ConditionKind.valueOf(k.toUpperCase(Locale.ROOT))); } catch (IllegalArgumentException e) { problems.add(id + ": unknown condition " + k); }
        }
        String text = s.text("text", "");
        b.story(text, text, "", s.text("ending", ""), "");
        if (s.has("objective")) {
            String[] p = s.text("objective", "").split(":");
            try {
                ObjectiveType type = ObjectiveType.valueOf(p[0].toUpperCase(Locale.ROOT));
                b.obj(o(type, s.text("objectiveText", type.name().toLowerCase(Locale.ROOT)), p.length > 1 ? p[1] : "", p.length > 2 ? p[2] : "1", p.length > 3 ? p[3] : "settlement"));
            } catch (RuntimeException e) { problems.add(id + ": bad objective " + s.text("objective", "")); }
        }
        if (s.has("reward")) {
            String[] p = s.text("reward", "").split(":");
            try { b.reward(new RewardSpec(RewardSpec.Kind.valueOf(p[0].toUpperCase(Locale.ROOT)), p.length > 1 ? p[1] : "1", p.length > 2 ? p[2] : "", "")); }
            catch (RuntimeException e) { problems.add(id + ": bad reward"); }
        }
        if (s.has("reputation")) {
            String[] p = s.text("reputation", "").split(":");
            b.reward(RewardSpec.reputation(p[0], p.length > 1 ? p[1] : "0.05"));
        }
        for (String g : s.list("givers")) b.givers(g.toLowerCase(Locale.ROOT));
        for (String season : s.list("seasons")) Season.parse(season).ifPresent(b::seasons);
        if (b.when.isEmpty()) { problems.add(id + ": no condition (when=...)"); return; }
        templates.put(id, b.build());
    }
}
