package com.knowvault;

import com.knowvault.database.DatabaseManager;
import com.knowvault.exception.ValidationException;
import com.knowvault.model.*;
import com.knowvault.repository.CategoryRepository;
import com.knowvault.repository.NoteRepository;
import com.knowvault.service.ExportService;
import com.knowvault.service.NoteService;
import com.knowvault.util.TextAnalyzer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class KnowVaultIntegrationTest {
    @TempDir Path temp;
    private DatabaseManager database;
    private NoteRepository repository;
    private NoteService service;
    private Category category;

    @BeforeEach void setUp() throws Exception {
        database=new DatabaseManager(temp.resolve("test.db"));
        try(Connection c=database.getConnection(); Statement s=c.createStatement()){
            s.executeUpdate("DELETE FROM activity_logs");s.executeUpdate("DELETE FROM search_history");s.executeUpdate("DELETE FROM note_history");
            s.executeUpdate("DELETE FROM note_tags");s.executeUpdate("DELETE FROM tags");s.executeUpdate("DELETE FROM notes");s.executeUpdate("DELETE FROM categories");
        }
        repository=new NoteRepository(database);service=new NoteService(repository);
        category=new CategoryRepository(database).save(new Category(0,"Learning","#8B5CF6"));
    }
    @AfterEach void tearDown(){service.close();database.close();}

    @Test void createReadUpdateDeletePersistsTagsAndFavorite(){
        Note note=note("Generics","Type-safe reusable code","java","generics");note.setFavorite(true);
        Note saved=service.save(note);assertTrue(saved.getId()>0);assertEquals(2,saved.getTags().size());assertTrue(saved.isFavorite());
        saved.setTitle("Java Generics");Note updated=service.save(saved);assertEquals("Java Generics",updated.getTitle());
        service.delete(updated.getId());assertTrue(repository.findById(updated.getId()).isEmpty());
    }

    @Test void validatesRequiredTitle(){
        Note note=note(" ","body","tag");assertThrows(ValidationException.class,()->service.save(note));
    }

    @Test void searchesTitleContentAndTagAndRecordsResults(){
        service.save(note("Streams","map and filter pipelines","functional"));service.save(note("Database","prepared statements","jdbc"));
        assertEquals(1,service.search("streams").size());assertEquals(1,service.search("filter").size());assertEquals(1,service.search("jdbc").size());
    }

    @Test void ranksRecommendationsByJaccardTagSimilarity(){
        Note source=service.save(note("Source","","java","collections"));
        service.save(note("Close","","java","collections","study"));service.save(note("Far","","java","jdbc"));service.save(note("None","","design"));
        List<RelatedNote> related=service.related(source.getId());assertEquals(2,related.size());assertEquals("Close",related.getFirst().note().getTitle());assertTrue(related.getFirst().similarityPercent()>related.getLast().similarityPercent());
    }

    @Test void createsHistoryAndRestoresPreviousVersion(){
        Note note=service.save(note("Version one","old text","history"));note.setTitle("Version two");note.setContent("new text");service.save(note);
        List<NoteHistory> history=service.history(note.getId());assertEquals(1,history.size());Note restored=service.restore(history.getFirst().id());
        assertEquals("Version one",restored.getTitle());assertEquals("old text",restored.getContent());assertEquals(2,service.history(note.getId()).size());
    }

    @Test void exportsTxtCsvAndCopiesFiles() throws Exception {
        Note note=service.save(note("CSV, Quoting","Line one\nLine \"two\"","export"));ExportService exporter=new ExportService();
        Path txt=exporter.exportText(note,temp.resolve("out/note.txt"));assertTrue(exporter.preview(txt).contains("CSV, Quoting"));
        Path collectionTxt=exporter.exportText(List.of(note),temp.resolve("out/collection.txt"));assertTrue(Files.readString(collectionTxt).contains("Line \"two\""));
        Path csv=exporter.exportCsv(List.of(note),temp.resolve("out/notes.csv"));String value=Files.readString(csv);assertTrue(value.contains("\"CSV, Quoting\""));assertTrue(value.contains("\"Line one\nLine \"\"two\"\"\""));
        Path copy=exporter.copy(csv,temp.resolve("copy.csv"));assertArrayEquals(Files.readAllBytes(csv),Files.readAllBytes(copy));
    }

    @Test void dashboardAndTopicAnalyticsReflectDatabase(){
        Note first=service.save(note("Java Streams","Java map filter","java","streams"));service.save(note("JDBC","Java database","java","jdbc"));service.open(first.getId());
        DashboardStats stats=service.dashboard();assertEquals(2,stats.notes());assertEquals(3,stats.tags());assertEquals(1,stats.totalViews());assertEquals("java",stats.topTags().getFirst().tag());assertEquals(2,stats.topTags().getFirst().count());
        assertEquals(3,TextAnalyzer.frequentTopics(service.all(),10).get("java"));
    }

    private Note note(String title,String content,String... tags){Note n=new Note(0,title,content,category.id(),false);for(String tag:tags)n.getTags().add(new Tag(0,tag));return n;}
}
