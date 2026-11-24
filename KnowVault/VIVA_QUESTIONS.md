# KnowVault Viva Questions and Answers

1. **What problem does KnowVault solve?**  
   It provides an offline personal knowledge base where notes can be organized, searched, connected, revised and analyzed without sending data to a server.

2. **Why was JavaFX selected?**  
   JavaFX provides native desktop controls, CSS, charts, animation, concurrency utilities and 3D rendering in one Java ecosystem.

3. **Why use SQLite?**  
   SQLite is embedded, transactional, portable and requires no database server, making it appropriate for a private offline desktop app.

4. **What is the application's architecture?**  
   It is layered into model, view, controller, service, repository, database, utility and exception packages.

5. **What does the model layer contain?**  
   Domain state such as notes, tags, categories, history versions, related-note results and dashboard statistics.

6. **What is the repository layer responsible for?**  
   It executes JDBC operations and maps relational rows to domain models while hiding SQL from services and UI code.

7. **What is the role of `NoteService`?**  
   It validates notes, coordinates repository operations, maintains session recency/activity structures and owns background execution.

8. **How is encapsulation demonstrated?**  
   `Note` fields are private and accessed through methods, so callers do not directly manipulate its storage representation.

9. **Where is inheritance used?**  
   The application extends JavaFX `Application`, cells extend `ListCell`, and background jobs subclass JavaFX `Task`.

10. **How is polymorphism used in the UI?**  
    Different charts, panes, controls and 3D/fallback components are handled through the common JavaFX `Node` type.

11. **Why is `GenericRepository<T, ID>` useful?**  
    It defines a type-safe common CRUD contract while allowing each repository to implement entity-specific SQL.

12. **How are tags represented in the database?**  
    Tags have their own table and are related to notes through the `note_tags` many-to-many join table.

13. **Why does `note_tags` have a composite primary key?**  
    `(note_id, tag_id)` prevents the same tag from being attached to one note more than once.

14. **How does search work?**  
    A parameterized SQL query performs case-insensitive partial matching against note title, content and joined tag names.

15. **Why are search parameters not concatenated into SQL?**  
    `PreparedStatement` safely binds user input, avoids quoting errors and protects against SQL injection.

16. **What makes search “instant”?**  
    A short `PauseTransition` debounces typing, after which a background JavaFX `Task` updates the visible list.

17. **How are related notes calculated?**  
    Jaccard similarity divides the number of shared tags by the size of the union of both notes' tag sets.

18. **Why use a priority queue for recommendations?**  
    It naturally retrieves the highest-scoring related notes first without manually scanning for each next result.

19. **How is edit history captured?**  
    Before every update, the current title, content, category, favorite state and tag list are inserted into `note_history`.

20. **Why snapshot before restoring a version?**  
    It preserves the current state, making restoration reversible through a later history restore.

21. **What is a database transaction?**  
    It groups multiple statements into one atomic unit that either commits completely or rolls back completely.

22. **Where are transactions important in KnowVault?**  
    Note/tag saves, history restore, sample initialization and deletion/activity logging use transactions.

23. **What happens if tag insertion fails during a note update?**  
    The transaction rolls back, so the note, its history snapshot and its tags remain consistent.

24. **How are foreign keys enabled in SQLite?**  
    `PRAGMA foreign_keys = ON` is executed for every newly opened JDBC connection.

25. **What indexes are defined and why?**  
    Indexes cover updated-time sorting, category lookup, tag joins and note-history retrieval to speed common queries.

26. **How is data retained after application restart?**  
    SQLite writes to `~/.knowvault/knowvault.db`, separate from compiled files and build output.

27. **How are dashboard statistics produced?**  
    SQL counts, sums, groups and sorted queries read live values from notes, tags and categories.

28. **What is the difference between recently edited and frequently viewed?**  
    Recently edited sorts by `updated_at`; frequently viewed sorts by the persisted `view_count`.

29. **When is view count incremented?**  
    It increments when an existing note is opened in the editor, not merely when it appears in a list.

30. **How are frequent topics identified?**  
    Note text is tokenized, common stop words are removed, and Stream `groupingBy` plus counting ranks tokens.

31. **Why keep database work off the JavaFX Application Thread?**  
    Blocking that thread would freeze painting and input, so background work keeps the UI responsive.

32. **What is the difference between `Runnable` and `Callable`?**  
    `Runnable` returns no value, while `Callable<T>` returns a value and may throw checked exceptions.

33. **What is a `Future`?**  
    It represents the eventual result of an asynchronous computation, such as an asynchronously saved note.

34. **Why use `ExecutorService` instead of creating every worker manually?**  
    A bounded reusable pool controls concurrency, reuses threads and offers managed shutdown.

35. **Where is synchronization required?**  
    Session recency and activity queues can be accessed from worker threads, so their compound operations are synchronized.

36. **Why are tags stored in a `LinkedHashSet` in `Note`?**  
    A set prevents duplicates, while linked insertion order gives stable presentation.

37. **Why use a `TreeSet` during tag persistence?**  
    It creates a deterministic alphabetical order and uses `Tag`'s comparison behavior.

38. **Why are `HashSet`s used in similarity calculation?**  
    Set intersection and union directly model shared and combined tags and are efficient for membership operations.

39. **How are TXT files written?**  
    A UTF-8 `FileWriter` wrapped by `BufferedWriter` writes the title, category, tags and content.

40. **How are CSV special characters handled?**  
    Every text field is quoted and internal quotes are doubled, preserving commas, quotes and newlines.

41. **Why use try-with-resources?**  
    It closes connections, statements, result sets and file streams reliably even when exceptions occur.

42. **What custom exceptions exist?**  
    `KnowVaultException` represents application failures, and `ValidationException` specializes it for invalid input.

43. **How does the 3D sphere work?**  
    A material-lit `Sphere` sits in a depth-buffered `SubScene` with a perspective camera and infinite Y-axis rotation transition.

44. **What is the graceful 3D fallback?**  
    The controller checks JavaFX `ConditionalFeature.SCENE3D`; unsupported systems receive a styled explanatory card instead.

45. **How are animated counters implemented?**  
    A JavaFX `Timeline` interpolates an integer property from zero to the live statistic.

46. **How are page transitions implemented?**  
    `FadeTransition` and `TranslateTransition` run together in a `ParallelTransition` whenever content changes.

47. **What does the test suite isolate?**  
    Every test uses its own temporary SQLite file and clears seeded rows before arranging test data.

48. **Which feature tests demonstrate tag correctness?**  
    CRUD verifies tag persistence, search finds a note by tag, recommendations compare tag sets, and analytics counts usage.

49. **Why are the JavaFX dependencies managed by Maven?**  
    Maven resolves matching modules and native libraries, avoiding manual SDK path configuration in IDEs.

50. **What would be a sensible future improvement?**  
    SQLite FTS5 could improve search for very large vaults while preserving offline operation and the same layered architecture.
