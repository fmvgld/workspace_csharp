package com.gescal.dto;

public record ReceiptData(
        long operationId, String dateSaisie, int annee, double montant,
        String nom, String prenom, String mail,
        String nomRue, String complement, String cp, String ville,
        String tournee, String reglement) {

    public String nomComplet() { return value(nom) + " " + value(prenom); }
    public String adresseComplete() {
        return String.join(" ", value(nomRue), value(complement), value(cp), value(ville))
                .replaceAll("\\s+", " ").trim();
    }
    public boolean hasMail() { return mail != null && !mail.isBlank(); }
    private static String value(String value) { return value == null ? "" : value; }
}
