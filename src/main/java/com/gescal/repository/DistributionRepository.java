package com.gescal.repository;

import com.gescal.entity.DistributionEntity;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class DistributionRepository {
    private final JdbcTemplate jdbc;
    public DistributionRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<DistributionEntity> findAll(Integer annee) {
        String sql = "select d.iddistrib,d.idtourneedistrib,d.anneedistrib,d.nbdistrib,t.numtournee,t.libtournee from distribution d left join tournees t on t.idtournee=d.idtourneedistrib";
        if (annee != null) return jdbc.query(sql + " where d.anneedistrib=? order by t.numtournee", this::map, annee);
        return jdbc.query(sql + " order by d.anneedistrib desc,t.numtournee", this::map);
    }

    public Optional<DistributionEntity> findById(long id) {
        try { return Optional.ofNullable(jdbc.queryForObject(
                "select d.iddistrib,d.idtourneedistrib,d.anneedistrib,d.nbdistrib,t.numtournee,t.libtournee from distribution d left join tournees t on t.idtournee=d.idtourneedistrib where d.iddistrib=?", this::map, id)); }
        catch (EmptyResultDataAccessException ex) { return Optional.empty(); }
    }

    private DistributionEntity map(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        return new DistributionEntity(rs.getLong("iddistrib"), rs.getLong("idtourneedistrib"), (Integer) rs.getObject("anneedistrib"),
                (Integer) rs.getObject("nbdistrib"), (Integer) rs.getObject("numtournee"), rs.getString("libtournee"));
    }

    public long save(Long id, long tourneeId, Integer annee, Integer nombre) {
        if (id == null) return JdbcSupport.insertAndReturnKey(jdbc, "insert into distribution(idtourneedistrib,anneedistrib,nbdistrib) values(?,?,?)", tourneeId, annee, nombre);
        jdbc.update("update distribution set idtourneedistrib=?,anneedistrib=?,nbdistrib=? where iddistrib=?", tourneeId, annee, nombre, id);
        return id;
    }

    public void deleteById(long id) { jdbc.update("delete from distribution where iddistrib=?", id); }
}

