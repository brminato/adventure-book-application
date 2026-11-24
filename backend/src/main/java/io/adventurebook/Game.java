package io.adventurebook;

import java.util.List;
import java.util.stream.IntStream;

final class Game {
    private static final int MAX_HEALTH = 10;
    private static final int MAX_CHOICES = 1024;

    enum Status { PLAYING, COMPLETED, DEAD, LIMIT_REACHED }
    record Choice(int index, String description) {}
    record View(String title, int sectionId, String text, int health, Status status,
                String consequence, List<Choice> choices) {}

    static View play(BookLibrary.Entry entry, List<Integer> history) {
        if (history == null || history.size() > MAX_CHOICES) {
            throw new InvalidAdventureException("Choice history is missing or too long.");
        }
        Book.Section section = entry.beginning();
        int health = MAX_HEALTH;
        String consequence = null;
        for (Integer choice : history) {
            if (health == 0 || section.type() == Book.SectionType.END || choice == null
                    || choice < 0 || choice >= section.options().size()) {
                throw new InvalidAdventureException("Invalid choice for section " + section.id() + ".");
            }
            Book.Option option = section.options().get(choice);
            Book.Consequence effect = option.consequence();
            consequence = effect == null ? null : effect.text();
            if (effect != null) {
                health = switch (effect.type()) {
                    case LOSE_HEALTH -> Math.max(0, health - effect.value());
                    case GAIN_HEALTH -> health + Math.min(MAX_HEALTH - health, effect.value());
                };
            }
            section = entry.sections().get(option.gotoId());
        }
        Status status = status(section, health, history.size());
        List<Choice> choices = List.of();
        if (status == Status.PLAYING) {
            List<Book.Option> options = section.options();
            choices = IntStream.range(0, options.size())
                    .mapToObj(i -> new Choice(i, options.get(i).description())).toList();
        }
        return new View(entry.book().title(), section.id(), section.text(), health, status, consequence, choices);
    }

    private static Status status(Book.Section section, int health, int turns) {
        if (health == 0) {
            return Status.DEAD;
        }
        if (section.type() == Book.SectionType.END) {
            return Status.COMPLETED;
        }
        return turns == MAX_CHOICES ? Status.LIMIT_REACHED : Status.PLAYING;
    }
}
