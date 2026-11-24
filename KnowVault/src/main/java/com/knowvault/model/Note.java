package com.knowvault.model;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

public final class Note {
    private long id;
    private String title;
    private String content;
    private Long categoryId;
    private String categoryName;
    private boolean favorite;
    private int viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private final Set<Tag> tags = new LinkedHashSet<>();

    public Note() { this(0, "", "", null, false); }
    public Note(long id, String title, String content, Long categoryId, boolean favorite) {
        this.id = id; this.title = title; this.content = content; this.categoryId = categoryId; this.favorite = favorite;
    }
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }
    public int getViewCount() { return viewCount; }
    public void setViewCount(int viewCount) { this.viewCount = viewCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Set<Tag> getTags() { return tags; }
    public String tagsText() { return tags.stream().map(Tag::name).sorted(String.CASE_INSENSITIVE_ORDER).reduce((a,b) -> a + ", " + b).orElse(""); }
    @Override public String toString() { return title; }
}
