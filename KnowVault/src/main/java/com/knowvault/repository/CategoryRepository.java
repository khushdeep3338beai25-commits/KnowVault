package com.knowvault.repository;

import com.knowvault.database.DatabaseManager;
import com.knowvault.exception.KnowVaultException;
import com.knowvault.model.Category;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CategoryRepository implements GenericRepository<Category, Long> {
    private final DatabaseManager database;
    public CategoryRepository(DatabaseManager database) { this.database = database; }

    @Override public Category save(Category category) {
        String sql = category.id() == 0 ? "INSERT INTO categories(name,color) VALUES(?,?)" : "UPDATE categories SET name=?,color=? WHERE id=?";
        try (Connection c = database.getConnection(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, category.name().trim()); ps.setString(2, category.color());
            if (category.id() != 0) ps.setLong(3, category.id());
            ps.executeUpdate();
            if (category.id() != 0) return category;
            try (ResultSet keys = ps.getGeneratedKeys()) { keys.next(); return new Category(keys.getLong(1), category.name().trim(), category.color()); }
        } catch (SQLException e) { throw new KnowVaultException("Unable to save category", e); }
    }
    @Override public Optional<Category> findById(Long id) { return findAll().stream().filter(c -> c.id() == id).findFirst(); }
    @Override public List<Category> findAll() {
        List<Category> result = new ArrayList<>();
        try (Connection c = database.getConnection(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT id,name,color FROM categories ORDER BY name COLLATE NOCASE")) {
            while (rs.next()) result.add(new Category(rs.getLong(1), rs.getString(2), rs.getString(3)));
            return result;
        } catch (SQLException e) { throw new KnowVaultException("Unable to load categories", e); }
    }
    @Override public void deleteById(Long id) {
        try (Connection c = database.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM categories WHERE id=?")) { ps.setLong(1,id); ps.executeUpdate(); }
        catch (SQLException e) { throw new KnowVaultException("Unable to delete category", e); }
    }
}
