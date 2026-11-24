package com.knowvault.model;

public record Category(long id, String name, String color) {
    @Override public String toString() { return name; }
}
