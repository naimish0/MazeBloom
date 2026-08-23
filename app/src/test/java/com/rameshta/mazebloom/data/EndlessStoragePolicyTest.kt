package com.rameshta.mazebloom.data

import com.rameshta.mazebloom.core.EndlessGenerationState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EndlessStoragePolicyTest {
    @Test fun lowStorageFailsClosedWithoutChangingCandidateIdentityInputs() {
        val historyRoot = "a".repeat(64)
        val targetOrdinal = 101L
        val payloadBytes = 32_768L

        assertEquals(
            EndlessGenerationState.STORAGE_BLOCKED,
            EndlessStoragePolicy.commitBlock(payloadBytes, payloadBytes),
        )
        assertEquals("a".repeat(64), historyRoot)
        assertEquals(101L, targetOrdinal)
        assertNull(
            EndlessStoragePolicy.commitBlock(
                EndlessStoragePolicy.MIN_OPERATIONAL_FREE_BYTES + payloadBytes,
                payloadBytes,
            ),
        )
    }

    @Test fun workingSetAndPayloadCapsHaveExactBoundaries() {
        assertTrue(EndlessStoragePolicy.registryFits(EndlessStoragePolicy.MAX_SERIALIZED_REGISTRY_BYTES))
        assertFalse(EndlessStoragePolicy.registryFits(EndlessStoragePolicy.MAX_SERIALIZED_REGISTRY_BYTES + 1L))
        assertNull(EndlessStoragePolicy.commitBlock(Long.MAX_VALUE, EndlessStoragePolicy.MAX_PERSISTED_LEVEL_BYTES))
        assertEquals(
            EndlessGenerationState.INDEX_SPACE_EXHAUSTED,
            EndlessStoragePolicy.commitBlock(Long.MAX_VALUE, EndlessStoragePolicy.MAX_PERSISTED_LEVEL_BYTES + 1L),
        )
    }
}
