package com.gescal.repository;

import com.gescal.entity.AdresseEntity;
import com.gescal.entity.ClientEntity;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class ClientRepository {
    private final JdbcTemplate jdbc;
    public ClientRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<ClientEntity> findAll(String recherche) {
        String sql = select();
        List<Object> args = new ArrayList<>();
        if (recherche != null && !recherche.isBlank()) {
            sql += " where lower(coalesce(c.nom,'') || ' ' || coalesce(c.prenom,'') || ' ' || coalesce(c.mail,'') || ' ' || coalesce(a.ville,'')) like ?";
            args.add("%" + recherche.trim().toLowerCase() + "%");
        }
        return jdbc.query(sql + " order by c.nom collate nocase,c.prenom collate nocase", this::map, args.toArray());
    }

    public Optional<ClientEntity> findById(long id) {
        try { return Optional.ofNullable(jdbc.queryForObject(select() + " where c.idclient=?", this::map, id)); }
        catch (EmptyResultDataAccessException ex) { return Optional.empty(); }
    }

    private String select() {
        return "select c.idclient,c.nom,c.prenom,c.mail,c.remarque,a.idadresse,a.nomrue,a.complement,a.cp,a.ville " +
                "from clients c left join adresses a on a.idadresse=c.idadresse";
    }

    private ClientEntity map(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        AdresseEntity adresse = new AdresseEntity(rs.getLong("idadresse"), rs.getString("nomrue"),
                rs.getString("complement"), rs.getString("cp"), rs.getString("ville"));
        return new ClientEntity(rs.getLong("idclient"), rs.getString("nom"), rs.getString("prenom"),
                rs.getString("mail"), rs.getString("remarque"), adresse);
    }

    @Transactional
    public long save(Long id, String nom, String prenom, String mail, String remarque,
                     String nomRue, String complement, String cp, String ville) {
        validateFields(id, nom, nomRue, cp, ville);
        if (id == null) {
            long adresseId = JdbcSupport.insertAndReturnKey(jdbc,
                    "insert into adresses(nomrue,complement,cp,ville) values(?,?,?,?)", nomRue, complement, cp, ville);
            return JdbcSupport.insertAndReturnKey(jdbc,
                    "insert into clients(nom,prenom,mail,idadresse,remarque) values(?,?,?,?,?)", nom, prenom, mail, adresseId, remarque);
        }
        ClientEntity current = findById(id).orElseThrow();
        long adresseId = current.adresseId();
        if (adresseId == 0) {
            adresseId = JdbcSupport.insertAndReturnKey(jdbc,
                    "insert into adresses(nomrue,complement,cp,ville) values(?,?,?,?)", nomRue, complement, cp, ville);
            jdbc.update("update clients set idadresse=? where idclient=?", adresseId, id);
        } else {
            jdbc.update("update adresses set nomrue=?,complement=?,cp=?,ville=? where idadresse=?",
                    nomRue, complement, cp, ville, adresseId);
        }
        jdbc.update("update clients set nom=?,prenom=?,mail=?,remarque=? where idclient=?", nom, prenom, mail, remarque, id);
        return id;
    }

    private void validateFields(Long id, String nom, String nomRue, String cp, String ville) {
        if (nom == null || nom.isBlank()) throw new IllegalArgumentException("Le nom est obligatoire.");
        if (id == null) {
            if (nomRue == null || nomRue.isBlank()) throw new IllegalArgumentException("La rue est obligatoire.");
            if (cp == null || !cp.matches("\\d{5}")) throw new IllegalArgumentException("Le code postal doit contenir 5 chiffres.");
            if (ville == null || ville.isBlank()) throw new IllegalArgumentException("La ville est obligatoire.");
        } else if (cp != null && !cp.isBlank() && !cp.matches("\\d{5}")) {
            throw new IllegalArgumentException("Le code postal doit contenir 5 chiffres.");
        }
    }

    @Transactional
    public boolean deleteById(long id) {
        Long used = jdbc.queryForObject("select count(*) from operations where iduserop=?", Long.class, id);
        if (used != null && used > 0) return false;
        Optional<ClientEntity> current = findById(id);
        jdbc.update("delete from clients where idclient=?", id);
        current.filter(c -> c.adresseId() > 0).ifPresent(c -> jdbc.update("delete from adresses where idadresse=?", c.adresseId()));
        return true;
    }
}
