package com.gescal.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;
import java.sql.Statement;

final class JdbcSupport {
    private JdbcSupport() { }

    static long insertAndReturnKey(JdbcTemplate jdbc, String sql, Object... args) {
        KeyHolder holder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
            return ps;
        }, holder);
        if (holder.getKey() == null) throw new IllegalStateException("SQLite n'a pas retourné l'identifiant créé");
        return holder.getKey().longValue();
    }
}

