# KnowVault – Personal Knowledge Base & Smart Notes

KnowVault is an offline-first JavaFX desktop knowledge manager. Notes, categories, tags, history, searches, activity and view counts are stored in a local SQLite database. The dashboard and analytics are calculated from that database—there are no hardcoded statistics.

## Requirements

- JDK 21 (the project compiles with `--release 21`)
- Maven 3.9+ or internet access on the first Maven Wrapper run
- Windows, macOS or Linux with a graphical desktop

## Run

```bash
./run.sh
```

On Windows, double-click `run.bat`, or run:

```bat
run.bat
```

Direct Maven commands (replace `mvn` with `./mvnw` when Maven is not installed):

```bash
mvn clean test
mvn javafx:run
mvn clean package
```

The persistent database is created at `~/.knowvault/knowvault.db`. Three sample Java notes are inserted only when a brand-new database contains no notes. Delete that file only if you intentionally want a clean database.

## Features

- Create, edit, delete, favorite, categorize and tag notes
- Debounced, background keyword search over title, content and tag names
- Jaccard similarity recommendations based on shared tags
- Automatic pre-edit history snapshots and one-click restoration
- Individual and collection exports to UTF-8 TXT or correctly quoted CSV
- Recently edited, most-viewed, favorites, sorting and persisted view counts
- Database-backed counters, tag/category charts, topic frequency and tag cloud
- Pinterest-inspired blush, dusty-rose and mauve glassmorphism UI with hover/glow states, transitions, animated counters and a rotating JavaFX 3D sphere
- Automatic non-3D fallback when JavaFX reports that `SCENE3D` is unavailable
- Foreign keys, constraints, indexes, transactions and rollback
- Background JavaFX `Task` and executor-based data work

## Project structure

```text
src/main/java/com/knowvault/
├── controller/   UI coordination and background tasks
├── database/     SQLite connection and schema initialization
├── exception/    Domain-specific failures
├── model/        Notes, tags, categories, history and analytics records
├── repository/   Generic persistence contract and JDBC repositories
├── service/      Validation, use cases, concurrency and exports
├── util/         CSV and text analytics helpers
└── view/         Reusable JavaFX shell
```

Resources live under `src/main/resources`; tests live under `src/test/java`.

## IntelliJ IDEA

1. Install JDK 21 and Maven 3.9+.
2. Choose **File → Open** and select this folder's `pom.xml`.
3. Set **Project SDK** and Maven runner JRE to JDK 21.
4. Wait for Maven import.
5. Run the `javafx:run` Maven goal, or run `com.knowvault.KnowVaultApp` using the generated Maven classpath.
6. Run tests from `KnowVaultIntegrationTest` or the Maven `test` goal.

No manual JavaFX SDK/module-path configuration is needed when using Maven.

## Eclipse

1. Install JDK 21, Maven 3.9+, and Eclipse IDE for Java Developers.
2. Choose **File → Import → Maven → Existing Maven Projects** and select this folder.
3. Confirm the project JRE is Java 21 (**Properties → Java Build Path**).
4. Use **Run As → Maven build…**, goal `javafx:run`.
5. Use **Run As → JUnit Test** on the test class, or Maven goal `test`.

If Eclipse reports stale dependencies, choose **Maven → Update Project**.

## Build output

`mvn clean package` produces:

- `target/knowvault-1.0.0.jar`
- `target/lib/` with runtime dependencies for the current platform
- Surefire test reports in `target/surefire-reports/`

For cross-platform distribution, build on the destination OS (OpenJFX native artifacts are platform-specific) or simply distribute the source project and run through Maven.

## Data and privacy

KnowVault makes no network requests at runtime. Exports are written only to the location selected by the user. SQLite connections are short-lived, and foreign keys are enabled for every connection.

## Testing

The JUnit 5 integration suite uses an isolated temporary SQLite database. It tests CRUD, input validation, search, tags, recommendations, history/restore, exports, copying and analytics. See `PROJECT_REPORT.md` for design details and `JAVA_CONCEPT_MAPPING.md` for syllabus traceability.
