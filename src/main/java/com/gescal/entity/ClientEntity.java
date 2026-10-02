package com.gescal.entity;

public record ClientEntity(long id, String nom, String prenom, String mail, String remarque, AdresseEntity adresse) {
    public String nomComplet() { return value(nom) + " " + value(prenom); }
    public long adresseId() { return adresse == null ? 0 : adresse.id(); }
    public String nomRue() { return adresse == null ? "" : adresse.nomRue(); }
    public String complement() { return adresse == null ? "" : adresse.complement(); }
    public String cp() { return adresse == null ? "" : adresse.cp(); }
    public String ville() { return adresse == null ? "" : adresse.ville(); }
    public String adresseComplete() { return adresse == null ? "" : adresse.adresseComplete(); }
    private static String value(String value) { return value == null ? "" : value; }
}
