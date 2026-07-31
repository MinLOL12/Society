package io.github.minlol12.society.core.util;

import java.util.Collection;
import java.util.Random;

import io.github.minlol12.society.core.types.CultureOrigin;

/**
 * Flavoured name generation: person names, family names and settlement
 * names all draw from culture-specific pools so a Desert town reads
 * differently from a Snow town before you ever see its buildings.
 */
public final class NameGen {

    private NameGen() { }

    // ----------------------------------------------------------------
    // Person first names (gender-neutral, lightly flavoured per origin)
    // ----------------------------------------------------------------
    private static final String[][] FIRST = {
        // PLAINS - solid farm names
        {"Alda","Brant","Corin","Della","Edda","Fenn","Greta","Harlan","Isolde","Joren",
         "Katrin","Ludo","Maren","Nils","Osric","Petra","Rowan","Sable","Tilda","Ulric",
         "Vera","Wendel","Yara","Stefan","Milo","Doria","Bram","Hettie","Colin","Sanna"},
        // FOREST - soft and mossy
        {"Aspen","Briar","Crispin","Diantha","Elowen","Fern","Galen","Hazel","Ivo","Juniper",
         "Kestrel","Linden","Maple","Norwin","Osmond","Piper","Quill","Rill","Sorrel","Tansy",
         "Ula","Vervain","Wren","Xanthe","Ywain","Zelie","Alder","Birch","Clover","Damson"},
        // MOUNTAIN - hard consonants
        {"Astrid","Bjorn","Dagny","Einar","Freya","Gunnar","Hilda","Ivar","Jorunn","Kelda",
         "Leif","Magni","Norna","Odd","Runa","Sigrid","Torsten","Ulf","Vigdis","Yrsa",
         "Sten","Brynja","Dagnar","Eirik","Solvei","Trygve","Ragna","Halvor","Sigurd","Valka"},
        // COASTAL - wind and salt
        {"Anchoret","Beryl","Cordelia","Dorian","Eglantine","Finnian","Gull","Heliotrope","Iona","Jasper",
         "Kerrin","Lirael","Marlin","Nerissa","Ondine","Pelagia","Quillon","Ridley","Serein","Taliesin",
         "Undine","Vortigern","Waverly","Ysolina","Zephyr","Arietta","Brine","Coral","Drexel","Misty"},
        // DESERT - long vowels, heat shimmer
        {"Amara","Bashir","Cleon","Dalila","Ezram","Farah","Ghalib","Hassiba","Idris","Jamila",
         "Kazimir","Leila","Mazin","Nadira","Ophir","Rashida","Sabah","Tariq","Umar","Vashti",
         "Widad","Xerxes","Yasmin","Zafir","Aminah","Badr","Qadira","Sayyid","Zuleika","Feruz"},
        // SNOWY - short, warm sounds for a cold land
        {"Ahti","Birgit","Cele","Dyre","Eira","Frode","Greta","Hakon","Iben","Jussi",
         "Kaija","Lumi","Milla","Nivi","Olavi","Piri","Risto","Sanna","Toivo","Ulla",
         "Veikko","Wyanet","Ylva","Aino","Esko","Hilla","Ilma","Onni","Sisu","Viena"},
        // JUNGLE - bright and quick
        {"Acai","Bonita","Citlali","Dario","Ephra","Fenella","Ixchel","Jacinto","Kiri","Liana",
         "Manu","Naiara","Ozel","Paloa","Quetzal","Rocio","Silva","Taina","Uxmal","Vicoya",
         "Waira","Xiu","Yara","Zelia","Amaru","Chasca","Inaya","Orin","Sumaq","Tiare"},
        // SWAMP - heavy, secretive
        {"Absolon","Beatrix","Corvus","Drusia","Eremon","Fenwick","Greta","Hogatha","Ichabod","Jilt",
         "Kermit","Ligeia","Morcant","Nelda","Odo","Petunia","Quentin","Rusalka","Silas","Thaddeus",
         "Umber","Vesper","Watto","Yorick","Zilla","Agnis","Bogdan","Cypra","Edda","Mirena"}
    };

    // ----------------------------------------------------------------
    // Family name roots + suffixes, spliced together ("Barley" + "ford")
    // ----------------------------------------------------------------
    private static final String[][] FAMILY_ROOT = {
        // PLAINS
        {"Alder","Barley","Briar","Corn","Fallow","Gold","Harrow","Hay","Heath","Mead","Oat","Thist","Wheat","Willow"},
        // FOREST
        {"Ash","Beck","Bryn","Eller","Fern","Hazel","Holly","Lark","Linden","Moss","Row","Thorn","Wren","Yew"},
        // MOUNTAIN
        {"Brakka","Dolmen","Fjell","Gragarn","Harr","Iron","Krag","Orm","Skarde","Sten","Torv","Ulf","Varg","Ymir"},
        // COASTAL
        {"Aran","Brine","Carrick","Dover","Farren","Galley","Haven","Kelp","Maris","Ostram","Quay","Reeve","Skerry","Tide"},
        // DESERT
        {"Amon","Bakir","Cinnabar","Djinn","Efreet","Fahad","Gadir","Hesper","Irem","Jafari","Khaldun","Mirage","Orion","Siroc"},
        // SNOWY
        {"Alde","Bjorn","Eira","Frost","Gran","Hvit","Isarn","Jokull","Kalla","Lumi","Nor","Rime","Skadi","Vinter"},
        // JUNGLE
        {"Amashka","Balam","Ceiba","Huaya","Ixtab","Kayna","Macaw","Nahual","Orinoco","Quilo","Sacha","Wayra","Itza","Zuma"},
        // SWAMP
        {"Abelard","Boggs","Crane","Dragomir","Fen","Grimm","Hollow","Lurk","Morvain","Peat","Quagmire","Slough","Umbra","Vetch"}
    };

