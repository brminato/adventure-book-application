package io.adventurebook;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.DefaultResourceLoader;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdventureTest {
    @TempDir Path storage;
    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void startsWithFullHealthAndCompletesAnEnding() throws Exception {
        var book = library().get("lantern-reef");
        assertThat(Game.play(book, List.of()).health()).isEqualTo(10);
        var ending = Game.play(book, List.of(0, 0));
        assertThat(ending.status()).isEqualTo(Game.Status.COMPLETED);
        assertThat(ending.health()).isEqualTo(10);
        assertThat(ending.choices()).isEmpty();
    }

    @Test
    void damageCanKillEvenWhenTheDestinationIsAnEnding() throws Exception {
        var book = library().get("lantern-reef");
        assertThat(Game.play(book, List.of(1)).health()).isEqualTo(4);
        var death = Game.play(book, List.of(1, 1));
        assertThat(death.status()).isEqualTo(Game.Status.DEAD);
        assertThat(death.health()).isZero();
        assertThat(death.choices()).isEmpty();
        assertThatThrownBy(() -> Game.play(book, List.of(1, 1, 0)))
                .isInstanceOf(InvalidAdventureException.class);
    }

    @Test
    void healingRestoresHealthAndInvalidChoicesAreRejected() throws Exception {
        var book = library().get("lantern-reef");
        assertThat(Game.play(book, List.of(1, 0, 0)).health()).isEqualTo(7);
        assertThatThrownBy(() -> Game.play(book, List.of(2)))
                .isInstanceOf(InvalidAdventureException.class);
    }

    @Test
    void importValidatesTheGraphAndPersistsAcceptedBooks() throws Exception {
        var library = library();
        Book book = library.get("lantern-reef").book();
        var imported = library.add(new ByteArrayInputStream(json.writeValueAsBytes(book)));
        assertThat(library().get(imported.id()).book()).isEqualTo(book);

        var start = book.sections().getFirst();
        var ending = book.sections().getLast();
        var broken = new Book("Broken", "Author", Book.Difficulty.EASY, List.of(start, ending));
        assertThatThrownBy(broken::validate).hasMessageContaining("destination");
        var stranded = new Book.Section(99, "A dead end.", Book.SectionType.NODE, List.of());
        var invalid = new Book("Broken", "Author", Book.Difficulty.EASY, List.of(start, stranded, ending));
        assertThatThrownBy(invalid::validate).hasMessageContaining("needs at least one choice");
        assertThatThrownBy(() -> new Book("Broken", "Author", Book.Difficulty.EASY,
                List.of(ending)).validate()).hasMessageContaining("exactly one beginning");
        assertThatThrownBy(() -> new Book("Broken", "Author", Book.Difficulty.EASY,
                List.of(start)).validate()).hasMessageContaining("at least one ending");
    }

    private BookLibrary library() throws Exception {
        return new BookLibrary(json, new DefaultResourceLoader(), storage);
    }
}
