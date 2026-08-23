package com.rameshta.mazebloom.core

const val CURRENT_CERTIFICATION_PROFILE_VERSION = 3

/** Versioned authoring contract. Published profile-v1/v2 definitions remain valid and immutable. */
object DifficultyProfiles {
    fun optimalMoves(band: DifficultyBand, profileVersion: Int = CURRENT_CERTIFICATION_PROFILE_VERSION): IntRange =
        if (profileVersion < CURRENT_CERTIFICATION_PROFILE_VERSION) legacyOptimalMoves(band) else when (band) {
            DifficultyBand.TUTORIAL -> 2..5
            DifficultyBand.EASY -> 4..8
            DifficultyBand.NORMAL -> 6..11
            DifficultyBand.HARD -> 8..14
            DifficultyBand.EXPERT -> 10..17
            DifficultyBand.MASTER -> 12..20
        }

    fun accepts(
        band: DifficultyBand,
        metrics: DifficultyReport,
        profileVersion: Int = CURRENT_CERTIFICATION_PROFILE_VERSION,
    ): Boolean = if (profileVersion < CURRENT_CERTIFICATION_PROFILE_VERSION) {
        when (band) {
            DifficultyBand.TUTORIAL -> true
            DifficultyBand.EASY -> metrics.meaningfulDecisionCount >= 1 &&
                metrics.minimumBloomAssistedStops >= 1 && metrics.forcedMoveRatio <= .85
            DifficultyBand.NORMAL -> metrics.meaningfulDecisionCount >= 2 &&
                metrics.minimumBloomAssistedStops >= 1 && metrics.forcedMoveRatio <= .75
            DifficultyBand.HARD -> metrics.meaningfulDecisionCount >= 3 &&
                metrics.minimumBloomAssistedStops >= 2 && metrics.doomedStateCount >= 1 && metrics.forcedMoveRatio <= .70
            DifficultyBand.EXPERT -> metrics.meaningfulDecisionCount >= 4 &&
                metrics.minimumBloomAssistedStops >= 2 && metrics.canonicalReplayMaxCreatorUseDistance >= 3 &&
                metrics.forcedMoveRatio <= .65
            DifficultyBand.MASTER -> metrics.meaningfulDecisionCount >= 5 &&
                metrics.minimumBloomAssistedStops >= 3 && metrics.canonicalReplayMaxCreatorUseDistance >= 4 &&
                metrics.forcedMoveRatio <= .60
        }
    } else {
        when (band) {
            DifficultyBand.TUTORIAL -> metrics.meaningfulDecisionCount >= 1 &&
                metrics.minimumBloomAssistedStops >= 1 && metrics.forcedMoveRatio <= .90
            DifficultyBand.EASY -> metrics.meaningfulDecisionCount >= 2 &&
                metrics.minimumBloomAssistedStops >= 1 && metrics.forcedMoveRatio <= .80
            DifficultyBand.NORMAL -> metrics.meaningfulDecisionCount >= 3 &&
                metrics.minimumBloomAssistedStops >= 1 && metrics.doomedStateCount >= 2 && metrics.forcedMoveRatio <= .70
            DifficultyBand.HARD -> metrics.meaningfulDecisionCount >= 4 &&
                metrics.minimumBloomAssistedStops >= 2 && metrics.doomedStateCount >= 2 &&
                metrics.canonicalReplayMaxCreatorUseDistance >= 3 && metrics.forcedMoveRatio <= .65
            DifficultyBand.EXPERT -> metrics.meaningfulDecisionCount >= 5 &&
                metrics.minimumBloomAssistedStops >= 2 && metrics.doomedStateCount >= 2 &&
                metrics.canonicalReplayMaxCreatorUseDistance >= 4 && metrics.forcedMoveRatio <= .60
            DifficultyBand.MASTER -> metrics.meaningfulDecisionCount >= 6 &&
                metrics.minimumBloomAssistedStops >= 3 && metrics.doomedStateCount >= 3 &&
                metrics.canonicalReplayMaxCreatorUseDistance >= 5 && metrics.forcedMoveRatio <= .55
        }
    }

    private fun legacyOptimalMoves(band: DifficultyBand): IntRange = when (band) {
        DifficultyBand.TUTORIAL -> 1..4
        DifficultyBand.EASY -> 3..7
        DifficultyBand.NORMAL -> 5..10
        DifficultyBand.HARD -> 7..13
        DifficultyBand.EXPERT -> 9..16
        DifficultyBand.MASTER -> 11..20
    }
}
