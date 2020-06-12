package shared;

import java.util.Arrays;
import java.util.List;

public interface MultipleInitSeeds {

    List<String> posAdj = Arrays.asList("absolut", "anbetungswürdig", "erstaunlich", "fantastisch", "aktiv",
            "ansprechend", "wohltuend", "brillant", "schön", "tapfer", "ruhig", "niedlich", "sauber", "charmant",
            "göttlich", "ethisch", "einfach", "ausgezeichnet", "effizient", "aufregend", "fair", "fein", "freundlich",
            "gut", "großartig", "strahlend", "herzhaft", "gesund", "intelligent", "ideal", "fröhlich", "freundlich", "glücklich",
            "sinnvoll", "angenehm", "produktiv", "sicher", "qualifiziert", "erfolgreich", "kompetent");
    List<String> posSubst = Arrays.asList("Leistung", "Zustimmung", "Eignung", "Erfolg",
            "Schönheit", "Nutzen", "Champion", "Charme", "Kreativität", "Göttlichkeit", "Ethik", "Energie", "Fairness",
            "Freund", "Familie", "Himmel", "Harmonie", "Herz", "Ehre", "Ehrlichkeit", "Gesundheit", "Umarmung", "Phantasie",
            "Innovation", "intellektuell", "Freude", "Wissen", "Natur", "Paradies", "Prinzip", "Qualität",
            "Können", "Sonne", "Lächeln", "Seele", "Reichtum", "Gut", "Sieg", "Tugend", "Wahrheit");
    List<String> posVerbs = Arrays.asList("bewundern", "akzeptieren", "vollbringen", "verschönern", "glauben", "segnen",
            "blühen", "erhellen", "beruhigen", "pflegen", "fesseln", "reinigen", "bezaubern", "trösten", "gratulieren", "schaffen",
            "heilen", "erfreuen", "spenden", "erleichtern", "erregen", "reparieren", "graduieren", "strahlen", "harmonisieren", "ehren", "helfen",
            "umarmen", "erhellen", "verbessern", "inspirieren", "lieben", "bitte", "fortschreiten", "reparieren", "befriedigen", "unterstützen",
            "vertrauen", "erheben", "verstehen", "willkommen");
    List<String> negAdj = Arrays.asList("wütend", "ängstlich", "schlecht", "langweilig", "banal", "kaputt", "verrückt",
            "grausam", "verwirrt", "korrupt", "beschädigt", "beraubt", "schmutzig", "schrecklich", "abscheulich", "unehrlich",
            "deprimiert", "ekelhaft", "böse", "gescheitert", "faul", "gierig", "schuldig", "hart", "verletzend", "schädlich",
            "unmöglich", "verrückt", "ignorant", "missverstanden", "gemein", "negativ", "beleidigend", "unhöflich", "traurig",
            "beängstigend", "stinkend", "stressig", "bedrohlich", "rachsüchtig");
    List<String> negSubst = Arrays.asList("Wut", "Apathie", "Alarm", "schlecht", "Grausamkeit", "Kriechen", "Verwirren",
            "kriminell", "Schaden", "Krankheit", "Not", "Depression", "Bosheit", "Versagen", "Kampf", "Angst", "Gier",
            "Schuld", "Schaden", "verletzt", "krank", "Verletzung", "Schrott", "Chaos", "Monster", "Stöhnen", "niemand", "Beleidigung", "Schmerz",
            "Pessimist", "Ablehnung", "Revolution", "Rache", "Traurigkeit", "Angst", "Geruch", "Stress", "Krankheit",
            "Terror", "Bedrohung", "Wunde");
    List<String> negVerbs = Arrays.asList("ärgern", "widrig", "langweilen", "zerbrechen", "weinen", "zusammenbrechen", "korrumpieren",
            "kriechen", "verwirren", "beschädigen", "sterben", "leugnen", "entziehen", "beschädigen", "ekeln", "entehren", "erzürnen", "fürchten",
            "erschrecken", "versagen", "kämpfen", "Schuld", "verletzen", "verletzen", "hassen", "verletzen", "missverstehen", "verneinen", "beleidigen",
            "unterdrücken", "beunruhigen", "zurückweisen", "rächen", "aufbegehren", "abwehren", "stressen", "erschrecken", "stinken", "drohen",
            "erschrecken", "aufregen", "verwunden");
    List<String> posAll = Arrays.asList("absolut", "anbetungswürdig", "erstaunlich", "fantastisch", "aktiv",
            "ansprechend", "wohltuend", "brillant", "schön", "tapfer", "ruhig", "niedlich", "sauber", "charmant",
            "göttlich", "ethisch", "einfach", "ausgezeichnet", "effizient", "aufregend", "fair", "fein", "freundlich",
            "gut", "großartig", "strahlend", "herzhaft", "gesund", "intelligent", "ideal", "fröhlich", "freundlich", "glücklich",
            "sinnvoll", "angenehm", "produktiv", "sicher", "qualifiziert", "erfolgreich", "kompetent",
            "Leistung", "Zustimmung", "Eignung", "Erfolg",
            "Schönheit", "Nutzen", "Champion", "Charme", "Kreativität", "Göttlichkeit", "Ethik", "Energie", "Fairness",
            "Freund", "Familie", "Himmel", "Harmonie", "Herz", "Ehre", "Ehrlichkeit", "Gesundheit", "Umarmung", "Phantasie",
            "Innovation", "intellektuell", "Freude", "Wissen", "Natur", "Paradies", "Prinzip", "Qualität",
            "Können", "Sonne", "Lächeln", "Seele", "Reichtum", "Gut", "Sieg", "Tugend", "Wahrheit",
            "bewundern", "akzeptieren", "vollbringen", "verschönern", "glauben", "segnen",
            "blühen", "erhellen", "beruhigen", "pflegen", "fesseln", "reinigen", "bezaubern", "trösten", "gratulieren", "schaffen",
            "heilen", "erfreuen", "spenden", "erleichtern", "erregen", "reparieren", "graduieren", "strahlen", "harmonisieren", "ehren", "helfen",
            "umarmen", "erhellen", "verbessern", "inspirieren", "lieben", "bitte", "fortschreiten", "reparieren", "befriedigen", "unterstützen",
            "vertrauen", "erheben", "verstehen", "willkommen");
    List<String> negAll = Arrays.asList("wütend", "ängstlich", "schlecht", "langweilig", "banal", "kaputt", "verrückt",
            "grausam", "verwirrt", "korrupt", "beschädigt", "beraubt", "schmutzig", "schrecklich", "abscheulich", "unehrlich",
            "deprimiert", "ekelhaft", "böse", "gescheitert", "faul", "gierig", "schuldig", "hart", "verletzend", "schädlich",
            "unmöglich", "verrückt", "ignorant", "missverstanden", "gemein", "negativ", "beleidigend", "unhöflich", "traurig",
            "beängstigend", "stinkend", "stressig", "bedrohlich", "rachsüchtig",
            "Wut", "Apathie", "Alarm", "schlecht", "Grausamkeit", "Kriechen", "Verwirren",
            "kriminell", "Schaden", "Krankheit", "Not", "Depression", "Bosheit", "Versagen", "Kampf", "Angst", "Gier",
            "Schuld", "Schaden", "verletzt", "krank", "Verletzung", "Schrott", "Chaos", "Monster", "Stöhnen", "niemand", "Beleidigung", "Schmerz",
            "Pessimist", "Ablehnung", "Revolution", "Rache", "Traurigkeit", "Angst", "Geruch", "Stress", "Krankheit",
            "Terror", "Bedrohung", "Wunde",
            "ärgern", "widrig", "langweilen", "zerbrechen", "weinen", "zusammenbrechen", "korrumpieren",
            "kriechen", "verwirren", "beschädigen", "sterben", "leugnen", "entziehen", "beschädigen", "ekeln", "entehren", "erzürnen", "fürchten",
            "erschrecken", "versagen", "kämpfen", "Schuld", "verletzen", "verletzen", "hassen", "verletzen", "missverstehen", "verneinen", "beleidigen",
            "unterdrücken", "beunruhigen", "zurückweisen", "rächen", "aufbegehren", "abwehren", "stressen", "erschrecken", "stinken", "drohen",
            "erschrecken", "aufregen", "verwunden");
}
