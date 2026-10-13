package com.pintu.service.matching;

public record MatchEvaluation(
        boolean eligible,
        double startDistanceKm,
        double endDistanceKm,
        long timeDifferenceMinutes,
        int routeOverlap,
        int timeScore,
        int comprehensiveScore,
        String reason) {
}
