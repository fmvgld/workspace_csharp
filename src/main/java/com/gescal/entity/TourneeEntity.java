package com.gescal.entity;

public record TourneeEntity(long id, Integer numero, String libelle, String annee) {
    public String designation() { return (numero == null ? "" : numero + " - ") + libelle + " (" + annee + ")"; }
}

