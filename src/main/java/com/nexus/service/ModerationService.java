package com.nexus.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ModerationService {

    // 1. Lista base de palabras prohibidas (limpias, en minúscula y sin tildes)
    // Puedes añadir las que quieras, el sistema generará sus variantes automáticamente.
	private static final String[] PALABRAS_BASE = {
		    // ══════════════════════════════════════════
		    // SEXUAL / PORNOGRÁFICO (ESPAÑOL)
		    // ══════════════════════════════════════════
		    "follar", "follando", "follada", "follados", "folladas", "follame", "follatelo", "follazo",
		    "sexo", "sexual", "sexualidad", "sexuales", "sexotico", "sexoterapia",
		    "pornografia", "porno", "porn", "erotico", "erotica", "erotismo", "eroticismo",
		    "pene", "penes", "polla", "pollas", "pollon", "pollones", "pito", "pirula", "pirulo", "nabo",
		    "vagina", "vaginas", "vulva", "vulvas", "clitoris",
		    "coño", "coños", "conejo", "chocho", "pepita",
		    "cojon", "cojones", "huevo", "huevos", "testiculos", "escroto",
		    "masturbacion", "masturbar", "masturbarse", "masturbando", "paja", "pajas", "pajero", "pajera",
		    "felacion", "mamada", "mamar", "chupar", "lamer", "correrse", "corrida",
		    "orgasmo", "orgasmos", "eyacular", "eyaculacion", "semen", "esperma",
		    "anal", "anales", "culo", "culos", "ano", "anos", "sodomia",
		    "prostituta", "prostitutas", "prostituto", "prostitutos", "prostitucion",
		    "puta", "putas", "puto", "putos", "putita", "putito", "putazo", "putaza",
		    "escort", "escorts", "meretriz",
		    "gangbang", "hardcore", "softcore", "bdsm", "bondage", "fetiche", "fetiches",
		    "sadomasoquismo", "sadismo", "masoquismo", "dominacion", "sumision",
		    "desnudo", "desnuda", "desnudos", "desnudas", "desnudez",
		    "hentai", "voyeur", "exhibicionismo", "striptease", "stripper", "strip",
		    "pornoamateur", "sexvideo", "sexcam", "camgirl", "camboy", "webcamsex",
		    "pechos", "tetas", "teton", "tetona", "tetonas", "senos", "busto",
		    "nalgas", "pompas", "trasero", "retaguardia",
		    "penetracion", "penetrar", "coito", "copula", "copular",
		    "orgias", "orgia", "threesome", "menage",
		    "sexooral", "sexoanal", "sexovaginal", "cunnilingus", "anilingus",
		    "fetichismo", "zoofilia", "necrofilia", "incesto",
		    "sexshop", "juguetessexuales", "dildo", "vibrador", "consolador",
		    "lesbiana", "lesbianas", "lesbico", "lesbica",
		    "bisexual", "bisexuales", "transexual", "transexuales",
		    "fornicacion", "fornicando", "fornicador",
		    "lujuria", "lujurioso", "lujuriosa", "lascivia", "lascivo", "lasciva",
		    "putrefaccion", "obsceno", "obscena", "obscenidad", "indecente", "indecencia",
		    "pornochico", "pornomujer", "pornohombre",
		    "swinger", "swingers", "intercambio de parejas",
		    "eyaculacion precoz", "impotencia sexual",
		    "mamada", "mamando", "chupada", "chupando",
		    "culiado", "culear", "culiao", "culeando",

		    // ══════════════════════════════════════════
		    // SEXUAL / PORNOGRÁFICO (INGLÉS)
		    // ══════════════════════════════════════════
		    "fuck", "fucking", "fucked", "fucker", "fuckers", "fuckup", "fuckface", "motherfucker", "motherfucking",
		    "shit", "shits", "shitting", "shitty", "bullshit", "horseshit", "apeshit",
		    "ass", "asshole", "assholes", "jackass", "dumbass", "badass", "smartass", "fatass", "lardass",
		    "dick", "dicks", "dickhead", "dickheads", "dickface", "cock", "cocks", "cocksucker", "cocksucking",
		    "pussy", "pussies",
		    "cunt", "cunts",
		    "bitch", "bitches", "bitching", "bitchy", "son of a bitch",
		    "whore", "whores", "slut", "sluts", "slutty", "skank", "skanky",
		    "blowjob", "blowjobs", "handjob", "handjobs", "rimjob", "rimjobs",
		    "boobs", "boob", "tits", "titties", "nipple", "nipples",
		    "penis", "penises", "vagina", "vaginas", "vulva",
		    "clitoris", "clit",
		    "cum", "cumming", "cumshot", "creampie",
		    "orgasm", "orgasms", "masturbate", "masturbating", "masturbation", "jerk off", "jerking off",
		    "horny", "naughty", "kinky",
		    "porn", "pornography", "porno", "pornographic",
		    "naked", "nude", "nudity",
		    "erection", "boner",
		    "testicles", "balls", "ballsack", "scrotum",
		    "dildo", "vibrator",
		    "anal", "butt", "butthole", "butt crack",
		    "rape", "raping", "rapist", "rapists",
		    "sexy", "sex", "sexual",
		    "hooker", "hookers", "prostitute", "prostitutes", "prostitution",
		    "pimp", "pimping",
		    "threesome", "foursome",
		    "orgy", "orgies",
		    "fetish", "fetishes",
		    "bondage", "bdsm",
		    "softcore", "hardcore",
		    "hentai", "ecchi",
		    "erotic", "erotica",
		    "pervert", "perverts", "pervy",
		    "incest", "necrophilia", "bestiality", "zoophilia",
		    "pedophile", "pedophiles", "pedophilia",
		    "nympho", "nymphomaniac",
		    "exhibitionist", "voyeur",
		    "stripper", "striptease",
		    "camgirl", "camboy",
		    "jizz", "sperm", "semen",
		    "fingering", "fisting",
		    "deepthroat",
		    "gangbang",
		    "creep", "creeper", "creepy",
		    "wank", "wanking", "wanker",
		    "shag", "shagging",
		    "bonk", "bonking",
		    "screw", "screwing",
		    "smut", "lewd",
		    "lust", "lustful",
		    "nymphomaniac",

		    // ══════════════════════════════════════════
		    // INSULTOS / DESPRECIO (ESPAÑOL)
		    // ══════════════════════════════════════════
		    "zorra", "zorras", "zorron", "zorrilla", "zorritas",
		    "perra", "perras", "perraco",
		    "cabron", "cabrona", "cabrones", "cabroncete", "cabronazo", "cabroncito",
		    "gilipollas", "gilipollez", "gilipollitas", "gilipolleces",
		    "maricon", "maricona", "mariconazo", "maricones", "mariconsete",
		    "idiota", "idiotas", "idiotazo", "idiotez",
		    "imbecil", "imbeciles",
		    "estupido", "estupida", "estupidos", "estupidas",
		    "tonto", "tonta", "tontos", "tontas", "tontolaba", "tontorrón",
		    "subnormal", "subnormales",
		    "capullo", "capullos",
		    "bastardo", "bastarda", "bastardos", "bastardas",
		    "malparido", "malparida",
		    "hijoputa", "hijaputa", "hijo de puta", "hija de puta", "hijos de puta",
		    "escoria",
		    "desgraciado", "desgraciada", "desgraciados", "desgraciadas",
		    "cretino", "cretina", "cretinos", "cretinas", "cretinazo",
		    "tarado", "tarada", "tarados", "taradas",
		    "pendejo", "pendeja", "pendejos", "pendejas", "pendejada",
		    "mierda", "mierdas", "mierdoso", "mierdosa",
		    "maldito", "maldita", "malditos", "malditas",
		    "cojonudo", "cojonuda",
		    "despreciable", "repugnante", "vomitivo", "detestable", "repelente",
		    "infeliz", "infelices",
		    "flaite",
		    "zoquete", "zopenco", "zopencos",
		    "cerdo", "cerda", "cerdos", "cerdas", "cerdada",
		    "burro", "burra", "burros", "burras", "burrada",
		    "guarro", "guarra", "guarros", "guarras",
		    "mugre", "mugriento", "mugrienta",
		    "choto", "chota", "chotos", "chotas",
		    "rata", "raton",
		    "perdedor", "perdedora", "perdedores", "perdedoras",
		    "fracasado", "fracasada", "fracasados", "fracasadas",
		    "feo", "fea", "feos", "feas", "feazo", "feaza",
		    "bruto", "bruta", "brutos", "brutas",
		    "animal", "bestia", "bestias", "salvaje", "salvajes",
		    "retrasado", "retrasada", "retrasados", "retrasadas",
		    "mongolico", "mongolica",
		    "palurdo", "palurda",
		    "cateto", "cateta",
		    "memo", "memos", "mema", "memas",
		    "pelmazo", "pelmazos",
		    "pesado", "pesada",
		    "cagon", "cagona", "cagones",
		    "bocazas",
		    "boquifloja", "boquiflojo",
		    "mentecato", "mentecata",
		    "chalado", "chalada",
		    "chiflado", "chiflada",
		    "majadero", "majadora",
		    "torpe", "torpes",
		    "necio", "necia", "necios", "necias",
		    "sinverguenza", "sinverguenzas",
		    "caradura", "caraduras",
		    "golfillo", "golfilla",
		    "vago", "vaga", "vagos", "vagas",
		    "holgazan", "holgazana",
		    "inutil", "inutiles",
		    "patan", "patana",
		    "bellaco", "bellaca",
		    "ladron", "ladrona", "ladrones",
		    "mentiroso", "mentirosa", "mentirosos", "mentirosas",
		    "hipocrita", "hipocritas",
		    "cobarde", "cobardes",
		    "cobardica",
		    "traidor", "traidora", "traidores",
		    "ruin", "ruines",
		    "miserable", "miserables",
		    "vil", "viles",
		    "infame", "infames",
		    "abyecto", "abyecta",
		    "pervertido", "pervertida",
		    "degenerado", "degenerada",
		    "troll", "hater",
		    "basura",
		    "pajero", "pajera",

		    // ══════════════════════════════════════════
		    // INSULTOS / DESPRECIO (INGLÉS)
		    // ══════════════════════════════════════════
		    "idiot", "idiots", "idiocy",
		    "moron", "morons", "moronic",
		    "retard", "retards", "retarded",
		    "stupid", "stupids", "stupidity",
		    "dumb", "dumber", "dumbest", "dumbass",
		    "loser", "losers",
		    "jerk", "jerks",
		    "cretin", "cretins", "cretinous",
		    "imbecile", "imbeciles",
		    "dunce", "dunces",
		    "dimwit", "dimwits",
		    "halfwit", "halfwits",
		    "nitwit", "nitwits",
		    "twat", "twats",
		    "wanker", "wankers",
		    "prick", "pricks",
		    "turd", "turds",
		    "jerk off", "jackoff",
		    "scumbag", "scumbags",
		    "scum", "scums",
		    "trash", "trashes",
		    "garbage",
		    "worthless",
		    "pathetic",
		    "disgusting",
		    "repulsive",
		    "vile",
		    "despicable",
		    "loathsome",
		    "detestable",
		    "coward", "cowards",
		    "liar", "liars",
		    "thief", "thieves",
		    "cheater", "cheaters",
		    "traitor", "traitors",
		    "hypocrite", "hypocrites",
		    "freak", "freaks",
		    "weirdo", "weirdos",
		    "pervert", "perverts",
		    "degenerate", "degenerates",
		    "slimeball", "slimeballs",
		    "lowlife", "lowlifes",
		    "vermin",
		    "pond scum",
		    "tool", "tools",
		    "clown", "clowns",
		    "buffoon", "buffoons",
		    "pea brain",
		    "airhead", "airheads",
		    "birdbrain",
		    "blockhead", "blockheads",
		    "bonehead", "boneheads",
		    "knucklehead",
		    "meathead",
		    "numbskull",
		    "fathead",
		    "pinhead",
		    "lamebrain",
		    "nincompoop",
		    "doofus",
		    "dolt", "dolts",
		    "oaf", "oafs",
		    "lout", "louts",
		    "slob", "slobs",
		    "pig", "pigs",
		    "swine",
		    "rat", "rats",
		    "snake", "snakes",
		    "worm", "worms",
		    "toad", "toads",
		    "punk", "punks",
		    "thug", "thugs",
		    "hooligan", "hooligans",
		    "delinquent", "delinquents",
		    "lowlife",
		    "scoundrel", "scoundrels",
		    "villain", "villains",
		    "wretch", "wretches",
		    "sorry excuse",
		    "waste of space",
		    "good for nothing",
		    "no good",
		    "bum", "bums",
		    "deadbeat", "deadbeats",
		    "dirtbag", "dirtbags",
		    "douchebag", "douchebags", "douche",
		    "piss off", "pissed",
		    "damn", "damned",
		    "hell",
		    "crap", "crappy", "craps",
		    "suck", "sucks", "sucking",
		    "hate", "hatred", "hater", "haters",

		    // ══════════════════════════════════════════
		    // INSULTOS RACISTAS / DISCRIMINATORIOS
		    // ══════════════════════════════════════════
		    "negro", "negros", "negra", "negras", // en contexto ofensivo
		    "moro", "moros", "mora", "moras",
		    "sudaca", "sudacas",
		    "chino", "chinos",
		    "paki", "pakis",
		    "spic", "spics",
		    "wetback", "wetbacks",
		    "beaner", "beaners",
		    "gook", "gooks",
		    "chink", "chinks",
		    "jap", "japs",
		    "towelhead", "towelheads",
		    "sandnigger",
		    "nigger", "niggers", "nigga", "niggas",
		    "cracker", "crackers",
		    "honky", "honkies",
		    "kike", "kikes",
		    "spook", "spooks",
		    "coon", "coons",
		    "sambo",
		    "zipperhead",
		    "slope",
		    "raghead",
		    "camel jockey",
		    "gyp", "gyps", "gypsy", // when used pejoratively
		    "kraut", "krauts",
		    "frog", // as slur
		    "limey",
		    "gringo", "gringos",
		    "gabacho", "gabachos",
		    "pocho", "pochos",
		    "indio", "indios", // cuando es peyorativo
		    "gitano", "gitanos", // cuando es peyorativo

		    // ══════════════════════════════════════════
		    // HOMOFOBIA / TRANSFOBIA
		    // ══════════════════════════════════════════
		    "fag", "fags", "faggot", "faggots", "faggotry",
		    "dyke", "dykes",
		    "queer", // cuando es ofensivo
		    "tranny", "trannies",
		    "shemale", "shemales",
		    "ladyboy",
		    "homo", "homos",
		    "sissy", "sissies",
		    "pansy", "pansies",
		    "fairy",
		    "fruity",
		    "mariposa", // cuando es peyorativo
		    "mariquita",
		    "invertido",
		    "sodomita", "sodomitas",
		    "degenerado sexual",

		    // ══════════════════════════════════════════
		    // VIOLENCIA / EXTREMOS (ESPAÑOL)
		    // ══════════════════════════════════════════
		    "violacion", "violar", "violador", "violadores", "violada", "violado",
		    "asesinar", "asesinato", "asesino", "asesina", "asesinos",
		    "matar", "matarte", "matalo", "matala",
		    "degollar", "degollado", "degollina",
		    "decapitar", "decapitacion",
		    "desmembrar", "desmembramiento",
		    "tortura", "torturar", "torturado", "torturador",
		    "suicidio", "suicidarse", "suicidate",
		    "autolesion", "autolesionarse", "autolesionate",
		    "bomba", "bombas", "bombazo",
		    "terrorismo", "terrorista", "terroristas",
		    "secuestrar", "secuestro", "secuestrador",
		    "abusar", "abuso", "abusador",
		    "masacre", "masacrar",
		    "genocidio",
		    "golpear", "golpiza", "golpe",
		    "apuñalar", "apuñalado",
		    "sangre", "sangriento", "sangrienta",
		    "machacar",
		    "aniquilar", "aniquilacion",
		    "homicidio",
		    "ejecutar", "ejecucion",
		    "fusilar", "fusilamiento",
		    "lapidar", "lapidacion",
		    "agredir", "agresion", "agresor",
		    "destruir", "destruirte",
		    "crimen", "crimenes",
		    "criminal", "criminales",
		    "violento", "violenta", "violentos", "violentas",
		    "brutal", "brutalidad",
		    "cruel", "crueldad",
		    "sadico", "sadica",
		    "salvajismo",
		    "carniceria",
		    "matanza",
		    "exterminio",
		    "linchamiento",
		    "linchamientos",
		    "quemar vivo", "quemar viva",
		    "ahorcar", "ahorcado", "ahorcarse", "ahorcate",
		    "disparar", "disparo",
		    "amenazar", "amenaza", "amenazas", "amenazante",
		    "cuchillo", "navaja",
		    "pistola", "arma",
		    "explosivo", "explosivos",

		    // ══════════════════════════════════════════
		    // VIOLENCIA / AMENAZAS (INGLÉS)
		    // ══════════════════════════════════════════
		    "kill", "killing", "killed", "killer", "killers",
		    "murder", "murdering", "murdered", "murderer", "murderers",
		    "rape", "raping", "raped", "rapist", "rapists",
		    "assault", "assaulting", "assaulted", "assaulter",
		    "attack", "attacking", "attacked", "attacker",
		    "shoot", "shooting", "shooter", "shooters",
		    "stab", "stabbing", "stabbed", "stabber",
		    "torture", "torturing", "tortured", "torturer",
		    "bomb", "bombing", "bomber",
		    "explode", "explosion", "explosive",
		    "massacre", "massacring", "massacred",
		    "genocide",
		    "terrorism", "terrorist", "terrorists",
		    "kidnap", "kidnapping", "kidnapped", "kidnapper",
		    "abuse", "abusing", "abused", "abuser",
		    "molest", "molesting", "molested", "molester",
		    "harm", "harming", "harmed",
		    "hurt", "hurting",
		    "threaten", "threatening", "threatened", "threat", "threats",
		    "destroy", "destroying", "destroyed",
		    "eliminate", "eliminating", "eliminated",
		    "exterminate", "exterminating", "exterminated", "extermination",
		    "execute", "executing", "executed", "execution",
		    "slaughter", "slaughtering", "slaughtered",
		    "butcher", "butchering", "butchered",
		    "strangle", "strangling", "strangled", "strangler",
		    "suffocate", "suffocating", "suffocated",
		    "drown", "drowning", "drowned",
		    "poison", "poisoning", "poisoned",
		    "suicide", "suicidal",
		    "self harm", "self-harm", "selfharm",
		    "cut yourself", "cut myself",
		    "hang yourself",
		    "overdose",
		    "lynch", "lynching", "lynched",
		    "behead", "beheading", "beheaded",
		    "decapitate", "decapitation",
		    "dismember", "dismembering", "dismembered",
		    "mutilate", "mutilating", "mutilated", "mutilation",

		    // ══════════════════════════════════════════
		    // IDEOLOGÍA EXTREMA / RACISMO / DISCRIMINACIÓN
		    // ══════════════════════════════════════════
		    "nazi", "nazis", "nazismo", "nazista",
		    "fascista", "fascistas", "fascismo",
		    "racista", "racistas", "racismo",
		    "supremacista", "supremacistas", "supremacismo",
		    "antisemita", "antisemitas", "antisemitismo",
		    "homofobo", "homofoba", "homofobia",
		    "xenofobo", "xenofobia", "xenofobos",
		    "islamofobo", "islamofobia",
		    "antimusulman", "antijudio", "antinegro", "antiblanco",
		    "segregacionista", "segregacionismo",
		    "extremista", "extremistas", "extremismo",
		    "pedofilo", "pedofila", "pedofilia", "pedofilos",
		    "pederasta", "pederastas", "pederastia",
		    "neonazi", "neonazis",
		    "ku klux klan", "kkk",
		    "white supremacy", "white supremacist",
		    "neo nazi",
		    "hate crime", "hate crimes",
		    "ethnic cleansing",
		    "white power",
		    "heil hitler",
		    "aryan", "aryan nation",

		    // ══════════════════════════════════════════
		    // DROGAS / SUSTANCIAS ILEGALES
		    // ══════════════════════════════════════════
		    "drogas", "droga",
		    "cocaina", "coca", "perico", "farla",
		    "heroina",
		    "metanfetamina", "meta", "cristal",
		    "cannabis", "marihuana", "hierba", "mota", "porro", "canuto",
		    "hachis", "hash",
		    "mdma", "extasis", "pastillas",
		    "lsd", "acido",
		    "fentanilo",
		    "crack",
		    "speed", "anfetamina",
		    "ketamina",
		    "mescalina",
		    "psilocibina",
		    "drugs", "drug dealer", "dealer", "dealers",
		    "weed", "weed dealer",
		    "cocaine", "coke", "blow",
		    "heroin", "smack",
		    "meth", "methamphetamine", "crystal meth",
		    "ecstasy", "molly",
		    "acid", "lsd",
		    "overdose",
		    "dope", "doping",
		    "stoned", "high", "wasted", "tripping",
		    "shooting up", "shoot up", "junkie", "junkies",

		    // ══════════════════════════════════════════
		    // AUTOAGRESIÓN / SUICIDIO
		    // ══════════════════════════════════════════
		    "suicidate", "matate", "ahorcate", "cortate", "quitate",
		    "autolesionate", "destruirte",
		    "ahorcarse", "colgarse",
		    "veneno", "envenenarse",
		    "kill yourself", "kys",
		    "go kill yourself",
		    "end your life",
		    "end it all",
		    "worthless go die",

		    // ══════════════════════════════════════════
		    // JERGA OFENSIVA VARIADA
		    // ══════════════════════════════════════════
		    "culo_pedo", "pedo",
		    "pedorro", "pedorra",
		    "flatulencia",
		    "flatulento",
		    "orina", "orinar",
		    "excremento",
		    "defecacion",
		    "heces",
		    "vomitar", "vomito",
		    "asco",
		    "asqueroso", "asquerosa",
		    "muerto de hambre",
		    "mequetrefe",
		    "mammarracho", "mamarracho",
		    "esperpento",
		    "engendro",
		    "bicho raro",
		    "bicho",
		};

    // 2. Patrón compilado dinámicamente una sola vez al arrancar (Ultra rápido)
    private static final Pattern BAD_WORDS_PATTERN = compilarPatronAvanzado();

    /**
     * Construye el Súper-Regex combinando todas las palabras y sus posibles evasiones.
     */
    private static Pattern compilarPatronAvanzado() {
        String regexCombinado = Arrays.stream(PALABRAS_BASE)
            .map(ModerationService::generarRegexParaPalabra)
            .collect(Collectors.joining("|"));

        // (?iu)      -> Insensible a mayúsculas y soporte Unicode (detecta ñ, tildes).
        // (?:^|\P{L}) -> Límite inicial: principio del texto o cualquier carácter que NO sea una letra.
        // (?:$|\P{L}) -> Límite final: fin del texto o cualquier carácter que NO sea una letra.
        // Esto evita el "Scunthorpe problem" (ej. NO bloqueará la palabra "disputa" porque la 's' es una letra).
        return Pattern.compile("(?iu)(?:^|\\P{L})(" + regexCombinado + ")(?:$|\\P{L})");
    }

    /**
     * Convierte la palabra "puta" en -> [p]+[\W_]*[uúùüûv]+[\W_]*[t]+[\W_]*[aáàäâ@4]+
     */
    private static String generarRegexParaPalabra(String palabra) {
        StringBuilder sb = new StringBuilder();
        for (char c : palabra.toCharArray()) {
            sb.append(obtenerVariaciones(c)).append("[\\W_]*");
        }
        // Eliminar el último [\W_]* sobrante (7 caracteres de longitud)
        if (sb.length() >= 7) {
            sb.setLength(sb.length() - 7);
        }
        return sb.toString();
    }

    /**
     * Diccionario de sustitución L33T Speak y tildes.
     */
    private static String obtenerVariaciones(char c) {
        return switch (c) {
            case 'a' -> "[aáàäâ@4]+";      // Cubre 'a', '@', '4', repetidas (aaaa), etc.
            case 'e' -> "[eéèëê3]+";       // Cubre 'e', '3'
            case 'i' -> "[iíìïî1!]+";      // Cubre 'i', '1', '!'
            case 'o' -> "[oóòöô0]+";       // Cubre 'o', '0'
            case 'u' -> "[uúùüûv]+";       // Cubre 'u', y 'v' (para detectar "pvta")
            case 'c' -> "[cçkq]+";         // Cubre 'c', 'k', 'q' (para detectar "kabron")
            case 's' -> "[s5z]+";          // Cubre 's', '5', 'z'
            case 'b', 'v' -> "[bv]+";      // Cubre intercambios b/v
            case 'n' -> "[nñ]+";           // Cubre la ñ
            default  -> "[" + c + "]+";    // Cualquier otra letra admite repeticiones (ej. [p]+)
        };
    }

    /**
     * Verifica si el texto cumple con las normas de la comunidad.
     * @param texto El texto a analizar.
     * @return true si es limpio, false si es inapropiado.
     */
    public boolean esContenidoApropiado(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return true;
        }

        Matcher matcher = BAD_WORDS_PATTERN.matcher(texto);
        if (matcher.find()) {
            // Descomenta esto en desarrollo si quieres ver exactamente qué palabra hizo saltar el filtro:
            // System.out.println("🚨 Moderación: Bloqueado por coincidencia exacta -> '" + matcher.group().trim() + "'");
            return false;
        }
        return true;
    }

    /**
     * Método asíncrono para moderar imágenes.
     */
    @Async
    public CompletableFuture<Boolean> esImagenApropiada(String imageUrl) {
        // Preparado para conectar a la API de Moderación de Cloudinary u otra IA visual
        return CompletableFuture.completedFuture(true);
    }
}