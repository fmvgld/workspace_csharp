package com.gescal.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class StatistiqueRepository {
    private final JdbcTemplate jdbc;
    public StatistiqueRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Map<String, Number> dashboard(int annee) {
        Map<String, Number> result = new LinkedHashMap<>();
        result.put("clients", jdbc.queryForObject("select count(*) from clients", Long.class));
        result.put("tournees", jdbc.queryForObject("select count(*) from tournees where annee=?", Long.class, String.valueOf(annee)));
        result.put("operations", jdbc.queryForObject("select count(*) from operations where anneeop=?", Long.class, annee));
        result.put("montant", jdbc.queryForObject("select coalesce(sum(montant),0) from operations where anneeop=?", Double.class, annee));
        result.put("recusAEnvoyer", jdbc.queryForObject("select count(*) from operations where anneeop=? and envoirecu=1 and recuenvoye=0", Long.class, annee));
        return result;
    }

    public List<Map<String, Object>> parReglement(int annee) {
        return jdbc.queryForList("select coalesce(r.libreglement,'Non renseigné') libelle,count(*) nombre,round(sum(o.montant),2) montant from operations o left join reglements r on r.idreglement=o.idreglementop where o.anneeop=? group by r.idreglement,r.libreglement order by montant desc", annee);
    }
    public List<Map<String, Object>> parTournee(int annee) {
        return jdbc.queryForList("select case when t.idtournee is null then 'Non renseignée' else coalesce(t.numtournee || ' - ','') || coalesce(t.libtournee,'') end libelle,count(*) nombre,round(sum(o.montant),2) montant from operations o left join tournees t on t.idtournee=o.idtourneeop where o.anneeop=? group by t.idtournee,t.numtournee,t.libtournee order by t.numtournee", annee);
    }
    public List<Map<String, Object>> parVille(int annee) {
        return jdbc.queryForList("select coalesce(a.ville,'Non renseignée') libelle,count(*) nombre,round(sum(o.montant),2) montant from operations o left join clients c on c.idclient=o.iduserop left join adresses a on a.idadresse=c.idadresse where o.anneeop=? group by a.ville order by montant desc limit 30", annee);
    }

    public List<Map<String, Object>> chiffreAffairesParAnnee() {
        return jdbc.queryForList("select anneeop annee,count(*) nombre,round(coalesce(sum(montant),0),2) montant from operations where anneeop is not null group by anneeop order by anneeop");
    }
}
