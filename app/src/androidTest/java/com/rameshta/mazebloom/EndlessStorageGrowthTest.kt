package com.rameshta.mazebloom

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rameshta.mazebloom.data.GeneratedLevelRecordEntity
import com.rameshta.mazebloom.data.MazeBloomDatabase
import com.rameshta.mazebloom.data.UniquenessRecordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class EndlessStorageGrowthTest {
    @Test fun measuresActualRoomGrowthForOneThousandRepresentativeAcceptedLevels() {
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            context.deleteDatabase(NAME)
            open(context).also { database ->
                withContext(Dispatchers.IO) { database.openHelper.writableDatabase }
                database.close()
            }
            val emptyBytes = databaseBytes(context)
            val database = open(context)
            withContext(Dispatchers.IO) {
                database.withTransaction {
                    val dao = database.mazeBloomDao()
                    repeat(1_000) { index ->
                        val ordinal = 101L + index
                        val levelId = "auto-v1-${ordinal.toString().padStart(15, '0')}"
                        val unique = index.toString(16).padStart(64, '0')
                        dao.insertGeneratedLevel(
                            GeneratedLevelRecordEntity(
                                levelId = levelId,
                                ordinal = ordinal,
                                candidateKey = "representative|$ordinal",
                                seedHex = ordinal.toString(16).padStart(16, '0'),
                                generationSegmentId = 1,
                                namespaceVersion = 1,
                                generatorVersion = 15,
                                certificationProfileVersion = 3,
                                fingerprintVersion = 2,
                                uniquenessProfileVersion = 2,
                                definitionJson = "{\"id\":\"$levelId\",\"representativePayload\":\"${"d".repeat(512)}\"}",
                                canonicalReplay = "UP,RIGHT,DOWN,LEFT,UP,RIGHT",
                                certificatePayload = "certificate-${"c".repeat(768)}-$ordinal",
                                certificateHash = unique,
                                definitionHash = unique,
                                geometricFingerprint = "g-$unique",
                                dynamicFingerprint = "d-$unique",
                                structuralFingerprint = "s-$unique",
                                grammarFingerprint = "r-$unique",
                                difficulty = "HARD",
                                boardSize = if (index % 25 < 7) 5 else 6,
                                candidateAttempt = index % 65_536,
                                lifecycleState = "READY",
                                historyRoot = unique,
                            ),
                        )
                        dao.insertUniqueness(
                            List(8) { transform ->
                                UniquenessRecordEntity(
                                    levelId = levelId,
                                    transformId = transform,
                                    ordinal = ordinal,
                                    boardSize = if (index % 10 < 3) 5 else 6,
                                    wallBits = ordinal * 17 + transform,
                                    budBits = ordinal * 31 + transform,
                                    startCell = transform,
                                    definitionHash = unique,
                                    geometricEncoding = "$ordinal|$transform|$unique",
                                )
                            },
                        )
                    }
                }
            }
            database.close()
            val populatedBytes = databaseBytes(context)
            val growthBytes = populatedBytes - emptyBytes
            println("MAZEBLOOM_DB_GROWTH_1000_BYTES=$growthBytes")
            assertTrue("Room growth must be positive", growthBytes > 0L)
            assertTrue("Representative 1,000-level history unexpectedly exceeded 32 MiB", growthBytes < 32L * 1024L * 1024L)
            context.deleteDatabase(NAME)
        }
    }

    private fun open(context: Context): MazeBloomDatabase = Room.databaseBuilder(
        context,
        MazeBloomDatabase::class.java,
        NAME,
    ).build()

    private fun databaseBytes(context: Context): Long {
        val base = context.getDatabasePath(NAME)
        return listOf(base, File("${base.path}-wal"), File("${base.path}-shm"))
            .filter(File::exists)
            .sumOf(File::length)
    }

    companion object { private const val NAME = "endless-growth-measurement.db" }
}
