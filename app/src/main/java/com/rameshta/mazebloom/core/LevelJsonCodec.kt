package com.rameshta.mazebloom.core

object LevelJsonCodec {
    fun decode(line: String, verifySourceChecksum: Boolean = true): LevelDefinition {
        val source = line.trim()
        val checksum = string(source, "certificationChecksum")
        if (verifySourceChecksum) {
            require(checksum.length == 64 && checksum.all { it in '0'..'9' || it in 'a'..'f' }) { "invalid source checksum encoding" }
            val checksumSuffix = ",\"certificationChecksum\":\"$checksum\"}"
            require(source.endsWith(checksumSuffix)) { "certification checksum must be the final canonical field" }
            val logicalPayload = source.removeSuffix(checksumSuffix) + "}"
            require(LevelFingerprints.sha256(logicalPayload) == checksum) { "source checksum mismatch" }
        }
        val replay = stringArray(source, "canonicalReplay").map { value ->
            Direction.entries.firstOrNull { it.name == value } ?: error("unknown direction: $value")
        }
        val stones = intArray(source, "stones")
        val buds = intArray(source, "buds")
        require(stones == stones.sorted() && stones.distinct().size == stones.size) { "stones must be sorted and unique" }
        require(buds == buds.sorted() && buds.distinct().size == buds.size) { "buds must be sorted and unique" }
        val difficultyName = string(source, "difficulty")
        val level = LevelDefinition(
            id = string(source, "id"),
            schemaVersion = integer(source, "schemaVersion"),
            contentVersion = integer(source, "contentVersion"),
            rulesVersion = integer(source, "rulesVersion"),
            width = integer(source, "width"),
            height = integer(source, "height"),
            staticWalls = CellMask.of(stones),
            startCell = integer(source, "start"),
            initialBuds = CellMask.of(buds),
            chapter = integer(source, "chapter"),
            campaignOrder = integer(source, "campaignOrder"),
            generatorVersion = integer(source, "generatorVersion"),
            generatorSeed = string(source, "generatorSeed"),
            solverVersion = integer(source, "solverVersion"),
            certificationProfileVersion = integer(source, "certificationProfileVersion"),
            fingerprintVersion = integer(source, "fingerprintVersion"),
            difficulty = DifficultyBand.entries.firstOrNull { it.name == difficultyName }
                ?: error("unknown difficulty: $difficultyName"),
            certifiedOptimalMoves = integer(source, "optimalMoves"),
            canonicalReplay = replay,
            certificationChecksum = checksum,
        )
        require(level.validate() is LevelValidationResult.Valid) { "invalid level ${level.id}: ${level.validate()}" }
        return level
    }

    fun encode(level: LevelDefinition): String {
        val logical = buildString {
            append("{\"id\":\"").append(level.id).append("\"")
            append(",\"schemaVersion\":").append(level.schemaVersion)
            append(",\"contentVersion\":").append(level.contentVersion)
            append(",\"rulesVersion\":").append(level.rulesVersion)
            append(",\"width\":").append(level.width)
            append(",\"height\":").append(level.height)
            append(",\"stones\":").append(level.staticWalls.cells(level.width * level.height).joinToString(",", "[", "]"))
            append(",\"start\":").append(level.startCell)
            append(",\"buds\":").append(level.initialBuds.cells(level.width * level.height).joinToString(",", "[", "]"))
            append(",\"chapter\":").append(level.chapter)
            append(",\"campaignOrder\":").append(level.campaignOrder)
            append(",\"generatorVersion\":").append(level.generatorVersion)
            append(",\"generatorSeed\":\"").append(level.generatorSeed).append("\"")
            append(",\"solverVersion\":").append(level.solverVersion)
            append(",\"certificationProfileVersion\":").append(level.certificationProfileVersion)
            append(",\"fingerprintVersion\":").append(level.fingerprintVersion)
            append(",\"difficulty\":\"").append(level.difficulty.name).append("\"")
            append(",\"optimalMoves\":").append(level.certifiedOptimalMoves)
            append(",\"canonicalReplay\":").append(level.canonicalReplay.joinToString(",", "[", "]") { "\"${it.name}\"" })
            append('}')
        }
        return logical.dropLast(1) + ",\"certificationChecksum\":\"${LevelFingerprints.sha256(logical)}\"}"
    }

    private fun string(source: String, field: String): String =
        Regex("\\\"$field\\\":\\\"([^\\\"]*)\\\"").find(source)?.groupValues?.get(1)
            ?: error("missing string field: $field")

    private fun integer(source: String, field: String): Int =
        Regex("\\\"$field\\\":(-?[0-9]+)").find(source)?.groupValues?.get(1)?.toInt()
            ?: error("missing integer field: $field")

    private fun intArray(source: String, field: String): List<Int> {
        val body = Regex("\\\"$field\\\":\\[([^]]*)]").find(source)?.groupValues?.get(1)
            ?: error("missing array field: $field")
        return if (body.isBlank()) emptyList() else body.split(',').map { it.toInt() }
    }

    private fun stringArray(source: String, field: String): List<String> {
        val body = Regex("\\\"$field\\\":\\[([^]]*)]").find(source)?.groupValues?.get(1)
            ?: error("missing array field: $field")
        return if (body.isBlank()) emptyList() else body.split(',').map { it.removeSurrounding("\"") }
    }
}
