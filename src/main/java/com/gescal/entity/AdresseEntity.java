package com.gescal.entity;

public record AdresseEntity(long id, String nomRue, String complement, String cp, String ville) {
    public String adresseComplete() {
        return String.join(" ", value(nomRue), value(complement), value(cp), value(ville))
                .replaceAll("\\s+", " ").trim();
    }

    private static String value(String value) { return value == null ? "" : value; }
}
