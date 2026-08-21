package com.rameshta.mazebloom.data

import android.content.Context
import com.rameshta.mazebloom.core.LevelDefinition
import com.rameshta.mazebloom.core.LevelJsonCodec

interface ContentRepository {
    val campaign: List<LevelDefinition>
    val dailyPool: List<LevelDefinition>
    fun level(id: String): LevelDefinition? = (campaign + dailyPool).firstOrNull { it.id == id }
}

class BundledContentRepository(context: Context) : ContentRepository {
    override val campaign: List<LevelDefinition> = context.assets.open("content/campaign.jsonl").bufferedReader().useLines { lines ->
        lines.filter { it.isNotBlank() }.map(LevelJsonCodec::decode).sortedBy { it.campaignOrder }.toList()
    }.also { levels ->
        require(levels.size == 100 && levels.map { it.id }.distinct().size == 100)
        require(levels.map { it.campaignOrder } == (1..100).toList())
    }

    override val dailyPool: List<LevelDefinition> = context.assets.open("content/daily.jsonl").bufferedReader().useLines { lines ->
        lines.filter { it.isNotBlank() }.map(LevelJsonCodec::decode).toList()
    }.also { levels ->
        require(levels.size >= 120 && levels.map { it.id }.distinct().size == levels.size)
    }
}
