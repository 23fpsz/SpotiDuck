package com.spotifuck.music

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews

class HomeWidget : AppWidgetProvider() {

    companion object {
        private val handler = Handler(Looper.getMainLooper())
        private var isUpdateRunning = false

        private val updateRunnable = object : Runnable {
            override fun run() {
                if (WebService.isServiceRunning && WebService.isPlaying) {
                    val context = AppSingleton.appContext
                    updateAllWidgets(context)
                    // Updated to 2 seconds for progress bar only.
                    // Chronometer handles the text timer.
                    handler.postDelayed(this, 2000)
                } else {
                    isUpdateRunning = false
                }
            }
        }

        @JvmStatic
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, HomeWidget::class.java))
            for (appWidgetId in appWidgetIds) {
                updateWidget(context, appWidgetManager, appWidgetId)
            }

            if (WebService.isServiceRunning && WebService.isPlaying && !isUpdateRunning) {
                isUpdateRunning = true
                handler.post(updateRunnable)
            }
        }

        private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)

            // Increased threshold from 100 to 128 to avoid 1-grid height widgets showing expanded layout on some devices
            val isExpanded = minHeight >= 128
            val layoutId = if (isExpanded) R.layout.widget_expanded else R.layout.home_widget
            val remoteViews = RemoteViews(context.packageName, layoutId)

            // Reset state defaults for safety
            if (isExpanded) {
                remoteViews.setChronometer(R.id.txt_position_chrono, SystemClock.elapsedRealtime(), null, false)
                remoteViews.setViewVisibility(R.id.txt_position, View.VISIBLE)
                remoteViews.setViewVisibility(R.id.txt_position_chrono, View.GONE)
            }

            if (WebService.isServiceRunning) {
                if (isExpanded) {
                    remoteViews.setTextViewText(R.id.txt_title, WebService.trackName)
                    remoteViews.setTextViewText(R.id.txt_artist, WebService.artistName)
                    remoteViews.setImageViewResource(R.id.btn_play, if (WebService.isPlaying) R.drawable.pause else R.drawable.play)
                    remoteViews.setImageViewResource(R.id.btn_repeat, WebService.repeatIconResId)
                    remoteViews.setImageViewResource(R.id.btn_shuffle, WebService.shuffleIconResId)
                    remoteViews.setImageViewResource(R.id.btn_fav, if (WebService.isFavorite) R.drawable.favon else R.drawable.favoff)
                    
                    val duration = WebService.trackDuration
                    var position = WebService.trackPosition
                    
                    if (WebService.isPlaying && WebService.lastPositionUpdateTime > 0) {
                        val timeSinceLastUpdate = SystemClock.elapsedRealtime() - WebService.lastPositionUpdateTime
                        position += timeSinceLastUpdate
                        
                        // Use Chronometer for smooth ticking without app waking up
                        remoteViews.setViewVisibility(R.id.txt_position, View.GONE)
                        remoteViews.setViewVisibility(R.id.txt_position_chrono, View.VISIBLE)
                        // Use null format to avoid string formatting issues in RemoteViews
                        remoteViews.setChronometer(R.id.txt_position_chrono, SystemClock.elapsedRealtime() - position, null, true)
                    } else {
                        // Show static text when paused
                        remoteViews.setViewVisibility(R.id.txt_position, View.VISIBLE)
                        remoteViews.setViewVisibility(R.id.txt_position_chrono, View.GONE)
                        // Make sure to stop the chronometer when not playing
                        remoteViews.setChronometer(R.id.txt_position_chrono, SystemClock.elapsedRealtime(), null, false)
                        remoteViews.setTextViewText(R.id.txt_position, formatTime(position))
                    }
                    
                    if (duration > 0 && position > duration) position = duration
                    remoteViews.setTextViewText(R.id.txt_duration, formatTime(duration))
                    
                    if (duration > 0) {
                        remoteViews.setProgressBar(R.id.progress_bar, 1000, (position * 1000 / duration).toInt(), false)
                    } else {
                        remoteViews.setProgressBar(R.id.progress_bar, 1000, 0, false)
                    }
                } else {
                    remoteViews.setTextViewText(R.id.txt_fulltitle, "${WebService.artistName} - ${WebService.trackName}")
                    remoteViews.setImageViewResource(R.id.btn_wplay, if (WebService.isPlaying) R.drawable.pause else R.drawable.play)
                    remoteViews.setImageViewResource(R.id.btn_wrepeat, WebService.repeatIconResId)
                    remoteViews.setImageViewResource(R.id.btn_wfav, if (WebService.isFavorite) R.drawable.favon else R.drawable.favoff)
                }

                WebService.albumArt?.let {
                    remoteViews.setImageViewBitmap(R.id.img_cover, it)
                } ?: remoteViews.setImageViewResource(R.id.img_cover, R.drawable.logo)
            } else {
                // Reset widget to idle state when service is not running
                if (isExpanded) {
                    remoteViews.setTextViewText(R.id.txt_title, context.getString(R.string.txt_notrack))
                    remoteViews.setTextViewText(R.id.txt_artist, context.getString(R.string.txt_noartist))
                    remoteViews.setImageViewResource(R.id.btn_play, R.drawable.play)
                    remoteViews.setViewVisibility(R.id.txt_position, View.VISIBLE)
                    remoteViews.setViewVisibility(R.id.txt_position_chrono, View.GONE)
                    remoteViews.setChronometer(R.id.txt_position_chrono, SystemClock.elapsedRealtime(), null, false)
                    remoteViews.setTextViewText(R.id.txt_position, "0:00")
                    remoteViews.setTextViewText(R.id.txt_duration, "0:00")
                    remoteViews.setProgressBar(R.id.progress_bar, 1000, 0, false)
                } else {
                    remoteViews.setTextViewText(R.id.txt_fulltitle, context.getString(R.string.txt_notrack))
                    remoteViews.setImageViewResource(R.id.btn_wplay, R.drawable.play)
                }
                remoteViews.setImageViewResource(R.id.img_cover, R.drawable.logo)
            }

            val mainIntent = PendingIntent.getActivity(context, appWidgetId, Intent(context, MainActivity::class.java), getFlags())
            remoteViews.setOnClickPendingIntent(R.id.widget_root, mainIntent)

            if (isExpanded) {
                remoteViews.setOnClickPendingIntent(R.id.btn_play, createPendingIntent(context, "WIDGET_PLAYPAUSE"))
                remoteViews.setOnClickPendingIntent(R.id.btn_prev, createPendingIntent(context, "WIDGET_PREV"))
                remoteViews.setOnClickPendingIntent(R.id.btn_next, createPendingIntent(context, "WIDGET_NEXT"))
                remoteViews.setOnClickPendingIntent(R.id.btn_repeat, createPendingIntent(context, "REPEAT_ACTION"))
                remoteViews.setOnClickPendingIntent(R.id.btn_shuffle, createPendingIntent(context, "SHUFFLE_ACTION"))
                remoteViews.setOnClickPendingIntent(R.id.btn_fav, createPendingIntent(context, "ADDTOFAV_ACTION"))
            } else {
                remoteViews.setOnClickPendingIntent(R.id.btn_wplay, createPendingIntent(context, "WIDGET_PLAYPAUSE"))
                remoteViews.setOnClickPendingIntent(R.id.btn_wprev, createPendingIntent(context, "WIDGET_PREV"))
                remoteViews.setOnClickPendingIntent(R.id.btn_wnext, createPendingIntent(context, "WIDGET_NEXT"))
                remoteViews.setOnClickPendingIntent(R.id.btn_wrepeat, createPendingIntent(context, "REPEAT_ACTION"))
                remoteViews.setOnClickPendingIntent(R.id.btn_wfav, createPendingIntent(context, "ADDTOFAV_ACTION"))
            }

            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        }

        private fun createPendingIntent(context: Context, action: String): PendingIntent {
            val intent = Intent(context, WebService.MediaActionReceiver::class.java).apply { setAction(action) }
            return PendingIntent.getBroadcast(context, action.hashCode(), intent, getFlags())
        }

        private fun getFlags() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        private fun formatTime(millis: Long): String {
            if (millis <= 0) return "0:00"
            val totalSeconds = millis / 1000
            val seconds = totalSeconds % 60
            val minutes = totalSeconds / 60
            return String.format("%d:%02d", minutes, seconds)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        updateWidget(context, appWidgetManager, appWidgetId)
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }
}
