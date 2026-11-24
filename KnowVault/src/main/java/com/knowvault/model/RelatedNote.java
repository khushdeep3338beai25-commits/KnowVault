package com.knowvault.model;

public record RelatedNote(Note note, double similarityPercent, int sharedTags) { }
