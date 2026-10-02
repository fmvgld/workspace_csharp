package com.gescal.repository;

import com.gescal.entity.TourneeEntity;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TourneeRepository {
    private final JdbcTemplate jdbc;
    private final ArchiveRepository archives;
    public TourneeRepository(JdbcTemplate jdbc, ArchiveRepository archives) {
        this.jdbc = jdbc;
        this.archives = archives;
    }

    public List<TourneeEntity> findAll(String annee) {
        String sql = "select idtournee,numtournee,libtournee,annee from tournees";
        if (annee != null && !annee.isBlank()) return jdbc.query(sql + " where annee=? order by numtournee,libtournee", this::map, annee);
        return jdbc.query(sql + " order by annee desc,numtournee,libtournee", this::map);
    }

    public Optional<TourneeEntity> findById(long id) {
        try { return Optional.ofNullable(jdbc.queryForObject("select idtournee,numtournee,libtournee,annee from tournees where idtournee=?", this::map, id)); }
        catch (EmptyResultDataAccessException ex) { return Optional.empty(); }
    }

    private TourneeEntity map(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        return new TourneeEntity(rs.getLong("idtournee"), (Integer) rs.getObject("numtournee"), rs.getString("libtournee"), rs.getString("annee"));
    }

    public long save(Long id, Integer numero, String libelle, String annee) {
        validate(id, numero, libelle, annee);
        if (id != null) {
            TourneeEntity current = findById(id).orElseThrow(() -> new IllegalArgumentException("Tournée introuvable."));
            archives.requireWritable(Integer.parseInt(current.annee()));
        }
        archives.requireWritable(Integer.parseInt(annee));
        if (id == null) return JdbcSupport.insertAndReturnKey(jdbc, "insert into tournees(numtournee,libtournee,annee) values(?,?,?)", numero, libelle, annee);
        jdbc.update("update tournees set numtournee=?,libtournee=?,annee=? where idtournee=?", numero, libelle, annee, id);
        return id;
    }

    private void validate(Long id, Integer numero, String libelle, String annee) {
        if (numero == null || numero <= 0) throw new IllegalArgumentException("Le numéro de tournée est obligatoire et doit être positif.");
        if (libelle == null || libelle.isBlank()) throw new IllegalArgumentException("Le libellé de la tournée est obligatoire.");
        if (annee == null || !annee.matches("\\d{4}")) throw new IllegalArgumentException("L'année doit contenir 4 chiffres.");
        String sql = "select count(*) from tournees where annee=? and numtournee=? and lower(trim(libtournee))=lower(trim(?))";
        Long duplicates;
        if (id == null) duplicates = jdbc.queryForObject(sql, Long.class, annee, numero, libelle);
        else duplicates = jdbc.queryForObject(sql + " and idtournee<>?", Long.class, annee, numero, libelle, id);
        if (duplicates != null && duplicates > 0) {
            throw new IllegalArgumentException("Une tournée " + numero + " - " + libelle.trim() + " existe déjà pour " + annee + ".");
        }
    }

    public boolean deleteById(long id) {
        TourneeEntity current = findById(id).orElseThrow(() -> new IllegalArgumentException("Tournée introuvable."));
        archives.requireWritable(Integer.parseInt(current.annee()));
        Long used = jdbc.queryForObject("select (select count(*) from operations where idtourneeop=?) + (select count(*) from distribution where idtourneedistrib=?)", Long.class, id, id);
        if (used != null && used > 0) return false;
        jdbc.update("delete from tournees where idtournee=?", id);
        return true;
    }
}
