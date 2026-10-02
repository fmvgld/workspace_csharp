package com.gescal.repository;

import com.gescal.entity.OperationEntity;
import com.gescal.dto.ReceiptData;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

@Repository
public class OperationRepository {
    private final JdbcTemplate jdbc;
    private final ArchiveRepository archives;
    public OperationRepository(JdbcTemplate jdbc, ArchiveRepository archives) {
        this.jdbc = jdbc;
        this.archives = archives;
    }

    public List<OperationEntity> findAll(Integer annee, String recherche) {
        StringBuilder sql = new StringBuilder(select()).append(" where 1=1");
        List<Object> args = new ArrayList<>();
        if (annee != null) { sql.append(" and o.anneeop=?"); args.add(annee); }
        if (recherche != null && !recherche.isBlank()) {
            sql.append(" and lower(coalesce(c.nom,'') || ' ' || coalesce(c.prenom,'') || ' ' || coalesce(t.libtournee,'')) like ?");
            args.add("%" + recherche.trim().toLowerCase() + "%");
        }
        return jdbc.query(sql.append(" order by o.datesaisie desc,o.idoperation desc").toString(), this::map, args.toArray());
    }

    public Optional<OperationEntity> findById(long id) {
        try { return Optional.ofNullable(jdbc.queryForObject(select() + " where o.idoperation=?", this::map, id)); }
        catch (EmptyResultDataAccessException ex) { return Optional.empty(); }
    }

    private String select() {
        return "select o.idoperation,o.datesaisie,o.iduserop,c.nom,c.prenom,o.idtourneeop,t.numtournee,t.libtournee," +
                "o.montant,o.idreglementop,r.libreglement,o.recu,o.enveloppe,o.envoirecu,o.recuenvoye,o.anneeop,o.dateenvoi " +
                "from operations o left join clients c on c.idclient=o.iduserop left join tournees t on t.idtournee=o.idtourneeop left join reglements r on r.idreglement=o.idreglementop";
    }

    private OperationEntity map(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        return new OperationEntity(rs.getLong("idoperation"), rs.getString("datesaisie"), rs.getLong("iduserop"), rs.getString("nom"),
                rs.getString("prenom"), rs.getLong("idtourneeop"), (Integer) rs.getObject("numtournee"), rs.getString("libtournee"),
                rs.getDouble("montant"), rs.getLong("idreglementop"), rs.getString("libreglement"), rs.getInt("recu") != 0,
                rs.getInt("enveloppe") != 0, rs.getInt("envoirecu") != 0, rs.getInt("recuenvoye") != 0,
                (Integer) rs.getObject("anneeop"), rs.getString("dateenvoi"));
    }

    public long save(Long id, String dateSaisie, long clientId, long tourneeId, double montant, long reglementId,
                     boolean recu, boolean enveloppe, boolean envoiRecu, boolean recuEnvoye, Integer annee, String dateEnvoi) {
        if (annee == null) throw new IllegalArgumentException("L'année de l'opération est obligatoire.");
        if (id != null) {
            OperationEntity current = findById(id).orElseThrow(() -> new IllegalArgumentException("Opération introuvable."));
            if (current.annee() != null) archives.requireWritable(current.annee());
        }
        archives.requireWritable(annee);
        Object[] values = {dateSaisie, clientId, tourneeId, montant, reglementId, bool(recu), bool(enveloppe), bool(envoiRecu), bool(recuEnvoye), annee, blankToNull(dateEnvoi)};
        if (id == null) return JdbcSupport.insertAndReturnKey(jdbc,
                "insert into operations(datesaisie,iduserop,idtourneeop,montant,idreglementop,recu,enveloppe,envoirecu,recuenvoye,anneeop,dateenvoi) values(?,?,?,?,?,?,?,?,?,?,?)", values);
        jdbc.update("update operations set datesaisie=?,iduserop=?,idtourneeop=?,montant=?,idreglementop=?,recu=?,enveloppe=?,envoirecu=?,recuenvoye=?,anneeop=?,dateenvoi=? where idoperation=?",
                dateSaisie, clientId, tourneeId, montant, reglementId, bool(recu), bool(enveloppe), bool(envoiRecu), bool(recuEnvoye), annee, blankToNull(dateEnvoi), id);
        return id;
    }

    public void deleteById(long id) {
        OperationEntity current = findById(id).orElseThrow(() -> new IllegalArgumentException("Opération introuvable."));
        if (current.annee() != null) archives.requireWritable(current.annee());
        jdbc.update("delete from operations where idoperation=?", id);
    }

    public List<ReceiptData> findPendingReceipts(int annee) {
        return jdbc.query(receiptSelect() + " where o.anneeop=? and o.recu=1 and o.envoirecu=1 and o.recuenvoye=0 " +
                "order by c.nom collate nocase,c.prenom collate nocase", this::mapReceipt, annee);
    }

    public Optional<ReceiptData> findReceipt(long operationId) {
        try { return Optional.ofNullable(jdbc.queryForObject(receiptSelect() + " where o.idoperation=?", this::mapReceipt, operationId)); }
        catch (EmptyResultDataAccessException ex) { return Optional.empty(); }
    }

    private String receiptSelect() {
        return "select o.idoperation,o.datesaisie,o.anneeop,o.montant,c.nom,c.prenom,c.mail," +
                "a.nomrue,a.complement,a.cp,a.ville," +
                "coalesce(t.numtournee || ' - ','') || coalesce(t.libtournee,'') tournee,r.libreglement " +
                "from operations o join clients c on c.idclient=o.iduserop left join adresses a on a.idadresse=c.idadresse " +
                "left join tournees t on t.idtournee=o.idtourneeop left join reglements r on r.idreglement=o.idreglementop";
    }

    private ReceiptData mapReceipt(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        return new ReceiptData(rs.getLong("idoperation"), rs.getString("datesaisie"), rs.getInt("anneeop"),
                rs.getDouble("montant"), rs.getString("nom"), rs.getString("prenom"), rs.getString("mail"),
                rs.getString("nomrue"), rs.getString("complement"), rs.getString("cp"),
                rs.getString("ville"), rs.getString("tournee"), rs.getString("libreglement"));
    }

    public void markReceiptSent(long operationId) {
        jdbc.update("update operations set recuenvoye=1,dateenvoi=? where idoperation=?", LocalDate.now().toString(), operationId);
    }
    private int bool(boolean value) { return value ? 1 : 0; }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value; }
}
