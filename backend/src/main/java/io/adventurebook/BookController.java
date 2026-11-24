package io.adventurebook;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.JacksonException;

@RestController
@RequestMapping("/api/books")
class BookController {
    record Turn(List<Integer> choices) {}

    private final BookLibrary library;

    BookController(BookLibrary library) {
        this.library = library;
    }

    @GetMapping
    List<BookLibrary.Summary> books() {
        return library.list();
    }

    @PostMapping("/{id}/play")
    Game.View play(@PathVariable String id, @RequestBody Turn turn) {
        return Game.play(library.get(id), turn.choices());
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    BookLibrary.Summary importBook(@RequestParam("book") MultipartFile file) throws IOException {
        try (var source = file.getInputStream()) {
            return library.add(source);
        } catch (JacksonException exception) {
            throw new InvalidAdventureException("Invalid JSON or unsupported book fields. See the README for the format.");
        }
    }

    @ExceptionHandler(InvalidAdventureException.class)
    ProblemDetail invalid(InvalidAdventureException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }
}
