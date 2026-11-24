package com.knowvault.model;

public record Tag(long id, String name) implements Comparable<Tag> {
    @Override public int compareTo(Tag other) { return name.compareToIgnoreCase(other.name); }
    @Override public String toString() { return name; }
}