    private static final String[][] FAMILY_SUFFIX = {
        {"field","ford","ham","wick","ton","stead","well","brook","acre","sham"},        // PLAINS
        {"wood","shade","glade","bough","root","leaf","fen","hurst","grove","wynd"},     // FOREST
        {"fell","rock","heim","gard","thor","brand","stein","ing","garth","sku"},        // MOUNTAIN
        {"port","reach","strand","firth","calm","wreck","shore","line","mast","drift"},  // COASTAL
        {"sand","dune","zar","qir","mir","oasis","fal","star","wind","ash"},             // DESERT
        {"fall","mount","snaw","hearth","fell","strand","ness","dal","gard","sen"},      // SNOWY
        {"can","yotl","nala","tara","ra","puna","huar","rana","selva","co"},             // JUNGLE
        {"water","marsh","moss","mire","hole","croft","wick","bottom","shade","fen"}     // SWAMP
    };

    // ----------------------------------------------------------------
    // Settlement names: prefix + suffix ("Oakholt", "Djinnzar")
    // ----------------------------------------------------------------
    private static final String[][] TOWN_PREFIX = {
        {"Barley","Fen","Great","Hay","Mill","Oak","Stow","Thorn","Wheat","Wool","Golden","Little","Apple","Barn","Dale"},
        {"Ash","Birch","Elder","Fern","Holly","Lark","Moss","Rowan","Thorn","Willow","Alder","Bramble","Cedar","Deer","Wren"},
        {"Bryn","Dun","Grim","High","Iron","Krag","Raven","Skarde","Sten","Thor","Ulf","Wolf","Frost","Stone","Bjorn"},
        {"Brine","Carrick","Grey","Haven","Kelp","Low","Quay","Salt","Skerry","Storm","Tide","White","Coral","Fair","Long"},
        {"Amon","Bakir","Cinder","Djinn","Fahad","Golden","Jafir","Khar","Mirage","Red","Siroc","Zar","Irem","Orion","Ash"},
        {"Alde","Bjorn","Frost","Gran","Hvit","Isen","Kald","Lumi","Rime","Skadi","Snow","Vinter","Hearth","White","Nord"},
        {"Ama","Balam","Green","Huaya","Itza","Kay","Nahua","Orin","Quilo","Sacha","Selva","Way","Xiu","Zuma","Cei"},
        {"Bog","Dark","Fen","Grey","Mire","Moss","Peat","Reed","Sedge","Slough","Umber","Vet","Willow","Cran","Quag"}
    };

    private static final String[][] TOWN_SUFFIX = {
        {"field","ford","ham","market","mead","stead","ton","wick","acre","bury"},
        {"holt","shade","glade","grove","wood","bough","leaf","reach","den","wynd"},
        {"fell","gard","heim","hold","rock","thor","watch","brand","gate","fell"},
        {"bay","cove","firth","haven","port","quay","reach","strand","mouth","hook"},
        {"dune","oasis","sands","well","zar","qir","mir","rest","cliff","bazaar"},
        {"gard","hall","hearth","hus","mount","ness","snaw","stead","strand","dal"},
        {"can","puna","ra","selva","tara","xal","yotl","nala","huar","rana"},
        {"bottom","croft","den","fen","flood","hollow","mire","moss","pus","swamp"}
    };

    private static String[] pool(String[][] pools, CultureOrigin origin) {
        int index = origin.ordinal();
        if (index < 0 || index >= pools.length) index = 0;
        return pools[index];
    }

    public static String firstName(CultureOrigin origin, Random random) {
        String[] pool = pool(FIRST, origin);
        return pool[random.nextInt(pool.length)];
    }

    public static String familyName(CultureOrigin origin, Random random) {
        String[] roots = pool(FAMILY_ROOT, origin);
        String[] suffixes = pool(FAMILY_SUFFIX, origin);
        String root = roots[random.nextInt(roots.length)];
        String suffix = suffixes[random.nextInt(suffixes.length)];
        // Avoid smashing identical consonant pairs ("Thor" + "thor").
        if (!root.isEmpty() && !suffix.isEmpty()
                && root.charAt(root.length() - 1) == suffix.charAt(0)
                && Character.toLowerCase(root.charAt(root.length() - 1)) != 'a') {
            suffix = suffix.substring(1);
        }
        return root + suffix;
    }

    public static String settlementName(CultureOrigin origin, Random random) {
        String[] prefixes = pool(TOWN_PREFIX, origin);
        String[] suffixes = pool(TOWN_SUFFIX, origin);
        return prefixes[random.nextInt(prefixes.length)] + suffixes[random.nextInt(suffixes.length)];
    }

    /** Finds a settlement name not colliding with any existing name. */
    public static String uniqueSettlementName(CultureOrigin origin, Random random, Collection<String> taken) {
        for (int attempt = 0; attempt < 40; attempt++) {
            String name = settlementName(origin, random);
            if (!taken.contains(name)) return name;
        }
        // Deterministic fallback keeps trying with numbers.
        String name = settlementName(origin, random);
        int counter = 2;
        String candidate = name + " " + counter;
        while (taken.contains(candidate) && counter < 100) {
            counter++;
            candidate = name + " " + counter;
        }
        return candidate;
    }
}
