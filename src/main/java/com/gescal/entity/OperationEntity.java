package com.gescal.entity;

public record OperationEntity(
        long id, String dateSaisie, long clientId, String nom, String prenom,
        long tourneeId, Integer numeroTournee, String libelleTournee,
        double montant, long reglementId, String libelleReglement,
        boolean recu, boolean enveloppe, boolean envoiRecu, boolean recuEnvoye,
        Integer annee, String dateEnvoi) {
}

