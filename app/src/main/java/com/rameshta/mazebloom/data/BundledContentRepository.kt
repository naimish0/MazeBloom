package com.rameshta.mazebloom.data

import android.content.Context
import android.util.JsonReader
import com.rameshta.mazebloom.core.CampaignCatalog
import com.rameshta.mazebloom.core.CampaignLevelSummary
import com.rameshta.mazebloom.core.ChapterDescriptor
import com.rameshta.mazebloom.core.DifficultyBand
import com.rameshta.mazebloom.core.GardenDescriptor
import com.rameshta.mazebloom.core.LevelDefinition
import com.rameshta.mazebloom.core.LevelJsonCodec
import com.rameshta.mazebloom.core.ENDLESS_BASELINE_COUNT
import com.rameshta.mazebloom.core.ENDLESS_BASELINE_ROOT
import java.io.InputStreamReader
import java.security.MessageDigest
import java.util.LinkedHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface ContentLoadResult<out T> {
    data class Success<T>(val value: T) : ContentLoadResult<T>
    data class Missing(val assetPath: String) : ContentLoadResult<Nothing>
    data class Corrupt(val assetPath: String, val reason: String) : ContentLoadResult<Nothing>
}

interface ContentRepository {
    val catalog: CampaignCatalog
    val dailyPool: List<LevelDefinition>
    val dailyPoolVersion: Int
    suspend fun loadProgressivePool(): ContentLoadResult<List<LevelDefinition>>
    suspend fun loadEndlessBaseline(): ContentLoadResult<List<LevelDefinition>>
    suspend fun loadChapter(chapterId: String): ContentLoadResult<List<LevelDefinition>>
    suspend fun loadLevel(levelId: String): ContentLoadResult<LevelDefinition>
    suspend fun loadLevel(campaignOrder: Int): ContentLoadResult<LevelDefinition>
    fun cachedChapterIds(): List<String>
}

class BundledContentRepository(private val context: Context) : ContentRepository {
    private data class RootManifest(
        val dailyPoolVersion: Int,
        val progressivePoolVersion: Int,
        val progressiveCount: Int,
        val progressiveContentRoot: String,
    )

    private val rootManifest: RootManifest = context.assets.open(ROOT_MANIFEST_PATH).use { input ->
        JsonReader(InputStreamReader(input, Charsets.UTF_8)).use { reader ->
            var dailyVersion = 0
            var progressiveVersion = 0
            var progressiveCount = 0
            var progressiveRoot = ""
            reader.beginObject()
            while (reader.hasNext()) when (reader.nextName()) {
                "dailyPoolVersion" -> dailyVersion = reader.nextInt()
                "progressivePoolVersion" -> progressiveVersion = reader.nextInt()
                "progressiveCount" -> progressiveCount = reader.nextInt()
                "progressiveContentRoot" -> progressiveRoot = reader.nextString()
                else -> reader.skipValue()
            }
            reader.endObject()
            RootManifest(dailyVersion, progressiveVersion, progressiveCount, progressiveRoot)
        }
    }.also { require(it.dailyPoolVersion > 0 && it.progressivePoolVersion > 0 && it.progressiveCount == 100) }

    override val catalog: CampaignCatalog = context.assets.open(CATALOG_PATH).use { input ->
        JsonReader(InputStreamReader(input, Charsets.UTF_8)).use(::readCatalog)
    }.also { value ->
        require(value.campaignCount == 2_000 && value.gardens.size == 20 && value.chapters.size == 100)
        require(value.chapters.all { it.levelCount == 20 } && value.levels.map { it.campaignOrder } == (1..2_000).toList())
    }

    override val dailyPool: List<LevelDefinition> = context.assets.open(DAILY_PATH).bufferedReader().useLines { lines ->
        lines.filter { it.isNotBlank() }.map(LevelJsonCodec::decode).toList()
    }.also { levels ->
        require(levels.size >= 120 && levels.map { it.id }.distinct().size == levels.size)
    }

    override val dailyPoolVersion: Int = rootManifest.dailyPoolVersion

