package com.rameshta.mazebloom.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.content.FileProvider
import com.rameshta.mazebloom.R
import com.rameshta.mazebloom.core.GameState
import com.rameshta.mazebloom.core.LevelDefinition
import java.io.File
import java.io.FileOutputStream

object ShareCard {
    fun share(context: Context, level: LevelDefinition, state: GameState, stars: Int) {
        val bitmap = Bitmap.createBitmap(1080, 1080, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawColor(Color.rgb(250, 244, 224))
        paint.color = Color.rgb(45, 88, 65)
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 82f
        paint.isFakeBoldText = true
        canvas.drawText(context.getString(R.string.app_name), 540f, 130f, paint)
        paint.textSize = 42f
        paint.isFakeBoldText = false
        val label = if (level.chapter > 0) context.getString(R.string.share_level, level.chapter, level.campaignOrder) else context.getString(R.string.daily_bloom)
        canvas.drawText(label, 540f, 205f, paint)
        val cell = 650f / level.width
        val left = 215f
        val top = 260f
        for (index in 0 until level.width * level.height) {
            val x = left + (index % level.width) * cell
            val y = top + (index / level.width) * cell
            paint.color = when {
                index in level.staticWalls -> Color.rgb(117, 108, 88)
                index in state.bloom -> Color.rgb(78, 132, 84)
                else -> Color.rgb(235, 224, 190)
            }
            canvas.drawRoundRect(x + 5, y + 5, x + cell - 5, y + cell - 5, 18f, 18f, paint)
        }
        paint.color = Color.rgb(168, 86, 54)
        paint.textSize = 48f
        canvas.drawText(context.resources.getQuantityString(R.plurals.moves_count, state.moveCount, state.moveCount), 540f, 970f, paint)
        paint.color = Color.rgb(220, 156, 48)
        canvas.drawText("★".repeat(stars), 540f, 1035f, paint)
        val directory = File(context.cacheDir, "share").apply { mkdirs() }
        directory.listFiles()?.filter { it.name != "mazebloom.png" }?.forEach { it.delete() }
        val image = File(directory, "mazebloom.png")
        FileOutputStream(image).use { bitmap.compress(Bitmap.CompressFormat.PNG, 95, it) }
        bitmap.recycle()
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", image)
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_message))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, context.getString(R.string.share_title)))
    }
}
