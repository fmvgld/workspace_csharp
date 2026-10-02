package com.gescal.dto;

public class AnnualRevenueLine {
    private final int annee;
    private final int nombre;
    private final double montant;
    private final Double evolution;

    public AnnualRevenueLine(int annee, int nombre, double montant, Double evolution) {
        this.annee = annee;
        this.nombre = nombre;
        this.montant = montant;
        this.evolution = evolution;
    }

    public int getAnnee() { return annee; }
    public int getNombre() { return nombre; }
    public double getMontant() { return montant; }
    public Double getEvolution() { return evolution; }
}