    private val chapterById = catalog.chapters.associateBy(ChapterDescriptor::id)
    private val summaryById = catalog.levels.associateBy(CampaignLevelSummary::id)
    private val summaryByOrder = catalog.levels.associateBy(CampaignLevelSummary::campaignOrder)
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val inFlight = mutableMapOf<String, Deferred<ContentLoadResult<List<LevelDefinition>>>>()
    private var progressiveInFlight: Deferred<ContentLoadResult<List<LevelDefinition>>>? = null
    private var progressiveCache: List<LevelDefinition>? = null
    private val cache = object : LinkedHashMap<String, List<LevelDefinition>>(4, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<LevelDefinition>>?): Boolean = size > 3
    }
    @Volatile private var cachedKeys: List<String> = emptyList()

    override suspend fun loadChapter(chapterId: String): ContentLoadResult<List<LevelDefinition>> {
        val descriptor = chapterById[chapterId] ?: return ContentLoadResult.Missing(chapterId)
        val request = mutex.withLock {
            cache[chapterId]?.let {
                cachedKeys = cache.keys.toList()
                return ContentLoadResult.Success(it)
            }
            inFlight.getOrPut(chapterId) { ioScope.async { readChapter(descriptor) } }
        }
        val result = request.await()
        mutex.withLock {
            inFlight.remove(chapterId, request)
            if (result is ContentLoadResult.Success) {
                cache[chapterId] = result.value
                cachedKeys = cache.keys.toList()
            }
        }
        return result
    }

    override suspend fun loadLevel(levelId: String): ContentLoadResult<LevelDefinition> {
        val summary = summaryById[levelId] ?: return ContentLoadResult.Missing(levelId)
        return loadFromSummary(summary)
    }

    override suspend fun loadLevel(campaignOrder: Int): ContentLoadResult<LevelDefinition> {
        val summary = summaryByOrder[campaignOrder] ?: return ContentLoadResult.Missing("campaign-order-$campaignOrder")
        return loadFromSummary(summary)
    }

    override suspend fun loadProgressivePool(): ContentLoadResult<List<LevelDefinition>> {
        val request = mutex.withLock {
            progressiveCache?.let { return ContentLoadResult.Success(it) }
            progressiveInFlight ?: ioScope.async { readProgressivePool() }.also { progressiveInFlight = it }
        }
        val result = request.await()
        mutex.withLock {
            if (progressiveInFlight === request) progressiveInFlight = null
            if (result is ContentLoadResult.Success) progressiveCache = result.value
        }
        return result
    }

    override suspend fun loadEndlessBaseline(): ContentLoadResult<List<LevelDefinition>> {
        val declared = runCatching {
            context.assets.open(ENDLESS_BASELINE_PATH).use { input ->
                JsonReader(InputStreamReader(input, Charsets.UTF_8)).use { reader ->
                    var count = 0
                    var root = ""
                    reader.beginObject()
                    while (reader.hasNext()) when (reader.nextName()) {
                        "recordCount" -> count = reader.nextInt()
                        "endlessBaselineRoot" -> root = reader.nextString()
                        else -> reader.skipValue()
                    }
                    reader.endObject()
                    count to root
                }
            }
        }.getOrElse { error ->
            return ContentLoadResult.Corrupt(ENDLESS_BASELINE_PATH, error.message ?: "baseline manifest unreadable")
        }
        if (declared.first != ENDLESS_BASELINE_COUNT || declared.second != ENDLESS_BASELINE_ROOT) {
            return ContentLoadResult.Corrupt(ENDLESS_BASELINE_PATH, "BASELINE_MISMATCH")
        }
        val levels = ArrayList<LevelDefinition>(ENDLESS_BASELINE_COUNT)
        for (chapter in catalog.chapters) when (val loaded = loadChapter(chapter.id)) {
            is ContentLoadResult.Success -> levels.addAll(loaded.value)
            is ContentLoadResult.Missing -> return loaded
            is ContentLoadResult.Corrupt -> return loaded
        }
        levels.addAll(dailyPool)
        when (val progressive = loadProgressivePool()) {
            is ContentLoadResult.Success -> levels.addAll(progressive.value)
            is ContentLoadResult.Missing -> return progressive
            is ContentLoadResult.Corrupt -> return progressive
        }
        return if (levels.size == ENDLESS_BASELINE_COUNT && levels.map(LevelDefinition::id).distinct().size == ENDLESS_BASELINE_COUNT) {
            ContentLoadResult.Success(levels)
        } else ContentLoadResult.Corrupt(ENDLESS_BASELINE_PATH, "BASELINE_MISMATCH")
    }

    override fun cachedChapterIds(): List<String> = cachedKeys

    private suspend fun loadFromSummary(summary: CampaignLevelSummary): ContentLoadResult<LevelDefinition> =
        when (val chapter = loadChapter(summary.chapterId)) {
            is ContentLoadResult.Success -> chapter.value.firstOrNull { it.id == summary.id }
                ?.let { ContentLoadResult.Success(it) }
                ?: ContentLoadResult.Corrupt(summary.chapterId, "level missing from declared shard")
            is ContentLoadResult.Missing -> chapter
            is ContentLoadResult.Corrupt -> chapter
        }

    private fun readChapter(descriptor: ChapterDescriptor): ContentLoadResult<List<LevelDefinition>> = runCatching {
        val text = context.assets.open(descriptor.shardPath).bufferedReader().use { it.readText() }
        require(sha256(text) == descriptor.shardSha256) { "shard digest mismatch" }
        val levels = extractLevelObjects(text).map(LevelJsonCodec::decode)
        require(levels.size == 20) { "expected 20 levels" }
        require(levels.map { it.campaignOrder } == (descriptor.firstCampaignOrder until descriptor.firstCampaignOrder + 20).toList()) {
            "shard campaign range mismatch"
        }
        levels
    }.fold(
        onSuccess = { ContentLoadResult.Success(it) },
        onFailure = { error ->
            if (error is java.io.FileNotFoundException) ContentLoadResult.Missing(descriptor.shardPath)
            else ContentLoadResult.Corrupt(descriptor.shardPath, error.message ?: error::class.java.simpleName)
        },
    )

    private fun readProgressivePool(): ContentLoadResult<List<LevelDefinition>> = runCatching {
        val lines = context.assets.open(PROGRESSIVE_PATH).bufferedReader().useLines { sequence -> sequence.filter(String::isNotBlank).toList() }
        require(lines.size == rootManifest.progressiveCount) { "progressive count mismatch" }
        require(hashFields(lines) == rootManifest.progressiveContentRoot) { "progressive digest mismatch" }
        lines.map(LevelJsonCodec::decode).also { levels ->
            require(levels.map { it.id } == (1..rootManifest.progressiveCount).map { "progressive-${it.toString().padStart(4, '0')}" })
        }
    }.fold(
        onSuccess = { ContentLoadResult.Success(it) },
        onFailure = { error ->
            if (error is java.io.FileNotFoundException) ContentLoadResult.Missing(PROGRESSIVE_PATH)
            else ContentLoadResult.Corrupt(PROGRESSIVE_PATH, error.message ?: error::class.java.simpleName)
        },
    )

    companion object {
        private const val CATALOG_PATH = "content/campaign/campaign-manifest.json"
        private const val DAILY_PATH = "content/daily.jsonl"
        private const val ROOT_MANIFEST_PATH = "content/manifest.json"
        private const val PROGRESSIVE_PATH = "content/progressive.jsonl"
        private const val ENDLESS_BASELINE_PATH = "content/endless-baseline.json"

        private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

        private fun hashFields(values: List<String>): String = sha256(values.joinToString("") { value ->
            "${value.toByteArray(Charsets.UTF_8).size}:$value"
        })

        internal fun extractLevelObjects(source: String): List<String> {
            val marker = "\"levels\":["
            var index = source.indexOf(marker)
            require(index >= 0) { "missing levels array" }
            index += marker.length
            val records = mutableListOf<String>()
            var start = -1
            var depth = 0
            var quoted = false
            var escaped = false
            while (index < source.length) {
                val character = source[index]
                if (quoted) {
                    if (escaped) escaped = false
                    else if (character == '\\') escaped = true
                    else if (character == '"') quoted = false
                } else {
                    when (character) {
                        '"' -> quoted = true
                        '{' -> {
                            if (depth == 0) start = index
                            depth++
                        }
                        '}' -> {
                            depth--
                            if (depth == 0 && start >= 0) records += source.substring(start, index + 1)
                        }
                        ']' -> if (depth == 0) return records
                    }
                }
                index++
            }
            error("unterminated levels array")
        }

        private fun readCatalog(reader: JsonReader): CampaignCatalog {
            var schemaVersion = 0
            var catalogVersion = 0
            var campaignCount = 0
            var contentRoot = ""
            var auditRoot = ""
            var campaignRoot = ""
            val gardens = mutableListOf<GardenDescriptor>()
            reader.beginObject()
            while (reader.hasNext()) when (reader.nextName()) {
                "schemaVersion" -> schemaVersion = reader.nextInt()
                "campaignCatalogVersion" -> catalogVersion = reader.nextInt()
                "campaignCount" -> campaignCount = reader.nextInt()
                "campaignContentRoot" -> contentRoot = reader.nextString()
                "campaignAuditRoot" -> auditRoot = reader.nextString()
                "campaignRoot" -> campaignRoot = reader.nextString()
                "gardens" -> {
                    reader.beginArray()
                    while (reader.hasNext()) gardens += readGarden(reader)
                    reader.endArray()
                }
                else -> reader.skipValue()
            }
            reader.endObject()
            return CampaignCatalog(schemaVersion, catalogVersion, campaignCount, gardens, contentRoot, auditRoot, campaignRoot)
        }

        private fun readGarden(reader: JsonReader): GardenDescriptor {
            var id = ""
            var order = 0
            var nameKey = ""
            val chapters = mutableListOf<ChapterDescriptor>()
            reader.beginObject()
            while (reader.hasNext()) when (reader.nextName()) {
                "id" -> id = reader.nextString()
                "order" -> order = reader.nextInt()
                "nameKey" -> nameKey = reader.nextString()
                "chapters" -> {
                    reader.beginArray()
                    while (reader.hasNext()) chapters += readChapterDescriptor(reader)
                    reader.endArray()
                }
                else -> reader.skipValue()
            }
            reader.endObject()
            return GardenDescriptor(id, order, nameKey, chapters)
        }

        private fun readChapterDescriptor(reader: JsonReader): ChapterDescriptor {
            var id = ""
            var gardenId = ""
            var order = 0
            var first = 0
            var count = 0
            var path = ""
            var sha = ""
            val levels = mutableListOf<CampaignLevelSummary>()
            reader.beginObject()
            while (reader.hasNext()) when (reader.nextName()) {
                "id" -> id = reader.nextString()
                "gardenId" -> gardenId = reader.nextString()
                "orderWithinGarden" -> order = reader.nextInt()
                "firstCampaignOrder" -> first = reader.nextInt()
                "levelCount" -> count = reader.nextInt()
                // AssetManager paths are rooted at assets/, so retain the manifest's
                // canonical content/ prefix (for example content/campaign/...).
                "shardPath" -> path = reader.nextString().removePrefix("/")
                "shardSha256" -> sha = reader.nextString()
                "levels" -> {
                    reader.beginArray()
                    while (reader.hasNext()) levels += readSummary(reader)
                    reader.endArray()
                }
                else -> reader.skipValue()
            }
            reader.endObject()
            return ChapterDescriptor(id, gardenId, order, first, count, path, sha, levels)
        }

        private fun readSummary(reader: JsonReader): CampaignLevelSummary {
            var id = ""
            var order = 0
            var gardenId = ""
            var chapterId = ""
            var chapterOrder = 0
            var levelOrder = 0
            var difficulty = DifficultyBand.EASY
            var boardSize = 5
            var optimal = 0
            var checksum = ""
            reader.beginObject()
            while (reader.hasNext()) when (reader.nextName()) {
                "id" -> id = reader.nextString()
                "campaignOrder" -> order = reader.nextInt()
                "gardenId" -> gardenId = reader.nextString()
                "chapterId" -> chapterId = reader.nextString()
                "chapterOrderWithinGarden" -> chapterOrder = reader.nextInt()
                "levelOrderWithinChapter" -> levelOrder = reader.nextInt()
                "difficulty" -> difficulty = DifficultyBand.valueOf(reader.nextString())
                "boardSize" -> boardSize = reader.nextInt()
                "optimalMoves" -> optimal = reader.nextInt()
                "definitionChecksum" -> checksum = reader.nextString()
                else -> reader.skipValue()
            }
            reader.endObject()
            return CampaignLevelSummary(id, order, gardenId, chapterId, chapterOrder, levelOrder, difficulty, boardSize, optimal, checksum)
        }
    }
}
