package com.gescal.repository;

import com.gescal.entity.ArchiveAnneeEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class ArchiveRepository {
    private final JdbcTemplate jdbc;

    public ArchiveRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public boolean isArchived(int annee) {
        if (!tableExists()) return false;
        Long count = jdbc.queryForObject("select count(*) from archives_annees where annee=?", Long.class, annee);
        return count != null && count > 0;
    }

    public List<ArchiveAnneeEntity> findAll() {
        if (!tableExists()) return List.of();
        return jdbc.query("select annee,archive_le,nb_operations,nb_tournees from archives_annees order by annee desc",
                (rs, row) -> new ArchiveAnneeEntity(rs.getInt("annee"), rs.getString("archive_le"),
                        rs.getLong("nb_operations"), rs.getLong("nb_tournees")));
    }

    @Transactional
    public ArchiveAnneeEntity archive(int annee) {
        if (annee < 2000 || annee > 9999) throw new IllegalArgumentException("L'année d'archive est invalide.");
        createTableIfNecessary();
        if (isArchived(annee)) throw new IllegalStateException("L'année " + annee + " est déjà archivée.");
        long operations = count("select count(*) from operations where anneeop=?", annee);
        long tournees = count("select count(*) from tournees where annee=?", String.valueOf(annee));
        if (operations == 0 && tournees == 0) {
            throw new IllegalArgumentException("Aucune opération ni tournée ne correspond à l'année " + annee + ".");
        }
        String archivedAt = LocalDateTime.now().withNano(0).toString();
        jdbc.update("insert into archives_annees(annee,archive_le,nb_operations,nb_tournees) values(?,?,?,?)",
                annee, archivedAt, operations, tournees);
        return new ArchiveAnneeEntity(annee, archivedAt, operations, tournees);
    }

    public void requireWritable(int annee) {
        if (isArchived(annee)) {
            throw new IllegalStateException("L'année " + annee + " est archivée : ses opérations et tournées sont en lecture seule.");
        }
    }

    private long count(String sql, Object value) {
        Long result = jdbc.queryForObject(sql, Long.class, value);
        return result == null ? 0 : result;
    }

    private boolean tableExists() {
        Long count = jdbc.queryForObject("select count(*) from sqlite_master where type='table' and name='archives_annees'", Long.class);
        return count != null && count > 0;
    }

    private void createTableIfNecessary() {
        jdbc.execute("create table if not exists archives_annees (" +
                "annee integer primary key not null," +
                "archive_le text not null," +
                "nb_operations integer not null," +
                "nb_tournees integer not null)");
    }
}

