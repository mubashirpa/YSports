package ysports.app.player

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import ysports.app.R

class DescriptionAdapter(val context: Context) : PlayerNotificationManager.MediaDescriptionAdapter {

    override fun getCurrentContentTitle(player: Player): CharSequence {
        val title = player.mediaMetadata.title
        if (title != null && title != "null") return title
        return context.getString(R.string.unknown)
    }

    override fun createCurrentContentIntent(player: Player): PendingIntent? {
        return null
    }

    override fun getCurrentContentText(player: Player): CharSequence? {
        return null
    }

    override fun getCurrentLargeIcon(
        player: Player,
        callback: PlayerNotificationManager.BitmapCallback
    ): Bitmap? {
        return null
    }
}