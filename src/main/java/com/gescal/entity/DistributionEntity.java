package com.gescal.entity;

public record DistributionEntity(long id, long tourneeId, Integer annee, Integer nombre,
                                 Integer numeroTournee, String libelleTournee) {
}

