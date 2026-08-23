package com.rameshta.mazebloom.data

import android.content.Context
import android.os.StatFs
import com.rameshta.mazebloom.core.CellMask
import com.rameshta.mazebloom.core.D4Transform
import com.rameshta.mazebloom.core.ENDLESS_BASELINE_ROOT
import com.rameshta.mazebloom.core.ENDLESS_FIRST_GENERATED_ORDINAL
import com.rameshta.mazebloom.core.ENDLESS_MAX_ORDINAL
import com.rameshta.mazebloom.core.ENDLESS_PREFIX_COUNT
import com.rameshta.mazebloom.core.EndlessAcceptedLevel
import com.rameshta.mazebloom.core.EndlessGenerationResult
import com.rameshta.mazebloom.core.EndlessGenerationState
import com.rameshta.mazebloom.core.EndlessGeneratorCheckpoint
import com.rameshta.mazebloom.core.EndlessLevelGenerator
import com.rameshta.mazebloom.core.EndlessRejectionCounts
import com.rameshta.mazebloom.core.GenerationSegment
import com.rameshta.mazebloom.core.LevelDefinition
import com.rameshta.mazebloom.core.LevelFingerprints
import com.rameshta.mazebloom.core.LevelJsonCodec
import com.rameshta.mazebloom.core.endlessNextHistoryRoot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class EndlessGardenSnapshot(
    val unlocked: Boolean,
    val nextPlayableOrdinal: Long,
    val completedCount: Long,
    val acceptedGeneratedCount: Long,
    val readyOrdinals: List<Long>,
    val generationState: EndlessGenerationState,
    val terminalReason: String,
)

sealed interface EndlessLevelResult {
    data class Ready(val level: LevelDefinition, val ordinal: Long, val generated: Boolean) : EndlessLevelResult
    data class Unavailable(val state: EndlessGenerationState, val reason: String) : EndlessLevelResult
}

interface EndlessRepository {
    suspend fun snapshot(): EndlessGardenSnapshot
    suspend fun level(ordinal: Long): EndlessLevelResult
    suspend fun ensureGenerated(ordinal: Long): EndlessLevelResult
    suspend fun replenish(playhead: Long)
}

internal object EndlessStoragePolicy {
    const val MAX_SERIALIZED_REGISTRY_BYTES = 21L * 1024L * 1024L // conservative ~6x decoded/index overhead under 128 MiB
    const val MAX_PERSISTED_LEVEL_BYTES = 1024L * 1024L
    const val MIN_OPERATIONAL_FREE_BYTES = 8L * 1024L * 1024L

    fun registryFits(serializedBytes: Long): Boolean = serializedBytes in 0..MAX_SERIALIZED_REGISTRY_BYTES

    fun commitBlock(availableBytes: Long, payloadBytes: Long): EndlessGenerationState? = when {
        payloadBytes !in 0..MAX_PERSISTED_LEVEL_BYTES -> EndlessGenerationState.INDEX_SPACE_EXHAUSTED
        availableBytes < Math.addExact(MIN_OPERATIONAL_FREE_BYTES, payloadBytes) -> EndlessGenerationState.STORAGE_BLOCKED
        else -> null
    }
}

