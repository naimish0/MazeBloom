package com.rameshta.mazebloom.ui

import android.content.ClipData
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.FileProvider
import androidx.core.view.drawToBitmap
import androidx.lifecycle.lifecycleScope
import com.rameshta.mazebloom.BuildConfig
import com.rameshta.mazebloom.R
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object ShareCard {
    fun shareCelebration(context: Context, sourceView: View) {
        val activity = context.findActivity() ?: return
        val captureView = activity.findViewById<View>(android.R.id.content) ?: sourceView.rootView
        activity.lifecycleScope.launch {
            try {
                // Let the pressed state clear so the shared image matches the settled celebration screen.
                delay(80)
                check(captureView.width > 0 && captureView.height > 0)
                val screenshot = captureView.drawToBitmap(Bitmap.Config.ARGB_8888)
                val uri = withContext(Dispatchers.IO) { writeScreenshot(activity, screenshot) }
                val playStoreUrl = playStoreAppUrl(BuildConfig.APPLICATION_ID)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, activity.getString(R.string.share_message, playStoreUrl))
                    clipData = ClipData.newUri(
                        activity.contentResolver,
                        activity.getString(R.string.share_image_label),
                        uri,
                    )
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                activity.startActivity(
                    Intent.createChooser(shareIntent, activity.getString(R.string.share_title)),
                )
            } catch (_: Exception) {
                Toast.makeText(activity, R.string.share_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun writeScreenshot(context: Context, screenshot: Bitmap) = try {
        val directory = File(context.cacheDir, "share").apply { mkdirs() }
        directory.listFiles()?.filter(File::isFile)?.forEach(File::delete)
        val image = File(directory, "mazebloom-celebration.png")
        FileOutputStream(image).use { output ->
            check(screenshot.compress(Bitmap.CompressFormat.PNG, 100, output))
        }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", image)
    } finally {
        screenshot.recycle()
    }
}

internal fun playStoreAppUrl(applicationId: String): String =
    "https://play.google.com/store/apps/details?id=$applicationId"

private tailrec fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
