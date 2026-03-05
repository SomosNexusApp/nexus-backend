package com.nexus.service;

import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.util.*;

@Service
public class SynonymService {

    private static final Map<String, List<String>> SYNONYMS = new LinkedHashMap<>();

    static {
        // ── PON AQUÍ TUS LÍNEAS ──────────────────────────────────────────
        // Ejemplo:
    	SYNONYMS.put("ps5",List.of("plei","play","maquinita","playstation 5","play station 5","playstation5","ps 5","sony playstation 5","ps5 fat","ps5 slim","ps5 pro","ps5 digital","ps5 digital edition","playstation 5 slim","playstation 5 pro","ps five","playstation five"));
    	SYNONYMS.put("ps4",List.of("plei","play","maquinita","playstation 4","play station 4","playstation4","ps 4","sony playstation 4","ps4 fat","ps4 slim","ps4 pro","playstation 4 slim","playstation 4 pro","ps four","playstation four"));
    	SYNONYMS.put("ps3",List.of("plei","play","maquinita","playstation 3","play station 3","playstation3","ps 3","sony playstation 3","ps3 fat","ps3 slim","ps3 super slim","ps three","playstation three"));
    	SYNONYMS.put("ps2",List.of("plei","play","maquinita","playstation 2","play station 2","playstation2","ps 2","sony playstation 2","ps2 fat","ps2 slim","ps two","playstation two"));
    	SYNONYMS.put("ps1",List.of("plei","play","maquinita","playstation 1","play station 1","playstation1","psx","sony psx","ps one","psone","ps classic","ps one slim"));
    	SYNONYMS.put("xbox series",List.of("xbox series x","xbox series s","series x","series s","xbox sx","xbox ss","microsoft xbox series"));
    	SYNONYMS.put("xbox one",List.of("microsoft xbox one","xbox one x","xbox one s","xbone","x one","xbox one fat"));
    	SYNONYMS.put("xbox 360",List.of("microsoft xbox 360","xbox360","xbox 360 slim","xbox 360 elite","xbox 360 arcade"));
    	SYNONYMS.put("xbox original",List.of("xbox clasica","xbox 2001","microsoft xbox 2001","xbox primera generacion"));
    	SYNONYMS.put("switch",List.of("nintendo switch","switch oled","switch lite","switch v1","switch v2","nswitch","n switch","consola switch"));
    	SYNONYMS.put("wii",List.of("nintendo wii","wii u","nintendo wii u","wii mini"));
    	SYNONYMS.put("gamecube",List.of("nintendo gamecube","game cube","ngc","gc nintendo"));
    	SYNONYMS.put("n64",List.of("nintendo 64","nintendo64","ultra 64","n 64"));
    	SYNONYMS.put("snes",List.of("super nintendo","super nes","super nintendo entertainment system","snes classic","super famicom"));
    	SYNONYMS.put("nes",List.of("nintendo entertainment system","nintendo nes","nes classic","famicom","family computer"));
    	SYNONYMS.put("gameboy",List.of("game boy","nintendo gameboy","gameboy classic","gameboy color","gbc","gameboy advance","gba","gb advance"));
    	SYNONYMS.put("gba",List.of("game boy advance","gameboy advance","gba sp","game boy advance sp"));
    	SYNONYMS.put("3ds",List.of("nintendo 3ds","new 3ds","3ds xl","new 3ds xl","2ds","2ds xl"));
    	SYNONYMS.put("ds",List.of("nintendo ds","nds","ds lite","dsi","dsi xl"));
    	SYNONYMS.put("psp",List.of("playstation portable","sony psp","psp 1000","psp 2000","psp 3000","psp street"));
    	SYNONYMS.put("vita",List.of("ps vita","playstation vita","sony vita","psvita","ps vita slim","vita 2000"));
    	SYNONYMS.put("steam deck",List.of("steamdeck","valve steam deck","steam deck oled","deck valve","consola portatil steam"));
    	SYNONYMS.put("atari 2600",List.of("atari2600","atari vcs","atari video computer system","atari clasica"));
    	SYNONYMS.put("atari 7800",List.of("atari7800","atari 7800 pro system"));
    	SYNONYMS.put("sega mega drive",List.of("megadrive","sega genesis","genesis sega","mega drive"));
    	SYNONYMS.put("sega master system",List.of("master system","sega mastersystem"));
    	SYNONYMS.put("sega saturn",List.of("saturn sega","sega 32 bit"));
    	SYNONYMS.put("sega dreamcast",List.of("dreamcast sega","dc sega","sega dc"));
    	SYNONYMS.put("neo geo",List.of("neo geo aes","neo geo mvs","snk neo geo"));
    	SYNONYMS.put("pc engine",List.of("turbografx 16","turbo grafx","nec pc engine"));
    	SYNONYMS.put("ouya",List.of("ouya console","android ouya"));
    	SYNONYMS.put("stadia",List.of("google stadia","stadia google","consola stadia"));
    	SYNONYMS.put("rog ally",List.of("asus rog ally","rogally","consola asus ally"));
    	SYNONYMS.put("legion go",List.of("lenovo legion go","legiongo","consola lenovo legion"));

        // ── Gaming — Juegos ───────────────────────────────────────────────
    	SYNONYMS.put("zelda",List.of("the legend of zelda","legend of zelda","zelda ocarina of time","zelda master quest","zelda majora's mask","wind waker","hyrule warriors","twilight princess","skyward sword","a link to the past","link's awakening","breath of the wild","tears of the kingdom","botw","totk","phantom hourglass","spirit tracks"));
    	SYNONYMS.put("mario",List.of("super mario","super mario bros","super mario bros 2","super mario bros 3","super mario world","super mario 64","super mario sunshine","super mario galaxy","super mario galaxy 2","super mario odyssey","new super mario bros","mario kart","mario kart 64","mario kart ds","mario kart wii","mario kart 8","mario kart 8 deluxe","mario party","dr mario","mario sports","mario golf","mario tennis","mario & luigi","paper mario"));
    	SYNONYMS.put("pokemon",List.of("pokemon rojo","pokemon azul","pokemon amarillo","pokemon oro","pokemon plata","pokemon cristal","pokemon ruby","pokemon sapphire","pokemon emerald","pokemon diamond","pokemon pearl","pokemon platinum","pokemon black","pokemon white","pokemon x","pokemon y","pokemon sun","pokemon moon","pokemon sword","pokemon shield","pokemon scarlet","pokemon violet","pokemon legends arceus","pokemon go","pokemon mystery dungeon"));
    	SYNONYMS.put("gta",List.of("grand theft auto","gta 1","gta 2","gta 3","gta vice city","gta san andreas","gta 4","gta iv","gta 5","gta v","gta 6","gta vi","gta online","grand theft auto iv","grand theft auto v","gta vice","rockstar gta"));
    	SYNONYMS.put("cod",List.of("call of duty","cod 1","cod 2","cod 3","cod 4","cod 4 modern warfare","cod modern warfare","cod modern warfare 2","cod modern warfare 3","cod black ops","cod black ops 2","cod black ops 3","cod cold war","cod warzone","cod infinite warfare","cod advanced warfare"));
    	SYNONYMS.put("fifa",List.of("ea sports fc","ea fc","fut","fifa 98","fifa 99","fifa 2000","fifa 21","fifa 22","fifa 23","fifa 24","fc 24","fc 25","ea sports fifa","nike fifa"));
    	SYNONYMS.put("minecraft",List.of("minecraft java edition","minecraft bedrock edition","minecraft pocket edition","minecraft dungeons","mc java","mc bedrock","minecraft survival","minecraft creative","mojang minecraft"));
    	SYNONYMS.put("cyberpunk",List.of("cyberpunk 2077","cyberpunk2077","cyberpunk phantom liberty","cp2077","cd projekt red","night city"));
    	SYNONYMS.put("elden ring",List.of("elden ring","eldenring shadow of the erdtree","fromsoftware elden ring","soulslike","souls like","dark souls","dark souls 2","dark souls 3","bloodborne","sekiro","eldenrdt"));
    	SYNONYMS.put("spider man",List.of("spiderman","spider-man","marvel's spider man","spider man ps4","spider man ps5","spiderman miles morales","spider man 2","insomniac spiderman"));
    	SYNONYMS.put("horizon",List.of("horizon zero dawn","horizon forbidden west","horizon burning shores","aloy","guerrilla games horizon"));
    	SYNONYMS.put("god of war",List.of("god of war","gow","god of war 2018","god of war ragnarok","kratos","atreus","santa monica gow"));
    	SYNONYMS.put("resident evil",List.of("resident evil","re1 remake","re2","re2 remake","re3","re3 remake","re4","re4 remake","re5","re6","re7 biohazard","re8 village","re9 requiem","biohazard"));
    	SYNONYMS.put("final fantasy",List.of("final fantasy","ff1","ff2","ff3","ff4","ff5","ff6","ff7","ff7 remake","ff8","ff9","ff10","ff12","ff13","ff14 online","ff15","ff16"));
    	SYNONYMS.put("assassins creed",List.of("assassins creed","ac 1","ac 2","ac brotherhood","ac revelations","ac odyssey","ac origins","ac valhalla","ac shadows"));
    	SYNONYMS.put("sonic",List.of("sonic the hedgehog","sonic 1","sonic 2","sonic 3","sonic & knuckles","sonic adventure","sonic adventure 2","sonic unleashed","sonic generations","sonic colors","sonic forces"));
    	SYNONYMS.put("tetris",List.of("tetris","tetris effect","tetris 99","tetris ds","tetris classic"));
    	SYNONYMS.put("the sims",List.of("the sims","sims 2","sims 3","sims 4","sims medieval"));
    	SYNONYMS.put("lego",List.of("lego star wars","lego marvel","lego harry potter","lego batman","lego jurassic world","lego hobbit"));
    	SYNONYMS.put("animal crossing",List.of("animal crossing","animal crossing wild world","animal crossing new leaf","animal crossing new horizons","acnh"));
    	SYNONYMS.put("metroid",List.of("metroid","metroid prime","metroid prime 2","metroid prime 3","metroid dread","metroid fusion"));
    	SYNONYMS.put("halo",List.of("halo","halo ce","halo 2","halo 3","halo reach","halo 4","halo 5 guardians","halo infinite"));
    	SYNONYMS.put("fallout",List.of("fallout","fallout 3","fallout new vegas","fallout 4","fallout 76"));
    	SYNONYMS.put("bioshock",List.of("bioshock","bioshock 2","bioshock infinite"));
    	SYNONYMS.put("doom",List.of("doom","doom 2","doom 3","doom eternal"));
    	SYNONYMS.put("mass effect",List.of("mass effect","mass effect 2","mass effect 3","mass effect andromeda"));
    	SYNONYMS.put("uncharted",List.of("uncharted","uncharted 2","uncharted 3","uncharted 4","uncharted lost legacy"));
    	SYNONYMS.put("red dead redemption",List.of("red dead redemption","red dead redemption 2","rdr2"));
    	SYNONYMS.put("forza horizon",List.of("forza horizon","forza horizon 4","forza horizon 5"));
    	SYNONYMS.put("need for speed",List.of("need for speed","nfs underground","nfs most wanted","nfs heat","nfs rivals"));
    	SYNONYMS.put("diablo",List.of("diablo 1","diablo ii","diablo 2 resurrected","diablo 3","diablo 4","blizzard diablo"));
    	SYNONYMS.put("warcraft",List.of("warcraft","warcraft 1","warcraft 2","warcraft 3","warcraft 3 reforged","warcraft reforged","world of warcraft","wow"));
    	SYNONYMS.put("starcraft",List.of("starcraft","starcraft 1","starcraft 2","starcraft remastered"));
    	SYNONYMS.put("league of legends",List.of("league of legends","lol","league","riot games lol"));
    	SYNONYMS.put("dota",List.of("dota","dota 2","dota2"));
    	SYNONYMS.put("counter strike",List.of("counter strike","cs","cs 1.6","csgo","cs go","counterstrike global offensive"));
    	SYNONYMS.put("overwatch",List.of("overwatch","overwatch 2","blizzard overwatch"));
    	SYNONYMS.put("apex legends",List.of("apex legends","apex","respawn apex"));
    	SYNONYMS.put("fortnite",List.of("fortnite","fortnite battle royale","epic games fortnite"));
    	SYNONYMS.put("pubg",List.of("pubg","playerunknown's battlegrounds"));
    	SYNONYMS.put("counter strike 2",List.of("cs2","counter strike 2"));
    	SYNONYMS.put("tekken",List.of("tekken","tekken 3","tekken 5","tekken 7","tekken tag tournament"));
    	SYNONYMS.put("street fighter",List.of("street fighter","sfii","street fighter 2","street fighter 3","street fighter 5","street fighter 6"));
    	SYNONYMS.put("mortal kombat",List.of("mortal kombat","mk","mortal kombat 1","mortal kombat 11","mortal kombat x","mortal kombat trilogy"));
    	SYNONYMS.put("persona",List.of("persona 3","persona 4","persona 5","persona 5 royal"));
    	SYNONYMS.put("yakuza",List.of("yakuza","yakuza 0","yakuza 6","yakuza like a dragon"));
    	SYNONYMS.put("witcher",List.of("the witcher","witcher 2","witcher 3","witcher wild hunt"));
    	SYNONYMS.put("dragon age",List.of("dragon age origins","dragon age 2","dragon age inquisition","dragon age dreadwolf"));
    	SYNONYMS.put("mass effect legendary",List.of("mass effect legendary edition","mass effect remastered"));
    	SYNONYMS.put("borderlands",List.of("borderlands","borderlands 2","borderlands 3","borderlands pre sequel"));
    	SYNONYMS.put("fall guys",List.of("fall guys","fall guys ultimate knockout"));
    	SYNONYMS.put("Among Us",List.of("among us","amongus"));
    	SYNONYMS.put("stardew valley",List.of("stardew valley","stardew"));
    	SYNONYMS.put("hades",List.of("hades","hades supergiant"));
    	SYNONYMS.put("celeste",List.of("celeste","celeste game"));
    	SYNONYMS.put("spelunky",List.of("spelunky","spelunky 2"));
    	SYNONYMS.put("factorio",List.of("factorio","factorio game"));
    	SYNONYMS.put("rimworld",List.of("rimworld"));
    	SYNONYMS.put("age of empires",List.of("age of empires","aoe","age of empires 2","age of empires 3","age of empires 4"));
    	SYNONYMS.put("total war",List.of("total war","total war rome","total war shogun","total war warhammer"));
    	SYNONYMS.put("cities skylines",List.of("cities skylines","cities sk"));
    	SYNONYMS.put("portal",List.of("portal","portal 2"));
    	SYNONYMS.put("half life",List.of("half life","half life 2","hl1","hl2","half life alyx"));
    	SYNONYMS.put("quantum break",List.of("quantun break"));
    	SYNONYMS.put("darkest dungeon",List.of("darkest dungeon","darkest dungeon game"));
    	SYNONYMS.put("terraria",List.of("terraria"));
    	SYNONYMS.put("rocket league",List.of("rocket league"));
    	SYNONYMS.put("guilty gear",List.of("guilty gear strive","guilty gear xx"));
    	SYNONYMS.put("gran turismo",List.of("gran turismo","gran turismo sport","gran turismo 7"));
    	SYNONYMS.put("donkey kong",List.of("donkey kong","donkeykong","donkey kong country","donky kong country","dkc","dk country","dk tropical freeze","donkey kong trpical freeze"));
    	SYNONYMS.put("kirby",List.of("kirby","kirbi","kirby star allies","kirby planet robobot","kirby super star","kirby dream land","kirby dreamland"));
    	SYNONYMS.put("fire emblem",List.of("fire emblem","fir emblem","fire emblem awakening","fire emblem 3 houses","fire emblem three houses","fire emblem shadow dragon","fire emblem fates","fire emblem fate"));
    	SYNONYMS.put("splatoon",List.of("splatoon","splatton","splatoon 2","splatoon2","splatoon 3","splaton 3"));
    	SYNONYMS.put("bayonetta",List.of("bayonetta","bayonet","bayonetta 2","bayoneta 2","bayonetta 3"));
    	SYNONYMS.put("metal gear solid",List.of("metal gear solid","mgs","metal gear solld","metal gear solid 1","metal gear solid 2","metal gear solid 3","metal gear solid 4","metal gear solid v","metal gear rising","kojima metal gear","mgs rising"));
    	SYNONYMS.put("castlevania",List.of("castlevania","castelvania","castlevania symphony of the night","castlevania dawn of sorrow","castlevania aria of sorrow","castlevania lords of shadow"));
    	SYNONYMS.put("megaman",List.of("mega man","megaman","rockman","megaman x","megaman zero","megaman battle network","megaman legends","megaman 11","mega man x","mega man zero"));
    	SYNONYMS.put("ratchet & clank",List.of("ratchet & clank","ratchet clank","ratchet and clank","ratchet & clank rift apart","ratchet & clank ps4","ratchet & clank ps5","insomniac ratchet","ratchet n clank"));
    	SYNONYMS.put("sly cooper",List.of("sly cooper","sly cuper","sly cooper thieves in time","sly 1","sly 2","sly 3","sly 4","sucker punch sly"));
    	SYNONYMS.put("jack & daxter",List.of("jak and daxter","jack & daxter","jak n daxter","jak 1","jak 2","jak 3","jak xd","jak ps2"));
    	SYNONYMS.put("uncharted",List.of("uncharted","uncharted 1","uncharted 2","uncharted 3","uncharted 4","uncharted lost legacy","uncharted ps4","uncharted ps5","uncharted remastered","unchartered"));
    	SYNONYMS.put("little big planet",List.of("little big planet","lbp","lbp 2","lbp 3","playstation little big planet","littlebigplanet","litle big planet"));
    	SYNONYMS.put("fable",List.of("fable","fabel","fable anniversary","fable 2","fable 3","fable legends"));
    	SYNONYMS.put("forza",List.of("forza","forza motorsport","forza horizon","forza horizon 4","forza horizon 5","forza 7","forza horzon"));
    	SYNONYMS.put("halo",List.of("halo","halo ce","halo 2","halo 3","halo 4","halo 5","halo infinite","bungie halo","halo infinits"));
    	SYNONYMS.put("gears of war",List.of("gears of war","gow","gears of war 2","gears of war 3","gears 4","gears 5","gears tactics","gears of wer"));
    	SYNONYMS.put("dead space",List.of("dead space","dead space 2","dead space 3","ded space"));
    	SYNONYMS.put("alan wake",List.of("alan wake","alan wake american nightmare","allan wake","alan wake americain nightmare"));
    	SYNONYMS.put("max payne",List.of("max payne","max payn","max payne 2","max payne 3"));
    	SYNONYMS.put("dishonored",List.of("dishonored","dishonored 2","dishonored death of the outsider","dishonord"));
    	SYNONYMS.put("prey",List.of("prey","prey 2017","prei"));
    	SYNONYMS.put("hitman",List.of("hitman","hit man","hitman 2","hitman 3","hitman absolution","hitman codename 47","hitmen"));
    	SYNONYMS.put("splinter cell",List.of("splinter cell","splintercel","splinter cell blacklist","splinter cell conviction","splinter cell chaos theory"));
    	SYNONYMS.put("far cry",List.of("far cry","far cry 2","far cry 3","far cry 3 blood dragon","far cry 4","far cry primal","far cry 5","far cry 6","far cri"));
    	SYNONYMS.put("tomb raider",List.of("tomb raider","tomb raider anniversary","tomb raider legend","tomb raider underworld","tomb raider reboot","shadow of the tomb raider","rise of the tomb raider","tomb raidar"));
    	
    	

        // ── Móviles ───────────────────────────────────────────────────────
        SYNONYMS.put("iphone",       List.of("apple iphone", "iphone 15", "iphone 14", "iphone 13", "iphone 12", "iphone 11"));
        SYNONYMS.put("ipad",         List.of("apple ipad", "tablet apple", "ipad pro", "ipad air", "ipad mini"));
        SYNONYMS.put("samsung",      List.of("samsung galaxy", "galaxy s", "galaxy a", "samsung s23", "samsung s22"));
        SYNONYMS.put("galaxy",       List.of("samsung galaxy", "galaxy s24", "galaxy s23", "galaxy a54"));
        SYNONYMS.put("xiaomi",       List.of("redmi", "redmi note", "xiaomi 13", "poco", "poco x5"));
        SYNONYMS.put("redmi",        List.of("xiaomi redmi", "redmi note 13", "redmi note 12"));
        SYNONYMS.put("huawei",       List.of("huawei p60", "huawei mate", "honor", "huawei nova"));
        SYNONYMS.put("honor",        List.of("huawei honor", "honor 90", "honor magic"));
        SYNONYMS.put("oppo",         List.of("oppo find", "oppo reno", "oneplus"));
        SYNONYMS.put("oneplus",      List.of("one plus", "oppo oneplus", "oneplus 11", "oneplus nord"));
        SYNONYMS.put("motorola",     List.of("moto g", "moto edge", "motorola edge", "razr"));
        SYNONYMS.put("google pixel", List.of("pixel 8", "pixel 7", "pixel 6", "pixel pro"));
        SYNONYMS.put("tablet",       List.of("ipad", "tablet samsung", "galaxy tab", "lenovo tab"));

        // ── Apple Ecosystem ───────────────────────────────────────────────
        SYNONYMS.put("airpods",      List.of("apple airpods", "auriculares apple", "airpods pro", "airpods max"));
        SYNONYMS.put("airpods pro",  List.of("apple airpods pro", "airpods pro 2", "airpods pro usbc"));
        SYNONYMS.put("airpods max",  List.of("apple airpods max", "auriculares over ear apple"));
        SYNONYMS.put("apple watch",  List.of("iwatch", "smartwatch apple", "reloj apple", "apple watch ultra"));
        SYNONYMS.put("macbook",      List.of("mac book", "apple macbook", "portatil apple", "macbook pro", "macbook air", "macbook m2", "macbook m3"));
        SYNONYMS.put("macbook pro",  List.of("mac book pro", "apple macbook pro", "mbp", "macbook pro m2"));
        SYNONYMS.put("macbook air",  List.of("mac book air", "apple macbook air", "mba", "macbook air m2"));
        SYNONYMS.put("imac",         List.of("apple imac", "mac desktop", "all in one apple", "imac 24"));
        SYNONYMS.put("mac mini",     List.of("apple mac mini", "macmini", "mac mini m2"));
        SYNONYMS.put("homepod",      List.of("apple homepod", "altavoz apple", "homepod mini"));

        // ── Informática ───────────────────────────────────────────────────
        SYNONYMS.put("portatil",     List.of("portátil", "laptop", "notebook", "ordenador portatil"));
        SYNONYMS.put("portátil",     List.of("portatil", "laptop", "notebook"));
        SYNONYMS.put("laptop",       List.of("portatil", "portátil", "notebook"));
        SYNONYMS.put("pc",           List.of("ordenador", "computadora", "desktop", "torre", "sobremesa", "pc gaming"));
        SYNONYMS.put("pc gaming",    List.of("ordenador gaming", "torre gaming", "pc gamer", "gaming pc"));
        SYNONYMS.put("gpu",          List.of("tarjeta grafica", "tarjeta gráfica", "graphics card", "rtx", "rx"));
        SYNONYMS.put("rtx",          List.of("nvidia rtx", "geforce rtx", "rtx 4090", "rtx 4080", "rtx 4070", "rtx 4060", "rtx 3080", "rtx 3070", "rtx 3060"));
        SYNONYMS.put("gtx",          List.of("nvidia gtx", "geforce gtx", "gtx 1080", "gtx 1070", "gtx 1660"));
        SYNONYMS.put("rx",           List.of("amd rx", "radeon rx", "rx 7900", "rx 7800", "rx 6800"));
        SYNONYMS.put("cpu",          List.of("procesador", "processor", "ryzen", "intel core"));
        SYNONYMS.put("procesador",   List.of("cpu", "processor", "ryzen 9", "ryzen 7", "ryzen 5", "i9", "i7", "i5"));
        SYNONYMS.put("ryzen",        List.of("amd ryzen", "ryzen 9 7950x", "ryzen 9 7900x", "ryzen 7 7700x", "ryzen 5 7600x"));
        SYNONYMS.put("intel",        List.of("intel core", "core i9", "core i7", "core i5", "core i3", "raptor lake"));
        SYNONYMS.put("ram",          List.of("memoria ram", "memoria ddr", "ddr4", "ddr5", "dimm"));
        SYNONYMS.put("ssd",          List.of("disco ssd", "solid state drive", "nvme", "m.2", "ssd nvme"));
        SYNONYMS.put("nvme",         List.of("ssd nvme", "m.2 nvme", "disco nvme", "ssd m2"));
        SYNONYMS.put("hdd",          List.of("disco duro", "hard drive", "disco mecanico", "wd blue", "seagate"));
        SYNONYMS.put("placa base",   List.of("motherboard", "placa madre", "mainboard"));
        SYNONYMS.put("monitor",      List.of("pantalla", "display", "monitor gaming", "monitor 4k", "monitor 144hz"));

        // ── Periféricos ───────────────────────────────────────────────────
        SYNONYMS.put("teclado",      List.of("keyboard", "teclado mecanico", "teclado gaming", "teclado rgb"));
        SYNONYMS.put("raton",        List.of("ratón", "mouse", "raton gaming", "raton inalambrico"));
        SYNONYMS.put("auriculares",  List.of("headphones", "headset", "cascos", "earbuds", "auriculares gaming", "auriculares bluetooth"));
        SYNONYMS.put("webcam",       List.of("camara web", "cámara web", "web cam", "camara streaming"));
        SYNONYMS.put("microfono",    List.of("micrófono", "microphone", "micro streaming", "blue yeti"));
        SYNONYMS.put("mando",        List.of("gamepad", "control", "joystick", "dualsense", "mando xbox", "controller"));
        SYNONYMS.put("silla gaming", List.of("silla gamer", "gaming chair", "secretlab", "noblechairs"));

        // ── Audio ─────────────────────────────────────────────────────────
        SYNONYMS.put("altavoz",      List.of("speaker", "altavoces", "bafle", "altavoz bluetooth"));
        SYNONYMS.put("bose",         List.of("bose quietcomfort", "bose 700", "bose soundlink", "auriculares bose"));
        SYNONYMS.put("sony wh",      List.of("sony wh-1000xm5", "sony wh-1000xm4", "headphones sony anc"));
        SYNONYMS.put("jbl",          List.of("jbl charge", "jbl flip", "jbl xtreme", "altavoz jbl", "auriculares jbl"));
        SYNONYMS.put("marshall",     List.of("altavoz marshall", "marshall stanmore", "marshall acton"));
        SYNONYMS.put("vinilo",       List.of("disco vinilo", "lp", "álbum vinilo", "tocadiscos", "vinyl"));
        SYNONYMS.put("sonos",        List.of("altavoz sonos", "sonos one", "sonos five", "sonos beam"));
        SYNONYMS.put("amplificador", List.of("amp", "amplifier", "receiver", "hifi", "hi-fi"));

        // ── TV y Vídeo ────────────────────────────────────────────────────
        SYNONYMS.put("tele",         List.of("television", "televisor", "tv", "smart tv"));
        SYNONYMS.put("tv",           List.of("televisor", "television", "tele", "smart tv", "oled tv", "qled tv"));
        SYNONYMS.put("oled",         List.of("tv oled", "monitor oled", "lg oled", "samsung oled", "sony oled"));
        SYNONYMS.put("qled",         List.of("tv qled", "samsung qled", "neo qled", "mini led qled"));
        SYNONYMS.put("proyector",    List.of("projector", "cañon proyector", "mini proyector"));
        SYNONYMS.put("chromecast",   List.of("google chromecast", "chromecast 4k", "streaming stick"));
        SYNONYMS.put("fire tv",      List.of("amazon fire tv stick", "fire stick", "streaming amazon"));
        SYNONYMS.put("netflix",      List.of("netflix premium", "suscripcion netflix", "cuenta netflix", "streaming netflix"));
        SYNONYMS.put("spotify",      List.of("spotify premium", "suscripcion spotify", "cuenta spotify"));
        SYNONYMS.put("hbo",          List.of("hbo max", "max streaming", "suscripcion hbo"));
        SYNONYMS.put("disney",       List.of("disney plus", "disney+", "suscripcion disney"));

        // ── Cámaras ───────────────────────────────────────────────────────
        SYNONYMS.put("camara",       List.of("cámara", "camera", "camara reflex", "camara sin espejo", "camara digital"));
        SYNONYMS.put("mirrorless",   List.of("sin espejo", "cámara sin espejo", "sony alpha", "fujifilm x", "nikon z"));
        SYNONYMS.put("gopro",        List.of("go pro", "action cam", "camara accion", "deportiva camara"));
        SYNONYMS.put("drone",        List.of("dron", "dji", "dji mini", "dji air", "dji mavic", "cuadricoptero"));
        SYNONYMS.put("dji",          List.of("dji mini 4 pro", "dji mini 3 pro", "dji air 3", "drone dji"));
        SYNONYMS.put("polaroid",     List.of("camara instantanea", "fujifilm instax", "instax mini"));

        // ── Vehículos ─────────────────────────────────────────────────────
        SYNONYMS.put("coche",        List.of("carro", "auto", "automovil", "automóvil", "car", "turismo", "vehiculo"));
        SYNONYMS.put("moto",         List.of("motocicleta", "motorcycle", "motorbike", "moto sport", "naked", "enduro"));
        SYNONYMS.put("scooter",      List.of("vespa", "moto scooter", "ciclomotor", "maxiscooter"));
        SYNONYMS.put("furgoneta",    List.of("furgo", "van", "transporter", "transito", "trafic", "vito"));
        SYNONYMS.put("electrico",    List.of("eléctrico", "electric", "tesla", "ev", "coche electrico", "bev"));
        SYNONYMS.put("tesla",        List.of("tesla model 3", "tesla model y", "tesla model s", "coche tesla", "ev tesla"));
        SYNONYMS.put("bmw",          List.of("bayerische motoren", "bmw serie 3", "bmw serie 5", "bmw x5", "bmw m3"));
        SYNONYMS.put("mercedes",     List.of("mercedes benz", "benz", "clase a", "clase c", "clase e", "amg"));
        SYNONYMS.put("audi",         List.of("audi a3", "audi a4", "audi a6", "audi q5", "quattro"));
        SYNONYMS.put("volkswagen",   List.of("vw", "golf", "polo", "passat", "tiguan", "id4"));
        SYNONYMS.put("seat",         List.of("seat ibiza", "seat leon", "seat ateca", "seat arona", "cupra"));
        SYNONYMS.put("ford",         List.of("ford focus", "ford fiesta", "ford mustang", "ford kuga", "ford transit"));
        SYNONYMS.put("peugeot",      List.of("peugeot 208", "peugeot 308", "peugeot 3008", "peugeot 508"));
        SYNONYMS.put("renault",      List.of("renault clio", "renault megane", "renault kadjar", "renault captur"));
        SYNONYMS.put("toyota",       List.of("toyota corolla", "toyota yaris", "toyota rav4", "toyota prius", "toyota hilux"));
        SYNONYMS.put("honda",        List.of("honda civic", "honda cr-v", "honda jazz", "honda cbr", "honda hornet"));
        SYNONYMS.put("yamaha",       List.of("yamaha mt", "yamaha r1", "yamaha r6", "yamaha tenere", "moto yamaha"));
        SYNONYMS.put("kawasaki",     List.of("kawasaki z", "kawasaki ninja", "kawasaki er6", "moto kawasaki"));
        SYNONYMS.put("harley",       List.of("harley davidson", "harley-davidson", "chopper harley", "softail"));
        SYNONYMS.put("bicicleta",    List.of("bici", "bike", "ciclo", "bicicleta electrica", "bicicleta carretera"));
        SYNONYMS.put("patinete",     List.of("patinete electrico", "scooter electrico", "kickboard", "ninebot"));
        SYNONYMS.put("quad",         List.of("cuatrimoto", "atv", "all terrain vehicle"));

        // ── Ropa y Moda ───────────────────────────────────────────────────
        SYNONYMS.put("zapatillas",   List.of("sneakers", "tenis", "zapatos deportivos", "trainers", "deportivas"));
        SYNONYMS.put("nike",         List.of("nike air", "nike air max", "nike air force", "nike jordan", "swoosh"));
        SYNONYMS.put("adidas",       List.of("adidas ultraboost", "adidas superstar", "adidas stan smith", "adidas samba", "yeezy"));
        SYNONYMS.put("jordan",       List.of("air jordan", "jordan 1", "jordan 4", "jordan 11", "aj1"));
        SYNONYMS.put("air max",      List.of("nike air max", "air max 90", "air max 95", "air max 97", "am90"));
        SYNONYMS.put("yeezy",        List.of("adidas yeezy", "yeezy boost", "yeezy 350", "yeezy 700"));
        SYNONYMS.put("converse",     List.of("chuck taylor", "all star", "converse all star"));
        SYNONYMS.put("vans",         List.of("vans old skool", "vans sk8-hi", "vans authentic"));
        SYNONYMS.put("new balance",  List.of("nb 574", "nb 990", "nb 550", "new balance 574"));
        SYNONYMS.put("abrigo",       List.of("chaqueta", "cazadora", "parka", "gabardina", "anorak"));
        SYNONYMS.put("canada goose", List.of("canada goose expedition", "parka canada goose", "parka premium"));
        SYNONYMS.put("north face",   List.of("the north face", "tnf", "north face parka", "north face chaqueta"));
        SYNONYMS.put("levi",         List.of("levis", "levi's", "vaqueros levis", "jeans levi's", "501"));
        SYNONYMS.put("vaqueros",     List.of("jeans", "pantalon vaquero", "denim", "tejano"));
        SYNONYMS.put("bolso",        List.of("bolsa", "bag", "handbag", "bandolera", "cartera"));
        SYNONYMS.put("reloj",        List.of("watch", "relojes", "smartwatch", "cronografo"));
        SYNONYMS.put("rolex",        List.of("rolex submariner", "rolex datejust", "reloj lujo rolex"));

        // ── Hogar ─────────────────────────────────────────────────────────
        SYNONYMS.put("aspiradora",   List.of("roomba", "aspirador", "robot aspirador", "vacuum", "dyson"));
        SYNONYMS.put("dyson",        List.of("dyson v15", "dyson v12", "dyson airwrap", "aspiradora dyson"));
        SYNONYMS.put("roomba",       List.of("irobot roomba", "robot aspirador roomba", "roomba j7", "roomba i7"));
        SYNONYMS.put("thermomix",    List.of("thermo mix", "robot cocina", "monsieur cuisine"));
        SYNONYMS.put("airfryer",     List.of("air fryer", "freidora aire", "freidora sin aceite", "ninja airfryer"));
        SYNONYMS.put("nevera",       List.of("frigorífico", "frigorifico", "refrigerador", "fridge"));
        SYNONYMS.put("lavadora",     List.of("washing machine", "lg lavadora", "samsung lavadora", "bosch lavadora"));
        SYNONYMS.put("cafetera",     List.of("coffee maker", "espresso", "nespresso", "dolce gusto", "de longhi"));
        SYNONYMS.put("nespresso",    List.of("cafetera nespresso", "nespresso vertuo", "capsulas nespresso"));
        SYNONYMS.put("sofa",         List.of("sofá", "divano", "couch", "tresillo", "sofa cama"));
        SYNONYMS.put("sofá",         List.of("sofa", "divano", "couch", "tresillo"));
        SYNONYMS.put("colchon",      List.of("colchón", "mattress", "colchon viscoelastico", "tempur"));
        SYNONYMS.put("escritorio",   List.of("mesa escritorio", "desk", "mesa trabajo", "mesa ordenador"));
        SYNONYMS.put("armario",      List.of("wardrobe", "ropero", "closet", "pax ikea", "armario empotrado"));
        SYNONYMS.put("aire acondicionado", List.of("ac", "climatizador", "split", "mitsubishi ac", "daikin"));

        // ── Libros ────────────────────────────────────────────────────────
        SYNONYMS.put("libro",        List.of("novela", "book", "comic", "manga", "tebeo"));
        SYNONYMS.put("manga",        List.of("anime manga", "comic japones", "shonen", "seinen", "tankōbon"));
        SYNONYMS.put("comic",        List.of("cómic", "tebeo", "marvel comic", "dc comic", "graphic novel"));
        SYNONYMS.put("kindle",       List.of("amazon kindle", "kindle paperwhite", "ereader amazon", "ebook reader"));

        // ── Deporte ───────────────────────────────────────────────────────
        SYNONYMS.put("raqueta",      List.of("raqueta tenis", "raqueta padel", "pala padel", "raqueta squash"));
        SYNONYMS.put("padel",        List.of("pala padel", "palas padel", "raqueta padel"));
        SYNONYMS.put("pesas",        List.of("mancuernas", "dumbbells", "discos pesas", "barra pesas", "kettlebell"));
        SYNONYMS.put("bicicleta estatica", List.of("bici estatica", "cycling indoor", "spinning bike", "wahoo kickr"));
        SYNONYMS.put("yoga",         List.of("esterilla yoga", "mat yoga", "yoga mat", "pilates"));
        SYNONYMS.put("esqui",        List.of("esquís", "skis", "botas esqui", "ski alpin", "snowboard"));

        // ── Juguetes ──────────────────────────────────────────────────────
        SYNONYMS.put("lego",         List.of("lego technic", "lego city", "lego star wars", "lego creator", "lego set"));
        SYNONYMS.put("funko",        List.of("funko pop", "funko vinyl", "pop vinyl", "bobblehead funko"));
        SYNONYMS.put("figura",       List.of("figure", "action figure", "figura coleccion", "nendoroid", "figma"));
        SYNONYMS.put("pokemon card", List.of("carta pokemon", "cartas pokemon", "sobres pokemon", "pokemon tcg"));

        // ── Instrumentos ──────────────────────────────────────────────────
        SYNONYMS.put("guitarra",     List.of("guitar", "guitarra electrica", "guitarra acustica", "bajo electrico"));
        SYNONYMS.put("piano",        List.of("teclado piano", "piano digital", "yamaha piano", "casio piano"));
        SYNONYMS.put("bateria",      List.of("drums", "batería", "kit bateria", "bateria electronica", "roland bateria"));

        // ── Herramientas ──────────────────────────────────────────────────
        SYNONYMS.put("taladro",      List.of("drill", "taladradora", "taladro percutor", "bosch taladro", "makita taladro"));
        SYNONYMS.put("sierra",       List.of("sierra circular", "sierra caladora", "jigsaw", "circular saw"));

        // ── Inmuebles ─────────────────────────────────────────────────────
        SYNONYMS.put("piso",         List.of("apartamento", "flat", "vivienda", "piso alquiler", "piso venta"));
        SYNONYMS.put("casa",         List.of("chalet", "vivienda", "casa alquiler", "adosado", "unifamiliar"));
    }
    /**
     * Índice inverso bidireccional construido al arrancar.
     *
     * Para cada término (clave o valor) guarda TODOS los del grupo:
     *   "maquinita"    → [ps5, plei, play, playstation 5, ...]
     *   "ps5"          → [plei, play, maquinita, playstation 5, ...]
     *   "playstation5" → [ps5, plei, play, maquinita, ...]
     *
     * Así buscar cualquier palabra del grupo devuelve todos los demás.
     */
    private final Map<String, Set<String>> reverseIndex = new HashMap<>();

