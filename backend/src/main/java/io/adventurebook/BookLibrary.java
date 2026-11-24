package io.adventurebook;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

@Service
class BookLibrary {
    record Entry(String id, Book book, Map<Integer, Book.Section> sections, Book.Section beginning) {}
    record Summary(String id, String title, String author, Book.Difficulty difficulty, String description) {}

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final Path storage;
    private final JsonMapper json;

    BookLibrary(JsonMapper json, ResourceLoader resources,
                @Value("${adventure.books-dir:./data/books}") Path storage) throws IOException {
        this.json = json;
        this.storage = storage;
        for (String id : List.of("lantern-reef", "last-signal", "winter-orchard")) {
            try (InputStream source = resources.getResource("classpath:stories/" + id + ".json").getInputStream()) {
                entries.put(id, index(id, json.readValue(source, Book.class)));
            }
        }
        Files.createDirectories(storage);
        try (var files = Files.list(storage)) {
            for (Path file : files.filter(path -> path.getFileName().toString().endsWith(".json")).toList()) {
                String id = file.getFileName().toString().replaceFirst("\\.json$", "");
                entries.putIfAbsent(id, index(id, json.readValue(file.toFile(), Book.class)));
            }
        }
    }

    List<Summary> list() {
        return entries.values().stream().map(this::summary)
                .sorted(Comparator.comparing(Summary::title, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Summary::id))
                .toList();
    }

    Entry get(String id) {
        Entry entry = entries.get(id);
        if (entry == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found.");
        }
        return entry;
    }

    Summary add(InputStream source) throws IOException {
        Book book = json.readValue(source, Book.class);
        if (book == null) {
            throw new InvalidAdventureException("The file must contain a book object.");
        }
        String id = UUID.randomUUID().toString();
        Entry entry = index(id, book);
        Path pending = Files.createTempFile(storage, "book-", ".tmp");
        try {
            json.writeValue(pending.toFile(), book);
            Files.move(pending, storage.resolve(id + ".json"), StandardCopyOption.ATOMIC_MOVE);
            entries.put(id, entry);
        } finally {
            Files.deleteIfExists(pending);
        }
        return summary(entry);
    }

    private Entry index(String id, Book book) {
        var sections = book.validate();
        var beginning = book.sections().stream().filter(section -> section.type() == Book.SectionType.BEGIN)
                .findFirst().orElseThrow();
        return new Entry(id, book, sections, beginning);
    }

    private Summary summary(Entry entry) {
        Book book = entry.book();
        return new Summary(entry.id(), book.title(), book.author(), book.difficulty(), entry.beginning().text());
    }
}
