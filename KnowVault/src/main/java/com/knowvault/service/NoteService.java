package com.knowvault.service;

import com.knowvault.exception.ValidationException;
import com.knowvault.model.*;
import com.knowvault.repository.NoteRepository;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

public final class NoteService implements AutoCloseable {
    private final NoteRepository notes;
    private final ExecutorService executor = Executors.newFixedThreadPool(3, r -> {
        Thread t = new Thread(r, "knowvault-worker"); t.setDaemon(true); return t;
    });
    private final Deque<Long> recentIds = new ArrayDeque<>();
    private final Queue<String> activityQueue = new LinkedList<>();
    private final StringBuffer synchronizedAudit = new StringBuffer();

    public NoteService(NoteRepository notes) { this.notes = notes; }

    public Note save(Note note) {
        validate(note); Note saved = notes.save(note); recordActivity("Saved " + saved.getId()); return saved;
    }
    public Future<Note> saveAsync(Note note) { return executor.submit((Callable<Note>) () -> save(note)); }
    public List<Note> all() { return notes.findAll(); }
    public List<Note> search(String query) { return notes.search(query); }
    public CompletableFuture<List<Note>> searchAsync(String query) { return supply(() -> notes.search(query)); }
    public void delete(long id) { notes.deleteById(id); recordActivity("Deleted " + id); }
    public Note open(long id) {
        notes.incrementViews(id);
        synchronized (recentIds) { recentIds.remove(id); recentIds.addFirst(id); while (recentIds.size() > 10) recentIds.removeLast(); }
        return notes.findById(id).orElseThrow(() -> new ValidationException("Note no longer exists"));
    }
    public List<NoteHistory> history(long id) { return notes.history(id); }
    public Note restore(long historyId) { return notes.restore(historyId); }
    public List<RelatedNote> related(long id) { return notes.related(id,5); }
    public DashboardStats dashboard() { return notes.dashboard(); }
    public <T> CompletableFuture<T> supply(Supplier<T> supplier) { return CompletableFuture.supplyAsync(supplier,executor); }
    public Runnable refreshJob(Runnable callback) { return () -> { callback.run(); recordActivity("Background refresh"); }; }
    public List<Long> sessionRecentIds() { synchronized (recentIds) { return List.copyOf(recentIds); } }
    public synchronized List<String> drainActivities() { List<String> values=new ArrayList<>(); while(!activityQueue.isEmpty()) values.add(activityQueue.poll()); return values; }
    public String auditText() { return synchronizedAudit.toString(); }

    private void validate(Note note) {
        if (note == null) throw new ValidationException("Note is required");
        if (note.getTitle() == null || note.getTitle().isBlank()) throw new ValidationException("Title is required");
        if (note.getTitle().strip().length() > 200) throw new ValidationException("Title must be 200 characters or fewer");
        if (note.getContent() == null) note.setContent("");
        if (note.getTags().size() > 30) throw new ValidationException("A note can have at most 30 tags");
    }
    private synchronized void recordActivity(String value) { activityQueue.offer(value); synchronizedAudit.append(value).append(System.lineSeparator()); }
    @Override public void close() { executor.shutdown(); try { if(!executor.awaitTermination(2,TimeUnit.SECONDS)) executor.shutdownNow(); } catch(InterruptedException e){ executor.shutdownNow(); Thread.currentThread().interrupt(); } }
}
