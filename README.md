
# KnowVault 🧠
### Your Knowledge. Organized. Connected. Always Accessible.

KnowVault is a Java-based desktop personal knowledge management application designed to help users capture, organize, search, and manage their knowledge in one centralized place.

We developed KnowVault as a group project to provide an organized and searchable offline knowledge space where users can store notes, categorize information, discover related content, and track their knowledge through an intuitive graphical interface.

Our goal is to make personal knowledge management simple, efficient, and accessible without depending on an internet connection.

---

## ✨ Key Features

### 1. Note Management
- Create, view, edit, and delete notes.
- Store note titles and detailed content.
- Organize information in a structured format.
- Maintain note editing history.

### 2. Categories and Tags
- Organize notes using categories.
- Add multiple tags to notes.
- Filter and manage information efficiently.
- Mark important notes as favorites.

### 3. Smart Search
- Search notes using titles and content.
- Find notes through associated tags.
- Sort and filter search results.
- Quickly access relevant information.

### 4. Knowledge Recommendations
- Discover related notes automatically.
- Uses Jaccard similarity to identify content relationships.
- Helps users connect information stored across different notes.

### 5. Edit History
- Maintain previous versions of notes.
- Review earlier changes.
- Restore previous note content when needed.

### 6. Dashboard and Analytics
- View knowledge management statistics.
- Track notes and categories.
- Get an overview of stored information.

### 7. Data Export
- Export notes into TXT format.
- Export structured information into CSV format.

### 8. Knowledge Visualization
- Includes a Knowledge Sphere visualization concept.
- Provides a fallback visualization when the advanced 3D visualization is unavailable.

### 9. Offline Storage
- Uses a local SQLite database.
- Stores information directly on the user's computer.
- Works without requiring an internet connection.

---

## 🛠️ Technology Stack

| Technology | Purpose |
|---|---|
| Java 21 | Core programming language |
| JavaFX 21 | Desktop graphical user interface |
| JavaFX CSS | Application styling |
| SQLite | Local database |
| JDBC | Database connectivity |
| Maven | Dependency and build management |
| JUnit 5 | Unit testing |

---

## 🏗️ Project Architecture

KnowVault follows a layered architecture to maintain separation of concerns, modularity, and code maintainability.

```text
KnowVault
│
├── Model
│   └── Represents application data
│
├── View
│   └── JavaFX user interface
│
├── Controller
│   └── Handles user interactions
│
├── Service
│   └── Application business logic
│
├── Repository
│   └── Data access operations
│
├── Database
│   └── SQLite database management
│
├── Utility
│   └── Reusable helper classes
│
└── Exception
    └── Custom exception handling
```

### Architecture Workflow

1. The user interacts with the JavaFX interface.
2. Controllers receive and process user actions.
3. Services execute the required business logic.
4. Repositories handle database operations.
5. SQLite stores and retrieves application data.
6. The results are displayed through the interface.

---

## 📚 Java Collections Framework

We use the Java Collections Framework to efficiently manage and manipulate application data.

| Collection | Usage |
|---|---|
| ArrayList | Storing and managing lists of notes |
| LinkedList | Dynamic list operations |
| HashSet | Maintaining unique elements |
| LinkedHashSet | Maintaining unique elements with insertion order |
| TreeSet | Maintaining sorted unique elements |
| HashMap | Key-value data management |
| LinkedHashMap | Maintaining insertion order in mappings |
| TreeMap | Sorted key-value mappings |
| Queue | Managing elements in queue order |
| Deque | Supporting operations at both ends |
| PriorityQueue | Priority-based element processing |
| Iterator | Traversing collections |
| Comparable | Natural ordering of objects |
| Comparator | Custom sorting logic |

These collections help us improve data organization, searching, sorting, and application performance.

---

## ⚙️ Installation and Setup

### Prerequisites

Make sure the following are installed:

- Java Development Kit (JDK) 21
- Maven
- Git

### Step 1: Clone the Repository

```bash
git clone <YOUR_REPOSITORY_URL>
```

### Step 2: Navigate to the Project

```bash
cd KnowVault
```

### Step 3: Build the Project

```bash
mvn clean install
```

### Step 4: Run the Application

```bash
mvn javafx:run
```

### Step 5: Run Tests

```bash
mvn test
```

---

## 💾 Database Information

KnowVault uses SQLite for persistent local data storage.

**Database location:**

```text
~/.knowvault/knowvault.db
```

The database stores application information locally, allowing users to access their knowledge without relying on an external database server.

---

## ⚡ Performance and Multithreading

To maintain a responsive user interface, we use background processing for operations that may take additional time.

Our implementation includes:

- JavaFX background tasks.
- A fixed thread pool of three threads.
- Daemon threads for background execution.
- Synchronized structures for managing shared activity and recent-note data.

This helps prevent lengthy operations from blocking the main JavaFX application thread.

---

## 🧪 Testing

We use JUnit 5 to test important application functionality.

Testing helps us verify:
- Application business logic.
- Data management operations.
- Core application functionality.
- Expected application behavior.

**Recorded test status:** 7/7 tests passed.

---

## 🎯 Project Objectives

Our main objectives while developing KnowVault were:

1. Build an offline personal knowledge management system.
2. Implement object-oriented programming principles.
3. Apply the Java Collections Framework in practical scenarios.
4. Understand JavaFX-based desktop application development.
5. Implement database connectivity using JDBC and SQLite.
6. Develop a modular, layered application architecture.
7. Practice exception handling and multithreading.
8. Improve our understanding of software development through collaborative work.

---

## 🚀 Future Scope

The following are potential improvements that could be introduced in future versions:

- Cloud synchronization.
- AI-powered note summarization.
- Advanced knowledge graph visualization.
- Cross-device accessibility.
- Enhanced recommendation algorithms.
- Additional import and export formats.

---

## 👥 Team Project

KnowVault is developed as a collaborative academic group project.

Through this project, we apply our Java programming knowledge to a practical software application while gaining experience in:

- Object-Oriented Programming.
- Java Collections Framework.
- JavaFX development.
- Database Management.
- Multithreading.
- Software Architecture.
- Testing and Debugging.
- Collaborative Development.

---

## 📌 Conclusion

KnowVault demonstrates how Java can be used to develop a practical, modular, and user-friendly desktop application.

By combining JavaFX, SQLite, JDBC, and the Java Collections Framework, we have developed a centralized offline platform for managing, organizing, and connecting personal knowledge.

**KnowVault — Think Better. Organize Smarter. Remember More.**
