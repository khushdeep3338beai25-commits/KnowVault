# KnowVault Project Report

## 1. Objective

KnowVault is a private desktop personal knowledge base designed for students and developers. It turns notes into a connected collection through tags, categories, search, revision history, recommendations and usage analytics while remaining fully functional without an internet connection.

## 2. Technology stack

- Java 21 language level
- JavaFX 21 controls, CSS, charts, animation and 3D APIs
- SQLite 3 through Xerial JDBC
- Maven build lifecycle
- JUnit Jupiter 5 integration tests

## 3. Architecture

The application uses a layered design. Models contain state; the view constructs the reusable sidebar/content shell; the controller handles UI events and JavaFX background tasks; services validate and coordinate use cases; repositories translate models to JDBC operations; and `DatabaseManager` owns schema creation and connection configuration.

Dependencies point inward from UI to services and repositories. `GenericRepository<T, ID>` abstracts common persistence operations. Short-lived connections avoid shared-connection threading hazards.

## 4. Database design

Seven required tables are created automatically: `notes`, `categories`, `tags`, `note_tags`, `note_history`, `search_history`, and `activity_logs`. The many-to-many note/tag relation uses a composite primary key. Foreign keys cascade link/history deletion and set a deleted category to null. Checks enforce nonblank note/tag values, boolean favorites and nonnegative view counts. Indexes cover edited-date ordering, category joins, tag joins and version lookup.

Note insert/update, tag replacement and activity logging are one transaction. Updates first copy the previous note and its tag list to `note_history`. Restore snapshots the current version before applying an older one. Each transaction commits on success and rolls back on failure.

## 5. Functional design

The Notes page supports creation, update, deletion, favorite state, category assignment, multiple comma-separated tags, sorting and export. Search is debounced and executed in a JavaFX `Task`; SQL matches lowercased title, content and tag names. Opening a note increments its persistent view count.

Related notes use Jaccard similarity: `shared tags / union of tags × 100`. A priority queue ranks candidates. History stores complete editable state, including tag names, and restoration is transactional.

The dashboard calculates counts, latest notes, most-viewed notes and top tags from SQLite. JavaFX bar/pie charts visualize real values. Analytics additionally tokenizes note text and uses stream grouping to find frequent topics.

## 6. Interface design

The interface uses a Pinterest-inspired blush palette with dusty rose, soft mauve, cream and deep berry accents. CSS supplies airy translucent cards, rounded borders, warm shadows, hover movement, tag bubbles and gentle glow effects. Page changes combine fade and translation transitions; counters animate using `Timeline`. A rose material-lit JavaFX `Sphere` rotates in a perspective `SubScene`. When 3D support is unavailable, a readable fallback card is rendered instead.

The layout has a fixed modern navigation rail and a fluid content area. Lists, charts and forms expand with the window, whose minimum size prevents destructive compression.

## 7. Concurrency and reliability

Database-heavy page loading, searching and saving run outside the JavaFX Application Thread. The service also exposes executor, `Callable`, `Future` and `CompletableFuture` operations. Daemon threads prevent background jobs from keeping the application alive, and executor shutdown is handled during application stop.

Validation rejects missing/oversized titles and excessive tags. Custom exceptions preserve causes while presenting readable messages. Try-with-resources closes JDBC and file handles. CSV fields are escaped according to common CSV rules.

## 8. Testing

`KnowVaultIntegrationTest` creates a fresh temporary SQLite database per test. Seven tests cover:

1. Note CRUD, favorite and tag persistence
2. Title validation
3. Title/content/tag search
4. Jaccard recommendation order
5. History creation and restoration
6. TXT/CSV export and byte-for-byte copying
7. Dashboard and frequent-topic analytics

The verified build result is 7 tests run, 0 failures, 0 errors and 0 skipped.

## 9. Persistence and deployment

The user database resides in `~/.knowvault/knowvault.db`, so it survives application restarts and project rebuilds. `mvn package` copies current-platform runtime dependencies to `target/lib`. Normal execution through the JavaFX Maven plugin automatically selects the appropriate native JavaFX artifacts for the OS.

## 10. Limitations and future scope

The current similarity model intentionally uses interpretable shared tags rather than semantic embeddings, keeping the app private and lightweight. SQLite LIKE search is sufficient for a personal library; a future large-scale version could use SQLite FTS5. Exports are TXT and CSV as requested; Markdown/PDF and encrypted vaults are natural future additions.
