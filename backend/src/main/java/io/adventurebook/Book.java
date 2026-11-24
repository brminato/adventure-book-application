package io.adventurebook;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

record Book(String title, String author, Difficulty difficulty, List<Section> sections) {
    enum Difficulty { EASY, MEDIUM, HARD }
    enum SectionType { BEGIN, NODE, END }
    enum Effect { LOSE_HEALTH, GAIN_HEALTH }

    record Section(Integer id, String text, SectionType type, List<Option> options) {}
    record Option(String description, Integer gotoId, Consequence consequence) {}
    record Consequence(Effect type, Integer value, String text) {}

    Map<Integer, Section> validate() {
        require(hasText(title) && hasText(author) && difficulty != null,
                "A book needs a title, author and difficulty.");
        require(sections != null && !sections.isEmpty(), "A book needs sections.");
        var index = new HashMap<Integer, Section>();
        int beginnings = 0;
        int endings = 0;
        for (Section section : sections) {
            require(section != null && section.id() != null && section.type() != null
                    && hasText(section.text()), "Each section needs an ID, text and type.");
            require(index.putIfAbsent(section.id(), section) == null,
                    "Duplicate section ID: " + section.id());
            if (section.type() == SectionType.BEGIN) {
                beginnings++;
            }
            if (section.type() == SectionType.END) {
                endings++;
                require(section.options() == null || section.options().isEmpty(),
                        "Ending sections cannot have choices.");
            } else {
                require(section.options() != null && !section.options().isEmpty(),
                        "Section " + section.id() + " needs at least one choice.");
            }
        }
        require(beginnings == 1, "A book needs exactly one beginning.");
        require(endings > 0, "A book needs at least one ending.");
        for (Section section : sections) {
            if (section.options() == null) {
                continue;
            }
            for (Option option : section.options()) {
                require(option != null && hasText(option.description()) && index.containsKey(option.gotoId()),
                        "Section " + section.id() + " has an invalid choice or destination.");
                Consequence effect = option.consequence();
                if (effect != null) {
                    require(effect.type() != null && effect.value() != null && effect.value() >= 0
                                    && hasText(effect.text()),
                            "Section " + section.id() + " has an invalid consequence.");
                }
            }
        }
        return Map.copyOf(index);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static void require(boolean valid, String message) {
        if (!valid) {
            throw new InvalidAdventureException(message);
        }
    }
}

class InvalidAdventureException extends RuntimeException {
    InvalidAdventureException(String message) {
        super(message);
    }
}
