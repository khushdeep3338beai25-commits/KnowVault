package com.knowvault.service;

import com.knowvault.exception.KnowVaultException;
import com.knowvault.model.Note;
import com.knowvault.util.CsvUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

public final class ExportService {
    public Path exportText(Note note, Path destination) {
        ensureParent(destination);
        StringBuilder body = new StringBuilder().append(note.getTitle()).append(System.lineSeparator())
                .append("Category: ").append(note.getCategoryName() == null ? "Uncategorized" : note.getCategoryName()).append(System.lineSeparator())
                .append("Tags: ").append(note.tagsText()).append(System.lineSeparator()).append(System.lineSeparator()).append(note.getContent());
        try (FileWriter writer = new FileWriter(destination.toFile(), StandardCharsets.UTF_8); BufferedWriter buffered = new BufferedWriter(writer)) {
            buffered.write(body.toString()); return destination;
        } catch (IOException e) { throw new KnowVaultException("Unable to export text", e); }
    }

    public Path exportText(Collection<Note> notes, Path destination) {
        ensureParent(destination);
        try (FileWriter writer = new FileWriter(destination.toFile(), StandardCharsets.UTF_8); BufferedWriter buffered = new BufferedWriter(writer)) {
            boolean first = true;
            for (Note note : notes) {
                if (!first) buffered.write(System.lineSeparator() + "=".repeat(72) + System.lineSeparator());
                buffered.write(note.getTitle()); buffered.newLine();
                buffered.write("Category: " + (note.getCategoryName() == null ? "Uncategorized" : note.getCategoryName())); buffered.newLine();
                buffered.write("Tags: " + note.tagsText()); buffered.newLine(); buffered.newLine(); buffered.write(note.getContent()); buffered.newLine();
                first = false;
            }
            return destination;
        } catch (IOException e) { throw new KnowVaultException("Unable to export text collection", e); }
    }

    public Path exportCsv(Collection<Note> notes, Path destination) {
        ensureParent(destination);
        try (FileOutputStream output = new FileOutputStream(destination.toFile());
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8))) {
            writer.write("id,title,category,tags,favorite,views,updated,content"); writer.newLine();
            for (Note n : notes) {
                writer.write(Long.toString(n.getId())); writer.write(','); writer.write(CsvUtil.escape(n.getTitle())); writer.write(',');
                writer.write(CsvUtil.escape(n.getCategoryName())); writer.write(','); writer.write(CsvUtil.escape(n.tagsText())); writer.write(',');
                writer.write(Boolean.toString(n.isFavorite())); writer.write(','); writer.write(Integer.toString(n.getViewCount())); writer.write(',');
                writer.write(CsvUtil.escape(String.valueOf(n.getUpdatedAt()))); writer.write(','); writer.write(CsvUtil.escape(n.getContent())); writer.newLine();
            }
            return destination;
        } catch (IOException e) { throw new KnowVaultException("Unable to export CSV", e); }
    }

    public String preview(Path file) {
        StringBuilder result = new StringBuilder();
        try (FileReader reader = new FileReader(file.toFile(), StandardCharsets.UTF_8); BufferedReader buffered = new BufferedReader(reader)) {
            String line; while ((line = buffered.readLine()) != null) result.append(line).append(System.lineSeparator());
            return result.toString();
        } catch (IOException e) { throw new KnowVaultException("Unable to read export", e); }
    }

    public Path copy(Path source, Path destination) {
        ensureParent(destination);
        try (FileInputStream input = new FileInputStream(source.toFile()); FileOutputStream output = new FileOutputStream(destination.toFile())) {
            input.transferTo(output); return destination;
        } catch (IOException e) { throw new KnowVaultException("Unable to copy export", e); }
    }

    private void ensureParent(Path destination) {
        try { Path parent=destination.toAbsolutePath().getParent(); if(parent!=null) Files.createDirectories(parent); }
        catch(IOException e){ throw new KnowVaultException("Unable to create export directory",e); }
    }
}
