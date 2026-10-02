package com.gescal.repository;

import com.gescal.entity.ReglementEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ReglementRepository {
    private final JdbcTemplate jdbc;
    public ReglementRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<ReglementEntity> findAll() {
        return jdbc.query("select idreglement,libreglement from reglements order by idreglement",
                (rs, row) -> new ReglementEntity(rs.getLong("idreglement"), rs.getString("libreglement")));
    }
}

