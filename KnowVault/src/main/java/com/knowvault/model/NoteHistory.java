package com.knowvault.model;

import java.time.LocalDateTime;

public record NoteHistory(long id, long noteId, String title, String content, Long categoryId,
                          boolean favorite, String tags, LocalDateTime savedAt) { }
