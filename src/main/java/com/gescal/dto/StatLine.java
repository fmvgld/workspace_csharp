package com.gescal.dto;

public class StatLine {
    private final String libelle;
    private final int nombre;
    private final double montant;
    private final double part;

    public StatLine(String libelle, int nombre, double montant, double part) {
        this.libelle = libelle;
        this.nombre = nombre;
        this.montant = montant;
        this.part = part;
    }
    public String getLibelle() { return libelle; }
    public int getNombre() { return nombre; }
    public double getMontant() { return montant; }
    public double getPart() { return part; }
}

