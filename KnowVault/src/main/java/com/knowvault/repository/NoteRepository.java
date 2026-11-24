package com.knowvault.repository;

import com.knowvault.database.DatabaseManager;
import com.knowvault.exception.KnowVaultException;
import com.knowvault.model.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public final class NoteRepository implements GenericRepository<Note, Long> {
    private final DatabaseManager database;
    private static final DateTimeFormatter DB_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public NoteRepository(DatabaseManager database) { this.database = database; }

    @Override public Note save(Note note) {
        try (Connection c = database.getConnection()) {
            boolean previous = c.getAutoCommit(); c.setAutoCommit(false);
            try {
                boolean isNew = note.getId() == 0;
                if (isNew) insertNote(c, note); else { snapshot(c, note.getId()); updateNote(c, note); }
                replaceTags(c, note);
                log(c, isNew ? "CREATE" : "UPDATE", note.getId(), note.getTitle());
                c.commit(); return findById(note.getId()).orElseThrow();
            } catch (Exception e) {
                c.rollback(); throw e;
            } finally { c.setAutoCommit(previous); }
        } catch (SQLException e) { throw new KnowVaultException("Unable to save note", e); }
    }

    private void insertNote(Connection c, Note note) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO notes(title,content,category_id,favorite,updated_at) VALUES(?,?,?,?,CURRENT_TIMESTAMP)", Statement.RETURN_GENERATED_KEYS)) {
            bindNote(ps, note); ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) { if (keys.next()) note.setId(keys.getLong(1)); }
        }
    }
    private void updateNote(Connection c, Note note) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE notes SET title=?,content=?,category_id=?,favorite=?,updated_at=CURRENT_TIMESTAMP WHERE id=?")) {
            bindNote(ps, note); ps.setLong(5, note.getId());
            if (ps.executeUpdate() == 0) throw new SQLException("Note not found: " + note.getId());
        }
    }
    private void bindNote(PreparedStatement ps, Note note) throws SQLException {
        ps.setString(1, note.getTitle().trim()); ps.setString(2, note.getContent());
        if (note.getCategoryId() == null) ps.setNull(3, Types.BIGINT); else ps.setLong(3, note.getCategoryId());
        ps.setBoolean(4, note.isFavorite());
    }
    private void replaceTags(Connection c, Note note) throws SQLException {
        try (PreparedStatement clear = c.prepareStatement("DELETE FROM note_tags WHERE note_id=?")) { clear.setLong(1,note.getId()); clear.executeUpdate(); }
        for (Tag tag : new TreeSet<>(note.getTags())) {
            String clean = tag.name().strip().toLowerCase(Locale.ROOT);
            if (clean.isEmpty()) continue;
            try (PreparedStatement add = c.prepareStatement("INSERT OR IGNORE INTO tags(name) VALUES(?)")) { add.setString(1,clean); add.executeUpdate(); }
            try (PreparedStatement link = c.prepareStatement("INSERT OR IGNORE INTO note_tags(note_id,tag_id) SELECT ?,id FROM tags WHERE name=? COLLATE NOCASE")) {
                link.setLong(1,note.getId()); link.setString(2,clean); link.executeUpdate();
            }
        }
    }
    private void snapshot(Connection c, long noteId) throws SQLException {
        String sql = """
            INSERT INTO note_history(note_id,title,content,category_id,favorite,tags)
            SELECT n.id,n.title,n.content,n.category_id,n.favorite,
                   COALESCE((SELECT group_concat(t.name, ',') FROM note_tags nt JOIN tags t ON t.id=nt.tag_id WHERE nt.note_id=n.id),'')
            FROM notes n WHERE n.id=?""";
        try (PreparedStatement ps = c.prepareStatement(sql)) { ps.setLong(1,noteId); ps.executeUpdate(); }
    }

    @Override public Optional<Note> findById(Long id) {
        String sql = "SELECT n.*,c.name category_name FROM notes n LEFT JOIN categories c ON c.id=n.category_id WHERE n.id=?";
        try (Connection c = database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1,id); try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(mapNote(c,rs)) : Optional.empty(); }
        } catch (SQLException e) { throw new KnowVaultException("Unable to load note", e); }
    }
    @Override public List<Note> findAll() { return listBySql("SELECT n.*,c.name category_name FROM notes n LEFT JOIN categories c ON c.id=n.category_id ORDER BY n.updated_at DESC", null); }

    public List<Note> search(String query) {
        String q = query == null ? "" : query.strip();
        if (q.isEmpty()) return findAll();
        String sql = """
            SELECT DISTINCT n.*,c.name category_name FROM notes n
            LEFT JOIN categories c ON c.id=n.category_id
            LEFT JOIN note_tags nt ON nt.note_id=n.id LEFT JOIN tags t ON t.id=nt.tag_id
            WHERE lower(n.title) LIKE ? OR lower(n.content) LIKE ? OR lower(t.name) LIKE ?
            ORDER BY n.updated_at DESC""";
        List<Note> result = listBySql(sql, "%" + q.toLowerCase(Locale.ROOT) + "%");
        try (Connection c = database.getConnection(); PreparedStatement ps = c.prepareStatement("INSERT INTO search_history(query,result_count) VALUES(?,?)")) {
            ps.setString(1,q); ps.setInt(2,result.size()); ps.executeUpdate();
        } catch (SQLException e) { throw new KnowVaultException("Unable to record search", e); }
        return result;
    }

    private List<Note> listBySql(String sql, String parameter) {
        List<Note> result = new ArrayList<>();
        try (Connection c = database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (parameter != null) { ps.setString(1,parameter); ps.setString(2,parameter); ps.setString(3,parameter); }
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) result.add(mapNote(c,rs)); }
            return result;
        } catch (SQLException e) { throw new KnowVaultException("Unable to list notes", e); }
    }
    private Note mapNote(Connection c, ResultSet rs) throws SQLException {
        Long category = rs.getObject("category_id") == null ? null : rs.getLong("category_id");
        Note n = new Note(rs.getLong("id"),rs.getString("title"),rs.getString("content"),category,rs.getBoolean("favorite"));
        n.setCategoryName(rs.getString("category_name")); n.setViewCount(rs.getInt("view_count"));
        n.setCreatedAt(parseTime(rs.getString("created_at"))); n.setUpdatedAt(parseTime(rs.getString("updated_at")));
        try (PreparedStatement tags = c.prepareStatement("SELECT t.id,t.name FROM tags t JOIN note_tags nt ON nt.tag_id=t.id WHERE nt.note_id=? ORDER BY t.name")) {
            tags.setLong(1,n.getId()); try (ResultSet tr = tags.executeQuery()) { while (tr.next()) n.getTags().add(new Tag(tr.getLong(1),tr.getString(2))); }
        }
        return n;
    }
    private LocalDateTime parseTime(String value) { return LocalDateTime.parse(value.substring(0,19), DB_TIME); }

    @Override public void deleteById(Long id) {
        try (Connection c = database.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement log = c.prepareStatement("INSERT INTO activity_logs(action,note_id,details) VALUES('DELETE',?,COALESCE((SELECT title FROM notes WHERE id=?),''))");
                 PreparedStatement del = c.prepareStatement("DELETE FROM notes WHERE id=?")) {
                log.setLong(1,id); log.setLong(2,id); log.executeUpdate(); del.setLong(1,id); del.executeUpdate(); c.commit();
            } catch (SQLException e) { c.rollback(); throw e; }
        } catch (SQLException e) { throw new KnowVaultException("Unable to delete note", e); }
    }

    public void incrementViews(long id) {
        try (Connection c = database.getConnection(); PreparedStatement ps = c.prepareStatement("UPDATE notes SET view_count=view_count+1 WHERE id=?")) { ps.setLong(1,id); ps.executeUpdate(); }
        catch (SQLException e) { throw new KnowVaultException("Unable to increment views", e); }
    }
    public List<NoteHistory> history(long noteId) {
        List<NoteHistory> result = new LinkedList<>();
        try (Connection c = database.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT * FROM note_history WHERE note_id=? ORDER BY saved_at DESC,id DESC")) {
            ps.setLong(1,noteId); try (ResultSet rs=ps.executeQuery()) { while(rs.next()) result.add(new NoteHistory(rs.getLong("id"),noteId,rs.getString("title"),rs.getString("content"),rs.getObject("category_id")==null?null:rs.getLong("category_id"),rs.getBoolean("favorite"),rs.getString("tags"),parseTime(rs.getString("saved_at")))); }
            return result;
        } catch(SQLException e) { throw new KnowVaultException("Unable to load history",e); }
    }
    public Note restore(long historyId) {
        try (Connection c=database.getConnection()) {
            c.setAutoCommit(false);
            long noteId;
            try (PreparedStatement get=c.prepareStatement("SELECT * FROM note_history WHERE id=?")) {
                get.setLong(1,historyId); try(ResultSet rs=get.executeQuery()) {
                    if(!rs.next()) throw new KnowVaultException("History version not found");
                    noteId=rs.getLong("note_id"); snapshot(c,noteId);
                    try(PreparedStatement update=c.prepareStatement("UPDATE notes SET title=?,content=?,category_id=?,favorite=?,updated_at=CURRENT_TIMESTAMP WHERE id=?")) {
                        update.setString(1,rs.getString("title")); update.setString(2,rs.getString("content"));
                        if(rs.getObject("category_id")==null) update.setNull(3,Types.BIGINT); else update.setLong(3,rs.getLong("category_id"));
                        update.setBoolean(4,rs.getBoolean("favorite")); update.setLong(5,noteId); update.executeUpdate();
                    }
                    Note restored=new Note(noteId,rs.getString("title"),rs.getString("content"),rs.getObject("category_id")==null?null:rs.getLong("category_id"),rs.getBoolean("favorite"));
                    Arrays.stream(rs.getString("tags").split(",")).filter(s->!s.isBlank()).forEach(s->restored.getTags().add(new Tag(0,s)));
                    replaceTags(c,restored); log(c,"RESTORE",noteId,"Version " + historyId); c.commit();
                }
            } catch(Exception e){ c.rollback(); throw e; }
            return findById(noteId).orElseThrow();
        } catch(SQLException e){ throw new KnowVaultException("Unable to restore history",e); }
    }

    public List<RelatedNote> related(long noteId, int limit) {
        Note source=findById(noteId).orElseThrow(); Set<String> a=source.getTags().stream().map(t->t.name().toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        PriorityQueue<RelatedNote> ranked=new PriorityQueue<>(Comparator.comparingDouble(RelatedNote::similarityPercent).reversed());
        for(Note candidate:findAll()) if(candidate.getId()!=noteId) {
            Set<String> b=candidate.getTags().stream().map(t->t.name().toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
            Set<String> intersection=new HashSet<>(a); intersection.retainAll(b); Set<String> union=new HashSet<>(a); union.addAll(b);
            double similarity=union.isEmpty()?0:(intersection.size()*100.0/union.size()); if(similarity>0) ranked.add(new RelatedNote(candidate,similarity,intersection.size()));
        }
        List<RelatedNote> result=new ArrayList<>(); while(!ranked.isEmpty()&&result.size()<limit) result.add(ranked.poll()); return result;
    }

    public DashboardStats dashboard() {
        int notes=scalar("SELECT COUNT(*) FROM notes"), favorites=scalar("SELECT COUNT(*) FROM notes WHERE favorite=1"), categories=scalar("SELECT COUNT(*) FROM categories"), tags=scalar("SELECT COUNT(*) FROM tags"), views=scalar("SELECT COALESCE(SUM(view_count),0) FROM notes");
        List<TagStat> topTags=new ArrayList<>();
        try(Connection c=database.getConnection(); Statement s=c.createStatement(); ResultSet rs=s.executeQuery("SELECT t.name,COUNT(nt.note_id) used FROM tags t LEFT JOIN note_tags nt ON nt.tag_id=t.id GROUP BY t.id ORDER BY used DESC,t.name LIMIT 8")) { while(rs.next()) topTags.add(new TagStat(rs.getString(1),rs.getInt(2))); }
        catch(SQLException e){ throw new KnowVaultException("Unable to load analytics",e); }
        return new DashboardStats(notes,favorites,categories,tags,views,topTags,
                listBySql("SELECT n.*,c.name category_name FROM notes n LEFT JOIN categories c ON c.id=n.category_id ORDER BY n.updated_at DESC LIMIT 5",null),
                listBySql("SELECT n.*,c.name category_name FROM notes n LEFT JOIN categories c ON c.id=n.category_id ORDER BY n.view_count DESC,n.updated_at DESC LIMIT 5",null));
    }
    private int scalar(String sql) { try(Connection c=database.getConnection(); Statement s=c.createStatement(); ResultSet rs=s.executeQuery(sql)){ return rs.next()?rs.getInt(1):0; } catch(SQLException e){ throw new KnowVaultException("Unable to calculate statistics",e); } }
    private void log(Connection c,String action,long id,String details)throws SQLException { try(PreparedStatement ps=c.prepareStatement("INSERT INTO activity_logs(action,note_id,details) VALUES(?,?,?)")){ ps.setString(1,action);ps.setLong(2,id);ps.setString(3,details);ps.executeUpdate(); } }
}
