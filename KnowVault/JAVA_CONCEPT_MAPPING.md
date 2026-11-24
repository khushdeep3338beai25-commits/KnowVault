# Java Concept Mapping

| Requirement | Working implementation |
|---|---|
| Classes and objects | `Note`, `NoteService`, `DatabaseManager`, `MainController` and other domain/runtime objects |
| Constructors | Model/service/repository constructors inject required state; `Note` provides default and full constructors |
| Encapsulation | `Note` keeps fields private and exposes controlled getters/setters |
| Inheritance | `KnowVaultApp extends Application`; custom cells extend `ListCell`; background operations subclass `Task` |
| Abstraction | Service/repository layers hide JDBC and UI details; records model immutable values |
| Polymorphism | `Node` values hold charts, panes, 3D/fallback content; `GenericRepository` references define persistence behavior |
| Interfaces | `GenericRepository<T, ID>`, `Runnable`, `Callable`, `Supplier`, `Consumer`, `Predicate` |
| `String` | Titles, content, tags, SQL and UI values throughout |
| `StringBuilder` | TXT construction, related-note summaries and export previews |
| `StringBuffer` | Synchronized in-memory service audit text shared safely by worker calls |
| Wrapper classes | Boxed `Long` category IDs/counts, `Integer`/`Number` chart and property values, `Boolean` conversion through JDBC |
| try/catch/finally | Transaction and executor cleanup paths in repositories, database and services |
| `throw` | Validation and missing-history/note failures are explicitly thrown |
| `throws` | JDBC/file helper methods declare checked `SQLException`/`Exception` where callers own recovery |
| Custom exceptions | `KnowVaultException` and `ValidationException` |
| `ArrayList` | Query results, recommendation results and export collections |
| `LinkedList` | History results and FIFO activity queue backing |
| `HashSet` | Recommendation intersection/union calculations |
| `LinkedHashSet` | Stable, duplicate-free note tags and editor tag parsing |
| `TreeSet` | Normalized deterministic tag persistence order |
| `HashMap` | Snapshot of grouped topic frequencies in `TextAnalyzer` |
| `LinkedHashMap` | Navigation insertion order and ranked topic result order |
| `TreeMap` | Alphabetically grouped category analytics |
| `Queue` | Service activity event queue |
| `Deque` | Bounded most-recently-opened note IDs |
| `PriorityQueue` | Highest-similarity related-note ranking |
| `Thread` | Named daemon workers for JavaFX tasks and executor thread factory |
| `Runnable` | Refresh jobs and UI sort actions |
| `ExecutorService` | Fixed background pool in `NoteService` |
| `Callable` | Asynchronous save and controller database jobs returning values |
| `Future` | `saveAsync` returns `Future<Note>` |
| Synchronization | Synchronized recent deque/activity queue and thread-safe audit buffer |
| JavaFX background tasks | Dashboard, note loading, search and saving use `javafx.concurrent.Task` |
| `Connection` | Short-lived SQLite connection per operation |
| `Statement` | Schema DDL, indexes and scalar/list queries |
| `PreparedStatement` | All parameterized note/category/tag/history/search writes and lookups |
| `ResultSet` | Model mapping, generated keys, counts and analytics |
| Transactions | Note/tag saves, restore, sample initialization and delete logging |
| Commit/rollback | Explicit commit on success and rollback in exception paths |
| Lambdas | UI event handlers, comparators, stream transformations and thread factory |
| Functional interfaces | `Consumer`, `Supplier`, `Predicate`, `Runnable` and `Callable` |
| Stream filtering | Favorites, valid tags, history tag parsing and topic stop-word filtering |
| Stream mapping | Tags to normalized names and note/tag presentation |
| Stream sorting | Topic ranking and deterministic tag presentation |
| `groupingBy` | Category totals and word-frequency analytics |
| `Collectors` | Sets, counts, maps and grouped analytics |
| Generics | `GenericRepository<T, ID>`, generic background/supply helpers and typed collections |
| `FileInputStream` | Byte-safe export copying |
| `FileOutputStream` | CSV output and file copying |
| `FileReader` | UTF-8 export preview input |
| `FileWriter` | UTF-8 individual TXT export |
| `BufferedReader` | Efficient line-by-line preview |
| `BufferedWriter` | Buffered TXT and CSV generation |

None of these are isolated “demo” classes: each participates in persistence, UI behavior, analytics, concurrency or export functionality.
