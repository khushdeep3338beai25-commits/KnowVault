package com.knowvault.model;

import java.util.List;

public record DashboardStats(int notes, int favorites, int categories, int tags, int totalViews,
                             List<TagStat> topTags, List<Note> recent, List<Note> mostViewed) { }