class RoomEndlessRepository(
    context: Context,
    private val content: ContentRepository,
    database: MazeBloomDatabase = MazeBloomDatabase.open(context),
) : EndlessRepository {
    private val appContext = context.applicationContext
    private val dao = database.mazeBloomDao()
    private val generationMutex = Mutex()

    override suspend fun snapshot(): EndlessGardenSnapshot = withContext(Dispatchers.IO) {
        val state = dao.autoProgressiveState()
        val ready = dao.readyGeneratedLevels(maxOf(state.nextPlayableOrdinal, ENDLESS_FIRST_GENERATED_ORDINAL), 3)
        val completed = dao.allProgress().keysCount { it.startsWith("progressive-") || it.startsWith("auto-v") } +
            dao.skippedLevelIds().count { it.startsWith("progressive-") || it.startsWith("auto-v") }
        EndlessGardenSnapshot(
            unlocked = state.generationState != EndlessGenerationState.LOCKED.name,
            nextPlayableOrdinal = state.nextPlayableOrdinal,
            completedCount = completed.toLong(),
            acceptedGeneratedCount = state.acceptedGeneratedCount,
            readyOrdinals = ready.map(GeneratedLevelRecordEntity::ordinal),
            generationState = runCatching { EndlessGenerationState.valueOf(state.generationState) }
                .getOrDefault(EndlessGenerationState.HISTORY_CORRUPT),
            terminalReason = state.terminalReason,
        )
    }

    override suspend fun level(ordinal: Long): EndlessLevelResult = withContext(Dispatchers.IO) {
        when {
            ordinal !in 1L..ENDLESS_MAX_ORDINAL -> EndlessLevelResult.Unavailable(EndlessGenerationState.GENERATION_EXHAUSTED, "Unsupported ordinal")
            ordinal <= ENDLESS_PREFIX_COUNT -> when (val starter = content.loadProgressivePool()) {
                is ContentLoadResult.Success -> starter.value.getOrNull((ordinal - 1L).toInt())
                    ?.let { EndlessLevelResult.Ready(it, ordinal, false) }
                    ?: EndlessLevelResult.Unavailable(EndlessGenerationState.BASELINE_MISMATCH, "Starter prefix is incomplete")
                is ContentLoadResult.Corrupt -> EndlessLevelResult.Unavailable(EndlessGenerationState.BASELINE_MISMATCH, starter.reason)
                is ContentLoadResult.Missing -> EndlessLevelResult.Unavailable(EndlessGenerationState.BASELINE_MISMATCH, starter.assetPath)
            }
            else -> dao.generatedLevel(ordinal)?.let { record ->
                runCatching { LevelJsonCodec.decode(record.definitionJson) }.fold(
                    onSuccess = { EndlessLevelResult.Ready(it, ordinal, true) },
                    onFailure = { EndlessLevelResult.Unavailable(EndlessGenerationState.HISTORY_CORRUPT, "Stored definition failed verification") },
                )
            } ?: EndlessLevelResult.Unavailable(EndlessGenerationState.PAUSED, "Level is not certified yet")
        }
    }

    override suspend fun ensureGenerated(ordinal: Long): EndlessLevelResult {
        if (ordinal <= ENDLESS_PREFIX_COUNT) return level(ordinal)
        require(ordinal <= ENDLESS_MAX_ORDINAL)
        generationMutex.withLock {
            level(ordinal).let { if (it is EndlessLevelResult.Ready) return it }
            val state = withContext(Dispatchers.IO) { dao.autoProgressiveState() }
            if (state.generationState == EndlessGenerationState.LOCKED.name) {
                return EndlessLevelResult.Unavailable(EndlessGenerationState.LOCKED, "Complete the Campaign first")
            }
            if (state.baselineRoot != ENDLESS_BASELINE_ROOT) return fail(EndlessGenerationState.BASELINE_MISMATCH, "Pinned baseline root changed")
            if (ordinal < state.nextGenerationOrdinal) return level(ordinal)
            var target = state.nextGenerationOrdinal
            while (target <= ordinal) {
                val generated = generateOne(target)
                if (generated !is EndlessLevelResult.Ready) return generated
                target++
            }
        }
        return level(ordinal)
    }

    override suspend fun replenish(playhead: Long) {
        if (playhead < 97L) return
        val upper = minOf(ENDLESS_MAX_ORDINAL, maxOf(ENDLESS_FIRST_GENERATED_ORDINAL, playhead) + 2L)
        for (ordinal in maxOf(ENDLESS_FIRST_GENERATED_ORDINAL, playhead)..upper) {
            if (ensureGenerated(ordinal) !is EndlessLevelResult.Ready) return
        }
    }

    private suspend fun generateOne(ordinal: Long): EndlessLevelResult {
        val (state, checkpoint) = withContext(Dispatchers.IO) {
            dao.updateGenerationState(EndlessGenerationState.GENERATING.name)
            val current = dao.autoProgressiveState()
            val stored = dao.generationCheckpoint()?.takeIf { saved ->
                saved.baselineRoot == current.baselineRoot && saved.generationSegmentId == current.generationSegmentId &&
                    saved.targetOrdinal == ordinal && saved.historyRootAtStart == current.historyRoot &&
                    saved.nextAttempt in 0..65_536 && saved.nextAttempt % 256 == 0 &&
                    saved.cheapSurvivors in 0..1_024 && saved.fullSurvivors in 0..128 &&
                    isValidRejectionSummary(saved.rejectionSummary, saved.nextAttempt)
            }
            if (stored == null) {
                dao.deleteGenerationCheckpoint()
            }
            val active = stored ?: GenerationCheckpointEntity(
                baselineRoot = current.baselineRoot,
                generationSegmentId = current.generationSegmentId,
                targetOrdinal = ordinal,
                historyRootAtStart = current.historyRoot,
                nextAttempt = 0,
                cheapSurvivors = 0,
                fullSurvivors = 0,
                rejectionSummary = "",
            ).also { created ->
                dao.putGenerationCheckpoint(created)
            }
            current to active
        }
        val segmentEntity = withContext(Dispatchers.IO) { dao.generationSegment(state.generationSegmentId) }
            ?: return fail(EndlessGenerationState.UNSUPPORTED_PROFILE, "Generation segment missing")
        val baseline = when (val loaded = content.loadEndlessBaseline()) {
            is ContentLoadResult.Success -> loaded.value
            is ContentLoadResult.Corrupt -> return fail(EndlessGenerationState.BASELINE_MISMATCH, loaded.reason)
            is ContentLoadResult.Missing -> return fail(EndlessGenerationState.BASELINE_MISMATCH, loaded.assetPath)
        }
        val baselineBytes = baseline.sumOf { LevelJsonCodec.encode(it).toByteArray(Charsets.UTF_8).size.toLong() }
        val retainedBytes = withContext(Dispatchers.IO) { dao.generatedDefinitionBytes() }
        if (!EndlessStoragePolicy.registryFits(Math.addExact(baselineBytes, retainedBytes))) {
            return fail(EndlessGenerationState.INDEX_SPACE_EXHAUSTED, "Retained uniqueness registry reached the 128 MiB working-set ceiling")
        }
        val retained = withContext(Dispatchers.IO) {
            dao.allGeneratedLevels().map { record -> LevelJsonCodec.decode(record.definitionJson) }
        }
        val segment = GenerationSegment(
            segmentEntity.id, segmentEntity.startOrdinal, segmentEntity.namespaceVersion, segmentEntity.generatorVersion,
            segmentEntity.certificationProfileVersion, segmentEntity.fingerprintVersion, segmentEntity.uniquenessProfileVersion,
        )
        val resume = EndlessGeneratorCheckpoint(
            checkpoint.nextAttempt,
            checkpoint.cheapSurvivors,
            checkpoint.fullSurvivors,
            decodeRejections(checkpoint.rejectionSummary),
        )
        val result = withContext(Dispatchers.Default) {
            val generationContext = currentCoroutineContext()
            EndlessLevelGenerator(baseline, retained, segment, state.baselineRoot).generate(ordinal, resume) { window ->
                generationContext.ensureActive()
                dao.putGenerationCheckpoint(
                    checkpoint.copy(
                        nextAttempt = window.nextAttempt,
                        cheapSurvivors = window.cheapSurvivors,
                        fullSurvivors = window.fullSurvivors,
                        rejectionSummary = encodeRejections(window.rejections),
                    ),
                )
            }
        }
        return when (result) {
            is EndlessGenerationResult.Failed -> {
                if (result.state == EndlessGenerationState.GENERATION_EXHAUSTED && segment.generatorVersion < MAX_BUNDLED_GENERATOR_VERSION) {
                    val next = GenerationSegmentEntity(
                        id = segment.id + 1L,
                        startOrdinal = ordinal,
                        namespaceVersion = segment.namespaceVersion,
                        generatorVersion = segment.generatorVersion + 1,
                        certificationProfileVersion = segment.certificationProfileVersion,
                        fingerprintVersion = segment.fingerprintVersion,
                        uniquenessProfileVersion = segment.uniquenessProfileVersion,
                        priorHistoryRoot = state.historyRoot,
                        reason = "Prior bounded candidate stream exhausted at ordinal $ordinal",
                    )
                    val upgraded = runCatching {
                        withContext(Dispatchers.IO) { dao.startGenerationSegment(segment.id, next) }
                    }.isSuccess
                    if (upgraded) generateOne(ordinal)
                    else fail(EndlessGenerationState.HISTORY_CORRUPT, "Could not activate the next versioned generation segment")
                } else fail(result.state, result.message)
            }
            is EndlessGenerationResult.Accepted -> commit(state, result.value)
        }
    }

    private suspend fun commit(state: AutoProgressiveStateEntity, accepted: EndlessAcceptedLevel): EndlessLevelResult {
        val nextHistory = endlessNextHistoryRoot(state.historyRoot, accepted.historyRecord)
        val definition = accepted.definition
        val entity = GeneratedLevelRecordEntity(
            levelId = definition.id,
            ordinal = accepted.ordinal,
            candidateKey = accepted.candidateKey.canonical(),
            seedHex = accepted.seedHex,
            generationSegmentId = accepted.candidateKey.generationSegment,
            namespaceVersion = accepted.candidateKey.generationNamespaceVersion,
            generatorVersion = accepted.candidateKey.generatorVersion,
            certificationProfileVersion = accepted.candidateKey.certificationProfileVersion,
            fingerprintVersion = accepted.candidateKey.fingerprintVersion,
            uniquenessProfileVersion = accepted.candidateKey.uniquenessProfileVersion,
            definitionJson = accepted.definitionJson,
            canonicalReplay = definition.canonicalReplay.joinToString(",") { it.name },
            certificatePayload = accepted.certificatePayload,
            certificateHash = accepted.certificateHash,
            definitionHash = accepted.fingerprints.definitionHash,
            geometricFingerprint = accepted.fingerprints.geometric,
            dynamicFingerprint = accepted.fingerprints.dynamic,
            structuralFingerprint = accepted.fingerprints.structural,
            grammarFingerprint = accepted.fingerprints.grammar,
            difficulty = definition.difficulty.name,
            boardSize = definition.width,
            candidateAttempt = accepted.attempt,
            lifecycleState = "READY",
            historyRoot = nextHistory,
        )
        val uniqueness = D4Transform.entries.map { transform ->
            val walls = CellMask.of(definition.staticWalls.cells(definition.width * definition.height).map {
                LevelFingerprints.transformCell(it, definition.width, transform)
            })
            val buds = CellMask.of(definition.initialBuds.cells(definition.width * definition.height).map {
                LevelFingerprints.transformCell(it, definition.width, transform)
            })
            val start = LevelFingerprints.transformCell(definition.startCell, definition.width, transform)
            UniquenessRecordEntity(
                definition.id, transform.ordinal, accepted.ordinal, definition.width, walls.bits, buds.bits, start,
                accepted.fingerprints.definitionHash,
                "${walls.bits.toULong().toString(16)}|$start|${buds.bits.toULong().toString(16)}",
            )
        }
        val payloadBytes = persistedPayloadBytes(entity, uniqueness)
        val availableBytes = runCatching {
            withContext(Dispatchers.IO) { StatFs(appContext.filesDir.path).availableBytes }
        }.getOrElse {
            return fail(EndlessGenerationState.STORAGE_BLOCKED, "Storage availability could not be verified; retry after checking free space")
        }
        when (EndlessStoragePolicy.commitBlock(availableBytes, payloadBytes)) {
            EndlessGenerationState.INDEX_SPACE_EXHAUSTED ->
                return fail(EndlessGenerationState.INDEX_SPACE_EXHAUSTED, "Certified level exceeds the deterministic persisted-payload limit")
            EndlessGenerationState.STORAGE_BLOCKED ->
                return fail(EndlessGenerationState.STORAGE_BLOCKED, "Not enough safe free space; the certified candidate is preserved for retry")
            else -> Unit
        }
        return runCatching {
            withContext(Dispatchers.IO) {
                dao.commitGeneratedLevel(entity, uniqueness, state.baselineRoot, state.historyRoot, nextHistory)
            }
            EndlessLevelResult.Ready(definition, accepted.ordinal, true)
        }.getOrElse { fail(EndlessGenerationState.INDEX_REBUILD_REQUIRED, it.message ?: "Atomic commit failed") }
    }

    private suspend fun fail(state: EndlessGenerationState, reason: String): EndlessLevelResult.Unavailable {
        withContext(Dispatchers.IO) { dao.updateGenerationState(state.name, reason.take(240)) }
        return EndlessLevelResult.Unavailable(state, reason)
    }

    private fun List<LevelProgressEntity>.keysCount(predicate: (String) -> Boolean): Int = count { predicate(it.levelId) }

    private fun persistedPayloadBytes(level: GeneratedLevelRecordEntity, uniqueness: List<UniquenessRecordEntity>): Long {
        val strings = listOf(
            level.levelId, level.candidateKey, level.seedHex, level.definitionJson, level.canonicalReplay,
            level.certificatePayload, level.certificateHash, level.definitionHash, level.geometricFingerprint,
            level.dynamicFingerprint, level.structuralFingerprint, level.grammarFingerprint, level.difficulty,
            level.lifecycleState, level.historyRoot,
        ) + uniqueness.flatMap { listOf(it.levelId, it.definitionHash, it.geometricEncoding) }
        val stringBytes = strings.sumOf { it.toByteArray(Charsets.UTF_8).size.toLong() }
        val numericBytes = 11L * Long.SIZE_BYTES + uniqueness.size * 7L * Long.SIZE_BYTES
        return Math.addExact(Math.addExact(stringBytes, numericBytes), PERSISTENCE_OVERHEAD_BYTES)
    }

    private fun encodeRejections(value: EndlessRejectionCounts): String = listOf(
        value.construction, value.trivialOrOneLine, value.probe, value.fullSolveOrBudget, value.profile,
        value.irrelevantOpenCell, value.serialization, value.exactOrGeometric, value.dynamic, value.structural,
        value.grammar, value.hardNear, value.reviewSimilarity,
    ).joinToString(",")

    private fun decodeRejections(value: String): EndlessRejectionCounts {
        if (value.isBlank()) return EndlessRejectionCounts()
        val counts = value.split(',').mapNotNull(String::toIntOrNull)
        if (counts.size != 13 || counts.any { it < 0 }) return EndlessRejectionCounts()
        return EndlessRejectionCounts(
            counts[0], counts[1], counts[2], counts[3], counts[4], counts[5], counts[6],
            counts[7], counts[8], counts[9], counts[10], counts[11], counts[12],
        )
    }

    private fun isValidRejectionSummary(value: String, nextAttempt: Int): Boolean {
        if (value.isBlank()) return nextAttempt == 0
        val counts = value.split(',').mapNotNull(String::toIntOrNull)
        return counts.size == 13 && counts.all { it >= 0 }
    }

    companion object {
        private const val MAX_BUNDLED_GENERATOR_VERSION = 40
        private const val PERSISTENCE_OVERHEAD_BYTES = 4L * 1024L
    }
}
