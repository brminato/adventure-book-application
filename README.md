# Adventure Book

Browse branching stories, choose a path, and try to reach an ending with health remaining. The catalogue supports title and author search, difficulty filtering, and JSON import. Save a run from the reading screen and resume it from the library in the same browser.

## Run with Docker Compose

```sh
docker compose up --build
```

Open [localhost:8080](http://localhost:8080). The frontend proxies API requests to the backend on the Compose network. The named `books` volume holds imports across restarts.

## Develop locally

- Backend with Java 25 and Maven 3.9. Run `mvn spring-boot:run` from `backend`.
- Frontend with Node 24. Run `npm ci` and `npm start` from `frontend`, then open [localhost:4200](http://localhost:4200). The development server proxies API requests to port 8080.
- Run `mvn -B verify` from `backend` and `npm test -- --watch=false` and `npm run build` from `frontend`.

## Import a book

Use **Import JSON** to add a file up to 256 KB. A book requires a title, author, difficulty (`EASY`, `MEDIUM`, or `HARD`), and sections. Each section has a unique integer ID, text, and type. Exactly one section must be `BEGIN` and at least one must be `END`. Every `BEGIN` or `NODE` needs choices whose `gotoId` refers to an existing section. An `END` has no choices. Choices may carry a `GAIN_HEALTH` or `LOSE_HEALTH` consequence with a nonnegative integer value and text. See [Lantern Reef](backend/src/main/resources/stories/lantern-reef.json) for a complete example.

## Design

- Angular 21 LTS owns the interface, search and filters, and browser-local saves. Spring Boot 4.2 on Java 25 validates books and computes each turn.
- Each run starts with 10 HP. The API replays choice indices against the validated book, clamps health to 0–10, and gives death precedence over reaching an ending. The per-run limit is 1,024 choices.
- The API exposes `GET /api/books`, `POST /api/books/{id}/play` with a JSON `choices` array, and multipart `POST /api/books` with a `book` file. Validation failures return problem details.
- The Compose setup is intended for local use. Imports are shared across users of one instance and persisted as JSON files, with atomic writes. Progress lives in browser storage and is scoped to the current origin.