    @PostConstruct
    void buildIndex() {
        SYNONYMS.forEach((key, values) -> {
            // El grupo completo = clave + todos sus valores (en minúsculas)
            Set<String> group = new LinkedHashSet<>();
            group.add(key.toLowerCase());
            values.forEach(v -> group.add(v.toLowerCase()));

            // Cada miembro apunta al grupo entero
            group.forEach(member ->
                reverseIndex
                    .computeIfAbsent(member, k -> new LinkedHashSet<>())
                    .addAll(group)
            );
        });
    }

    // ── Levenshtein ───────────────────────────────────────────────────────

    /** Tolerancia según longitud: ≤3→0, ≤5→1, ≤8→2, 9+→3 */
    private static int maxDist(String s) {
        int n = s.length();
        if (n <= 3) return 0;
        if (n <= 5) return 1;
        if (n <= 8) return 2;
        return 3;
    }

    public static int levenshtein(String a, String b) {
        int la = a.length(), lb = b.length();
        int[][] dp = new int[la + 1][lb + 1];
        for (int i = 0; i <= la; i++) dp[i][0] = i;
        for (int j = 0; j <= lb; j++) dp[0][j] = j;
        for (int i = 1; i <= la; i++)
            for (int j = 1; j <= lb; j++) {
                int cost = a.charAt(i-1) == b.charAt(j-1) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i-1][j] + 1, dp[i][j-1] + 1),
                    dp[i-1][j-1] + cost
                );
            }
        return dp[la][lb];
    }

    // ── expand ────────────────────────────────────────────────────────────

    /**
     * Expande el query con sinónimos + typos + subpalabras.
     *
     * Ejemplos:
     *   expand("ps5")          → [ps5, plei, play, maquinita, playstation 5, ...]
     *   expand("maquinita")    → [maquinita, ps5, plei, play, playstation 5, ...]
     *   expand("palystation")  → fuzzy match con "playstation 5" → grupo ps5 completo
     *   expand("playstation5") → exacto en el índice → grupo ps5 completo
     */
    public List<String> expand(String query) {
        if (query == null || query.isBlank()) return Collections.emptyList();

        String lower    = query.toLowerCase().trim();
        Set<String> out = new LinkedHashSet<>();
        out.add(lower);

        int dist = maxDist(lower);

        // 1. Exacto en el índice inverso
        Set<String> exactGroup = reverseIndex.get(lower);
        if (exactGroup != null) out.addAll(exactGroup);

        // 2. El query CONTIENE una clave del índice o viceversa
        reverseIndex.forEach((key, group) -> {
            if (lower.contains(key) || key.contains(lower))
                out.addAll(group);
        });

        // 3. Fuzzy: Levenshtein contra todas las claves del índice
        if (dist > 0) {
            reverseIndex.forEach((key, group) -> {
                if (!out.containsAll(group) && levenshtein(lower, key) <= dist)
                    out.addAll(group);
            });
        }

        // 4. Subpalabras: "playstation 5" → también busca "playstation"
        //    para encontrar productos llamados solo "PlayStation"
        new ArrayList<>(out).forEach(term -> {
            String[] parts = term.split("\\s+");
            if (parts.length > 1)
                for (String part : parts)
                    if (part.length() >= 4) out.add(part);
        });

        return new ArrayList<>(out);
    }
}